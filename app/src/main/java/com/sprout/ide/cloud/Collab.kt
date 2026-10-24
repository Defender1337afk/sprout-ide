package com.sprout.ide.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class Collab(
    val id: String,
    val title: String,
    val ownerId: String,
    val members: List<String>,
    val invited: List<String>,
    val memberNames: Map<String, String>,
    val updatedAt: Timestamp?,
) {
    fun name(uid: String) = memberNames[uid] ?: "?"

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(d: DocumentSnapshot) = Collab(
            d.id, d.getString("title") ?: "", d.getString("ownerId") ?: "",
            (d.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            (d.get("invited") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            (d.get("memberNames") as? Map<String, String>).orEmpty(),
            d.getTimestamp("updatedAt"),
        )
    }
}

class ConflictException(val path: String, val remoteContent: String, val by: String, val remoteVersion: Long) :
    CloudException("Файл $path уже изменил @$by")

/** Local marker inside a shared project folder: which collab it is and the last synced version of every file. */
class CollabMarker(val dir: File, val id: String, val versions: MutableMap<String, Long>) {
    fun save() {
        val v = JSONObject().apply { versions.forEach { (k, n) -> put(k, n) } }
        File(dir, FILE).writeText(JSONObject().put("id", id).put("versions", v).toString())
    }

    companion object {
        const val FILE = ".sprout-collab.json"

        fun read(dir: File): CollabMarker? = runCatching {
            val j = JSONObject(File(dir, FILE).readText())
            val v = j.getJSONObject("versions")
            CollabMarker(dir, j.getString("id"), v.keys().asSequence().associateWith { v.getLong(it) }.toMutableMap())
        }.getOrNull()
    }
}

/** Shared projects: several accounts edit one project; files live in collabs/{id}/files. Access rules are in firestore.rules. */
object Collabs {
    private fun col() = Cloud.db.collection("collabs")
    private fun files(id: String) = col().document(id).collection("files")
    fun fileId(path: String): String =
        MessageDigest.getInstance("SHA-1").digest(path.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)

    suspend fun mine(uid: String): List<Collab> =
        col().whereArrayContains("members", uid).get().await().documents.map { Collab.from(it) }.sortedByDescending { it.updatedAt }

    suspend fun invites(uid: String): List<Collab> =
        col().whereArrayContains("invited", uid).get().await().documents.map { Collab.from(it) }

    suspend fun get(id: String): Collab? = col().document(id).get().await().takeIf { it.exists() }?.let { Collab.from(it) }

    /** Turns a local project into a shared one and uploads its text files. */
    suspend fun create(me: UserProfile, dir: File): Collab {
        if (me.banned) throw CloudException("Аккаунт заблокирован")
        val ref = col().document()
        ref.set(
            mapOf(
                "title" to dir.name.take(60), "ownerId" to me.uid, "members" to listOf(me.uid), "invited" to emptyList<String>(),
                "memberNames" to mapOf(me.uid to me.username),
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        val marker = CollabMarker(dir, ref.id, mutableMapOf())
        val local = withContext(Dispatchers.IO) { Cloud.collectFiles(dir).map { it.relativeTo(dir).invariantSeparatorsPath to it.readText() } }
        local.chunked(200).forEach { chunk ->
            Cloud.db.batch().apply {
                chunk.forEach { (path, content) ->
                    set(files(ref.id).document(fileId(path)), fileDoc(path, content, 1, me))
                    marker.versions[path] = 1
                }
            }.commit().await()
        }
        withContext(Dispatchers.IO) { marker.save() }
        return get(ref.id)!!
    }

    private fun fileDoc(path: String, content: String, version: Long, me: UserProfile) = mapOf(
        "path" to path, "content" to content, "version" to version,
        "updatedBy" to me.uid, "updatedByName" to me.username, "updatedAt" to FieldValue.serverTimestamp(),
    )

    /** Creates a local copy of a shared project the user was invited to. */
    suspend fun download(collab: Collab, into: File) = withContext(Dispatchers.IO) {
        into.mkdirs()
        val marker = CollabMarker(into, collab.id, mutableMapOf())
        val root = into.canonicalPath + File.separator
        files(collab.id).get().await().documents.forEach { d ->
            val path = d.getString("path") ?: return@forEach
            val target = File(into, path)
            if (!target.canonicalPath.startsWith(root)) return@forEach
            target.parentFile?.mkdirs()
            target.writeText(d.getString("content") ?: "")
            marker.versions[path] = d.getLong("version") ?: 1
        }
        marker.save()
    }

    suspend fun invite(collab: Collab, username: String): UserProfile {
        val p = Cloud.profileByUsername(username.trim().removePrefix("@").lowercase())
            ?: throw CloudException("Пользователь @$username не найден")
        if (p.uid in collab.members) throw CloudException("@${p.username} уже участник")
        if (collab.members.size + collab.invited.size >= 20) throw CloudException("Максимум 20 участников")
        col().document(collab.id).update(
            mapOf("invited" to FieldValue.arrayUnion(p.uid), "memberNames.${p.uid}" to p.username),
        ).await()
        return p
    }

    suspend fun accept(collab: Collab, me: UserProfile) {
        col().document(collab.id).update(
            mapOf(
                "members" to FieldValue.arrayUnion(me.uid), "invited" to FieldValue.arrayRemove(me.uid),
                "memberNames.${me.uid}" to me.username,
            ),
        ).await()
    }

    suspend fun decline(collab: Collab, uid: String) {
        col().document(collab.id).update("invited", FieldValue.arrayRemove(uid)).await()
    }

    suspend fun leave(collab: Collab, uid: String) {
        col().document(collab.id).update(mapOf("members" to FieldValue.arrayRemove(uid), "memberNames.$uid" to FieldValue.delete())).await()
    }

    suspend fun remove(collab: Collab, uid: String) {
        col().document(collab.id).update(
            mapOf("members" to FieldValue.arrayRemove(uid), "invited" to FieldValue.arrayRemove(uid), "memberNames.$uid" to FieldValue.delete()),
        ).await()
    }

    suspend fun delete(collab: Collab) {
        files(collab.id).get().await().documents.chunked(400).forEach { chunk ->
            Cloud.db.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
        }
        col().document(collab.id).delete().await()
    }

    // --- live sync ----------------------------------------------------------------------------------

    /**
     * Keeps a local folder in sync with a shared project while it is open.
     * Pushes are optimistic-concurrency transactions on the per-file version number.
     */
    class Session(
        val marker: CollabMarker,
        private val me: UserProfile,
        /** Returns false when the change can't be applied yet (local unsaved edits) — it then stays a pending conflict. */
        private val onRemote: (path: String, content: String?, by: String, version: Long) -> Boolean,
    ) {
        private var listener: ListenerRegistration? = null
        val id get() = marker.id

        fun start() {
            listener = files(marker.id).addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                for (ch in snap.documentChanges) {
                    val d = ch.document
                    val path = d.getString("path") ?: continue
                    val version = d.getLong("version") ?: 0
                    val known = marker.versions[path] ?: 0
                    when (ch.type) {
                        DocumentChange.Type.REMOVED -> if (known > 0 && onRemote(path, null, d.getString("updatedByName") ?: "", 0)) {
                            marker.versions.remove(path)
                        }
                        else -> if (version > known && !d.metadata.hasPendingWrites() &&
                            onRemote(path, d.getString("content") ?: "", d.getString("updatedByName") ?: "", version)
                        ) {
                            marker.versions[path] = version
                        }
                    }
                }
                runCatching { marker.save() }
            }
        }

        fun stop() {
            listener?.remove()
            listener = null
        }

        /** Uploads a local change; throws [ConflictException] when someone else saved a newer version first. */
        suspend fun push(path: String, content: String, force: Boolean = false) {
            val ref = files(marker.id).document(fileId(path))
            val known = marker.versions[path] ?: 0
            val newVersion = Cloud.db.runTransaction { tx ->
                val remote = tx.get(ref)
                val remoteV = remote.getLong("version") ?: 0
                if (!force && remoteV > known) {
                    throw ConflictException(path, remote.getString("content") ?: "", remote.getString("updatedByName") ?: "", remoteV)
                }
                if (remote.exists() && remote.getString("content") == content) return@runTransaction remoteV
                tx.set(ref, fileDoc(path, content, remoteV + 1, me))
                remoteV + 1
            }.await()
            marker.versions[path] = newVersion
            withContext(Dispatchers.IO) { marker.save() }
            col().document(marker.id).update("updatedAt", FieldValue.serverTimestamp())
        }

        suspend fun pushDelete(path: String) {
            files(marker.id).document(fileId(path)).delete().await()
            marker.versions.remove(path)
            withContext(Dispatchers.IO) { marker.save() }
        }

        /** Marks the other person's version as the one we now have locally (caller writes the file). */
        suspend fun acceptRemote(path: String, remoteVersion: Long) {
            marker.versions[path] = remoteVersion
            withContext(Dispatchers.IO) { marker.save() }
        }
    }
}

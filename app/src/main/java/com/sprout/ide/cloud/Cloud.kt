package com.sprout.ide.cloud

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sprout.ide.BuildConfig
import com.sprout.ide.lang.Languages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.Calendar

open class CloudException(message: String) : Exception(message)

object Cloud {
    /** False when the APK was built without app/google-services.json — the app then only offers guest mode. */
    var configured = false
        private set

    fun init(context: Context) {
        if (BuildConfig.FB_APIKEY.isEmpty() || BuildConfig.FB_APPID.isEmpty()) return
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FB_APIKEY)
                    .setApplicationId(BuildConfig.FB_APPID)
                    .setProjectId(BuildConfig.FB_PROJECTID)
                    .setGcmSenderId(BuildConfig.FB_SENDERID)
                    .build(),
            )
        }
        configured = true
    }

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // --- auth --------------------------------------------------------------------------------------

    suspend fun signInWithGoogle(activity: Activity): FirebaseUser {
        if (!configured) throw CloudException("Сервер не настроен: в сборке нет google-services.json")
        if (BuildConfig.FB_WEBCLIENTID.isEmpty()) throw CloudException("Включите вход через Google в Firebase и обновите google-services.json")
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.FB_WEBCLIENTID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            CredentialManager.create(activity).getCredential(activity, request)
        } catch (e: GetCredentialCancellationException) {
            throw CloudException("Вход отменён")
        }
        val cred = result.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw CloudException("Неожиданный тип учётных данных")
        }
        val google = GoogleIdTokenCredential.createFrom(cred.data)
        val firebaseCred = GoogleAuthProvider.getCredential(google.idToken, null)
        return auth.signInWithCredential(firebaseCred).await().user ?: throw CloudException("Не удалось войти")
    }

    fun signOut() = auth.signOut()

    // --- profiles ----------------------------------------------------------------------------------

    private fun users() = db.collection("users")
    private fun usernames() = db.collection("usernames")

    val usernameRegex = Regex("^[a-z0-9_]{3,20}$")

    suspend fun profile(uid: String): UserProfile? = UserProfile.from(users().document(uid).get().await())

    suspend fun profileByUsername(username: String): UserProfile? {
        val uid = usernames().document(username.lowercase()).get().await().getString("uid") ?: return null
        return profile(uid)
    }

    suspend fun isUsernameFree(username: String): Boolean = !usernames().document(username).get().await().exists()

    suspend fun createAccount(user: FirebaseUser, username: String, displayName: String) {
        val name = username.lowercase()
        if (!usernameRegex.matches(name)) throw CloudException("Юзернейм: 3–20 символов, a-z, 0-9 и _")
        if (!isUsernameFree(name)) throw CloudException("Юзернейм @$name уже занят")
        db.batch().apply {
            set(usernames().document(name), mapOf("uid" to user.uid))
            set(
                users().document(user.uid),
                mapOf(
                    "username" to name,
                    "displayName" to displayName.trim().take(40),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "bio" to "", "status" to "", "telegram" to "", "github" to "", "website" to "",
                    "bannerColors" to listOf("#1F6E36", "#8EE09C"), "bannerAngle" to 135,
                    "bannerImage" to false, "avatarImage" to false, "accent" to "#2E8B45", "nameColors" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }.commit().await()
    }

    suspend fun updateProfile(uid: String, fields: Map<String, Any?>) {
        val allowed = fields.filterKeys { it in USER_EDITABLE } + ("updatedAt" to FieldValue.serverTimestamp())
        users().document(uid).update(allowed).await()
    }

    suspend fun changeUsername(uid: String, old: String, new: String) {
        val name = new.lowercase()
        if (name == old) return
        if (!usernameRegex.matches(name)) throw CloudException("Юзернейм: 3–20 символов, a-z, 0-9 и _")
        if (!isUsernameFree(name)) throw CloudException("Юзернейм @$name уже занят")
        db.batch().apply {
            set(usernames().document(name), mapOf("uid" to uid))
            if (old.isNotEmpty()) delete(usernames().document(old))
            update(users().document(uid), mapOf("username" to name, "updatedAt" to FieldValue.serverTimestamp()))
        }.commit().await()
        // keep author name on published projects in sync
        projects().whereEqualTo("ownerId", uid).get().await().documents.forEach {
            it.reference.update("ownerUsername", name).await()
        }
    }

    suspend fun searchUsers(prefix: String): List<UserProfile> {
        val p = prefix.lowercase().trim().removePrefix("@")
        if (p.isEmpty()) return emptyList()
        return users().orderBy("username").startAt(p).endAt(p + "").limit(30).get().await()
            .documents.mapNotNull { UserProfile.from(it) }
    }

    // --- images (stored as small base64 JPEGs in users/{uid}/media/{kind}, no Cloud Storage needed) -----

    private val imageCache = HashMap<String, Bitmap?>()

    suspend fun uploadImage(context: Context, uid: String, kind: String, uri: Uri) = withContext(Dispatchers.IO) {
        val maxSide = if (kind == "banner") 1080 else 320
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw CloudException("Не удалось открыть изображение")
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw CloudException("Неподдерживаемый формат")
        val scale = minOf(1f, maxSide.toFloat() / maxOf(src.width, src.height))
        val bmp = Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
        var quality = 82
        var out: ByteArray
        do {
            out = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
            quality -= 12
        } while (out.size > 260_000 && quality > 20)
        users().document(uid).collection("media").document(kind)
            .set(mapOf("data" to Base64.encodeToString(out, Base64.NO_WRAP))).await()
        imageCache["$uid/$kind"] = bmp
        updateProfile(uid, mapOf(if (kind == "banner") "bannerImage" to true else "avatarImage" to true))
    }

    suspend fun image(uid: String, kind: String): Bitmap? {
        val key = "$uid/$kind"
        imageCache[key]?.let { return it }
        val data = runCatching { users().document(uid).collection("media").document(kind).get().await().getString("data") }.getOrNull()
        val bmp = data?.let { withContext(Dispatchers.Default) { Base64.decode(it, Base64.DEFAULT).let { b -> BitmapFactory.decodeByteArray(b, 0, b.size) } } }
        imageCache[key] = bmp
        return bmp
    }

    fun invalidateImage(uid: String, kind: String) { imageCache.remove("$uid/$kind") }

    suspend fun urlImage(url: String): Bitmap? {
        imageCache[url]?.let { return it }
        val bmp = withContext(Dispatchers.IO) {
            runCatching { URL(url).openStream().use { BitmapFactory.decodeStream(it) } }.getOrNull()
        }
        imageCache[url] = bmp
        return bmp
    }

    // --- badges ------------------------------------------------------------------------------------

    suspend fun badges(): Map<String, Badge> =
        db.collection("badges").get().await().documents.map { Badge.from(it) }.sortedBy { it.order }.associateBy { it.id }

    // --- projects ----------------------------------------------------------------------------------

    private fun projects() = db.collection("projects")

    private const val MAX_FILE = 400_000
    private const val MAX_TOTAL = 4_000_000
    private val skipDirs = setOf(".git", "build", "node_modules", ".gradle", "target", "__pycache__", ".idea")

    /** Text files that will be published; binaries and generated folders are skipped. */
    fun collectFiles(dir: File): List<File> = dir.walkTopDown()
        .onEnter { it == dir || it.name !in skipDirs }
        .filter { it.isFile && it.name != CollabMarker.FILE && it.length() <= MAX_FILE && isText(it) }
        .toList()

    private fun isText(f: File): Boolean = runCatching {
        val head = f.inputStream().use { s -> ByteArray(1024).let { b -> b.copyOf(s.read(b).coerceAtLeast(0)) } }
        head.none { it == 0.toByte() }
    }.getOrDefault(false)

    suspend fun publish(
        profile: UserProfile,
        dir: File,
        existingId: String?,
        title: String,
        description: String,
        license: License,
        tags: List<String>,
    ): String {
        if (profile.banned) throw CloudException("Публикация недоступна: аккаунт заблокирован")
        val year = Calendar.getInstance().get(Calendar.YEAR)
        // open source is mandatory: make sure the published tree carries the license
        val licenseFile = File(dir, "LICENSE")
        if (!licenseFile.exists()) licenseFile.writeText(license.text(profile.displayName.ifBlank { "@" + profile.username }, year))

        val files = withContext(Dispatchers.IO) { collectFiles(dir).map { it.relativeTo(dir).invariantSeparatorsPath to it.readText() } }
        val total = files.sumOf { it.second.length.toLong() }
        if (total > MAX_TOTAL) throw CloudException("Проект слишком большой (${total / 1024} КБ, максимум ${MAX_TOTAL / 1024} КБ)")
        if (files.isEmpty()) throw CloudException("В проекте нет текстовых файлов")

        val lang = files.groupingBy { Languages.forFile(it.first) }.eachCount()
            .filterKeys { it != Languages.Markdown && it != Languages.Plain && it != Languages.Json }
            .maxByOrNull { it.value }?.key ?: Languages.Plain

        val ref = existingId?.let { projects().document(it) } ?: projects().document()
        val meta = mapOf(
            "ownerId" to profile.uid,
            "ownerUsername" to profile.username,
            "title" to title.trim().take(60),
            "description" to description.trim().take(500),
            "license" to license.spdx,
            "language" to lang.id,
            "tags" to tags.map { it.trim().lowercase().take(20) }.filter { it.isNotEmpty() }.distinct().take(6),
            "fileCount" to files.size,
            "size" to total,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (existingId != null) {
            // replace previous file set
            ref.collection("files").get().await().documents.chunked(400).forEach { chunk ->
                db.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
            }
            ref.update(meta).await()
        } else {
            ref.set(meta + ("createdAt" to FieldValue.serverTimestamp())).await()
        }
        // batches stay well under Firestore's 10 MiB request limit
        var batch = db.batch()
        var bytes = 0
        var ops = 0
        for ((i, f) in files.withIndex()) {
            if (bytes + f.second.length > 3_000_000 || ops >= 400) {
                batch.commit().await(); batch = db.batch(); bytes = 0; ops = 0
            }
            batch.set(ref.collection("files").document("f%04d".format(i)), mapOf("path" to f.first, "content" to f.second))
            bytes += f.second.length; ops++
        }
        batch.commit().await()
        return ref.id
    }

    suspend fun recentProjects(featuredOnly: Boolean = false): List<PublicProject> {
        // featured list is small and sorted locally, so no composite index is needed
        if (featuredOnly) {
            return projects().whereEqualTo("featured", true).limit(50).get().await().documents
                .map { PublicProject.from(it) }.sortedByDescending { it.updatedAt }
        }
        return projects().orderBy("updatedAt", Query.Direction.DESCENDING).limit(50).get().await().documents.map { PublicProject.from(it) }
    }

    suspend fun projectsOf(uid: String): List<PublicProject> =
        projects().whereEqualTo("ownerId", uid).get().await().documents.map { PublicProject.from(it) }
            .sortedByDescending { it.updatedAt }

    suspend fun project(id: String): PublicProject? = projects().document(id).get().await().takeIf { it.exists() }?.let { PublicProject.from(it) }

    suspend fun files(projectId: String): List<PublicFile> =
        projects().document(projectId).collection("files").get().await().documents
            .map { PublicFile(it.getString("path") ?: it.id, it.getString("content") ?: "") }
            .sortedBy { it.path.lowercase() }

    suspend fun deleteProject(id: String) {
        val ref = projects().document(id)
        ref.collection("files").get().await().documents.chunked(400).forEach { chunk ->
            db.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
        }
        ref.delete().await()
    }
}

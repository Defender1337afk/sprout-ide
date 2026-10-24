package com.sprout.ide.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class Rating(val average: Double, val count: Long)

data class Comment(
    val id: String,
    val authorId: String,
    val authorUsername: String,
    val text: String,
    val createdAt: Timestamp?,
)

/** Ratings (1–5 stars, one per user) and comments under published projects. Limits are enforced in firestore.rules. */
object Social {
    private fun project(id: String) = Cloud.db.collection("projects").document(id)

    suspend fun rating(projectId: String): Rating {
        val stars = AggregateField.average("stars")
        val snap = project(projectId).collection("ratings")
            .aggregate(AggregateField.count(), stars).get(AggregateSource.SERVER).await()
        return Rating(snap.get(stars) ?: 0.0, snap.count)
    }

    suspend fun myRating(projectId: String, uid: String): Int =
        project(projectId).collection("ratings").document(uid).get().await().getLong("stars")?.toInt() ?: 0

    suspend fun rate(projectId: String, uid: String, stars: Int) {
        val ref = project(projectId).collection("ratings").document(uid)
        if (stars == 0) ref.delete().await()
        else ref.set(mapOf("stars" to stars.coerceIn(1, 5), "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun comments(projectId: String): List<Comment> =
        project(projectId).collection("comments").orderBy("createdAt", Query.Direction.ASCENDING).limit(300)
            .get().await().documents.map {
                Comment(it.id, it.getString("authorId") ?: "", it.getString("authorUsername") ?: "",
                    it.getString("text") ?: "", it.getTimestamp("createdAt"))
            }

    suspend fun addComment(projectId: String, me: UserProfile, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        if (me.banned) throw CloudException("Аккаунт заблокирован")
        project(projectId).collection("comments").add(
            mapOf(
                "authorId" to me.uid,
                "authorUsername" to me.username,
                "text" to t.take(1000),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun deleteComment(projectId: String, commentId: String) {
        project(projectId).collection("comments").document(commentId).delete().await()
    }
}

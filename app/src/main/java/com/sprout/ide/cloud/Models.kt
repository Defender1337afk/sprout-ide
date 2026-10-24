package com.sprout.ide.cloud

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/** Fields a user may change on their own profile. Everything else (badges, verified, role, perks, banned, title)
 *  is admin-only and enforced by firestore.rules. Keep this list in sync with the rules file. */
val USER_EDITABLE = listOf(
    "displayName", "bio", "status", "telegram", "github", "website", "photoUrl",
    "bannerColors", "bannerAngle", "bannerImage", "avatarImage", "accent", "nameColors", "updatedAt",
)

object Perks {
    const val NAME_GRADIENT = "name_gradient"
    const val ANIMATED_BANNER = "animated_banner"
    const val PREMIUM_GRADIENTS = "premium_gradients"
    const val AVATAR_RING = "avatar_ring"
    const val LONG_BIO = "long_bio"

    val all = listOf(
        NAME_GRADIENT to "Градиентное имя",
        ANIMATED_BANNER to "Анимированный баннер",
        PREMIUM_GRADIENTS to "Премиум-градиенты",
        AVATAR_RING to "Светящееся кольцо аватара",
        LONG_BIO to "Длинное описание (1000 символов)",
    )
}

data class UserProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val bio: String = "",
    val status: String = "",
    val telegram: String = "",
    val github: String = "",
    val website: String = "",
    val photoUrl: String = "",
    val avatarImage: Boolean = false,
    val bannerColors: List<String> = listOf("#1F6E36", "#8EE09C"),
    val bannerAngle: Int = 135,
    val bannerImage: Boolean = false,
    val accent: String = "#2E8B45",
    val nameColors: List<String> = emptyList(),
    // admin-only
    val badges: List<String> = emptyList(),
    val verified: Boolean = false,
    val role: String = "user",
    val perks: List<String> = emptyList(),
    val banned: Boolean = false,
    val title: String = "",
    val createdAt: Timestamp? = null,
) {
    fun has(perk: String) = perk in perks || role == "admin"
    val bioLimit get() = if (has(Perks.LONG_BIO)) 1000 else 300

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(d: DocumentSnapshot): UserProfile? {
            if (!d.exists()) return null
            fun s(k: String) = d.getString(k) ?: ""
            fun l(k: String) = (d.get(k) as? List<*>)?.filterIsInstance<String>().orEmpty()
            return UserProfile(
                uid = d.id, username = s("username"), displayName = s("displayName"), bio = s("bio"), status = s("status"),
                telegram = s("telegram"), github = s("github"), website = s("website"), photoUrl = s("photoUrl"),
                avatarImage = d.getBoolean("avatarImage") == true,
                bannerColors = l("bannerColors").ifEmpty { listOf("#1F6E36", "#8EE09C") },
                bannerAngle = d.getLong("bannerAngle")?.toInt() ?: 135,
                bannerImage = d.getBoolean("bannerImage") == true,
                accent = d.getString("accent") ?: "#2E8B45",
                nameColors = l("nameColors"),
                badges = l("badges"), verified = d.getBoolean("verified") == true, role = d.getString("role") ?: "user",
                perks = l("perks"), banned = d.getBoolean("banned") == true, title = s("title"),
                createdAt = d.getTimestamp("createdAt"),
            )
        }
    }
}

/** A badge is drawn as a gradient MaterialShape with an icon, or shown as an admin-uploaded picture ([image], base64 PNG). */
data class Badge(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val colors: List<String>,
    val order: Int,
    val shape: String = "cookie9",
    val icon: String = "",
    val image: String = "",
) {
    companion object {
        fun from(d: DocumentSnapshot) = Badge(
            d.id, d.getString("name") ?: d.id, d.getString("emoji") ?: "⭐", d.getString("description") ?: "",
            (d.get("colors") as? List<*>)?.filterIsInstance<String>().orEmpty().ifEmpty { listOf("#2E8B45", "#8EE09C") },
            d.getLong("order")?.toInt() ?: 0,
            d.getString("shape") ?: "cookie9", d.getString("icon") ?: "", d.getString("image") ?: "",
        )
    }
}

data class PublicProject(
    val id: String,
    val ownerId: String,
    val ownerUsername: String,
    val title: String,
    val description: String,
    val license: String,
    val language: String,
    val tags: List<String>,
    val fileCount: Int,
    val size: Long,
    val featured: Boolean,
    val updatedAt: Timestamp?,
) {
    companion object {
        fun from(d: DocumentSnapshot) = PublicProject(
            d.id, d.getString("ownerId") ?: "", d.getString("ownerUsername") ?: "", d.getString("title") ?: "",
            d.getString("description") ?: "", d.getString("license") ?: "", d.getString("language") ?: "text",
            (d.get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            d.getLong("fileCount")?.toInt() ?: 0, d.getLong("size") ?: 0, d.getBoolean("featured") == true,
            d.getTimestamp("updatedAt"),
        )
    }
}

data class PublicFile(val path: String, val content: String)

/** OSI-approved licenses offered when publishing. Publishing without one is impossible. */
enum class License(val spdx: String, val title: String) {
    MIT("MIT", "MIT"),
    Apache("Apache-2.0", "Apache 2.0"),
    GPL3("GPL-3.0-or-later", "GNU GPL v3"),
    LGPL3("LGPL-3.0-or-later", "GNU LGPL v3"),
    AGPL3("AGPL-3.0-or-later", "GNU AGPL v3"),
    MPL2("MPL-2.0", "Mozilla 2.0"),
    BSD3("BSD-3-Clause", "BSD 3-Clause"),
    BSD2("BSD-2-Clause", "BSD 2-Clause"),
    Unlicense("Unlicense", "Unlicense"),
    ;

    fun text(author: String, year: Int): String = if (this == MIT) """MIT License

Copyright (c) $year $author

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
""" else """Copyright (c) $year $author

SPDX-License-Identifier: $spdx

This project is licensed under the $title license.
Full license text: https://spdx.org/licenses/$spdx.html
"""

    companion object {
        fun bySpdx(s: String) = entries.firstOrNull { it.spdx == s }
    }
}

fun parseHex(hex: String, fallback: Color = Color(0xFF2E8B45)): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(fallback)

fun Color.toHex(): String = String.format("#%06X", (android.graphics.Color.argb(alpha, red, green, blue)) and 0xFFFFFF)

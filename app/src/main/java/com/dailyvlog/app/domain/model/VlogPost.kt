package com.dailyvlog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VlogPost(
    val id: String,
    val groupId: String,
    val authorUserId: String,
    val authorPseudo: String,
    val authorAvatarUrl: String? = null,
    val date: String, // "YYYY-MM-DD"
    val videoUri: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int,
    val caption: String = "",
    val viewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

package com.dailyvlog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VlogComment(
    val id: String,
    val vlogId: String,
    val authorUserId: String,
    val authorPseudo: String,
    val authorAvatarUrl: String? = null,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

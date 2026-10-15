package com.dailyvlog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VlogReaction(
    val id: String,
    val vlogId: String,
    val userId: String,
    val userPseudo: String,
    val emoji: String,
    val createdAt: Long = System.currentTimeMillis()
)

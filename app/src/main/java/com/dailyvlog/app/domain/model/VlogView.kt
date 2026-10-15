package com.dailyvlog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VlogView(
    val vlogId: String,
    val userId: String,
    val userPseudo: String,
    val viewedAt: Long = System.currentTimeMillis()
)

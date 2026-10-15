package com.dailyvlog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val pseudo: String,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

package com.dailyvlog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MemberRole {
    ADMIN,
    MEMBER
}

@Serializable
data class Group(
    val id: String,
    val name: String,
    val emoji: String = "🎬",
    val colorHex: String = "#FF5252",
    val adminId: String,
    val inviteCode: String,
    val switchTime: String = "09:00",
    val maxDurationSeconds: Int = 60,
    val timezone: String = "Europe/Paris",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class GroupMember(
    val id: String,
    val groupId: String,
    val userId: String,
    val pseudo: String,
    val avatarUrl: String? = null,
    val role: MemberRole = MemberRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis()
)

@Serializable
data class GroupWithMembers(
    val group: Group,
    val members: List<GroupMember>
)

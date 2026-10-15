package com.dailyvlog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AssignmentStatus {
    PENDING,   // Waiting for the designated member to post today's vlog
    POSTED,    // Today's vlog has been posted
    MISSED,    // Deadline passed without a vlog
    SKIPPED    // Member passed their turn
}

@Serializable
data class DailyAssignment(
    val id: String, // "$groupId-$date"
    val groupId: String,
    val date: String, // "YYYY-MM-DD"
    val roundNumber: Int,
    val assignedUserId: String,
    val assignedUserPseudo: String,
    val assignedUserAvatarUrl: String? = null,
    val status: AssignmentStatus = AssignmentStatus.PENDING,
    val vlogId: String? = null,
    val skippedUserIds: List<String> = emptyList(),
    val deadlineTimestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)

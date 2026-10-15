package com.dailyvlog.app.domain.repository

import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getUserGroups(userId: String): Flow<List<Group>>

    fun getGroupDetails(groupId: String): Flow<GroupWithMembers?>

    suspend fun createGroup(
        name: String,
        emoji: String,
        colorHex: String,
        switchTime: String,
        maxDurationSeconds: Int,
        adminUser: User
    ): Result<Group>

    suspend fun joinGroupByCode(code: String, user: User): Result<Group>

    suspend fun regenerateInviteCode(groupId: String, adminId: String): Result<String>

    suspend fun removeMember(groupId: String, memberUserId: String, adminId: String): Result<Unit>

    suspend fun leaveGroup(groupId: String, userId: String): Result<Unit>

    suspend fun deleteGroup(groupId: String, adminId: String): Result<Unit>
}

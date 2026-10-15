package com.dailyvlog.app.data.fake

import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupMember
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.model.MemberRole
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeGroupRepository @Inject constructor() : GroupRepository {

    private val mutex = Mutex()
    private val groupsFlow = MutableStateFlow<List<Group>>(emptyList())
    private val membersFlow = MutableStateFlow<Map<String, List<GroupMember>>>(emptyMap())

    init {
        // Pre-seed a default group "Les Potes" created by Alice per prompt §5.4 / §7
        val defaultGroupId = "group-les-potes-01"
        val defaultGroup = Group(
            id = defaultGroupId,
            name = "Les Potes",
            emoji = "🍿",
            colorHex = "#FF5252",
            adminId = "user-alice-01",
            inviteCode = "K7QX2M",
            switchTime = "09:00",
            maxDurationSeconds = 60
        )
        val aliceMember = GroupMember(
            id = "member-01",
            groupId = defaultGroupId,
            userId = "user-alice-01",
            pseudo = "Alice",
            role = MemberRole.ADMIN
        )

        groupsFlow.value = listOf(defaultGroup)
        membersFlow.value = mapOf(defaultGroupId to listOf(aliceMember))
    }

    override fun getUserGroups(userId: String): Flow<List<Group>> {
        return membersFlow.map { map ->
            val userGroupIds = map.filter { entry ->
                entry.value.any { it.userId == userId }
            }.keys
            groupsFlow.value.filter { it.id in userGroupIds }
        }
    }

    override fun getGroupDetails(groupId: String): Flow<GroupWithMembers?> {
        return membersFlow.map { map ->
            val group = groupsFlow.value.find { it.id == groupId } ?: return@map null
            val members = map[groupId] ?: emptyList()
            GroupWithMembers(group, members)
        }
    }

    override suspend fun createGroup(
        name: String,
        emoji: String,
        colorHex: String,
        switchTime: String,
        maxDurationSeconds: Int,
        adminUser: User
    ): Result<Group> = mutex.withLock {
        val trimmedName = name.trim()
        if (trimmedName.length < 2) {
            return Result.failure(IllegalArgumentException("Le nom du groupe doit comporter au moins 2 caractères"))
        }

        val groupId = UUID.randomUUID().toString()
        val code = generateCode()

        val group = Group(
            id = groupId,
            name = trimmedName,
            emoji = emoji.ifBlank { "🎬" },
            colorHex = colorHex.ifBlank { "#FF5252" },
            adminId = adminUser.id,
            inviteCode = code,
            switchTime = switchTime.ifBlank { "09:00" },
            maxDurationSeconds = if (maxDurationSeconds in 10..180) maxDurationSeconds else 60
        )

        val adminMember = GroupMember(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            userId = adminUser.id,
            pseudo = adminUser.pseudo,
            avatarUrl = adminUser.avatarUrl,
            role = MemberRole.ADMIN
        )

        groupsFlow.value = groupsFlow.value + group
        val currentMembers = membersFlow.value.toMutableMap()
        currentMembers[groupId] = listOf(adminMember)
        membersFlow.value = currentMembers

        return Result.success(group)
    }

    override suspend fun joinGroupByCode(code: String, user: User): Result<Group> = mutex.withLock {
        val cleanCode = code.trim().uppercase()
        val group = groupsFlow.value.find { it.inviteCode.equals(cleanCode, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Code d'invitation invalide ou expiré"))

        val currentMembersMap = membersFlow.value.toMutableMap()
        val groupMembers = currentMembersMap[group.id]?.toMutableList() ?: mutableListOf()

        if (groupMembers.any { it.userId == user.id }) {
            return Result.failure(IllegalArgumentException("Vous faites déjà partie de ce groupe"))
        }

        if (groupMembers.size >= 20) {
            return Result.failure(IllegalArgumentException("Ce groupe a atteint la taille maximale de 20 membres"))
        }

        val newMember = GroupMember(
            id = UUID.randomUUID().toString(),
            groupId = group.id,
            userId = user.id,
            pseudo = user.pseudo,
            avatarUrl = user.avatarUrl,
            role = MemberRole.MEMBER
        )

        groupMembers.add(newMember)
        currentMembersMap[group.id] = groupMembers
        membersFlow.value = currentMembersMap

        return Result.success(group)
    }

    override suspend fun regenerateInviteCode(groupId: String, adminId: String): Result<String> = mutex.withLock {
        val group = groupsFlow.value.find { it.id == groupId }
            ?: return Result.failure(IllegalArgumentException("Groupe introuvable"))

        if (group.adminId != adminId) {
            return Result.failure(IllegalAccessException("Seul l'administrateur peut régénérer le code"))
        }

        val newCode = generateCode()
        val updatedGroup = group.copy(inviteCode = newCode)
        groupsFlow.value = groupsFlow.value.map { if (it.id == groupId) updatedGroup else it }

        return Result.success(newCode)
    }

    override suspend fun removeMember(groupId: String, memberUserId: String, adminId: String): Result<Unit> = mutex.withLock {
        val group = groupsFlow.value.find { it.id == groupId }
            ?: return Result.failure(IllegalArgumentException("Groupe introuvable"))

        if (group.adminId != adminId) {
            return Result.failure(IllegalAccessException("Seul l'administrateur peut retirer un membre"))
        }
        if (memberUserId == adminId) {
            return Result.failure(IllegalArgumentException("L'administrateur ne peut pas se retirer lui-même"))
        }

        val currentMembersMap = membersFlow.value.toMutableMap()
        val members = currentMembersMap[groupId]?.filter { it.userId != memberUserId } ?: emptyList()
        currentMembersMap[groupId] = members
        membersFlow.value = currentMembersMap

        return Result.success(Unit)
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> = mutex.withLock {
        val group = groupsFlow.value.find { it.id == groupId }
            ?: return Result.failure(IllegalArgumentException("Groupe introuvable"))

        if (group.adminId == userId) {
            return Result.failure(IllegalArgumentException("L'administrateur ne peut pas quitter le groupe. Supprimez-le ou transférez vos droits."))
        }

        val currentMembersMap = membersFlow.value.toMutableMap()
        val members = currentMembersMap[groupId]?.filter { it.userId != userId } ?: emptyList()
        currentMembersMap[groupId] = members
        membersFlow.value = currentMembersMap

        return Result.success(Unit)
    }

    override suspend fun deleteGroup(groupId: String, adminId: String): Result<Unit> = mutex.withLock {
        val group = groupsFlow.value.find { it.id == groupId }
            ?: return Result.failure(IllegalArgumentException("Groupe introuvable"))

        if (group.adminId != adminId) {
            return Result.failure(IllegalAccessException("Seul l'administrateur peut supprimer le groupe"))
        }

        groupsFlow.value = groupsFlow.value.filter { it.id != groupId }
        val currentMembersMap = membersFlow.value.toMutableMap()
        currentMembersMap.remove(groupId)
        membersFlow.value = currentMembersMap

        return Result.success(Unit)
    }

    private fun generateCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // unambiguous characters
        val random = SecureRandom()
        return (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}

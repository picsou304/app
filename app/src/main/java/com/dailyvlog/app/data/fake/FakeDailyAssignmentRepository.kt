package com.dailyvlog.app.data.fake

import com.dailyvlog.app.core.time.AppClock
import com.dailyvlog.app.core.time.DebugClock
import com.dailyvlog.app.domain.engine.DailyPickerEngine
import com.dailyvlog.app.domain.model.AssignmentStatus
import com.dailyvlog.app.domain.model.DailyAssignment
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDailyAssignmentRepository @Inject constructor(
    private val groupRepository: GroupRepository,
    private val debugClock: DebugClock
) : DailyAssignmentRepository {

    private val engine = DailyPickerEngine()

    // Map of key ("$groupId-$date") to list of skipped user IDs
    private val skipsFlow = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    // Map of key ("$groupId-$date") to vlog ID
    private val postedVlogsFlow = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun getTodayAssignment(groupId: String): Flow<DailyAssignment?> {
        return combine(
            groupRepository.getGroupDetails(groupId),
            debugClock.offsetFlow,
            skipsFlow,
            postedVlogsFlow
        ) { groupDetails, _, skipsMap, postedMap ->
            if (groupDetails == null || groupDetails.members.isEmpty()) {
                return@combine null
            }

            val group = groupDetails.group
            val members = groupDetails.members
            val now = debugClock.currentInstant()

            val activeDate = DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
            val dateKey = "${group.id}-${activeDate.format(DailyPickerEngine.DATE_FORMATTER)}"

            val skipsForToday = skipsMap[dateKey] ?: emptyList()
            val postedVlogId = postedMap[dateKey]

            val assignment = engine.computeAssignment(
                group = group,
                members = members,
                now = now,
                currentDaySkippedUserIds = skipsForToday
            )

            if (assignment != null && postedVlogId != null) {
                assignment.copy(
                    status = AssignmentStatus.POSTED,
                    vlogId = postedVlogId
                )
            } else if (assignment != null && now.toEpochMilli() > assignment.deadlineTimestamp) {
                assignment.copy(
                    status = AssignmentStatus.MISSED
                )
            } else {
                assignment
            }
        }
    }

    override suspend fun skipTurn(groupId: String, userId: String): Result<DailyAssignment> {
        val groupDetails = groupRepository.getGroupDetails(groupId).firstOrNull()
            ?: return Result.failure(Exception("Groupe introuvable"))

        val now = debugClock.currentInstant()
        val group = groupDetails.group
        val activeDate = DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
        val dateKey = "${group.id}-${activeDate.format(DailyPickerEngine.DATE_FORMATTER)}"

        val currentSkips = skipsFlow.value[dateKey] ?: emptyList()
        if (userId in currentSkips) {
            return Result.failure(Exception("Vous avez déjà passé votre tour pour cette manche"))
        }

        val updatedSkips = currentSkips + userId
        val updatedMap = skipsFlow.value.toMutableMap()
        updatedMap[dateKey] = updatedSkips
        skipsFlow.value = updatedMap

        val newAssignment = engine.computeAssignment(
            group = group,
            members = groupDetails.members,
            now = now,
            currentDaySkippedUserIds = updatedSkips
        ) ?: return Result.failure(Exception("Impossible d'assigner un nouveau membre"))

        return Result.success(newAssignment)
    }

    override suspend fun markVlogPosted(groupId: String, vlogId: String): Result<DailyAssignment> {
        val groupDetails = groupRepository.getGroupDetails(groupId).firstOrNull()
            ?: return Result.failure(Exception("Groupe introuvable"))

        val now = debugClock.currentInstant()
        val group = groupDetails.group
        val activeDate = DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
        val dateKey = "${group.id}-${activeDate.format(DailyPickerEngine.DATE_FORMATTER)}"

        val updatedPosted = postedVlogsFlow.value.toMutableMap()
        updatedPosted[dateKey] = vlogId
        postedVlogsFlow.value = updatedPosted

        val currentSkips = skipsFlow.value[dateKey] ?: emptyList()
        val assignment = engine.computeAssignment(
            group = group,
            members = groupDetails.members,
            now = now,
            currentDaySkippedUserIds = currentSkips
        )?.copy(
            status = AssignmentStatus.POSTED,
            vlogId = vlogId
        ) ?: return Result.failure(Exception("Impossible de trouver l'assignation"))

        return Result.success(assignment)
    }
}

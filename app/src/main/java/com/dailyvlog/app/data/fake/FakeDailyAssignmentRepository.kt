package com.dailyvlog.app.data.fake

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
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
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

    // Map of key ("$groupId-$date") to manual missed flag
    private val missedDatesFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override fun getTodayAssignment(groupId: String): Flow<DailyAssignment?> {
        return combine(
            groupRepository.getGroupDetails(groupId),
            debugClock.offsetFlow,
            skipsFlow,
            postedVlogsFlow,
            missedDatesFlow
        ) { groupDetails, _, skipsMap, postedMap, missedMap ->
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
            val isExplicitlyMissed = missedMap[dateKey] == true

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
            } else if (assignment != null && (isExplicitlyMissed || now.toEpochMilli() > assignment.deadlineTimestamp)) {
                assignment.copy(
                    status = AssignmentStatus.MISSED
                )
            } else {
                assignment
            }
        }
    }

    override fun getYesterdayAssignment(groupId: String): Flow<DailyAssignment?> {
        return combine(
            groupRepository.getGroupDetails(groupId),
            debugClock.offsetFlow,
            skipsFlow,
            postedVlogsFlow,
            missedDatesFlow
        ) { groupDetails, _, skipsMap, postedMap, _ ->
            if (groupDetails == null || groupDetails.members.isEmpty()) {
                return@combine null
            }

            val group = groupDetails.group
            val members = groupDetails.members
            val now = debugClock.currentInstant()

            val activeDate = DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
            val yesterdayDate = activeDate.minusDays(1)
            val dateKey = "${group.id}-${yesterdayDate.format(DailyPickerEngine.DATE_FORMATTER)}"

            val groupCreationActiveDate = DailyPickerEngine.getActiveDate(
                Instant.ofEpochMilli(group.createdAt),
                group.switchTime,
                group.timezone
            )

            // If yesterday was before group creation, no assignment existed
            if (yesterdayDate.isBefore(groupCreationActiveDate)) {
                return@combine null
            }

            val skipsForYesterday = skipsMap[dateKey] ?: emptyList()
            val postedVlogId = postedMap[dateKey]

            // Calculate assignment for yesterday using an instant during yesterday
            val yesterdayInstant = now.minus(Duration.ofDays(1))
            val assignment = engine.computeAssignment(
                group = group,
                members = members,
                now = yesterdayInstant,
                currentDaySkippedUserIds = skipsForYesterday
            )

            if (assignment != null && postedVlogId != null) {
                assignment.copy(
                    status = AssignmentStatus.POSTED,
                    vlogId = postedVlogId
                )
            } else if (assignment != null) {
                // Since yesterday has already ended, if no vlog was posted, it was MISSED!
                assignment.copy(
                    status = AssignmentStatus.MISSED
                )
            } else {
                null
            }
        }
    }

    override suspend fun markDayMissed(groupId: String): Result<DailyAssignment> {
        val groupDetails = groupRepository.getGroupDetails(groupId).firstOrNull()
            ?: return Result.failure(Exception("Groupe introuvable"))

        val now = debugClock.currentInstant()
        val group = groupDetails.group
        val activeDate = DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
        val dateKey = "${group.id}-${activeDate.format(DailyPickerEngine.DATE_FORMATTER)}"

        val currentMissed = missedDatesFlow.value
        val isCurrentlyMissed = currentMissed[dateKey] == true
        missedDatesFlow.value = currentMissed + (dateKey to !isCurrentlyMissed)

        val assignment = getTodayAssignment(groupId).firstOrNull()
            ?: return Result.failure(Exception("Aucun tirage disponible"))

        return Result.success(assignment)
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
        skipsFlow.value = skipsFlow.value + (dateKey to updatedSkips)

        val nextAssignment = engine.computeAssignment(
            group = group,
            members = groupDetails.members,
            now = now,
            currentDaySkippedUserIds = updatedSkips
        ) ?: return Result.failure(Exception("Aucun autre membre disponible"))

        return Result.success(nextAssignment)
    }

    override suspend fun markVlogPosted(groupId: String, vlogId: String): Result<DailyAssignment> {
        val groupDetails = groupRepository.getGroupDetails(groupId).firstOrNull()
            ?: return Result.failure(Exception("Groupe introuvable"))

        val now = debugClock.currentInstant()
        val group = groupDetails.group
        val activeDate = DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
        val dateKey = "${group.id}-${activeDate.format(DailyPickerEngine.DATE_FORMATTER)}"

        postedVlogsFlow.value = postedVlogsFlow.value + (dateKey to vlogId)

        val assignment = getTodayAssignment(groupId).firstOrNull()
            ?: return Result.failure(Exception("Aucun tirage disponible"))

        return Result.success(assignment)
    }
}

package com.dailyvlog.app.domain.engine

import com.dailyvlog.app.domain.model.AssignmentStatus
import com.dailyvlog.app.domain.model.DailyAssignment
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupMember
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Random

class DailyPickerEngine {

    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        /**
         * Calculates the active vlog date based on current instant, switch time, and timezone.
         * For example, with switchTime = "09:00":
         * - 2026-10-15 08:30 -> Active date is 2026-10-14 (deadline 2026-10-15 09:00)
         * - 2026-10-15 09:01 -> Active date is 2026-10-15 (deadline 2026-10-16 09:00)
         */
        fun getActiveDate(
            now: Instant,
            switchTimeStr: String,
            timezoneStr: String
        ): LocalDate {
            val zoneId = parseZoneId(timezoneStr)
            val zonedDateTime = ZonedDateTime.ofInstant(now, zoneId)
            val switchTime = parseLocalTime(switchTimeStr)

            return if (zonedDateTime.toLocalTime().isBefore(switchTime)) {
                zonedDateTime.toLocalDate().minusDays(1)
            } else {
                zonedDateTime.toLocalDate()
            }
        }

        /**
         * Calculates the deadline timestamp (epoch millis) for a given active vlog date.
         */
        fun getDeadlineTimestamp(
            activeDate: LocalDate,
            switchTimeStr: String,
            timezoneStr: String
        ): Long {
            val zoneId = parseZoneId(timezoneStr)
            val switchTime = parseLocalTime(switchTimeStr)
            val deadlineDateTime = activeDate.plusDays(1).atTime(switchTime).atZone(zoneId)
            return deadlineDateTime.toInstant().toEpochMilli()
        }

        fun parseLocalTime(timeStr: String): LocalTime {
            return try {
                LocalTime.parse(timeStr)
            } catch (_: Exception) {
                LocalTime.of(9, 0)
            }
        }

        fun parseZoneId(zoneStr: String): ZoneId {
            return try {
                ZoneId.of(zoneStr)
            } catch (_: Exception) {
                ZoneId.of("Europe/Paris")
            }
        }

        /**
         * Deterministically computes the seed for a group's round.
         */
        fun computeRoundSeed(groupId: String, roundNumber: Int): Long {
            var hash = 1125899906842597L // FNV prime
            val key = "$groupId#round#$roundNumber"
            for (c in key) {
                hash = 31L * hash + c.code.toLong()
            }
            return hash
        }
    }

    /**
     * Computes the daily assignment for a target date and group state.
     * Supports:
     * - Fair rotation: every active member is assigned exactly once per round.
     * - Members joining mid-round: added to the current round pool.
     * - Members leaving: excluded from future assignments.
     * - Passing turn (skip): immediate reassignment to next eligible member.
     */
    fun computeAssignment(
        group: Group,
        members: List<GroupMember>,
        now: Instant,
        pastAssignments: List<DailyAssignment> = emptyList(),
        currentDaySkippedUserIds: List<String> = emptyList()
    ): DailyAssignment? {
        if (members.isEmpty()) return null

        val activeDate = getActiveDate(now, group.switchTime, group.timezone)
        val activeDateStr = activeDate.format(DATE_FORMATTER)
        val deadline = getDeadlineTimestamp(activeDate, group.switchTime, group.timezone)

        // If today's assignment is already stored and has a vlog posted, preserve it
        val existingToday = pastAssignments.find { it.date == activeDateStr }
        if (existingToday != null && existingToday.status == AssignmentStatus.POSTED) {
            return existingToday
        }

        // Calculate member timeline and assignment history from group creation date up to activeDate
        val groupCreationDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(group.createdAt),
            parseZoneId(group.timezone)
        ).toLocalDate()

        val historyByDate = pastAssignments.associateBy { it.date }
        val allSkipped = currentDaySkippedUserIds.toMutableList()
        if (existingToday != null) {
            allSkipped.addAll(existingToday.skippedUserIds.filter { it !in allSkipped })
        }

        // Replay/traverse from groupCreationDate to activeDate
        var currentDate = groupCreationDate
        var currentRoundNumber = 1
        val pickedInCurrentRound = mutableSetOf<String>()

        while (!currentDate.isAfter(activeDate)) {
            val dateStr = currentDate.format(DATE_FORMATTER)
            val isTargetDate = (currentDate == activeDate)

            // Eligible members on this date (joined on or before this date)
            val eligibleMembers = members.filter { member ->
                val joinDate = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(member.joinedAt),
                    parseZoneId(group.timezone)
                ).toLocalDate()
                !joinDate.isAfter(currentDate)
            }

            if (eligibleMembers.isNotEmpty()) {
                val eligibleUserIds = eligibleMembers.map { it.userId }.toSet()

                // Check if all eligible members have been picked in this round
                val unpickedInRound = eligibleMembers.filter { it.userId !in pickedInCurrentRound }
                if (unpickedInRound.isEmpty()) {
                    // Start next round
                    currentRoundNumber++
                    pickedInCurrentRound.clear()
                }

                // Shuffled order of all eligible members for this round
                val roundSeed = computeRoundSeed(group.id, currentRoundNumber)
                val shuffledOrder = eligibleMembers
                    .sortedBy { it.userId } // Canonical sort before shuffling
                    .toMutableList()

                // Deterministic shuffle
                val random = Random(roundSeed)
                for (i in shuffledOrder.size - 1 downTo 1) {
                    val j = random.nextInt(i + 1)
                    val temp = shuffledOrder[i]
                    shuffledOrder[i] = shuffledOrder[j]
                    shuffledOrder[j] = temp
                }

                val skipsForDate = if (isTargetDate) allSkipped else historyByDate[dateStr]?.skippedUserIds ?: emptyList()

                // Select the first eligible member that has not yet been picked in this round
                // and has not skipped on this day
                var candidate = shuffledOrder.firstOrNull { member ->
                    member.userId !in pickedInCurrentRound && member.userId !in skipsForDate
                }

                // If everyone unpicked in this round skipped today, pick from unpicked regardless of skips
                if (candidate == null) {
                    candidate = shuffledOrder.firstOrNull { member ->
                        member.userId !in pickedInCurrentRound
                    }
                }

                // Fallback to first available member if list exhausted
                val chosenMember = candidate ?: shuffledOrder.first()

                pickedInCurrentRound.add(chosenMember.userId)

                if (isTargetDate) {
                    return DailyAssignment(
                        id = "${group.id}-$dateStr",
                        groupId = group.id,
                        date = dateStr,
                        roundNumber = currentRoundNumber,
                        assignedUserId = chosenMember.userId,
                        assignedUserPseudo = chosenMember.pseudo,
                        assignedUserAvatarUrl = chosenMember.avatarUrl,
                        status = AssignmentStatus.PENDING,
                        skippedUserIds = skipsForDate,
                        deadlineTimestamp = deadline,
                        createdAt = now.toEpochMilli()
                    )
                }
            }

            currentDate = currentDate.plusDays(1)
        }

        return null
    }
}

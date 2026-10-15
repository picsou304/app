package com.dailyvlog.app.domain.engine

import com.dailyvlog.app.domain.model.DailyAssignment
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupMember
import com.dailyvlog.app.domain.model.MemberRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyPickerEngineTest {

    private lateinit var engine: DailyPickerEngine
    private val zoneId = ZoneId.of("Europe/Paris")

    private fun createMember(id: String, pseudo: String, joinedDate: LocalDate): GroupMember {
        val joinedMillis = joinedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return GroupMember(
            id = "gm-$id",
            groupId = "group-test",
            userId = id,
            pseudo = pseudo,
            role = MemberRole.MEMBER,
            joinedAt = joinedMillis
        )
    }

    private fun createGroup(createdDate: LocalDate, switchTime: String = "09:00"): Group {
        val createdMillis = createdDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return Group(
            id = "group-test",
            name = "Test Group",
            adminId = "user-1",
            inviteCode = "ABC123",
            switchTime = switchTime,
            timezone = "Europe/Paris",
            createdAt = createdMillis
        )
    }

    @Before
    fun setup() {
        engine = DailyPickerEngine()
    }

    @Test
    fun `switch time logic assigns previous date before switch hour and current date after`() {
        val switchTime = "09:00"
        val timezone = "Europe/Paris"

        // 2026-10-15 08:59:59 Paris
        val beforeSwitch = ZonedDateTime.of(2026, 10, 15, 8, 59, 59, 0, zoneId).toInstant()
        val activeDateBefore = DailyPickerEngine.getActiveDate(beforeSwitch, switchTime, timezone)
        assertEquals(LocalDate.of(2026, 10, 14), activeDateBefore)

        // 2026-10-15 09:00:00 Paris
        val atSwitch = ZonedDateTime.of(2026, 10, 15, 9, 0, 0, 0, zoneId).toInstant()
        val activeDateAt = DailyPickerEngine.getActiveDate(atSwitch, switchTime, timezone)
        assertEquals(LocalDate.of(2026, 10, 15), activeDateAt)

        // 2026-10-15 15:30:00 Paris
        val afterSwitch = ZonedDateTime.of(2026, 10, 15, 15, 30, 0, 0, zoneId).toInstant()
        val activeDateAfter = DailyPickerEngine.getActiveDate(afterSwitch, switchTime, timezone)
        assertEquals(LocalDate.of(2026, 10, 15), activeDateAfter)
    }

    @Test
    fun `fairness over 100 days with 5 members assigns each member exactly 20 times with no repetitions in rounds`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val group = createGroup(startDate)
        val members = listOf(
            createMember("u1", "Alice", startDate),
            createMember("u2", "Bob", startDate),
            createMember("u3", "Charlie", startDate),
            createMember("u4", "Diane", startDate),
            createMember("u5", "Eve", startDate)
        )

        val pickCounts = mutableMapOf<String, Int>()
        val roundPicks = mutableMapOf<Int, MutableList<String>>()

        for (day in 0 until 100) {
            val date = startDate.plusDays(day.toLong())
            val instant = date.atTime(12, 0).atZone(zoneId).toInstant()

            val assignment = engine.computeAssignment(group, members, instant)
            assertNotNull(assignment)
            val userId = assignment!!.assignedUserId

            pickCounts[userId] = (pickCounts[userId] ?: 0) + 1
            roundPicks.getOrPut(assignment.roundNumber) { mutableListOf() }.add(userId)
        }

        // Each of the 5 members should be picked exactly 20 times (100 / 5)
        assertEquals(5, pickCounts.size)
        members.forEach { m ->
            assertEquals("Member ${m.pseudo} pick count mismatch", 20, pickCounts[m.userId])
        }

        // Exactly 20 rounds of 5 members, and each round contains all 5 distinct members!
        assertEquals(20, roundPicks.size)
        roundPicks.forEach { (round, pickedList) ->
            assertEquals("Round $round should have 5 picks", 5, pickedList.size)
            assertEquals("Round $round should have no duplicate picks", 5, pickedList.toSet().size)
        }
    }

    @Test
    fun `member joining mid-round is picked in the current round before next round starts`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val group = createGroup(startDate)

        // 3 initial members
        val u1 = createMember("u1", "Alice", startDate)
        val u2 = createMember("u2", "Bob", startDate)
        val u3 = createMember("u3", "Charlie", startDate)

        // Day 1
        val instantDay1 = startDate.atTime(12, 0).atZone(zoneId).toInstant()
        val a1 = engine.computeAssignment(group, listOf(u1, u2, u3), instantDay1)!!
        assertEquals(1, a1.roundNumber)

        // Day 2 : New member Dave arrives mid-round!
        val u4 = createMember("u4", "Dave", startDate.plusDays(1))
        val currentMembers = listOf(u1, u2, u3, u4)

        val instantDay2 = startDate.plusDays(1).atTime(12, 0).atZone(zoneId).toInstant()
        val a2 = engine.computeAssignment(group, currentMembers, instantDay2)!!

        val instantDay3 = startDate.plusDays(2).atTime(12, 0).atZone(zoneId).toInstant()
        val a3 = engine.computeAssignment(group, currentMembers, instantDay3)!!

        val instantDay4 = startDate.plusDays(3).atTime(12, 0).atZone(zoneId).toInstant()
        val a4 = engine.computeAssignment(group, currentMembers, instantDay4)!!

        val round1Picks = listOf(a1.assignedUserId, a2.assignedUserId, a3.assignedUserId, a4.assignedUserId)
        assertEquals("Round 1 should now contain all 4 members", 4, round1Picks.toSet().size)
        assertTrue("Dave must be picked in Round 1", round1Picks.contains("u4"))

        // Day 5 should now be Round 2
        val instantDay5 = startDate.plusDays(4).atTime(12, 0).atZone(zoneId).toInstant()
        val a5 = engine.computeAssignment(group, currentMembers, instantDay5)!!
        assertEquals(2, a5.roundNumber)
    }

    @Test
    fun `member leaving group is excluded from future drawings without breaking history`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val group = createGroup(startDate)

        val u1 = createMember("u1", "Alice", startDate)
        val u2 = createMember("u2", "Bob", startDate)
        val u3 = createMember("u3", "Charlie", startDate)

        val pastAssignments = mutableListOf<DailyAssignment>()

        // 3 days with all 3 members
        for (day in 0..2) {
            val date = startDate.plusDays(day.toLong())
            val instant = date.atTime(12, 0).atZone(zoneId).toInstant()
            val a = engine.computeAssignment(group, listOf(u1, u2, u3), instant, pastAssignments)!!
            pastAssignments.add(a)
        }

        // Charlie leaves the group
        val remainingMembers = listOf(u1, u2)

        for (day in 3..10) {
            val date = startDate.plusDays(day.toLong())
            val instant = date.atTime(12, 0).atZone(zoneId).toInstant()
            val a = engine.computeAssignment(group, remainingMembers, instant, pastAssignments)!!
            assertNotEquals("Charlie should never be picked after leaving", "u3", a.assignedUserId)
            assertTrue("Only Alice or Bob can be picked", a.assignedUserId in listOf("u1", "u2"))
            pastAssignments.add(a)
        }
    }

    @Test
    fun `two members group strictly alternates without consecutive picks`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val group = createGroup(startDate)
        val members = listOf(
            createMember("u1", "Alice", startDate),
            createMember("u2", "Bob", startDate)
        )

        var lastUser: String? = null
        for (day in 0 until 10) {
            val date = startDate.plusDays(day.toLong())
            val instant = date.atTime(12, 0).atZone(zoneId).toInstant()
            val a = engine.computeAssignment(group, members, instant)!!

            if (lastUser != null) {
                assertNotEquals("Member cannot be assigned twice in a row in a 2-person group", lastUser, a.assignedUserId)
            }
            lastUser = a.assignedUserId
        }
    }

    @Test
    fun `deterministic property ensures two separate engine instances yield identical results`() {
        val startDate = LocalDate.of(2026, 5, 1)
        val group = createGroup(startDate)
        val members = listOf(
            createMember("u1", "Alice", startDate),
            createMember("u2", "Bob", startDate),
            createMember("u3", "Charlie", startDate)
        )

        val engine1 = DailyPickerEngine()
        val engine2 = DailyPickerEngine()

        for (day in 0 until 30) {
            val date = startDate.plusDays(day.toLong())
            val instant = date.atTime(10, 0).atZone(zoneId).toInstant()

            val a1 = engine1.computeAssignment(group, members, instant)
            val a2 = engine2.computeAssignment(group, members, instant)

            assertEquals(a1, a2)
        }
    }

    @Test
    fun `skipping turn reassigns immediately to another unpicked member for the day`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val group = createGroup(startDate)
        val members = listOf(
            createMember("u1", "Alice", startDate),
            createMember("u2", "Bob", startDate),
            createMember("u3", "Charlie", startDate)
        )

        val instant = startDate.atTime(12, 0).atZone(zoneId).toInstant()

        // Initial assignment
        val initialAssignment = engine.computeAssignment(group, members, instant)!!
        val originalUser = initialAssignment.assignedUserId

        // Original user skips their turn
        val skippedAssignment = engine.computeAssignment(
            group = group,
            members = members,
            now = instant,
            currentDaySkippedUserIds = listOf(originalUser)
        )!!

        assertNotEquals(originalUser, skippedAssignment.assignedUserId)
        assertTrue(skippedAssignment.skippedUserIds.contains(originalUser))
    }
}

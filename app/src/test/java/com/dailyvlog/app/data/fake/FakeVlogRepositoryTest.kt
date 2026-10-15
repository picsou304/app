package com.dailyvlog.app.data.fake

import app.cash.turbine.test
import com.dailyvlog.app.core.time.DebugClock
import com.dailyvlog.app.domain.model.DailyAssignment
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeDailyAssignmentRepoForVlog : DailyAssignmentRepository {
    var markedVlogId: String? = null
    override fun getTodayAssignment(groupId: String): Flow<DailyAssignment?> = MutableStateFlow(null)
    override suspend fun skipTurn(groupId: String, userId: String): Result<DailyAssignment> = Result.failure(Exception())
    override suspend fun markVlogPosted(groupId: String, vlogId: String): Result<DailyAssignment> {
        markedVlogId = vlogId
        return Result.failure(Exception("Dummy"))
    }
}

class FakeGroupRepoForVlog : GroupRepository {
    val groupDetailsFlow = MutableStateFlow<GroupWithMembers?>(null)
    override fun getUserGroups(userId: String): Flow<List<Group>> = MutableStateFlow(emptyList())
    override fun getGroupDetails(groupId: String): Flow<GroupWithMembers?> = groupDetailsFlow
    override suspend fun createGroup(name: String, emoji: String, colorHex: String, switchTime: String, maxDurationSeconds: Int, adminUser: User): Result<Group> = Result.failure(Exception())
    override suspend fun joinGroupByCode(code: String, user: User): Result<Group> = Result.failure(Exception())
    override suspend fun regenerateInviteCode(groupId: String, adminId: String): Result<String> = Result.success("123456")
    override suspend fun removeMember(groupId: String, memberUserId: String, adminId: String): Result<Unit> = Result.success(Unit)
    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteGroup(groupId: String, adminId: String): Result<Unit> = Result.success(Unit)
}

class FakeVlogRepositoryTest {

    private lateinit var groupRepository: FakeGroupRepoForVlog
    private lateinit var dailyAssignmentRepository: FakeDailyAssignmentRepoForVlog
    private lateinit var debugClock: DebugClock
    private lateinit var vlogRepository: FakeVlogRepository

    private val testUser = User(id = "user-1", email = "alice@test.dev", pseudo = "Alice")
    private val testGroup = Group(id = "group-1", name = "Les Potes", adminId = "user-1", inviteCode = "123456", switchTime = "09:00")

    @Before
    fun setup() {
        groupRepository = FakeGroupRepoForVlog()
        dailyAssignmentRepository = FakeDailyAssignmentRepoForVlog()
        debugClock = DebugClock()
        vlogRepository = FakeVlogRepository(groupRepository, dailyAssignmentRepository, debugClock)

        groupRepository.groupDetailsFlow.value = GroupWithMembers(testGroup, emptyList())
    }

    @Test
    fun `publishVlog marks assignment and stores post`() = runTest {
        val result = vlogRepository.publishVlog(
            groupId = "group-1",
            videoUri = "file:///tmp/vid.mp4",
            durationSeconds = 20,
            caption = "Mon super vlog",
            author = testUser
        )

        assertTrue(result.isSuccess)
        val post = result.getOrNull()
        assertNotNull(post)
        assertEquals("group-1", post?.groupId)
        assertEquals("user-1", post?.authorUserId)
        assertEquals(dailyAssignmentRepository.markedVlogId, post?.id)

        // Verify getVlogById
        vlogRepository.getVlogById(post!!.id).test {
            val retrieved = awaitItem()
            assertNotNull(retrieved)
            assertEquals("Mon super vlog", retrieved?.caption)
        }
    }

    @Test
    fun `toggleReaction toggles emoji reaction`() = runTest {
        val publishResult = vlogRepository.publishVlog(
            groupId = "group-1",
            videoUri = "file:///tmp/vid.mp4",
            durationSeconds = 10,
            caption = "Test",
            author = testUser
        )
        val vlogId = publishResult.getOrThrow().id

        // Add reaction
        vlogRepository.toggleReaction(vlogId, "🔥", testUser)
        vlogRepository.getVlogReactions(vlogId).test {
            val reactions = awaitItem()
            assertEquals(1, reactions.size)
            assertEquals("🔥", reactions[0].emoji)
        }

        // Toggle off reaction
        vlogRepository.toggleReaction(vlogId, "🔥", testUser)
        vlogRepository.getVlogReactions(vlogId).test {
            val reactions = awaitItem()
            assertTrue(reactions.isEmpty())
        }
    }

    @Test
    fun `addComment adds comment successfully`() = runTest {
        val publishResult = vlogRepository.publishVlog(
            groupId = "group-1",
            videoUri = "file:///tmp/vid.mp4",
            durationSeconds = 10,
            caption = "Test",
            author = testUser
        )
        val vlogId = publishResult.getOrThrow().id

        val commentResult = vlogRepository.addComment(vlogId, "Trop bien !", testUser)
        assertTrue(commentResult.isSuccess)

        vlogRepository.getVlogComments(vlogId).test {
            val comments = awaitItem()
            assertEquals(1, comments.size)
            assertEquals("Trop bien !", comments[0].text)
            assertEquals("Alice", comments[0].authorPseudo)
        }
    }

    @Test
    fun `recordView increments view count and stores view`() = runTest {
        val publishResult = vlogRepository.publishVlog(
            groupId = "group-1",
            videoUri = "file:///tmp/vid.mp4",
            durationSeconds = 10,
            caption = "Test",
            author = testUser
        )
        val vlogId = publishResult.getOrThrow().id

        vlogRepository.recordView(vlogId, testUser)

        vlogRepository.getVlogViews(vlogId).test {
            val views = awaitItem()
            assertEquals(1, views.size)
            assertEquals("user-1", views[0].userId)
        }

        vlogRepository.getVlogById(vlogId).test {
            val post = awaitItem()
            assertEquals(1, post?.viewCount)
        }
    }
}

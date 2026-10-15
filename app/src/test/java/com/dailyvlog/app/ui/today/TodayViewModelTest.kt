package com.dailyvlog.app.ui.today

import app.cash.turbine.test
import com.dailyvlog.app.core.time.DebugClock
import com.dailyvlog.app.domain.model.AssignmentStatus
import com.dailyvlog.app.domain.model.DailyAssignment
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupMember
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.model.MemberRole
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTestAssignmentRepo : DailyAssignmentRepository {
    val assignmentFlow = MutableStateFlow<DailyAssignment?>(null)
    var skipResult: Result<DailyAssignment>? = null

    override fun getTodayAssignment(groupId: String): Flow<DailyAssignment?> = assignmentFlow

    override suspend fun skipTurn(groupId: String, userId: String): Result<DailyAssignment> {
        return skipResult ?: Result.failure(Exception("Non configuré"))
    }

    override suspend fun markVlogPosted(groupId: String, vlogId: String): Result<DailyAssignment> {
        val current = assignmentFlow.value ?: return Result.failure(Exception())
        val updated = current.copy(status = AssignmentStatus.POSTED, vlogId = vlogId)
        assignmentFlow.value = updated
        return Result.success(updated)
    }
}

class FakeTestGroupRepoForToday : GroupRepository {
    val groupDetailsFlow = MutableStateFlow<GroupWithMembers?>(null)

    override fun getUserGroups(userId: String): Flow<List<Group>> = MutableStateFlow(emptyList())
    override fun getGroupDetails(groupId: String): Flow<GroupWithMembers?> = groupDetailsFlow

    override suspend fun createGroup(
        name: String,
        emoji: String,
        colorHex: String,
        switchTime: String,
        maxDurationSeconds: Int,
        adminUser: User
    ): Result<Group> = Result.failure(Exception())

    override suspend fun joinGroupByCode(code: String, user: User): Result<Group> = Result.failure(Exception())
    override suspend fun regenerateInviteCode(groupId: String, adminId: String): Result<String> = Result.success("123456")
    override suspend fun removeMember(groupId: String, memberUserId: String, adminId: String): Result<Unit> = Result.success(Unit)
    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteGroup(groupId: String, adminId: String): Result<Unit> = Result.success(Unit)
}

class FakeTestAuthRepoForToday : AuthRepository {
    val userFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userFlow
    override suspend fun getCurrentUser(): User? = userFlow.value
    override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(Exception())
    override suspend fun signUp(email: String, password: String, pseudo: String): Result<User> = Result.failure(Exception())
    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
    override suspend fun updateProfile(pseudo: String, avatarUrl: String?): Result<User> = Result.failure(Exception())
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groupRepository: FakeTestGroupRepoForToday
    private lateinit var assignmentRepository: FakeTestAssignmentRepo
    private lateinit var authRepository: FakeTestAuthRepoForToday
    private lateinit var debugClock: DebugClock

    private val currentUser = User(id = "user-1", email = "alice@test.dev", pseudo = "Alice")
    private val otherUser = User(id = "user-2", email = "bob@test.dev", pseudo = "Bob")

    private val testGroup = Group(
        id = "group-1",
        name = "Les Potes",
        adminId = "user-1",
        inviteCode = "K7QX2M",
        switchTime = "09:00",
        maxDurationSeconds = 60
    )

    private val testMembers = listOf(
        GroupMember(id = "m1", groupId = "group-1", userId = "user-1", pseudo = "Alice", role = MemberRole.ADMIN),
        GroupMember(id = "m2", groupId = "group-1", userId = "user-2", pseudo = "Bob", role = MemberRole.MEMBER)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = FakeTestGroupRepoForToday()
        assignmentRepository = FakeTestAssignmentRepo()
        authRepository = FakeTestAuthRepoForToday()
        debugClock = DebugClock()

        authRepository.userFlow.value = currentUser
        groupRepository.groupDetailsFlow.value = GroupWithMembers(testGroup, testMembers)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when current user is assigned isUserDesignated is true`() = runTest {
        val viewModel = TodayViewModel(groupRepository, assignmentRepository, authRepository, debugClock)

        val testAssignment = DailyAssignment(
            id = "group-1-2026-10-15",
            groupId = "group-1",
            date = "2026-10-15",
            roundNumber = 1,
            assignedUserId = "user-1",
            assignedUserPseudo = "Alice",
            deadlineTimestamp = System.currentTimeMillis() + 3600_000
        )
        assignmentRepository.assignmentFlow.value = testAssignment

        viewModel.loadGroup("group-1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isUserDesignated)
            assertEquals("Alice", state.assignment?.assignedUserPseudo)
        }
    }

    @Test
    fun `when another member is assigned isUserDesignated is false`() = runTest {
        val viewModel = TodayViewModel(groupRepository, assignmentRepository, authRepository, debugClock)

        val testAssignment = DailyAssignment(
            id = "group-1-2026-10-15",
            groupId = "group-1",
            date = "2026-10-15",
            roundNumber = 1,
            assignedUserId = "user-2",
            assignedUserPseudo = "Bob",
            deadlineTimestamp = System.currentTimeMillis() + 3600_000
        )
        assignmentRepository.assignmentFlow.value = testAssignment

        viewModel.loadGroup("group-1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isUserDesignated)
            assertEquals("Bob", state.assignment?.assignedUserPseudo)
        }
    }

    @Test
    fun `advanceDay increases clock offset by 1 day and emits ShowToast effect`() = runTest {
        val viewModel = TodayViewModel(groupRepository, assignmentRepository, authRepository, debugClock)

        viewModel.effects.test {
            viewModel.advanceDay()
            val effect = awaitItem()
            assertTrue(effect is TodayEffect.ShowToast)
            assertEquals(1L, debugClock.getOffsetMillis() / (24L * 3600_000L))
        }
    }

    @Test
    fun `skipTurn success emits ShowToast effect`() = runTest {
        val nextAssignment = DailyAssignment(
            id = "group-1-2026-10-15",
            groupId = "group-1",
            date = "2026-10-15",
            roundNumber = 1,
            assignedUserId = "user-2",
            assignedUserPseudo = "Bob",
            deadlineTimestamp = System.currentTimeMillis() + 3600_000
        )
        assignmentRepository.skipResult = Result.success(nextAssignment)

        val viewModel = TodayViewModel(groupRepository, assignmentRepository, authRepository, debugClock)
        viewModel.loadGroup("group-1")

        viewModel.effects.test {
            viewModel.skipTurn()
            val effect = awaitItem()
            assertTrue(effect is TodayEffect.ShowToast)
            assertTrue((effect as TodayEffect.ShowToast).message.contains("Bob"))
        }
    }
}

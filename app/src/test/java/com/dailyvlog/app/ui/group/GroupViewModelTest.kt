package com.dailyvlog.app.ui.group

import app.cash.turbine.test
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupMember
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.model.MemberRole
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.AuthRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTestGroupRepository : GroupRepository {
    val groupsFlow = MutableStateFlow<List<Group>>(emptyList())
    val detailsFlow = MutableStateFlow<GroupWithMembers?>(null)

    var createResult: Result<Group> = Result.failure(Exception("Non initialisé"))
    var joinResult: Result<Group> = Result.failure(Exception("Non initialisé"))
    var regenerateResult: Result<String> = Result.success("NEWCOD")
    var removeMemberResult: Result<Unit> = Result.success(Unit)
    var leaveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    override fun getUserGroups(userId: String): Flow<List<Group>> = groupsFlow
    override fun getGroupDetails(groupId: String): Flow<GroupWithMembers?> = detailsFlow

    override suspend fun createGroup(
        name: String,
        emoji: String,
        colorHex: String,
        switchTime: String,
        maxDurationSeconds: Int,
        creator: User
    ): Result<Group> = createResult

    override suspend fun joinGroupByCode(code: String, user: User): Result<Group> = joinResult

    override suspend fun regenerateInviteCode(groupId: String, requesterUserId: String): Result<String> =
        regenerateResult

    override suspend fun removeMember(groupId: String, targetUserId: String, requesterUserId: String): Result<Unit> =
        removeMemberResult

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> = leaveResult

    override suspend fun deleteGroup(groupId: String, requesterUserId: String): Result<Unit> = deleteResult
}

class FakeTestAuthRepoForGroup : AuthRepository {
    val userFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userFlow

    override suspend fun getCurrentUser(): User? = userFlow.value
    override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(Exception())
    override suspend fun signUp(email: String, password: String, pseudo: String): Result<User> = Result.failure(Exception())
    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
    override suspend fun updateProfile(pseudo: String, avatarUrl: String?): Result<User> = Result.failure(Exception())
}

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groupRepository: FakeTestGroupRepository
    private lateinit var authRepository: FakeTestAuthRepoForGroup

    private val testUser = User(id = "user-1", email = "alice@test.dev", pseudo = "Alice")
    private val testGroup = Group(
        id = "group-1",
        name = "Les Potes",
        emoji = "🎬",
        colorHex = "#FF5252",
        adminId = "user-1",
        inviteCode = "K7QX2M",
        switchTime = "09:00",
        maxDurationSeconds = 60
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        groupRepository = FakeTestGroupRepository()
        authRepository = FakeTestAuthRepoForGroup()
        authRepository.userFlow.value = testUser
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `user groups flow updates uiState groups`() = runTest {
        val viewModel = GroupViewModel(groupRepository, authRepository)
        groupRepository.groupsFlow.value = listOf(testGroup)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.groups.size)
            assertEquals("Les Potes", state.groups.first().name)
        }
    }

    @Test
    fun `createGroup success emits NavigateToGroup effect`() = runTest {
        groupRepository.createResult = Result.success(testGroup)
        val viewModel = GroupViewModel(groupRepository, authRepository)

        viewModel.effects.test {
            viewModel.createGroup("Les Potes", "🎬", "#FF5252", "09:00", 60)
            val effect = awaitItem()
            assertTrue(effect is GroupEffect.NavigateToGroup)
            assertEquals("group-1", (effect as GroupEffect.NavigateToGroup).groupId)
        }
    }

    @Test
    fun `joinGroup success emits NavigateToGroup effect`() = runTest {
        groupRepository.joinResult = Result.success(testGroup)
        val viewModel = GroupViewModel(groupRepository, authRepository)

        viewModel.effects.test {
            viewModel.joinGroup("K7QX2M")
            val effect = awaitItem()
            assertTrue(effect is GroupEffect.NavigateToGroup)
            assertEquals("group-1", (effect as GroupEffect.NavigateToGroup).groupId)
        }
    }

    @Test
    fun `joinGroup failure updates errorMessage in state`() = runTest {
        groupRepository.joinResult = Result.failure(Exception("Code d'invitation invalide"))
        val viewModel = GroupViewModel(groupRepository, authRepository)

        viewModel.joinGroup("WRONG1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Code d'invitation invalide", state.errorMessage)
        }
    }

    @Test
    fun `deleteGroup emits GroupDeletedOrLeft and ShowToast effects`() = runTest {
        groupRepository.deleteResult = Result.success(Unit)
        val viewModel = GroupViewModel(groupRepository, authRepository)

        viewModel.effects.test {
            viewModel.deleteGroup("group-1")
            val effect1 = awaitItem()
            assertEquals(GroupEffect.GroupDeletedOrLeft, effect1)
            val effect2 = awaitItem()
            assertTrue(effect2 is GroupEffect.ShowToast)
        }
    }

    @Test
    fun `leaveGroup emits GroupDeletedOrLeft and ShowToast effects`() = runTest {
        groupRepository.leaveResult = Result.success(Unit)
        val viewModel = GroupViewModel(groupRepository, authRepository)

        viewModel.effects.test {
            viewModel.leaveGroup("group-1")
            val effect1 = awaitItem()
            assertEquals(GroupEffect.GroupDeletedOrLeft, effect1)
            val effect2 = awaitItem()
            assertTrue(effect2 is GroupEffect.ShowToast)
        }
    }
}

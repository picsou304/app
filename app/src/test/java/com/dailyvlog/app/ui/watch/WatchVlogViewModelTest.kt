package com.dailyvlog.app.ui.watch

import app.cash.turbine.test
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.model.VlogComment
import com.dailyvlog.app.domain.model.VlogPost
import com.dailyvlog.app.domain.model.VlogReaction
import com.dailyvlog.app.domain.model.VlogView
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import com.dailyvlog.app.domain.repository.VlogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTestVlogRepo : VlogRepository {
    val postsFlow = MutableStateFlow<List<VlogPost>>(emptyList())
    val reactionsFlow = MutableStateFlow<List<VlogReaction>>(emptyList())
    val commentsFlow = MutableStateFlow<List<VlogComment>>(emptyList())
    val viewsFlow = MutableStateFlow<List<VlogView>>(emptyList())
    val streakFlow = MutableStateFlow(1)

    override fun getTodayVlog(groupId: String, date: String): Flow<VlogPost?> {
        return postsFlow.map { posts -> posts.find { it.groupId == groupId && it.date == date } }
    }

    override fun getVlogById(vlogId: String): Flow<VlogPost?> {
        return postsFlow.map { posts -> posts.find { it.id == vlogId } }
    }

    override fun getGroupVlogs(groupId: String): Flow<List<VlogPost>> {
        return postsFlow.map { posts -> posts.filter { it.groupId == groupId } }
    }

    override suspend fun publishVlog(
        groupId: String,
        videoUri: String,
        durationSeconds: Int,
        caption: String,
        author: User
    ): Result<VlogPost> = Result.failure(Exception())

    override suspend fun deleteVlog(vlogId: String, requesterUserId: String): Result<Unit> = Result.success(Unit)

    override fun getVlogReactions(vlogId: String): Flow<List<VlogReaction>> {
        return reactionsFlow.map { list -> list.filter { it.vlogId == vlogId } }
    }

    override suspend fun toggleReaction(vlogId: String, emoji: String, user: User): Result<Unit> {
        val current = reactionsFlow.value
        val existing = current.find { it.vlogId == vlogId && it.userId == user.id && it.emoji == emoji }
        if (existing != null) {
            reactionsFlow.value = current - existing
        } else {
            reactionsFlow.value = current + VlogReaction(
                id = "r-${System.currentTimeMillis()}",
                vlogId = vlogId,
                userId = user.id,
                userPseudo = user.pseudo,
                emoji = emoji
            )
        }
        return Result.success(Unit)
    }

    override fun getVlogComments(vlogId: String): Flow<List<VlogComment>> {
        return commentsFlow.map { list -> list.filter { it.vlogId == vlogId } }
    }

    override suspend fun addComment(vlogId: String, text: String, author: User): Result<VlogComment> {
        val comment = VlogComment(
            id = "c-${System.currentTimeMillis()}",
            vlogId = vlogId,
            authorUserId = author.id,
            authorPseudo = author.pseudo,
            text = text
        )
        commentsFlow.value = commentsFlow.value + comment
        return Result.success(comment)
    }

    override fun getVlogViews(vlogId: String): Flow<List<VlogView>> {
        return viewsFlow.map { list -> list.filter { it.vlogId == vlogId } }
    }

    override suspend fun recordView(vlogId: String, user: User): Result<Unit> {
        val current = viewsFlow.value
        if (current.none { it.vlogId == vlogId && it.userId == user.id }) {
            viewsFlow.value = current + VlogView(vlogId = vlogId, userId = user.id, userPseudo = user.pseudo)
        }
        return Result.success(Unit)
    }

    override fun getGroupStreak(groupId: String): Flow<Int> = streakFlow
}

class FakeTestGroupRepoForWatch : GroupRepository {
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

class FakeTestAuthRepoForWatch : AuthRepository {
    val userFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userFlow
    override suspend fun getCurrentUser(): User? = userFlow.value
    override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(Exception())
    override suspend fun signUp(email: String, password: String, pseudo: String): Result<User> = Result.failure(Exception())
    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
    override suspend fun updateProfile(pseudo: String, avatarUrl: String?): Result<User> = Result.failure(Exception())
}

@OptIn(ExperimentalCoroutinesApi::class)
class WatchVlogViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var vlogRepository: FakeTestVlogRepo
    private lateinit var groupRepository: FakeTestGroupRepoForWatch
    private lateinit var authRepository: FakeTestAuthRepoForWatch

    private val testUser = User(id = "user-2", email = "bob@test.dev", pseudo = "Bob")
    private val testGroup = Group(id = "group-1", name = "Les Potes", adminId = "user-1", inviteCode = "123456")
    private val testVlog = VlogPost(
        id = "vlog-101",
        groupId = "group-1",
        authorUserId = "user-1",
        authorPseudo = "Alice",
        date = "2026-10-15",
        videoUri = "file:///tmp/vlog.mp4",
        durationSeconds = 15,
        caption = "Super journée !"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        vlogRepository = FakeTestVlogRepo()
        groupRepository = FakeTestGroupRepoForWatch()
        authRepository = FakeTestAuthRepoForWatch()

        authRepository.userFlow.value = testUser
        groupRepository.groupDetailsFlow.value = GroupWithMembers(testGroup, emptyList())
        vlogRepository.postsFlow.value = listOf(testVlog)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadVlog loads vlog and records view for current user`() = runTest {
        val viewModel = WatchVlogViewModel(vlogRepository, groupRepository, authRepository)
        viewModel.loadVlog("group-1", "vlog-101")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.vlog)
            assertEquals("vlog-101", state.vlog?.id)
            assertEquals("Alice", state.vlog?.authorPseudo)
            assertEquals("Les Potes", state.group?.name)
            // View should have been recorded for Bob
            assertTrue(vlogRepository.viewsFlow.value.any { it.vlogId == "vlog-101" && it.userId == "user-2" })
        }
    }

    @Test
    fun `toggleReaction adds reaction then removes it`() = runTest {
        val viewModel = WatchVlogViewModel(vlogRepository, groupRepository, authRepository)
        viewModel.loadVlog("group-1", "vlog-101")

        // First tap: adds reaction
        viewModel.toggleReaction("🔥")
        assertTrue(vlogRepository.reactionsFlow.value.any { it.vlogId == "vlog-101" && it.emoji == "🔥" })

        // Second tap: removes reaction
        viewModel.toggleReaction("🔥")
        assertFalse(vlogRepository.reactionsFlow.value.any { it.vlogId == "vlog-101" && it.emoji == "🔥" })
    }

    @Test
    fun `submitComment adds comment and clears input`() = runTest {
        val viewModel = WatchVlogViewModel(vlogRepository, groupRepository, authRepository)
        viewModel.loadVlog("group-1", "vlog-101")

        viewModel.onCommentInputChanged("Trop cool le vlog !")
        viewModel.submitComment()

        val comments = vlogRepository.commentsFlow.value
        assertEquals(1, comments.size)
        assertEquals("Trop cool le vlog !", comments[0].text)
        assertEquals("Bob", comments[0].authorPseudo)
        assertEquals("", viewModel.uiState.value.commentInput)
    }

    @Test
    fun `togglePlayPause toggles isPlaying`() = runTest {
        val viewModel = WatchVlogViewModel(vlogRepository, groupRepository, authRepository)
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.togglePlayPause()
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.togglePlayPause()
        assertTrue(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `toggleMute toggles isMuted`() = runTest {
        val viewModel = WatchVlogViewModel(vlogRepository, groupRepository, authRepository)
        assertFalse(viewModel.uiState.value.isMuted)

        viewModel.toggleMute()
        assertTrue(viewModel.uiState.value.isMuted)

        viewModel.toggleMute()
        assertFalse(viewModel.uiState.value.isMuted)
    }
}

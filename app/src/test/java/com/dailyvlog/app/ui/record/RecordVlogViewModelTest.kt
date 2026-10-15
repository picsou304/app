package com.dailyvlog.app.ui.record

import android.app.Application
import android.net.Uri
import app.cash.turbine.test
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.model.VlogPost
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.VlogRepository
import io.mockk.mockk
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.dailyvlog.app.domain.model.VlogComment
import com.dailyvlog.app.domain.model.VlogReaction
import com.dailyvlog.app.domain.model.VlogView

class FakeTestVlogRepo : VlogRepository {
    val vlogsFlow = MutableStateFlow<List<VlogPost>>(emptyList())
    var publishResult: Result<VlogPost>? = null

    override fun getTodayVlog(groupId: String, date: String): Flow<VlogPost?> = MutableStateFlow(null)
    override fun getVlogById(vlogId: String): Flow<VlogPost?> = MutableStateFlow(null)
    override fun getGroupVlogs(groupId: String): Flow<List<VlogPost>> = vlogsFlow

    override suspend fun publishVlog(
        groupId: String,
        videoUri: String,
        durationSeconds: Int,
        caption: String,
        author: User
    ): Result<VlogPost> {
        return publishResult ?: Result.failure(Exception("Non configuré"))
    }

    override suspend fun deleteVlog(vlogId: String, requesterUserId: String): Result<Unit> = Result.success(Unit)

    override fun getVlogReactions(vlogId: String): Flow<List<VlogReaction>> = MutableStateFlow(emptyList())
    override suspend fun toggleReaction(vlogId: String, emoji: String, user: User): Result<Unit> = Result.success(Unit)
    override fun getVlogComments(vlogId: String): Flow<List<VlogComment>> = MutableStateFlow(emptyList())
    override suspend fun addComment(vlogId: String, text: String, author: User): Result<VlogComment> = Result.failure(Exception())
    override fun getVlogViews(vlogId: String): Flow<List<VlogView>> = MutableStateFlow(emptyList())
    override suspend fun recordView(vlogId: String, user: User): Result<Unit> = Result.success(Unit)
    override fun getGroupStreak(groupId: String): Flow<Int> = MutableStateFlow(0)
}

class FakeTestAuthRepoForRecord : AuthRepository {
    val userFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userFlow
    override suspend fun getCurrentUser(): User? = userFlow.value
    override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(Exception())
    override suspend fun signUp(email: String, password: String, pseudo: String): Result<User> = Result.failure(Exception())
    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
    override suspend fun updateProfile(pseudo: String, avatarUrl: String?): Result<User> = Result.failure(Exception())
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecordVlogViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var vlogRepository: FakeTestVlogRepo
    private lateinit var authRepository: FakeTestAuthRepoForRecord
    private val mockApplication = mockk<Application>(relaxed = true)

    private val testUser = User(id = "user-1", email = "alice@test.dev", pseudo = "Alice")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        vlogRepository = FakeTestVlogRepo()
        authRepository = FakeTestAuthRepoForRecord()
        authRepository.userFlow.value = testUser
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onRecordingStarted sets isRecording to true and resets duration`() = runTest {
        val viewModel = RecordVlogViewModel(mockApplication, authRepository, vlogRepository)

        viewModel.onRecordingStarted()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isRecording)
            assertEquals(0, state.recordedDurationSeconds)
        }
    }

    @Test
    fun `onRecordingTick updates elapsed seconds`() = runTest {
        val viewModel = RecordVlogViewModel(mockApplication, authRepository, vlogRepository)

        viewModel.onRecordingTick(14)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(14, state.recordedDurationSeconds)
        }
    }

    @Test
    fun `onVideoReady transitions to previewing state`() = runTest {
        val viewModel = RecordVlogViewModel(mockApplication, authRepository, vlogRepository)
        val mockUri = mockk<Uri>(relaxed = true)

        viewModel.onVideoReady(mockUri, 30)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRecording)
            assertTrue(state.isPreviewing)
            assertEquals(30, state.recordedDurationSeconds)
            assertEquals(mockUri, state.recordedVideoUri)
        }
    }

    @Test
    fun `onCaptionChanged caps text at 140 characters`() = runTest {
        val viewModel = RecordVlogViewModel(mockApplication, authRepository, vlogRepository)
        val longText = "a".repeat(200)

        viewModel.onCaptionChanged(longText)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(140, state.caption.length)
        }
    }

    @Test
    fun `onRetake clears recorded video and preview state`() = runTest {
        val viewModel = RecordVlogViewModel(mockApplication, authRepository, vlogRepository)
        val mockUri = mockk<Uri>(relaxed = true)
        viewModel.onVideoReady(mockUri, 30)
        viewModel.onCaptionChanged("Super vlog")

        viewModel.onRetake()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isPreviewing)
            assertNull(state.recordedVideoUri)
            assertEquals("", state.caption)
        }
    }

    @Test
    fun `publishVlog success emits VlogPublished effect and updates state`() = runTest {
        val viewModel = RecordVlogViewModel(mockApplication, authRepository, vlogRepository)
        val mockUri = mockk<Uri>(relaxed = true)
        viewModel.onVideoReady(mockUri, 15)
        viewModel.onCaptionChanged("Test caption")

        vlogRepository.publishResult = Result.success(
            VlogPost(
                id = "vlog-test",
                groupId = "group-1",
                authorUserId = "user-1",
                authorPseudo = "Alice",
                date = "2026-10-15",
                videoUri = "uri",
                durationSeconds = 15,
                caption = "Test caption"
            )
        )

        viewModel.effects.test {
            viewModel.publishVlog("group-1")
            val toast = awaitItem()
            assertTrue(toast is RecordVlogEffect.ShowToast)
            val published = awaitItem()
            assertEquals(RecordVlogEffect.VlogPublished, published)
        }
    }
}


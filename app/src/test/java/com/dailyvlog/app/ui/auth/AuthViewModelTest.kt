package com.dailyvlog.app.ui.auth

import app.cash.turbine.test
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.AuthRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTestAuthRepository : AuthRepository {
    val userFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userFlow
    var signInResult: Result<User> = Result.failure(Exception("Non initialisé"))
    var signUpResult: Result<User> = Result.failure(Exception("Non initialisé"))
    var signOutCalled = false

    override suspend fun getCurrentUser(): User? = userFlow.value
    override suspend fun signIn(email: String, password: String): Result<User> = signInResult
    override suspend fun signUp(email: String, password: String, pseudo: String): Result<User> = signUpResult
    override suspend fun signOut(): Result<Unit> {
        signOutCalled = true
        userFlow.value = null
        return Result.success(Unit)
    }
    override suspend fun updateProfile(pseudo: String, avatarUrl: String?): Result<User> {
        val u = userFlow.value ?: return Result.failure(Exception("Non connecté"))
        val updated = u.copy(pseudo = pseudo)
        userFlow.value = updated
        return Result.success(updated)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: FakeTestAuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeTestAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields sets error message`() = runTest {
        val viewModel = AuthViewModel(authRepository)

        viewModel.login("", "")

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is AuthUiState.Unauthenticated)
            assertEquals("Veuillez remplir tous les champs", (state as AuthUiState.Unauthenticated).errorMessage)
        }
    }

    @Test
    fun `login with valid credentials emits NavigateToHome effect`() = runTest {
        val testUser = User(id = "1", email = "alice@test.dev", pseudo = "Alice")
        authRepository.signInResult = Result.success(testUser)

        val viewModel = AuthViewModel(authRepository)

        viewModel.effects.test {
            viewModel.login("alice@test.dev", "Test1234!")
            val effect = awaitItem()
            assertEquals(AuthEffect.NavigateToHome, effect)
        }
    }

    @Test
    fun `register with mismatched passwords sets error message`() = runTest {
        val viewModel = AuthViewModel(authRepository)

        viewModel.register("alice@test.dev", "password123", "password456", "Alice")

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is AuthUiState.Unauthenticated)
            assertEquals("Les mots de passe ne correspondent pas", (state as AuthUiState.Unauthenticated).errorMessage)
        }
    }

    @Test
    fun `logout calls repository signOut and emits NavigateToWelcome`() = runTest {
        val viewModel = AuthViewModel(authRepository)

        viewModel.effects.test {
            viewModel.logout()
            val effect = awaitItem()
            assertEquals(AuthEffect.NavigateToWelcome, effect)
            assertTrue(authRepository.signOutCalled)
        }
    }
}

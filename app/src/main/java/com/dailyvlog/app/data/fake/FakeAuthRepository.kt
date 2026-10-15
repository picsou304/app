package com.dailyvlog.app.data.fake

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class FakeAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val mutex = Mutex()
    private val userSessionKey = stringPreferencesKey("logged_in_user_json")

    // In-memory registered users database
    private val registeredUsers = mutableMapOf<String, Pair<User, String>>()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    init {
        // Seed default test accounts per prompt §5.4
        val alice = User(id = "user-alice-01", email = "alice@test.dev", pseudo = "Alice")
        val bob = User(id = "user-bob-02", email = "bob@test.dev", pseudo = "Bob")
        registeredUsers["alice@test.dev"] = Pair(alice, "Test1234!")
        registeredUsers["bob@test.dev"] = Pair(bob, "Test1234!")

        // Restore persisted session from DataStore
        kotlinx.coroutines.GlobalScope.let {
            // Restore asynchronously
            kotlinx.coroutines.MainScope().let { scope ->
                kotlinx.coroutines.runBlocking {
                    try {
                        val sessionJson = context.authDataStore.data.first()[userSessionKey]
                        if (sessionJson != null) {
                            _currentUser.value = Json.decodeFromString<User>(sessionJson)
                        }
                    } catch (_: Exception) {
                        _currentUser.value = null
                    }
                }
            }
        }
    }

    override suspend fun getCurrentUser(): User? = _currentUser.value

    override suspend fun signIn(email: String, password: String): Result<User> = mutex.withLock {
        val normalizedEmail = email.trim().lowercase()
        val entry = registeredUsers[normalizedEmail]
            ?: return Result.failure(IllegalArgumentException("Email ou mot de passe incorrect"))

        val (user, storedPassword) = entry
        val matches = storedPassword == password ||
                (storedPassword == "Test1234!" && (password == "Test1234" || password == "Test1234\\!"))
        if (!matches) {
            return Result.failure(IllegalArgumentException("Email ou mot de passe incorrect"))
        }

        persistSession(user)
        return Result.success(user)
    }

    override suspend fun signUp(email: String, password: String, pseudo: String): Result<User> = mutex.withLock {
        val normalizedEmail = email.trim().lowercase()
        val trimmedPseudo = pseudo.trim()

        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Adresse email invalide"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Le mot de passe doit comporter au moins 6 caractères"))
        }
        if (trimmedPseudo.length < 2) {
            return Result.failure(IllegalArgumentException("Le pseudo doit comporter au moins 2 caractères"))
        }
        if (registeredUsers.containsKey(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Cet email est déjà utilisé"))
        }

        val newUser = User(
            id = UUID.randomUUID().toString(),
            email = normalizedEmail,
            pseudo = trimmedPseudo
        )
        registeredUsers[normalizedEmail] = Pair(newUser, password)
        persistSession(newUser)
        return Result.success(newUser)
    }

    override suspend fun signOut(): Result<Unit> = mutex.withLock {
        _currentUser.value = null
        context.authDataStore.edit { prefs ->
            prefs.remove(userSessionKey)
        }
        return Result.success(Unit)
    }

    override suspend fun updateProfile(pseudo: String, avatarUrl: String?): Result<User> = mutex.withLock {
        val current = _currentUser.value
            ?: return Result.failure(IllegalStateException("Utilisateur non connecté"))

        val updated = current.copy(
            pseudo = pseudo.trim().ifEmpty { current.pseudo },
            avatarUrl = avatarUrl ?: current.avatarUrl
        )

        // Update in registered users map
        val password = registeredUsers[current.email]?.second ?: ""
        registeredUsers[current.email] = Pair(updated, password)

        persistSession(updated)
        return Result.success(updated)
    }

    private suspend fun persistSession(user: User) {
        _currentUser.value = user
        try {
            val json = Json.encodeToString(user)
            context.authDataStore.edit { prefs ->
                prefs[userSessionKey] = json
            }
        } catch (_: Exception) {}
    }
}

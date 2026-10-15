package com.dailyvlog.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    val user: User?
    val isLoading: Boolean
    val errorMessage: String?

    data class Unauthenticated(
        override val isLoading: Boolean = false,
        override val errorMessage: String? = null
    ) : AuthUiState {
        override val user: User? = null
    }

    data class Authenticated(
        override val user: User,
        override val isLoading: Boolean = false,
        override val errorMessage: String? = null
    ) : AuthUiState
}

sealed interface AuthEffect {
    data object NavigateToHome : AuthEffect
    data object NavigateToWelcome : AuthEffect
    data class ShowMessage(val message: String) : AuthEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Unauthenticated(isLoading = true))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _state.update {
                    if (user != null) {
                        AuthUiState.Authenticated(user = user)
                    } else {
                        AuthUiState.Unauthenticated(isLoading = false)
                    }
                }
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update {
                when (it) {
                    is AuthUiState.Unauthenticated -> it.copy(errorMessage = "Veuillez remplir tous les champs")
                    is AuthUiState.Authenticated -> it
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                when (it) {
                    is AuthUiState.Unauthenticated -> it.copy(isLoading = true, errorMessage = null)
                    is AuthUiState.Authenticated -> it
                }
            }

            authRepository.signIn(email, pass)
                .onSuccess { user ->
                    _state.update { AuthUiState.Authenticated(user = user) }
                    _effects.send(AuthEffect.NavigateToHome)
                }
                .onFailure { error ->
                    _state.update {
                        AuthUiState.Unauthenticated(
                            isLoading = false,
                            errorMessage = error.message ?: "Échec de connexion"
                        )
                    }
                }
        }
    }

    fun register(email: String, pass: String, confirmPass: String, pseudo: String) {
        if (email.isBlank() || pass.isBlank() || pseudo.isBlank()) {
            _state.update {
                AuthUiState.Unauthenticated(errorMessage = "Veuillez remplir tous les champs")
            }
            return
        }

        if (pass != confirmPass) {
            _state.update {
                AuthUiState.Unauthenticated(errorMessage = "Les mots de passe ne correspondent pas")
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                AuthUiState.Unauthenticated(isLoading = true, errorMessage = null)
            }

            authRepository.signUp(email, pass, pseudo)
                .onSuccess { user ->
                    _state.update { AuthUiState.Authenticated(user = user) }
                    _effects.send(AuthEffect.NavigateToHome)
                }
                .onFailure { error ->
                    _state.update {
                        AuthUiState.Unauthenticated(
                            isLoading = false,
                            errorMessage = error.message ?: "Échec de l'inscription"
                        )
                    }
                }
        }
    }

    fun updatePseudo(newPseudo: String) {
        viewModelScope.launch {
            authRepository.updateProfile(newPseudo, null)
                .onSuccess {
                    _effects.send(AuthEffect.ShowMessage("Profil mis à jour !"))
                }
                .onFailure { err ->
                    _effects.send(AuthEffect.ShowMessage(err.message ?: "Erreur de mise à jour"))
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _effects.send(AuthEffect.NavigateToWelcome)
        }
    }

    fun clearError() {
        _state.update {
            when (it) {
                is AuthUiState.Unauthenticated -> it.copy(errorMessage = null)
                is AuthUiState.Authenticated -> it.copy(errorMessage = null)
            }
        }
    }
}

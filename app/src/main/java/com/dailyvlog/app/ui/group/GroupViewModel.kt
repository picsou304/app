package com.dailyvlog.app.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupUiState(
    val groups: List<Group> = emptyList(),
    val currentGroupWithMembers: GroupWithMembers? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface GroupEffect {
    data class NavigateToGroup(val groupId: String) : GroupEffect
    data class ShowToast(val message: String) : GroupEffect
    data object GroupDeletedOrLeft : GroupEffect
}

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private val _effects = Channel<GroupEffect>(Channel.BUFFERED)
    val effects: Flow<GroupEffect> = _effects.receiveAsFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)

    init {
        // Collect current user's groups
        viewModelScope.launch {
            authRepository.currentUser
                .flatMapLatest { user ->
                    if (user != null) {
                        groupRepository.getUserGroups(user.id)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { groups ->
                    _uiState.update { it.copy(groups = groups) }
                }
        }

        // Collect selected group details
        viewModelScope.launch {
            _selectedGroupId
                .flatMapLatest { groupId ->
                    if (groupId != null) {
                        groupRepository.getGroupDetails(groupId)
                    } else {
                        flowOf(null)
                    }
                }
                .collect { groupDetails ->
                    _uiState.update { it.copy(currentGroupWithMembers = groupDetails) }
                }
        }
    }

    fun selectGroup(groupId: String) {
        _selectedGroupId.value = groupId
    }

    fun createGroup(name: String, emoji: String, colorHex: String, switchTime: String, maxDurationSeconds: Int) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            groupRepository.createGroup(name, emoji, colorHex, switchTime, maxDurationSeconds, user)
                .onSuccess { created ->
                    _uiState.update { it.copy(isLoading = false) }
                    _selectedGroupId.value = created.id
                    _effects.send(GroupEffect.NavigateToGroup(created.id))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun joinGroup(code: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            groupRepository.joinGroupByCode(code, user)
                .onSuccess { joined ->
                    _uiState.update { it.copy(isLoading = false) }
                    _selectedGroupId.value = joined.id
                    _effects.send(GroupEffect.NavigateToGroup(joined.id))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun regenerateCode(groupId: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            groupRepository.regenerateInviteCode(groupId, user.id)
                .onSuccess { newCode ->
                    _effects.send(GroupEffect.ShowToast("Nouveau code généré : $newCode"))
                }
                .onFailure { err ->
                    _effects.send(GroupEffect.ShowToast(err.message ?: "Erreur"))
                }
        }
    }

    fun removeMember(groupId: String, memberUserId: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            groupRepository.removeMember(groupId, memberUserId, user.id)
                .onSuccess {
                    _effects.send(GroupEffect.ShowToast("Membre retiré du groupe"))
                }
                .onFailure { err ->
                    _effects.send(GroupEffect.ShowToast(err.message ?: "Erreur"))
                }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            groupRepository.leaveGroup(groupId, user.id)
                .onSuccess {
                    _selectedGroupId.value = null
                    _effects.send(GroupEffect.GroupDeletedOrLeft)
                    _effects.send(GroupEffect.ShowToast("Vous avez quitté le groupe"))
                }
                .onFailure { err ->
                    _effects.send(GroupEffect.ShowToast(err.message ?: "Erreur"))
                }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            groupRepository.deleteGroup(groupId, user.id)
                .onSuccess {
                    _selectedGroupId.value = null
                    _effects.send(GroupEffect.GroupDeletedOrLeft)
                    _effects.send(GroupEffect.ShowToast("Groupe supprimé"))
                }
                .onFailure { err ->
                    _effects.send(GroupEffect.ShowToast(err.message ?: "Erreur"))
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

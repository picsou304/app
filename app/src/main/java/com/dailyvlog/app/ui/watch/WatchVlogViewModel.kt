package com.dailyvlog.app.ui.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyvlog.app.domain.model.Group
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.model.VlogComment
import com.dailyvlog.app.domain.model.VlogPost
import com.dailyvlog.app.domain.model.VlogReaction
import com.dailyvlog.app.domain.model.VlogView
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import com.dailyvlog.app.domain.repository.VlogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchVlogUiState(
    val vlog: VlogPost? = null,
    val group: Group? = null,
    val reactions: List<VlogReaction> = emptyList(),
    val comments: List<VlogComment> = emptyList(),
    val views: List<VlogView> = emptyList(),
    val streak: Int = 0,
    val currentUser: User? = null,
    val isPlaying: Boolean = true,
    val isMuted: Boolean = false,
    val commentInput: String = "",
    val isSubmittingComment: Boolean = false,
    val showCommentsSheet: Boolean = false,
    val showViewersSheet: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface WatchVlogEffect {
    data class ShowToast(val message: String) : WatchVlogEffect
    data object NavigateBack : WatchVlogEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchVlogViewModel @Inject constructor(
    private val vlogRepository: VlogRepository,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchVlogUiState())
    val uiState: StateFlow<WatchVlogUiState> = _uiState.asStateFlow()

    private val _effects = Channel<WatchVlogEffect>(Channel.BUFFERED)
    val effects: Flow<WatchVlogEffect> = _effects.receiveAsFlow()

    private val currentVlogId = MutableStateFlow<String?>(null)

    init {
        // Collect current user
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                checkAndRecordView()
            }
        }

        // Collect reactions for active vlog
        viewModelScope.launch {
            currentVlogId.flatMapLatest { id ->
                if (id != null) vlogRepository.getVlogReactions(id) else flowOf(emptyList())
            }.collect { list ->
                _uiState.update { it.copy(reactions = list) }
            }
        }

        // Collect comments for active vlog
        viewModelScope.launch {
            currentVlogId.flatMapLatest { id ->
                if (id != null) vlogRepository.getVlogComments(id) else flowOf(emptyList())
            }.collect { list ->
                _uiState.update { it.copy(comments = list) }
            }
        }

        // Collect views for active vlog
        viewModelScope.launch {
            currentVlogId.flatMapLatest { id ->
                if (id != null) vlogRepository.getVlogViews(id) else flowOf(emptyList())
            }.collect { list ->
                _uiState.update { it.copy(views = list) }
            }
        }
    }

    fun loadVlog(groupId: String, vlogId: String?) {
        _uiState.update { it.copy(isLoading = true) }

        // Collect group details
        viewModelScope.launch {
            groupRepository.getGroupDetails(groupId).collect { details ->
                _uiState.update { it.copy(group = details?.group) }
            }
        }

        // Collect streak
        viewModelScope.launch {
            vlogRepository.getGroupStreak(groupId).collect { streakVal ->
                _uiState.update { it.copy(streak = streakVal) }
            }
        }

        // Collect vlog
        viewModelScope.launch {
            val vlogFlow = if (vlogId != null) {
                vlogRepository.getVlogById(vlogId)
            } else {
                vlogRepository.getGroupVlogs(groupId).flatMapLatest { vlogs ->
                    flowOf(vlogs.firstOrNull())
                }
            }

            vlogFlow.collect { post ->
                currentVlogId.value = post?.id
                _uiState.update {
                    it.copy(
                        vlog = post,
                        isLoading = false,
                        errorMessage = if (post == null) "Aucun vlog disponible" else null
                    )
                }
                checkAndRecordView()
            }
        }
    }

    private fun checkAndRecordView() {
        val vlog = _uiState.value.vlog
        val user = _uiState.value.currentUser
        if (vlog != null && user != null) {
            viewModelScope.launch {
                vlogRepository.recordView(vlog.id, user)
            }
        }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleReaction(emoji: String) {
        val vlog = _uiState.value.vlog ?: return
        val user = _uiState.value.currentUser ?: return

        viewModelScope.launch {
            vlogRepository.toggleReaction(vlog.id, emoji, user)
        }
    }

    fun onCommentInputChanged(text: String) {
        _uiState.update { it.copy(commentInput = text) }
    }

    fun submitComment() {
        val text = _uiState.value.commentInput.trim()
        val vlog = _uiState.value.vlog ?: return
        val user = _uiState.value.currentUser ?: return
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingComment = true) }
            val result = vlogRepository.addComment(vlog.id, text, user)
            if (result.isSuccess) {
                _uiState.update { it.copy(commentInput = "", isSubmittingComment = false) }
            } else {
                _uiState.update { it.copy(isSubmittingComment = false) }
                _effects.send(
                    WatchVlogEffect.ShowToast(
                        result.exceptionOrNull()?.message ?: "Erreur d'envoi"
                    )
                )
            }
        }
    }

    fun setShowCommentsSheet(show: Boolean) {
        _uiState.update { it.copy(showCommentsSheet = show) }
    }

    fun setShowViewersSheet(show: Boolean) {
        _uiState.update { it.copy(showViewersSheet = show) }
    }
}

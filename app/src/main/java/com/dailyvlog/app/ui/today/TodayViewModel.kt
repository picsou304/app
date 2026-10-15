package com.dailyvlog.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyvlog.app.core.time.DebugClock
import com.dailyvlog.app.domain.model.DailyAssignment
import com.dailyvlog.app.domain.model.GroupWithMembers
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TodayUiState(
    val groupWithMembers: GroupWithMembers? = null,
    val assignment: DailyAssignment? = null,
    val currentUserId: String? = null,
    val remainingHours: Long = 0,
    val remainingMinutes: Long = 0,
    val isUserDesignated: Boolean = false,
    val clockOffsetDays: Long = 0,
    val simulatedTimeString: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface TodayEffect {
    data class NavigateToRecord(val groupId: String, val maxDuration: Int) : TodayEffect
    data class NavigateToWatchVlog(val groupId: String, val vlogId: String? = null) : TodayEffect
    data class ShowToast(val message: String) : TodayEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val dailyAssignmentRepository: DailyAssignmentRepository,
    private val authRepository: AuthRepository,
    private val debugClock: DebugClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = _effects.receiveAsFlow()

    private val _groupId = MutableStateFlow<String?>(null)

    private val timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    init {
        // Collect current user
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUserId = user?.id) }
                recomputeDesignatedStatus()
            }
        }

        // Collect group details & assignment reactively
        viewModelScope.launch {
            _groupId.flatMapLatest { gid ->
                if (gid != null) {
                    combine(
                        groupRepository.getGroupDetails(gid),
                        dailyAssignmentRepository.getTodayAssignment(gid),
                        debugClock.offsetFlow
                    ) { groupDetails, assignment, offsetMillis ->
                        Triple(groupDetails, assignment, offsetMillis)
                    }
                } else {
                    flowOf(Triple(null, null, 0L))
                }
            }.collect { (groupDetails, assignment, offsetMillis) ->
                val simulatedZdt = debugClock.currentZonedDateTime()
                val offsetDays = offsetMillis / (24L * 60L * 60L * 1000L)

                _uiState.update {
                    it.copy(
                        groupWithMembers = groupDetails,
                        assignment = assignment,
                        clockOffsetDays = offsetDays,
                        simulatedTimeString = simulatedZdt.format(timeFormatter)
                    )
                }
                recomputeCountdownAndDesignation()
            }
        }
    }

    fun loadGroup(groupId: String) {
        _groupId.value = groupId
    }

    fun skipTurn() {
        val gid = _groupId.value ?: return
        val currentUid = _uiState.value.currentUserId ?: return

        viewModelScope.launch {
            dailyAssignmentRepository.skipTurn(gid, currentUid)
                .onSuccess { newAssignment ->
                    _effects.send(TodayEffect.ShowToast("Tour passé avec succès ! Nouveau désigné : @${newAssignment.assignedUserPseudo}"))
                }
                .onFailure { err ->
                    _effects.send(TodayEffect.ShowToast(err.message ?: "Erreur lors du passage de tour"))
                }
        }
    }

    fun onRecordClicked() {
        val state = _uiState.value
        val group = state.groupWithMembers?.group ?: return
        if (state.isUserDesignated) {
            viewModelScope.launch {
                _effects.send(TodayEffect.NavigateToRecord(group.id, group.maxDurationSeconds))
            }
        } else {
            viewModelScope.launch {
                _effects.send(TodayEffect.ShowToast("Seul le membre désigné peut enregistrer le vlog aujourd'hui"))
            }
        }
    }

    fun onWatchVlogClicked() {
        val state = _uiState.value
        val group = state.groupWithMembers?.group ?: return
        viewModelScope.launch {
            _effects.send(TodayEffect.NavigateToWatchVlog(group.id, state.assignment?.vlogId))
        }
    }

    fun advanceDay() {
        debugClock.advanceDays(1)
        viewModelScope.launch {
            _effects.send(TodayEffect.ShowToast("+1 Jour simulé (Bascule effectuée)"))
        }
    }

    fun advanceHour() {
        debugClock.advanceHours(1)
        viewModelScope.launch {
            _effects.send(TodayEffect.ShowToast("+1 Heure simulée"))
        }
    }

    fun resetClock() {
        debugClock.reset()
        viewModelScope.launch {
            _effects.send(TodayEffect.ShowToast("Horloge réinitialisée à l'heure réelle"))
        }
    }

    private fun recomputeCountdownAndDesignation() {
        val state = _uiState.value
        val assignment = state.assignment
        val currentUserId = state.currentUserId

        if (assignment != null) {
            val nowMillis = debugClock.currentTimeMillis()
            val remainingMillis = (assignment.deadlineTimestamp - nowMillis).coerceAtLeast(0L)
            val remainingHours = remainingMillis / (1000L * 60L * 60L)
            val remainingMinutes = (remainingMillis % (1000L * 60L * 60L)) / (1000L * 60L)
            val isDesignated = currentUserId != null && assignment.assignedUserId == currentUserId

            _uiState.update {
                it.copy(
                    remainingHours = remainingHours,
                    remainingMinutes = remainingMinutes,
                    isUserDesignated = isDesignated
                )
            }
        }
    }

    private fun recomputeDesignatedStatus() {
        val state = _uiState.value
        val isDesignated = state.currentUserId != null &&
                state.assignment != null &&
                state.assignment.assignedUserId == state.currentUserId
        _uiState.update { it.copy(isUserDesignated = isDesignated) }
    }
}

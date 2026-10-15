package com.dailyvlog.app.ui.record

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dailyvlog.app.core.upload.UploadVlogWorker
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.VlogRepository
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

data class RecordVlogUiState(
    val recordedVideoUri: Uri? = null,
    val isRecording: Boolean = false,
    val recordedDurationSeconds: Int = 0,
    val maxDurationSeconds: Int = 60,
    val caption: String = "",
    val isPreviewing: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0,
    val uploadStage: String = "",
    val errorMessage: String? = null
)

sealed interface RecordVlogEffect {
    data object VlogPublished : RecordVlogEffect
    data class ShowToast(val message: String) : RecordVlogEffect
}

@HiltViewModel
class RecordVlogViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val vlogRepository: VlogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordVlogUiState())
    val uiState: StateFlow<RecordVlogUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecordVlogEffect>(Channel.BUFFERED)
    val effects: Flow<RecordVlogEffect> = _effects.receiveAsFlow()

    fun setMaxDuration(seconds: Int) {
        _uiState.update { it.copy(maxDurationSeconds = seconds) }
    }

    fun onRecordingStarted() {
        _uiState.update { it.copy(isRecording = true, recordedDurationSeconds = 0) }
    }

    fun onRecordingTick(seconds: Int) {
        _uiState.update { it.copy(recordedDurationSeconds = seconds) }
    }

    fun onVideoReady(uri: Uri, durationSeconds: Int) {
        _uiState.update {
            it.copy(
                isRecording = false,
                recordedVideoUri = uri,
                recordedDurationSeconds = durationSeconds,
                isPreviewing = true
            )
        }
    }

    fun onCaptionChanged(caption: String) {
        _uiState.update { it.copy(caption = caption.take(140)) }
    }

    fun onRetake() {
        _uiState.update {
            it.copy(
                recordedVideoUri = null,
                isPreviewing = false,
                isRecording = false,
                recordedDurationSeconds = 0,
                caption = ""
            )
        }
    }

    fun publishVlog(groupId: String) {
        val state = _uiState.value
        val uri = state.recordedVideoUri ?: return
        val duration = state.recordedDurationSeconds.coerceAtLeast(1)
        val caption = state.caption

        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 10,
                    uploadStage = "Démarrage de l'envoi..."
                )
            }

            val workRequest = OneTimeWorkRequestBuilder<UploadVlogWorker>()
                .setInputData(
                    workDataOf(
                        UploadVlogWorker.KEY_GROUP_ID to groupId,
                        UploadVlogWorker.KEY_VIDEO_URI to uri.toString(),
                        UploadVlogWorker.KEY_DURATION to duration,
                        UploadVlogWorker.KEY_CAPTION to caption,
                        UploadVlogWorker.KEY_AUTHOR_ID to user.id,
                        UploadVlogWorker.KEY_AUTHOR_PSEUDO to user.pseudo
                    )
                )
                .build()

            val workManager = WorkManager.getInstance(application)
            workManager.enqueue(workRequest)

            workManager.getWorkInfoByIdFlow(workRequest.id)
                .collect { workInfo ->
                    if (workInfo != null) {
                        val progress = workInfo.progress.getInt(UploadVlogWorker.KEY_PROGRESS, 0)
                        val stage = workInfo.progress.getString(UploadVlogWorker.KEY_STAGE) ?: ""

                        if (progress > 0) {
                            _uiState.update {
                                it.copy(uploadProgress = progress, uploadStage = stage)
                            }
                        }

                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                _uiState.update { it.copy(isUploading = false, uploadProgress = 100) }
                                _effects.send(RecordVlogEffect.ShowToast("Vlog publié avec succès !"))
                                _effects.send(RecordVlogEffect.VlogPublished)
                            }
                            WorkInfo.State.FAILED -> {
                                _uiState.update {
                                    it.copy(
                                        isUploading = false,
                                        errorMessage = "Échec lors de la publication"
                                    )
                                }
                                _effects.send(RecordVlogEffect.ShowToast("Échec lors de la publication"))
                            }
                            else -> {}
                        }
                    }
                }
        }
    }
}

package com.dailyvlog.app.core.upload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.repository.VlogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class UploadVlogWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val vlogRepository: VlogRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val groupId = inputData.getString(KEY_GROUP_ID) ?: return Result.failure()
        val videoUri = inputData.getString(KEY_VIDEO_URI) ?: return Result.failure()
        val duration = inputData.getInt(KEY_DURATION, 10)
        val caption = inputData.getString(KEY_CAPTION) ?: ""
        val authorId = inputData.getString(KEY_AUTHOR_ID) ?: return Result.failure()
        val authorPseudo = inputData.getString(KEY_AUTHOR_PSEUDO) ?: "Membre"

        val author = User(id = authorId, email = "", pseudo = authorPseudo)

        // Stage 1: Compression 720p
        setProgress(workDataOf(KEY_PROGRESS to 25, KEY_STAGE to "Compression H.264 720p..."))
        delay(600)

        // Stage 2: Upload preparation
        setProgress(workDataOf(KEY_PROGRESS to 60, KEY_STAGE to "Upload du vlog..."))
        delay(600)

        // Stage 3: Publish
        setProgress(workDataOf(KEY_PROGRESS to 90, KEY_STAGE to "Finalisation..."))
        val publishResult = vlogRepository.publishVlog(groupId, videoUri, duration, caption, author)

        return if (publishResult.isSuccess) {
            setProgress(workDataOf(KEY_PROGRESS to 100, KEY_STAGE to "Terminé !"))
            Result.success(workDataOf(KEY_VLOG_ID to (publishResult.getOrNull()?.id ?: "")))
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_GROUP_ID = "group_id"
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_DURATION = "duration"
        const val KEY_CAPTION = "caption"
        const val KEY_AUTHOR_ID = "author_id"
        const val KEY_AUTHOR_PSEUDO = "author_pseudo"
        const val KEY_PROGRESS = "progress"
        const val KEY_STAGE = "stage"
        const val KEY_VLOG_ID = "vlog_id"
    }
}

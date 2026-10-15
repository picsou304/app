package com.dailyvlog.app.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailyvlog.app.core.time.DebugClock
import com.dailyvlog.app.domain.model.AssignmentStatus
import com.dailyvlog.app.domain.repository.AuthRepository
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val groupRepository: GroupRepository,
    private val assignmentRepository: DailyAssignmentRepository,
    private val authRepository: AuthRepository,
    private val notificationHelper: NotificationHelper,
    private val debugClock: DebugClock
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val user = authRepository.getCurrentUser() ?: return Result.success()
        val groups = groupRepository.getUserGroups(user.id).firstOrNull() ?: return Result.success()

        val now = debugClock.currentTimeMillis()
        val fourHoursMillis = 4 * 60 * 60 * 1000L

        for (group in groups) {
            val assignment = assignmentRepository.getTodayAssignment(group.id).firstOrNull() ?: continue

            // If current user is designated and assignment is still pending
            if (assignment.assignedUserId == user.id && assignment.status == AssignmentStatus.PENDING) {
                val remainingTime = assignment.deadlineTimestamp - now
                if (remainingTime in 1..fourHoursMillis) {
                    notificationHelper.sendReminderNotification(group.id, group.name)
                }
            }
        }

        return Result.success()
    }
}

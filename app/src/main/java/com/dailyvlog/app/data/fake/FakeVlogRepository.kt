package com.dailyvlog.app.data.fake

import com.dailyvlog.app.core.time.DebugClock
import com.dailyvlog.app.domain.engine.DailyPickerEngine
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.model.VlogPost
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import com.dailyvlog.app.domain.repository.VlogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeVlogRepository @Inject constructor(
    private val groupRepository: GroupRepository,
    private val dailyAssignmentRepository: DailyAssignmentRepository,
    private val debugClock: DebugClock
) : VlogRepository {

    private val postsFlow = MutableStateFlow<List<VlogPost>>(emptyList())

    override fun getTodayVlog(groupId: String, date: String): Flow<VlogPost?> {
        return postsFlow.map { posts ->
            posts.find { it.groupId == groupId && it.date == date }
        }
    }

    override fun getGroupVlogs(groupId: String): Flow<List<VlogPost>> {
        return postsFlow.map { posts ->
            posts.filter { it.groupId == groupId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun publishVlog(
        groupId: String,
        videoUri: String,
        durationSeconds: Int,
        caption: String,
        author: User
    ): Result<VlogPost> {
        val groupDetails = groupRepository.getGroupDetails(groupId).firstOrNull()
            ?: return Result.failure(Exception("Groupe introuvable"))

        val now = debugClock.currentInstant()
        val group = groupDetails.group
        val activeDate = DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
        val activeDateStr = activeDate.format(DailyPickerEngine.DATE_FORMATTER)

        val newPost = VlogPost(
            id = "vlog-${UUID.randomUUID().toString().take(8)}",
            groupId = groupId,
            authorUserId = author.id,
            authorPseudo = author.pseudo,
            authorAvatarUrl = author.avatarUrl,
            date = activeDateStr,
            videoUri = videoUri,
            durationSeconds = durationSeconds,
            caption = caption.take(140),
            createdAt = debugClock.currentTimeMillis()
        )

        postsFlow.value = postsFlow.value + newPost

        // Mark the assignment as posted in the assignment repository
        dailyAssignmentRepository.markVlogPosted(groupId, newPost.id)

        return Result.success(newPost)
    }

    override suspend fun deleteVlog(vlogId: String, requesterUserId: String): Result<Unit> {
        val current = postsFlow.value
        val post = current.find { it.id == vlogId }
            ?: return Result.failure(Exception("Vlog introuvable"))

        if (post.authorUserId != requesterUserId) {
            return Result.failure(Exception("Vous ne pouvez supprimer que vos propres vlogs"))
        }

        postsFlow.value = current.filterNot { it.id == vlogId }
        return Result.success(Unit)
    }
}

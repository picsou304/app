package com.dailyvlog.app.data.fake

import com.dailyvlog.app.core.time.DebugClock
import com.dailyvlog.app.domain.engine.DailyPickerEngine
import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.model.VlogComment
import com.dailyvlog.app.domain.model.VlogPost
import com.dailyvlog.app.domain.model.VlogReaction
import com.dailyvlog.app.domain.model.VlogView
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import com.dailyvlog.app.domain.repository.VlogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
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
    private val reactionsFlow = MutableStateFlow<List<VlogReaction>>(emptyList())
    private val commentsFlow = MutableStateFlow<List<VlogComment>>(emptyList())
    private val viewsFlow = MutableStateFlow<List<VlogView>>(emptyList())

    override fun getTodayVlog(groupId: String, date: String): Flow<VlogPost?> {
        return postsFlow.map { posts ->
            posts.find { it.groupId == groupId && it.date == date }
        }
    }

    override fun getVlogById(vlogId: String): Flow<VlogPost?> {
        return postsFlow.map { posts ->
            posts.find { it.id == vlogId }
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
        reactionsFlow.value = reactionsFlow.value.filterNot { it.vlogId == vlogId }
        commentsFlow.value = commentsFlow.value.filterNot { it.vlogId == vlogId }
        viewsFlow.value = viewsFlow.value.filterNot { it.vlogId == vlogId }
        return Result.success(Unit)
    }

    override fun getVlogReactions(vlogId: String): Flow<List<VlogReaction>> {
        return reactionsFlow.map { list ->
            list.filter { it.vlogId == vlogId }
        }
    }

    override suspend fun toggleReaction(vlogId: String, emoji: String, user: User): Result<Unit> {
        val current = reactionsFlow.value
        val existing = current.find { it.vlogId == vlogId && it.userId == user.id && it.emoji == emoji }

        if (existing != null) {
            reactionsFlow.value = current - existing
        } else {
            val reaction = VlogReaction(
                id = "react-${UUID.randomUUID().toString().take(8)}",
                vlogId = vlogId,
                userId = user.id,
                userPseudo = user.pseudo,
                emoji = emoji,
                createdAt = debugClock.currentTimeMillis()
            )
            reactionsFlow.value = current + reaction
        }
        return Result.success(Unit)
    }

    override fun getVlogComments(vlogId: String): Flow<List<VlogComment>> {
        return commentsFlow.map { list ->
            list.filter { it.vlogId == vlogId }.sortedBy { it.createdAt }
        }
    }

    override suspend fun addComment(vlogId: String, text: String, author: User): Result<VlogComment> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return Result.failure(Exception("Le commentaire ne peut pas être vide"))
        }

        val comment = VlogComment(
            id = "comment-${UUID.randomUUID().toString().take(8)}",
            vlogId = vlogId,
            authorUserId = author.id,
            authorPseudo = author.pseudo,
            authorAvatarUrl = author.avatarUrl,
            text = cleanText,
            createdAt = debugClock.currentTimeMillis()
        )

        commentsFlow.value = commentsFlow.value + comment
        return Result.success(comment)
    }

    override fun getVlogViews(vlogId: String): Flow<List<VlogView>> {
        return viewsFlow.map { list ->
            list.filter { it.vlogId == vlogId }
        }
    }

    override suspend fun recordView(vlogId: String, user: User): Result<Unit> {
        val current = viewsFlow.value
        val alreadyViewed = current.any { it.vlogId == vlogId && it.userId == user.id }
        if (!alreadyViewed) {
            val view = VlogView(
                vlogId = vlogId,
                userId = user.id,
                userPseudo = user.pseudo,
                viewedAt = debugClock.currentTimeMillis()
            )
            viewsFlow.value = current + view

            // Update view count in postsFlow
            postsFlow.value = postsFlow.value.map { post ->
                if (post.id == vlogId) post.copy(viewCount = post.viewCount + 1) else post
            }
        }
        return Result.success(Unit)
    }

    override fun getGroupStreak(groupId: String): Flow<Int> {
        return combine(
            postsFlow,
            debugClock.offsetFlow,
            groupRepository.getGroupDetails(groupId)
        ) { posts, _, groupDetails ->
            val groupVlogs = posts.filter { it.groupId == groupId }
            if (groupVlogs.isEmpty()) return@combine 0

            val datesSet = groupVlogs.mapNotNull {
                try {
                    LocalDate.parse(it.date, DailyPickerEngine.DATE_FORMATTER)
                } catch (_: Exception) {
                    null
                }
            }.toSet()

            val now = debugClock.currentInstant()
            val group = groupDetails?.group
            val activeDate = if (group != null) {
                DailyPickerEngine.getActiveDate(now, group.switchTime, group.timezone)
            } else {
                LocalDate.now()
            }

            var streak = 0
            var checkDate = if (activeDate in datesSet) activeDate else activeDate.minusDays(1)

            while (checkDate in datesSet) {
                streak++
                checkDate = checkDate.minusDays(1)
            }

            streak
        }
    }
}

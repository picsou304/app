package com.dailyvlog.app.domain.repository

import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.model.VlogComment
import com.dailyvlog.app.domain.model.VlogPost
import com.dailyvlog.app.domain.model.VlogReaction
import com.dailyvlog.app.domain.model.VlogView
import kotlinx.coroutines.flow.Flow

interface VlogRepository {
    fun getTodayVlog(groupId: String, date: String): Flow<VlogPost?>
    fun getVlogById(vlogId: String): Flow<VlogPost?>
    fun getGroupVlogs(groupId: String): Flow<List<VlogPost>>
    suspend fun publishVlog(
        groupId: String,
        videoUri: String,
        durationSeconds: Int,
        caption: String,
        author: User
    ): Result<VlogPost>
    suspend fun deleteVlog(vlogId: String, requesterUserId: String): Result<Unit>

    // Milestone M5: Reactions, Comments, Views, Streak
    fun getVlogReactions(vlogId: String): Flow<List<VlogReaction>>
    suspend fun toggleReaction(vlogId: String, emoji: String, user: User): Result<Unit>

    fun getVlogComments(vlogId: String): Flow<List<VlogComment>>
    suspend fun addComment(vlogId: String, text: String, author: User): Result<VlogComment>

    fun getVlogViews(vlogId: String): Flow<List<VlogView>>
    suspend fun recordView(vlogId: String, user: User): Result<Unit>

    fun getGroupStreak(groupId: String): Flow<Int>
}

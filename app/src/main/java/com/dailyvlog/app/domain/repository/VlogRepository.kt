package com.dailyvlog.app.domain.repository

import com.dailyvlog.app.domain.model.User
import com.dailyvlog.app.domain.model.VlogPost
import kotlinx.coroutines.flow.Flow

interface VlogRepository {
    fun getTodayVlog(groupId: String, date: String): Flow<VlogPost?>
    fun getGroupVlogs(groupId: String): Flow<List<VlogPost>>
    suspend fun publishVlog(
        groupId: String,
        videoUri: String,
        durationSeconds: Int,
        caption: String,
        author: User
    ): Result<VlogPost>
    suspend fun deleteVlog(vlogId: String, requesterUserId: String): Result<Unit>
}

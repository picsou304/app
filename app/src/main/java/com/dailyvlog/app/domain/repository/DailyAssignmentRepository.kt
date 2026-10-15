package com.dailyvlog.app.domain.repository

import com.dailyvlog.app.domain.model.DailyAssignment
import kotlinx.coroutines.flow.Flow

interface DailyAssignmentRepository {
    fun getTodayAssignment(groupId: String): Flow<DailyAssignment?>
    fun getYesterdayAssignment(groupId: String): Flow<DailyAssignment?>
    suspend fun skipTurn(groupId: String, userId: String): Result<DailyAssignment>
    suspend fun markVlogPosted(groupId: String, vlogId: String): Result<DailyAssignment>
    suspend fun markDayMissed(groupId: String): Result<DailyAssignment>
}

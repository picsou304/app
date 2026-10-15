package com.dailyvlog.app.domain.repository

import com.dailyvlog.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    
    suspend fun getCurrentUser(): User?
    
    suspend fun signIn(email: String, password: String): Result<User>
    
    suspend fun signUp(email: String, password: String, pseudo: String): Result<User>
    
    suspend fun signOut(): Result<Unit>
    
    suspend fun updateProfile(pseudo: String, avatarUrl: String?): Result<User>
}

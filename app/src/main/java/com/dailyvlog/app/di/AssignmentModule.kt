package com.dailyvlog.app.di

import com.dailyvlog.app.data.fake.FakeDailyAssignmentRepository
import com.dailyvlog.app.domain.repository.DailyAssignmentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AssignmentModule {

    @Binds
    @Singleton
    abstract fun bindDailyAssignmentRepository(
        fakeRepository: FakeDailyAssignmentRepository
    ): DailyAssignmentRepository
}

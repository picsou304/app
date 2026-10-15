package com.dailyvlog.app.di

import com.dailyvlog.app.data.fake.FakeGroupRepository
import com.dailyvlog.app.domain.repository.GroupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GroupModule {

    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        fakeGroupRepository: FakeGroupRepository
    ): GroupRepository
}

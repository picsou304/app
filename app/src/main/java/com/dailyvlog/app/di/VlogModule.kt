package com.dailyvlog.app.di

import com.dailyvlog.app.data.fake.FakeVlogRepository
import com.dailyvlog.app.domain.repository.VlogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VlogModule {

    @Binds
    @Singleton
    abstract fun bindVlogRepository(fakeRepository: FakeVlogRepository): VlogRepository
}

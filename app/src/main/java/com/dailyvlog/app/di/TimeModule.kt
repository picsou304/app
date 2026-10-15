package com.dailyvlog.app.di

import com.dailyvlog.app.core.time.AppClock
import com.dailyvlog.app.core.time.DebugClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {

    @Binds
    @Singleton
    abstract fun bindAppClock(debugClock: DebugClock): AppClock
}

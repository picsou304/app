package com.dailyvlog.app.core.time

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugClock @Inject constructor() : AppClock {

    private val offsetMillis = AtomicLong(0L)
    private val _offsetFlow = MutableStateFlow(0L)
    val offsetFlow: StateFlow<Long> = _offsetFlow.asStateFlow()

    override fun currentTimeMillis(): Long = System.currentTimeMillis() + offsetMillis.get()

    fun advanceDays(days: Long) {
        advanceMillis(days * 24L * 60L * 60L * 1000L)
    }

    fun advanceHours(hours: Long) {
        advanceMillis(hours * 60L * 60L * 1000L)
    }

    fun advanceMinutes(minutes: Long) {
        advanceMillis(minutes * 60L * 1000L)
    }

    fun advanceMillis(millis: Long) {
        val newOffset = offsetMillis.addAndGet(millis)
        _offsetFlow.value = newOffset
    }

    fun setTime(epochMillis: Long) {
        val newOffset = epochMillis - System.currentTimeMillis()
        offsetMillis.set(newOffset)
        _offsetFlow.value = newOffset
    }

    fun reset() {
        offsetMillis.set(0L)
        _offsetFlow.value = 0L
    }

    fun getOffsetMillis(): Long = offsetMillis.get()
}

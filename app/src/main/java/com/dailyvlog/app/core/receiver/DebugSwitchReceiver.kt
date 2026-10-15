package com.dailyvlog.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailyvlog.app.core.time.DebugClock
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DebugSwitchReceiver : BroadcastReceiver() {

    @Inject
    lateinit var debugClock: DebugClock

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_TRIGGER_SWITCH) {
            val advanceDays = intent.getLongExtra(EXTRA_ADVANCE_DAYS, 1L)
            val advanceHours = intent.getLongExtra(EXTRA_ADVANCE_HOURS, 0L)

            if (advanceHours > 0) {
                debugClock.advanceHours(advanceHours)
            } else {
                debugClock.advanceDays(advanceDays)
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_SWITCH = "com.dailyvlog.app.ACTION_TRIGGER_SWITCH"
        const val EXTRA_ADVANCE_DAYS = "advance_days"
        const val EXTRA_ADVANCE_HOURS = "advance_hours"
    }
}

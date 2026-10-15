package com.dailyvlog.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dailyvlog.app.MainActivity
import com.dailyvlog.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_TURN = "daily_turn_channel"
        const val CHANNEL_REMINDERS = "vlog_reminders_channel"
        const val CHANNEL_SOCIAL = "group_updates_channel"

        const val ACTION_OPEN_RECORD = "OPEN_RECORD"
        const val ACTION_OPEN_WATCH = "OPEN_WATCH"
        const val ACTION_OPEN_TODAY = "OPEN_TODAY"

        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_VLOG_ID = "extra_vlog_id"
        const val EXTRA_MAX_DURATION = "extra_max_duration"

        const val NOTIFICATION_ID_TURN = 1001
        const val NOTIFICATION_ID_REMINDER = 1002
        const val NOTIFICATION_ID_NEW_VLOG = 1003
        const val NOTIFICATION_ID_MISSED = 1004
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val turnChannel = NotificationChannel(
                CHANNEL_TURN,
                "Mon tour de vlogger 🎬",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifie quand c'est ton tour d'enregistrer le vlog quotidien"
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Rappels de vlog ⏰",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rappels 4h avant la date limite pour ne pas briser la streak"
                enableVibration(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL,
                "Mises à jour du groupe 🎉",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nouveaux vlogs, réactions, commentaires et invitations"
            }

            notificationManager.createNotificationChannels(listOf(turnChannel, reminderChannel, socialChannel))
        }
    }

    fun sendTurnNotification(groupId: String, groupName: String, maxDurationSeconds: Int = 60) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", ACTION_OPEN_RECORD)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_MAX_DURATION, maxDurationSeconds)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TURN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TURN)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("C'est ton tour aujourd'hui ! 🎬")
            .setContentText("C'est à toi de capturer le vlog du groupe \"$groupName\" !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TURN, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun sendReminderNotification(groupId: String, groupName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", ACTION_OPEN_TODAY)
            putExtra(EXTRA_GROUP_ID, groupId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Plus que 4h pour enregistrer !")
            .setContentText("Ne brise pas la streak du groupe \"$groupName\" !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, notification)
        } catch (_: SecurityException) {
        }
    }

    fun sendNewVlogNotification(groupId: String, groupName: String, authorPseudo: String, vlogId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", ACTION_OPEN_WATCH)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_VLOG_ID, vlogId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_NEW_VLOG,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("🎉 Nouveau vlog en ligne !")
            .setContentText("@$authorPseudo a publié son vlog dans \"$groupName\". Regarde-le !")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NEW_VLOG, notification)
        } catch (_: SecurityException) {
        }
    }

    fun sendMissedDayNotification(groupId: String, groupName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", ACTION_OPEN_TODAY)
            putExtra(EXTRA_GROUP_ID, groupId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MISSED,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Jour raté 😴")
            .setContentText("Aucun vlog n'a été publié hier dans \"$groupName\". La streak a pris fin.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MISSED, notification)
        } catch (_: SecurityException) {
        }
    }
}

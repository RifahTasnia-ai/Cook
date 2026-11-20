package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object TurboNotificationManager {

    const val EXTRA_NAV_ACTION = "turbo_nav_action"
    const val ACTION_OPEN_AUDIT = "open_audit"
    const val ACTION_OPEN_TIMER = "open_timer"
    const val ACTION_FINISH_BLOCK = "finish_block"
    const val ACTION_OPEN_SILENCE = "open_silence"
    const val ACTION_START_DAY = "start_day"

    // Versioned channel IDs guarantee IMPORTANCE_DEFAULT takes effect even after prior installs
    private const val CHANNEL_AUDIT = "turbo_audit_channel_v2"
    private const val CHANNEL_BLOCK = "turbo_active_block_v2"
    private const val CHANNEL_SILENCE = "turbo_silence_reset_v2"
    private const val CHANNEL_WAKEUP = "turbo_wakeup_channel_v2"

    private const val ID_AUDIT = 1001
    private const val ID_BLOCK = 1002
    private const val ID_SILENCE = 1003
    private const val ID_WAKEUP = 1004

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_AUDIT,
                "3-Hour Turbo Audit",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mandatory 3-hour accountability check-in call alerts."
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_BLOCK,
                "Active Time Block",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Live ongoing countdown for active Turbo Time Blocks."
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_SILENCE,
                "121s Silence Reset",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Live lock-screen stillness countdown after focus blocks."
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                CHANNEL_WAKEUP,
                "Wake-Up Task Trigger",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning trigger to activate the 3-hour audit engine."
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun launchPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_ACTION, action)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postAuditNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val answerIntent = launchPendingIntent(context, ACTION_OPEN_AUDIT, 201)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUDIT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming Turbo Audit")
            .setContentText("3-hour accountability check-in • Answer 3 core questions (+5 pts)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(answerIntent)
            .addAction(0, "Answer Audit", answerIntent)
            .setAutoCancel(true)
            .build()
        return safeNotify(context, ID_AUDIT, notification)
    }

    fun postActiveBlockNotification(
        context: Context,
        tierTitle: String,
        taskTitle: String,
        endEpochMillis: Long,
        progressPercent: Int
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val openIntent = launchPendingIntent(context, ACTION_OPEN_TIMER, 202)
        val finishIntent = launchPendingIntent(context, ACTION_FINISH_BLOCK, 203)
        val builder = NotificationCompat.Builder(context, CHANNEL_BLOCK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$tierTitle • Active Time Block")
            .setContentText(taskTitle)
            .setWhen(endEpochMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent)
            .addAction(0, "Open Timer", openIntent)
            .addAction(0, "Finish Block", finishIntent)
        return safeNotify(context, ID_BLOCK, builder.build())
    }

    fun postSilenceResetNotification(
        context: Context,
        endEpochMillis: Long
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val openSilenceIntent = launchPendingIntent(context, ACTION_OPEN_SILENCE, 205)
        val notification = NotificationCompat.Builder(context, CHANNEL_SILENCE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("121s Silence Reset")
            .setContentText("No phone or electronics • Alone in silence")
            .setWhen(endEpochMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openSilenceIntent)
            .build()
        return safeNotify(context, ID_SILENCE, notification)
    }

    fun postWakeUpNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val startDayIntent = launchPendingIntent(context, ACTION_START_DAY, 204)
        val notification = NotificationCompat.Builder(context, CHANNEL_WAKEUP)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Turbo Mode • Wake-Up Task")
            .setContentText("Tap once to activate your 3-hour audit engine")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(startDayIntent)
            .addAction(0, "Start Day", startDayIntent)
            .setAutoCancel(true)
            .build()
        return safeNotify(context, ID_WAKEUP, notification)
    }

    fun cancelActiveBlock(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_BLOCK)
    }

    fun cancelSilenceReset(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_SILENCE)
    }

    fun cancelAudit(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_AUDIT)
    }

    private fun safeNotify(context: Context, id: Int, notification: Notification): Boolean {
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

package com.example.notifications

import android.Manifest
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
    const val ACTION_START_DAY = "start_day"

    private const val CHANNEL_AUDIT = "turbo_audit_channel"
    private const val CHANNEL_BLOCK = "turbo_active_block_channel"
    private const val CHANNEL_SILENCE = "turbo_silence_reset_channel"
    private const val CHANNEL_WAKEUP = "turbo_wakeup_channel"

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
                description = "Mandatory 3-hour audit check-in reminders."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_BLOCK,
                "Active Time Block",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent ongoing countdown for active Turbo Time Blocks."
                setSound(null, null)
            },
            NotificationChannel(
                CHANNEL_SILENCE,
                "121s Silence Reset",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lock-screen stillness countdown without buttons."
                setSound(null, null)
            },
            NotificationChannel(
                CHANNEL_WAKEUP,
                "Wake-Up Task Trigger",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning trigger for Time 1 Check-In and 3-hour audit cadence."
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

    fun postAuditNotification(context: Context, nodeTitle: String): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val answerIntent = launchPendingIntent(context, ACTION_OPEN_AUDIT, 201)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUDIT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming Turbo Audit")
            .setContentText("$nodeTitle • 3-hour checkpoint")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(answerIntent)
            .addAction(0, "Answer Audit", answerIntent)
            .setAutoCancel(true)
            .build()
        return safeNotify(context, ID_AUDIT, notification)
    }

    fun postActiveBlockNotification(
        context: Context,
        badge: String,
        title: String,
        remainingFormatted: String,
        progressPercent: Int
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val openIntent = launchPendingIntent(context, ACTION_OPEN_TIMER, 202)
        val finishIntent = launchPendingIntent(context, ACTION_FINISH_BLOCK, 203)
        val notification = NotificationCompat.Builder(context, CHANNEL_BLOCK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$badge • $remainingFormatted")
            .setContentText(title)
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, "Open Timer", openIntent)
            .addAction(0, "Finish Block", finishIntent)
            .build()
        return safeNotify(context, ID_BLOCK, notification)
    }

    fun postSilenceResetNotification(context: Context, remainingFormatted: String): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_SILENCE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("121s Silence Reset • $remainingFormatted")
            .setContentText("No phone or electronics • Alone in silence")
            .setOngoing(true)
            .setSilent(true)
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
            .setContentText("Tap once to start Time 1 Check-In & 3-hour audit engine")
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

    private fun safeNotify(context: Context, id: Int, notification: android.app.Notification): Boolean {
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

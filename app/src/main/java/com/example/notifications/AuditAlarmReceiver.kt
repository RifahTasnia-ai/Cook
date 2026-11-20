package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

object AuditAlarmScheduler {

    const val THREE_HOURS_MILLIS: Long = 3L * 60L * 60L * 1000L
    private const val PREFS_NAME = "turbo_mode_audit_alarm_prefs"
    private const val KEY_WAKE_UP_ACTIVE = "wake_up_active"
    private const val KEY_SCHEDULED_AT_MILLIS = "scheduled_at_millis"
    private const val REQUEST_CODE_AUDIT_ALARM = 3101

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, AuditAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE_AUDIT_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun isWakeUpActive(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WAKE_UP_ACTIVE, false)
    }

    fun scheduleNextAudit(context: Context, triggerAtMillis: Long) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val safeTriggerAt = if (triggerAtMillis <= now + 5_000L) {
            now + THREE_HOURS_MILLIS
        } else {
            triggerAtMillis
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyActive = prefs.getBoolean(KEY_WAKE_UP_ACTIVE, false)
        val existingScheduled = prefs.getLong(KEY_SCHEDULED_AT_MILLIS, 0L)
        if (alreadyActive && kotlin.math.abs(existingScheduled - safeTriggerAt) < 15_000L) {
            return
        }

        prefs.edit()
            .putBoolean(KEY_WAKE_UP_ACTIVE, true)
            .putLong(KEY_SCHEDULED_AT_MILLIS, safeTriggerAt)
            .apply()

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = alarmPendingIntent(appContext)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            safeTriggerAt,
            pendingIntent
        )
    }

    fun cancelAudit(context: Context) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WAKE_UP_ACTIVE, false)
            .remove(KEY_SCHEDULED_AT_MILLIS)
            .apply()

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(alarmPendingIntent(appContext))
    }
}

class AuditAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!AuditAlarmScheduler.isWakeUpActive(context)) return
        TurboNotificationManager.postAuditNotification(context)
        val nextTrigger = System.currentTimeMillis() + AuditAlarmScheduler.THREE_HOURS_MILLIS
        AuditAlarmScheduler.scheduleNextAudit(context, nextTrigger)
    }
}

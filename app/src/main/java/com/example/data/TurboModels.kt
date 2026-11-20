package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PriorityTier(
    val code: String,
    val multiplier: Double,
    val multiplierLabel: String,
    val sectionTitle: String,
    val selectorSubtitle: String
) {
    S("S", 1.5, "1.5x", "Most Critical", "Top focus · Max 1"),
    A("A", 1.0, "1.0x", "Most Priority", "Core execution"),
    B("B", 0.75, "0.75x", "Priority", "High value"),
    C("C", 0.5, "0.5x", "Standard", "Routine work"),
    D("D", 0.0, "0x", "Delegate", "Assign out"),
    E("E", 0.0, "0x", "Eliminate", "Drop task");

    val isExecutableInBlock: Boolean
        get() = this == S || this == A || this == B || this == C

    val selectorTitle: String
        get() = if (isExecutableInBlock) "$sectionTitle · $multiplierLabel" else sectionTitle

    companion object {
        fun fromCode(raw: String): PriorityTier =
            entries.firstOrNull { it.code == raw } ?: A
    }
}

enum class DayState(val code: String) {
    WAKE_UP("WAKE_UP"),
    SLEEP("SLEEP");

    companion object {
        fun fromCode(raw: String): DayState =
            entries.firstOrNull { it.code == raw } ?: WAKE_UP
    }
}

@Keep
@IgnoreExtraProperties
data class PriorityTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val tier: String = "A",
    val orderIndex: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun tierEnum(): PriorityTier = PriorityTier.fromCode(tier)
}

@Keep
@IgnoreExtraProperties
data class DailyLog(
    val userId: String = "",
    val dateKey: String = todayDateKey(),
    val dayState: String = DayState.WAKE_UP.code,
    val totalScore: Double = 0.0,
    val s1CompletedCount: Long = 0L,
    val abCompletedCount: Long = 0L,
    val checkInPoints: Long = 0L,
    val trackedMinutes: Long = 0L,
    val completedNodes: List<Long> = emptyList(),
    val nextAuditEpochMillis: Long = System.currentTimeMillis() + 3L * 60L * 60L * 1000L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun dayStateEnum(): DayState = DayState.fromCode(dayState)

    fun nextCheckInSlot(): Int {
        val done = completedNodes.map { it.toInt() }.toSet()
        for (i in 1..7) {
            if (i !in done) return i
        }
        return 7
    }
}

@Keep
@IgnoreExtraProperties
data class TimeBlockRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val taskId: String = "",
    val taskBadge: String = "",
    val taskTitle: String = "",
    val tier: String = "A",
    val startTimeLabel: String = "",
    val durationMinutes: Long = 45L,
    val qualityRate: Long = 8L,
    val finishingRate: Long = 4L,
    val actionRate: Long = 16L,
    val multiplier: Double = 1.0,
    val earnedPoints: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val rawScore: Long
        get() = qualityRate + finishingRate + actionRate

    fun tierEnum(): PriorityTier = PriorityTier.fromCode(tier)
}

@Keep
@IgnoreExtraProperties
data class CheckInRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val nodeIndex: Long = 1L,
    val creativeIq: String = "",
    val emotionEq: String = "",
    val bestThinking: String = "",
    val dailySevenPoint: String = "",
    val durationSeconds: Long = 0L,
    val pointsAwarded: Long = 5L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DisplayTaskItem(
    val task: PriorityTask,
    val badge: String,
    val multiplierLabel: String
) {
    val tier: PriorityTier get() = task.tierEnum()
    val sectionTitle: String get() = tier.sectionTitle
}

fun buildDisplayTasks(tasks: List<PriorityTask>): List<DisplayTaskItem> {
    val counters = mutableMapOf<PriorityTier, Int>()
    val tierOrder = mapOf(
        PriorityTier.S to 0,
        PriorityTier.A to 1,
        PriorityTier.B to 2,
        PriorityTier.C to 3,
        PriorityTier.D to 4,
        PriorityTier.E to 5
    )
    return tasks
        .sortedWith(compareBy<PriorityTask> { tierOrder[it.tierEnum()] ?: 9 }.thenBy { it.orderIndex })
        .map { task ->
            val tier = task.tierEnum()
            val idx = (counters[tier] ?: 0) + 1
            counters[tier] = idx
            DisplayTaskItem(
                task = task,
                badge = "${tier.code}-$idx",
                multiplierLabel = tier.multiplierLabel
            )
        }
}

fun todayDateKey(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date())
}

fun currentTimeHhMm(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.US)
    return formatter.format(Date())
}

fun formatTimestampHhMm(timestamp: Timestamp?): String {
    if (timestamp == null) return currentTimeHhMm()
    val formatter = SimpleDateFormat("HH:mm", Locale.US)
    return formatter.format(timestamp.toDate())
}

fun currentDateSubtitle(): String {
    val formatter = SimpleDateFormat("EEE, MMM d", Locale.US)
    return formatter.format(Date())
}

fun formatMmSs(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

fun formatTrackedDuration(totalMinutes: Long): String {
    val safe = totalMinutes.coerceAtLeast(0L)
    val h = safe / 60L
    val m = safe % 60L
    return if (h > 0L) "${h}h ${m}m" else "${m}m"
}

fun formatScoreValue(score: Double): String {
    return if (score % 1.0 == 0.0) {
        score.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", score)
    }
}

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
    val dropdownLabel: String
) {
    S("S", 1.5, "1.5x", "S Tier (1.5x) — Primary Deliverable (Max 1)"),
    A("A", 1.0, "1.0x", "A Tier (1.0x) — Must Do"),
    B("B", 0.75, "0.75x", "B Tier (0.75x) — Should Do"),
    C("C", 0.5, "0.5x", "C Tier (0.5x) — Nice to Do"),
    D("D", 0.0, "Delegate", "D Tier — Delegate"),
    E("E", 0.0, "Eliminate", "E Tier — Eliminate");

    val isExecutableInBlock: Boolean
        get() = this == S || this == A || this == B || this == C

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

    fun activeNodeIndex(): Int {
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
)

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

data class SevenPointNodeSpec(
    val index: Int,
    val shortTitle: String,
    val fullPrompt: String
)

object DailySevenPointCatalog {
    val nodes: List<SevenPointNodeSpec> = listOf(
        SevenPointNodeSpec(
            index = 1,
            shortTitle = "Time 1 — Intention & S-1 Lock",
            fullPrompt = "Time 1 — Intention & S-1 Lock: What single S-1 deliverable, if completed cleanly today, makes every other effort easier or unnecessary?"
        ),
        SevenPointNodeSpec(
            index = 2,
            shortTitle = "Time 2 — Friction & Drift Audit",
            fullPrompt = "Time 2 — Friction & Drift Audit: Where did attention drift during the past three hours, and what concrete boundary eliminates that leak right now?"
        ),
        SevenPointNodeSpec(
            index = 3,
            shortTitle = "Time 3 — Judgment & Priming",
            fullPrompt = "Time 3 — Judgment & Priming: Is something superfluous to the matter at hand influencing my judgment right now—and if so, how do I adjust my perception accordingly?"
        ),
        SevenPointNodeSpec(
            index = 4,
            shortTitle = "Time 4 — Energy & Output Ratio",
            fullPrompt = "Time 4 — Energy & Output Ratio: Am I executing on high-leverage S/A work or hiding inside low-signal B/C maintenance?"
        ),
        SevenPointNodeSpec(
            index = 5,
            shortTitle = "Time 5 — Deep Execution Check",
            fullPrompt = "Time 5 — Deep Execution Check: What unfinished loop is occupying working memory that should either be completed next or explicitly deferred?"
        ),
        SevenPointNodeSpec(
            index = 6,
            shortTitle = "Time 6 — Finish Line Calibration",
            fullPrompt = "Time 6 — Finish Line Calibration: What remains between the current score and a clean Active Zone finish before day close?"
        ),
        SevenPointNodeSpec(
            index = 7,
            shortTitle = "Time 7 — Evening Closure & Reset",
            fullPrompt = "Time 7 — Evening Closure & Reset: What single operational adjustment will make tomorrow's Time 1 block start without hesitation?"
        )
    )

    fun forNode(index: Int): SevenPointNodeSpec =
        nodes.firstOrNull { it.index == index } ?: nodes[2]
}

data class DisplayTaskItem(
    val task: PriorityTask,
    val badge: String,
    val multiplierLabel: String
)

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

fun formatHhMmSs(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0L)
    val hours = clamped / 3600L
    val mins = (clamped % 3600L) / 60L
    val secs = clamped % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
}

fun formatTrackedDuration(totalMinutes: Long): String {
    val safe = totalMinutes.coerceAtLeast(0L)
    val h = safe / 60L
    val m = safe % 60L
    return if (h > 0L) "${h}h ${m}m" else "${m}m"
}

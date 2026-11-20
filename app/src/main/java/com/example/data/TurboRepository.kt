package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.round

interface TurboDataSource {
    fun observeTasks(): Flow<List<PriorityTask>>
    fun observeDailyLog(dateKey: String): Flow<DailyLog?>
    fun observeTodayTimeBlocks(dateKey: String): Flow<List<TimeBlockRecord>>
    fun observeTodayCheckIns(dateKey: String): Flow<List<CheckInRecord>>
    suspend fun ensureDailyLog(dateKey: String): Result<Unit>
    suspend fun addTask(title: String, tier: PriorityTier, existingTasks: List<PriorityTask>): Result<Unit>
    suspend fun toggleTaskCompleted(task: PriorityTask, dateKey: String): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun recordCompletedTimeBlock(
        dateKey: String,
        task: DisplayTaskItem,
        startTimeLabel: String,
        durationMinutes: Int,
        qualityRate: Int,
        finishingRate: Int,
        actionRate: Int
    ): Result<Double>
    suspend fun recordCheckIn(
        dateKey: String,
        nodeIndex: Int,
        creativeIq: String,
        emotionEq: String,
        bestThinking: String,
        durationSeconds: Int
    ): Result<Long>
    suspend fun updateDayState(dateKey: String, dayState: DayState): Result<Long>
    suspend fun seedSampleTurboDay(dateKey: String): Result<Unit>
}

class TurboRepository(private val db: FirebaseFirestore) : TurboDataSource {

    companion object {
        const val MIN_CHECKIN_ANSWER_LENGTH = 8
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    override fun observeTasks(): Flow<List<PriorityTask>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PriorityTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override fun observeDailyLog(dateKey: String): Flow<DailyLog?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/dailyLogs/$dateKey"
        emitAll(
            db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            DailyLog::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    override fun observeTodayTimeBlocks(dateKey: String): Flow<List<TimeBlockRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/timeBlocks"
        emitAll(
            db.collection("users").document(uid).collection("timeBlocks")
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TimeBlockRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override fun observeTodayCheckIns(dateKey: String): Flow<List<CheckInRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/checkIns"
        emitAll(
            db.collection("users").document(uid).collection("checkIns")
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CheckInRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun ensureDailyLog(dateKey: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/dailyLogs/$dateKey"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            path = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "dateKey" to dateKey,
                    "dayState" to DayState.WAKE_UP.code,
                    "totalScore" to 0.0,
                    "s1CompletedCount" to 0L,
                    "abCompletedCount" to 0L,
                    "checkInPoints" to 0L,
                    "trackedMinutes" to 0L,
                    "completedNodes" to emptyList<Long>(),
                    "nextAuditEpochMillis" to (System.currentTimeMillis() + 3L * 60L * 60L * 1000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun addTask(
        title: String,
        tier: PriorityTier,
        existingTasks: List<PriorityTask>
    ): Result<Unit> {
        val cleanTitle = title.trim().take(160)
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty."))
        }
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks"
        return try {
            val uid = requireUserId()
            val tasksCol = db.collection("users").document(uid).collection("tasks")

            if (tier == PriorityTier.S) {
                val existingS = existingTasks.filter { it.tierEnum() == PriorityTier.S && !it.completed }
                for (oldS in existingS) {
                    if (oldS.id.isNotBlank()) {
                        tasksCol.document(oldS.id).update(
                            mapOf(
                                "tier" to PriorityTier.A.code,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }

            val nextOrder = ((existingTasks.maxOfOrNull { it.orderIndex } ?: -1L) + 1L)
                .coerceIn(0L, 10000L)
            val docRef = tasksCol.document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "tier" to tier.code,
                "orderIndex" to nextOrder,
                "completed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun toggleTaskCompleted(task: PriorityTask, dateKey: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks/${task.id}"
        return try {
            val uid = requireUserId()
            require(task.id.isNotBlank()) { "Task ID is blank." }
            val newCompleted = !task.completed
            val taskRef = db.collection("users").document(uid).collection("tasks").document(task.id)
            path = taskRef.path
            taskRef.update(
                mapOf(
                    "completed" to newCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            ensureDailyLog(dateKey)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            val logSnap = logRef.get().await()
            if (logSnap.exists()) {
                val delta = if (newCompleted) 1L else -1L
                val tier = task.tierEnum()
                val curS1 = (logSnap.get("s1CompletedCount") as? Number)?.toLong() ?: 0L
                val curAB = (logSnap.get("abCompletedCount") as? Number)?.toLong() ?: 0L
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (tier == PriorityTier.S) {
                    updates["s1CompletedCount"] = (curS1 + delta).coerceIn(0L, 100L)
                } else if (tier == PriorityTier.A || tier == PriorityTier.B) {
                    updates["abCompletedCount"] = (curAB + delta).coerceIn(0L, 500L)
                }
                if (updates.size > 1) {
                    logRef.update(updates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank()) { "Task ID is blank." }
            val ref = db.collection("users").document(uid).collection("tasks").document(taskId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun recordCompletedTimeBlock(
        dateKey: String,
        task: DisplayTaskItem,
        startTimeLabel: String,
        durationMinutes: Int,
        qualityRate: Int,
        finishingRate: Int,
        actionRate: Int
    ): Result<Double> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/timeBlocks"
        return try {
            val uid = requireUserId()
            val safeDuration = durationMinutes.coerceIn(1, 480)
            val safeQ = qualityRate.coerceIn(1, 10)
            val safeF = finishingRate.coerceIn(1, 5)
            val safeA = actionRate.coerceIn(1, 20)
            val multiplier = task.task.tierEnum().multiplier
            val rawScore = safeQ + safeF + safeA
            val earnedPoints = round(rawScore * multiplier * 10.0) / 10.0

            val blockRef = db.collection("users").document(uid).collection("timeBlocks").document()
            path = blockRef.path
            val payload = mapOf(
                "userId" to uid,
                "dateKey" to dateKey,
                "taskId" to task.task.id.ifBlank { "manual_task" },
                "taskBadge" to task.badge.take(12),
                "taskTitle" to task.task.title.take(160),
                "tier" to task.task.tierEnum().code,
                "startTimeLabel" to startTimeLabel.take(16),
                "durationMinutes" to safeDuration.toLong(),
                "qualityRate" to safeQ.toLong(),
                "finishingRate" to safeF.toLong(),
                "actionRate" to safeA.toLong(),
                "multiplier" to multiplier,
                "earnedPoints" to earnedPoints,
                "createdAt" to FieldValue.serverTimestamp()
            )
            blockRef.set(payload).await()

            ensureDailyLog(dateKey)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            path = logRef.path
            val logSnap = logRef.get().await()
            val curScore = (logSnap.get("totalScore") as? Number)?.toDouble() ?: 0.0
            val curMinutes = (logSnap.get("trackedMinutes") as? Number)?.toLong() ?: 0L
            val curS1 = (logSnap.get("s1CompletedCount") as? Number)?.toLong() ?: 0L
            val curAB = (logSnap.get("abCompletedCount") as? Number)?.toLong() ?: 0L

            val newScore = (round((curScore + earnedPoints) * 10.0) / 10.0).coerceIn(0.0, 5000.0)
            val newMinutes = (curMinutes + safeDuration.toLong()).coerceIn(0L, 1440L)

            val taskFinished = safeF >= 5 && !task.task.completed
            val nextS1 = if (taskFinished && task.task.tierEnum() == PriorityTier.S) (curS1 + 1L).coerceIn(0L, 100L) else curS1
            val nextAB = if (taskFinished && (task.task.tierEnum() == PriorityTier.A || task.task.tierEnum() == PriorityTier.B)) (curAB + 1L).coerceIn(0L, 500L) else curAB

            logRef.update(
                mapOf(
                    "totalScore" to newScore,
                    "trackedMinutes" to newMinutes,
                    "s1CompletedCount" to nextS1,
                    "abCompletedCount" to nextAB,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (taskFinished && task.task.id.isNotBlank()) {
                db.collection("users").document(uid).collection("tasks").document(task.task.id)
                    .update(
                        mapOf(
                            "completed" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }

            Result.success(earnedPoints)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun recordCheckIn(
        dateKey: String,
        nodeIndex: Int,
        creativeIq: String,
        emotionEq: String,
        bestThinking: String,
        durationSeconds: Int
    ): Result<Long> {
        val cleanCreative = creativeIq.trim().take(2000)
        val cleanEmotion = emotionEq.trim().take(2000)
        val cleanThinking = bestThinking.trim().take(2000)
        if (cleanCreative.length < MIN_CHECKIN_ANSWER_LENGTH ||
            cleanEmotion.length < MIN_CHECKIN_ANSWER_LENGTH ||
            cleanThinking.length < MIN_CHECKIN_ANSWER_LENGTH
        ) {
            return Result.failure(
                IllegalArgumentException("All 3 check-in questions require meaningful written answers.")
            )
        }

        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/checkIns"
        return try {
            val uid = requireUserId()
            val safeNode = nodeIndex.coerceIn(1, 7)
            val safeDuration = durationSeconds.coerceIn(0, 10800)
            val points = 5L
            val nextAuditEpoch = System.currentTimeMillis() + 3L * 60L * 60L * 1000L

            val ciRef = db.collection("users").document(uid).collection("checkIns").document()
            path = ciRef.path
            val payload = mapOf(
                "userId" to uid,
                "dateKey" to dateKey,
                "nodeIndex" to safeNode.toLong(),
                "creativeIq" to cleanCreative,
                "emotionEq" to cleanEmotion,
                "bestThinking" to cleanThinking,
                "dailySevenPoint" to "",
                "durationSeconds" to safeDuration.toLong(),
                "pointsAwarded" to points,
                "createdAt" to FieldValue.serverTimestamp()
            )
            ciRef.set(payload).await()

            ensureDailyLog(dateKey)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            path = logRef.path
            val logSnap = logRef.get().await()
            val curScore = (logSnap.get("totalScore") as? Number)?.toDouble() ?: 0.0
            val curCiPts = (logSnap.get("checkInPoints") as? Number)?.toLong() ?: 0L
            val curNodesRaw = (logSnap.get("completedNodes") as? List<*>)
                ?.mapNotNull { (it as? Number)?.toLong() }
                ?: emptyList()
            val updatedNodes = (curNodesRaw + safeNode.toLong()).distinct().sorted().take(7)
            val newScore = (round((curScore + points) * 10.0) / 10.0).coerceIn(0.0, 5000.0)
            val newCiPts = (curCiPts + points).coerceIn(0L, 1000L)

            logRef.update(
                mapOf(
                    "totalScore" to newScore,
                    "checkInPoints" to newCiPts,
                    "completedNodes" to updatedNodes,
                    "nextAuditEpochMillis" to nextAuditEpoch,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(nextAuditEpoch)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateDayState(dateKey: String, dayState: DayState): Result<Long> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/dailyLogs/$dateKey"
        return try {
            val uid = requireUserId()
            ensureDailyLog(dateKey)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            path = logRef.path
            val nextAuditEpoch = System.currentTimeMillis() + 3L * 60L * 60L * 1000L
            val updates = mutableMapOf<String, Any>(
                "dayState" to dayState.code,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (dayState == DayState.WAKE_UP) {
                updates["nextAuditEpochMillis"] = nextAuditEpoch
            }
            logRef.update(updates).await()
            Result.success(nextAuditEpoch)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun seedSampleTurboDay(dateKey: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks"
        return try {
            val uid = requireUserId()
            val tasksCol = db.collection("users").document(uid).collection("tasks")
            val samples = listOf(
                Triple("Finalize core API architecture spec", PriorityTier.S, 0L),
                Triple("Review database migration script", PriorityTier.A, 1L),
                Triple("Draft Q4 engineering hiring rubric", PriorityTier.A, 2L),
                Triple("Clear blocking PR reviews", PriorityTier.A, 3L),
                Triple("Update onboarding docs", PriorityTier.B, 4L),
                Triple("Audit error log alerts", PriorityTier.B, 5L),
                Triple("Organize local workspace notes", PriorityTier.C, 6L),
                Triple("Hand off vendor invoice follow-up", PriorityTier.D, 7L),
                Triple("Unsubscribe low-signal newsletters", PriorityTier.E, 8L)
            )
            val batch = db.batch()
            samples.forEach { (title, tier, order) ->
                val ref = tasksCol.document()
                batch.set(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "title" to title,
                        "tier" to tier.code,
                        "orderIndex" to order,
                        "completed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            ensureDailyLog(dateKey)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}

/**
 * Local in-memory implementation used exclusively when the user explicitly chooses
 * "Enter Test Mode" for rapid device testing without Google Sign-In.
 */
class TestTurboRepository : TurboDataSource {
    private val testUid = "local_test_user"
    private val tasksFlow = MutableStateFlow<List<PriorityTask>>(emptyList())
    private val dailyLogFlow = MutableStateFlow(DailyLog(userId = testUid, dateKey = todayDateKey()))
    private val timeBlocksFlow = MutableStateFlow<List<TimeBlockRecord>>(emptyList())
    private val checkInsFlow = MutableStateFlow<List<CheckInRecord>>(emptyList())

    override fun observeTasks(): Flow<List<PriorityTask>> = tasksFlow

    override fun observeDailyLog(dateKey: String): Flow<DailyLog?> = dailyLogFlow

    override fun observeTodayTimeBlocks(dateKey: String): Flow<List<TimeBlockRecord>> =
        timeBlocksFlow.map { list -> list.filter { it.dateKey == dateKey } }

    override fun observeTodayCheckIns(dateKey: String): Flow<List<CheckInRecord>> =
        checkInsFlow.map { list -> list.filter { it.dateKey == dateKey } }

    override suspend fun ensureDailyLog(dateKey: String): Result<Unit> {
        if (dailyLogFlow.value.dateKey != dateKey) {
            dailyLogFlow.value = DailyLog(userId = testUid, dateKey = dateKey)
        }
        return Result.success(Unit)
    }

    override suspend fun addTask(
        title: String,
        tier: PriorityTier,
        existingTasks: List<PriorityTask>
    ): Result<Unit> {
        val cleanTitle = title.trim().take(160)
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty."))
        }
        val now = Timestamp.now()
        tasksFlow.update { current ->
            val adjusted = if (tier == PriorityTier.S) {
                current.map {
                    if (it.tierEnum() == PriorityTier.S && !it.completed) {
                        it.copy(tier = PriorityTier.A.code, updatedAt = now)
                    } else {
                        it
                    }
                }
            } else {
                current
            }
            val nextOrder = ((adjusted.maxOfOrNull { it.orderIndex } ?: -1L) + 1L).coerceIn(0L, 10000L)
            adjusted + PriorityTask(
                id = UUID.randomUUID().toString(),
                userId = testUid,
                title = cleanTitle,
                tier = tier.code,
                orderIndex = nextOrder,
                completed = false,
                createdAt = now,
                updatedAt = now
            )
        }
        return Result.success(Unit)
    }

    override suspend fun toggleTaskCompleted(task: PriorityTask, dateKey: String): Result<Unit> {
        val newCompleted = !task.completed
        val now = Timestamp.now()
        tasksFlow.update { current ->
            current.map {
                if (it.id == task.id) it.copy(completed = newCompleted, updatedAt = now) else it
            }
        }
        val delta = if (newCompleted) 1L else -1L
        val tier = task.tierEnum()
        dailyLogFlow.update { log ->
            log.copy(
                s1CompletedCount = if (tier == PriorityTier.S) (log.s1CompletedCount + delta).coerceIn(0L, 100L) else log.s1CompletedCount,
                abCompletedCount = if (tier == PriorityTier.A || tier == PriorityTier.B) (log.abCompletedCount + delta).coerceIn(0L, 500L) else log.abCompletedCount,
                updatedAt = now
            )
        }
        return Result.success(Unit)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        tasksFlow.update { current -> current.filterNot { it.id == taskId } }
        return Result.success(Unit)
    }

    override suspend fun recordCompletedTimeBlock(
        dateKey: String,
        task: DisplayTaskItem,
        startTimeLabel: String,
        durationMinutes: Int,
        qualityRate: Int,
        finishingRate: Int,
        actionRate: Int
    ): Result<Double> {
        val safeDuration = durationMinutes.coerceIn(1, 480)
        val safeQ = qualityRate.coerceIn(1, 10)
        val safeF = finishingRate.coerceIn(1, 5)
        val safeA = actionRate.coerceIn(1, 20)
        val multiplier = task.task.tierEnum().multiplier
        val rawScore = safeQ + safeF + safeA
        val earnedPoints = round(rawScore * multiplier * 10.0) / 10.0
        val now = Timestamp.now()

        val record = TimeBlockRecord(
            id = UUID.randomUUID().toString(),
            userId = testUid,
            dateKey = dateKey,
            taskId = task.task.id.ifBlank { "manual_task" },
            taskBadge = task.badge.take(12),
            taskTitle = task.task.title.take(160),
            tier = task.task.tierEnum().code,
            startTimeLabel = startTimeLabel.take(16),
            durationMinutes = safeDuration.toLong(),
            qualityRate = safeQ.toLong(),
            finishingRate = safeF.toLong(),
            actionRate = safeA.toLong(),
            multiplier = multiplier,
            earnedPoints = earnedPoints,
            createdAt = now
        )
        timeBlocksFlow.update { listOf(record) + it }

        val taskFinished = safeF >= 5 && !task.task.completed
        if (taskFinished) {
            tasksFlow.update { current ->
                current.map { if (it.id == task.task.id) it.copy(completed = true, updatedAt = now) else it }
            }
        }
        dailyLogFlow.update { log ->
            val newScore = (round((log.totalScore + earnedPoints) * 10.0) / 10.0).coerceIn(0.0, 5000.0)
            val newMinutes = (log.trackedMinutes + safeDuration.toLong()).coerceIn(0L, 1440L)
            val nextS1 = if (taskFinished && task.task.tierEnum() == PriorityTier.S) (log.s1CompletedCount + 1L).coerceIn(0L, 100L) else log.s1CompletedCount
            val nextAB = if (taskFinished && (task.task.tierEnum() == PriorityTier.A || task.task.tierEnum() == PriorityTier.B)) (log.abCompletedCount + 1L).coerceIn(0L, 500L) else log.abCompletedCount
            log.copy(
                totalScore = newScore,
                trackedMinutes = newMinutes,
                s1CompletedCount = nextS1,
                abCompletedCount = nextAB,
                updatedAt = now
            )
        }
        return Result.success(earnedPoints)
    }

    override suspend fun recordCheckIn(
        dateKey: String,
        nodeIndex: Int,
        creativeIq: String,
        emotionEq: String,
        bestThinking: String,
        durationSeconds: Int
    ): Result<Long> {
        val cleanCreative = creativeIq.trim().take(2000)
        val cleanEmotion = emotionEq.trim().take(2000)
        val cleanThinking = bestThinking.trim().take(2000)
        if (cleanCreative.length < TurboRepository.MIN_CHECKIN_ANSWER_LENGTH ||
            cleanEmotion.length < TurboRepository.MIN_CHECKIN_ANSWER_LENGTH ||
            cleanThinking.length < TurboRepository.MIN_CHECKIN_ANSWER_LENGTH
        ) {
            return Result.failure(
                IllegalArgumentException("All 3 check-in questions require meaningful written answers.")
            )
        }
        val safeNode = nodeIndex.coerceIn(1, 7)
        val points = 5L
        val nextAuditEpoch = System.currentTimeMillis() + 3L * 60L * 60L * 1000L
        val now = Timestamp.now()

        val record = CheckInRecord(
            id = UUID.randomUUID().toString(),
            userId = testUid,
            dateKey = dateKey,
            nodeIndex = safeNode.toLong(),
            creativeIq = cleanCreative,
            emotionEq = cleanEmotion,
            bestThinking = cleanThinking,
            dailySevenPoint = "",
            durationSeconds = durationSeconds.coerceIn(0, 10800).toLong(),
            pointsAwarded = points,
            createdAt = now
        )
        checkInsFlow.update { listOf(record) + it }
        dailyLogFlow.update { log ->
            val updatedNodes = (log.completedNodes + safeNode.toLong()).distinct().sorted().take(7)
            log.copy(
                totalScore = (round((log.totalScore + points) * 10.0) / 10.0).coerceIn(0.0, 5000.0),
                checkInPoints = (log.checkInPoints + points).coerceIn(0L, 1000L),
                completedNodes = updatedNodes,
                nextAuditEpochMillis = nextAuditEpoch,
                updatedAt = now
            )
        }
        return Result.success(nextAuditEpoch)
    }

    override suspend fun updateDayState(dateKey: String, dayState: DayState): Result<Long> {
        val nextAuditEpoch = System.currentTimeMillis() + 3L * 60L * 60L * 1000L
        dailyLogFlow.update { log ->
            log.copy(
                dayState = dayState.code,
                nextAuditEpochMillis = if (dayState == DayState.WAKE_UP) nextAuditEpoch else log.nextAuditEpochMillis,
                updatedAt = Timestamp.now()
            )
        }
        return Result.success(nextAuditEpoch)
    }

    override suspend fun seedSampleTurboDay(dateKey: String): Result<Unit> {
        val now = Timestamp.now()
        val samples = listOf(
            Triple("Finalize core API architecture spec", PriorityTier.S, 0L),
            Triple("Review database migration script", PriorityTier.A, 1L),
            Triple("Draft Q4 engineering hiring rubric", PriorityTier.A, 2L),
            Triple("Clear blocking PR reviews", PriorityTier.A, 3L),
            Triple("Update onboarding docs", PriorityTier.B, 4L),
            Triple("Audit error log alerts", PriorityTier.B, 5L),
            Triple("Organize local workspace notes", PriorityTier.C, 6L),
            Triple("Hand off vendor invoice follow-up", PriorityTier.D, 7L),
            Triple("Unsubscribe low-signal newsletters", PriorityTier.E, 8L)
        )
        tasksFlow.value = samples.map { (title, tier, order) ->
            PriorityTask(
                id = UUID.randomUUID().toString(),
                userId = testUid,
                title = title,
                tier = tier.code,
                orderIndex = order,
                completed = false,
                createdAt = now,
                updatedAt = now
            )
        }
        return Result.success(Unit)
    }
}

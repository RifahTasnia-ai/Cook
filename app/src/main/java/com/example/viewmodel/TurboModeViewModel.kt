package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyLog
import com.example.data.DailySevenPointCatalog
import com.example.data.DayState
import com.example.data.DisplayTaskItem
import com.example.data.PriorityTask
import com.example.data.PriorityTier
import com.example.data.TurboRepository
import com.example.data.buildDisplayTasks
import com.example.data.currentTimeHhMm
import com.example.data.formatMmSs
import com.example.data.todayDateKey
import com.example.notifications.TurboNotificationManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.round

sealed interface TurboDestination {
    data object Dashboard : TurboDestination
    data object StartBlockSheet : TurboDestination
    data object ActiveTimeBlock : TurboDestination
    data object PostBlockEvaluation : TurboDestination
    data object SilenceReset : TurboDestination
    data object IncomingCallAudit : TurboDestination
    data object SevenPointCheckIn : TurboDestination
    data object EndOfDaySummary : TurboDestination
    data object LockScreenNotifications : TurboDestination
}

data class ActiveBlockSession(
    val task: DisplayTaskItem,
    val startTimeLabel: String,
    val totalSeconds: Int,
    val remainingSeconds: Int
) {
    val elapsedSeconds: Int get() = (totalSeconds - remainingSeconds).coerceAtLeast(0)
    val progressFraction: Float
        get() = if (totalSeconds <= 0) 1f else (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val loggedMinutes: Int
        get() = ceil(elapsedSeconds / 60.0).toInt().coerceAtLeast(1)
}

data class EvaluationDraft(
    val task: DisplayTaskItem,
    val startTimeLabel: String,
    val loggedMinutes: Int,
    val qualityRate: Int = 8,
    val finishingRate: Int = 4,
    val actionRate: Int = 16
) {
    val rawScore: Int get() = qualityRate + finishingRate + actionRate
    val multiplier: Double get() = task.task.tierEnum().multiplier
    val earnedPoints: Double get() = round(rawScore * multiplier * 10.0) / 10.0
}

data class CheckInSession(
    val nodeIndex: Int,
    val startedAtLabel: String,
    val elapsedSeconds: Int = 0,
    val currentStep: Int = 0, // 0..3
    val answers: List<String> = listOf("", "", "", "")
)

data class TurboUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusToast: String? = null,
    val isBusy: Boolean = false,
    val dateKey: String = todayDateKey(),
    val destination: TurboDestination = TurboDestination.Dashboard,
    val tasks: List<PriorityTask> = emptyList(),
    val displayTasks: List<DisplayTaskItem> = emptyList(),
    val dailyLog: DailyLog = DailyLog(),
    val auditCountdownSeconds: Long = 3L * 3600L,
    // Start Block Sheet state
    val selectedTaskIdForBlock: String? = null,
    val blockStartTimeLabel: String = currentTimeHhMm(),
    val selectedDurationMinutes: Int = 45,
    val isCustomDuration: Boolean = false,
    val customDurationInput: String = "30",
    // Active focus block
    val activeBlock: ActiveBlockSession? = null,
    // Post-block self-evaluation
    val evaluationDraft: EvaluationDraft? = null,
    // 121-second silence reset
    val silenceRemainingSeconds: Int = 121,
    val showEarlyExitDialog: Boolean = false,
    // 3-hour audit & 7-point check-in
    val pendingAuditNodeIndex: Int = 1,
    val checkInSession: CheckInSession? = null,
    // Add task modal
    val showAddTaskDialog: Boolean = false
)

class TurboModeViewModel(
    application: Application,
    private val repository: TurboRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TurboUiState())
    val uiState: StateFlow<TurboUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        val dateKey = todayDateKey()
        viewModelScope.launch {
            repository.ensureDailyLog(dateKey)
        }
        observeFirestoreData(dateKey)
        startMasterClock()
    }

    private fun observeFirestoreData(dateKey: String) {
        viewModelScope.launch {
            combine(
                repository.observeTasks(),
                repository.observeDailyLog(dateKey)
            ) { tasks, log ->
                tasks to (log ?: DailyLog(dateKey = dateKey))
            }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = root.localizedMessage ?: "Failed to sync Turbo Mode data."
                        )
                    }
                }
                .collect { (tasks, log) ->
                    val display = buildDisplayTasks(tasks)
                    val remainingAudit = ((log.nextAuditEpochMillis - System.currentTimeMillis()) / 1000L)
                        .coerceAtLeast(0L)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = null,
                            tasks = tasks,
                            displayTasks = display,
                            dailyLog = log,
                            auditCountdownSeconds = remainingAudit,
                            pendingAuditNodeIndex = log.activeNodeIndex()
                        )
                    }
                }
        }
    }

    private fun startMasterClock() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                val nextAuditSec = ((state.dailyLog.nextAuditEpochMillis - System.currentTimeMillis()) / 1000L)
                    .coerceAtLeast(0L)

                var updatedBlock = state.activeBlock
                var updatedSilence = state.silenceRemainingSeconds
                var updatedCheckIn = state.checkInSession
                var nextDestination = state.destination
                var nextEval = state.evaluationDraft

                if (state.destination == TurboDestination.ActiveTimeBlock && updatedBlock != null) {
                    val nextRem = (updatedBlock.remainingSeconds - 1).coerceAtLeast(0)
                    updatedBlock = updatedBlock.copy(remainingSeconds = nextRem)
                    if (nextRem == 0) {
                        TurboNotificationManager.cancelActiveBlock(getApplication())
                        nextEval = EvaluationDraft(
                            task = updatedBlock.task,
                            startTimeLabel = updatedBlock.startTimeLabel,
                            loggedMinutes = (updatedBlock.totalSeconds / 60).coerceAtLeast(1)
                        )
                        nextDestination = TurboDestination.PostBlockEvaluation
                    } else if (nextRem % 15 == 0) {
                        TurboNotificationManager.postActiveBlockNotification(
                            context = getApplication(),
                            badge = updatedBlock.task.badge,
                            title = updatedBlock.task.task.title,
                            remainingFormatted = formatMmSs(nextRem),
                            progressPercent = (updatedBlock.progressFraction * 100f).toInt()
                        )
                    }
                }

                if (state.destination == TurboDestination.SilenceReset) {
                    val nextRem = (updatedSilence - 1).coerceAtLeast(0)
                    updatedSilence = nextRem
                    if (nextRem == 0) {
                        TurboNotificationManager.cancelSilenceReset(getApplication())
                        nextDestination = TurboDestination.Dashboard
                    } else if (nextRem % 15 == 0) {
                        TurboNotificationManager.postSilenceResetNotification(
                            context = getApplication(),
                            remainingFormatted = formatMmSs(nextRem)
                        )
                    }
                }

                if (state.destination == TurboDestination.SevenPointCheckIn && updatedCheckIn != null) {
                    updatedCheckIn = updatedCheckIn.copy(elapsedSeconds = updatedCheckIn.elapsedSeconds + 1)
                }

                _uiState.update {
                    it.copy(
                        auditCountdownSeconds = nextAuditSec,
                        activeBlock = updatedBlock,
                        silenceRemainingSeconds = updatedSilence,
                        checkInSession = updatedCheckIn,
                        destination = nextDestination,
                        evaluationDraft = nextEval
                    )
                }
            }
        }
    }

    // --- Dashboard & Task Queue ---

    fun setAddTaskDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddTaskDialog = visible) }
    }

    fun addTask(title: String, tier: PriorityTier) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.addTask(title, tier, _uiState.value.tasks)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    showAddTaskDialog = if (result.isSuccess) false else state.showAddTaskDialog,
                    errorMessage = result.exceptionOrNull()?.localizedMessage,
                    statusToast = if (result.isSuccess) "Task added to ${tier.code} tier" else null
                )
            }
        }
    }

    fun toggleTaskCompleted(task: PriorityTask) {
        viewModelScope.launch {
            val result = repository.toggleTaskCompleted(task, _uiState.value.dateKey)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to update task")
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = repository.deleteTask(taskId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to delete task")
                }
            }
        }
    }

    fun seedSampleDay() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.seedSampleTurboDay(_uiState.value.dateKey)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage,
                    statusToast = if (result.isSuccess) "Loaded sample Turbo Mode queue" else null
                )
            }
        }
    }

    // --- Screen 2: Start Time Block Sheet ---

    fun openStartBlockSheet(preselectedTaskId: String? = null) {
        _uiState.update {
            it.copy(
                destination = TurboDestination.StartBlockSheet,
                selectedTaskIdForBlock = preselectedTaskId,
                blockStartTimeLabel = currentTimeHhMm(),
                selectedDurationMinutes = 45,
                isCustomDuration = false
            )
        }
    }

    fun dismissStartBlockSheet() {
        _uiState.update { it.copy(destination = TurboDestination.Dashboard) }
    }

    fun selectTaskForBlock(taskId: String) {
        _uiState.update { it.copy(selectedTaskIdForBlock = taskId) }
    }

    fun updateBlockStartTime(timeLabel: String) {
        _uiState.update { it.copy(blockStartTimeLabel = timeLabel) }
    }

    fun selectPresetDuration(minutes: Int) {
        _uiState.update {
            it.copy(selectedDurationMinutes = minutes, isCustomDuration = false)
        }
    }

    fun enableCustomDuration() {
        val parsed = _uiState.value.customDurationInput.toIntOrNull()?.coerceIn(1, 240) ?: 30
        _uiState.update {
            it.copy(isCustomDuration = true, selectedDurationMinutes = parsed)
        }
    }

    fun updateCustomDurationInput(raw: String) {
        val filtered = raw.filter { it.isDigit() }.take(3)
        val parsed = filtered.toIntOrNull()?.coerceIn(1, 240) ?: _uiState.value.selectedDurationMinutes
        _uiState.update {
            it.copy(customDurationInput = filtered, selectedDurationMinutes = parsed)
        }
    }

    fun startActiveTimeBlock() {
        val state = _uiState.value
        val selected = state.displayTasks.firstOrNull { it.task.id == state.selectedTaskIdForBlock }
            ?: return
        val durationMin = state.selectedDurationMinutes.coerceIn(1, 240)
        val totalSec = durationMin * 60
        val session = ActiveBlockSession(
            task = selected,
            startTimeLabel = state.blockStartTimeLabel,
            totalSeconds = totalSec,
            remainingSeconds = totalSec
        )
        TurboNotificationManager.postActiveBlockNotification(
            context = getApplication(),
            badge = selected.badge,
            title = selected.task.title,
            remainingFormatted = formatMmSs(totalSec),
            progressPercent = 0
        )
        _uiState.update {
            it.copy(
                activeBlock = session,
                destination = TurboDestination.ActiveTimeBlock
            )
        }
    }

    // --- Screen 3 & 4: Active Block & Post-Block Evaluation ---

    fun finishActiveTimeBlock() {
        val block = _uiState.value.activeBlock ?: return
        TurboNotificationManager.cancelActiveBlock(getApplication())
        val draft = EvaluationDraft(
            task = block.task,
            startTimeLabel = block.startTimeLabel,
            loggedMinutes = block.loggedMinutes
        )
        _uiState.update {
            it.copy(
                evaluationDraft = draft,
                destination = TurboDestination.PostBlockEvaluation
            )
        }
    }

    fun updateQualityRate(value: Int) {
        _uiState.update { state ->
            state.copy(evaluationDraft = state.evaluationDraft?.copy(qualityRate = value.coerceIn(1, 10)))
        }
    }

    fun updateFinishingRate(value: Int) {
        _uiState.update { state ->
            state.copy(evaluationDraft = state.evaluationDraft?.copy(finishingRate = value.coerceIn(1, 5)))
        }
    }

    fun updateActionRate(value: Int) {
        _uiState.update { state ->
            state.copy(evaluationDraft = state.evaluationDraft?.copy(actionRate = value.coerceIn(1, 20)))
        }
    }

    fun submitEvaluationAndStartSilence() {
        val draft = _uiState.value.evaluationDraft ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.recordCompletedTimeBlock(
                dateKey = _uiState.value.dateKey,
                task = draft.task,
                startTimeLabel = draft.startTimeLabel,
                durationMinutes = draft.loggedMinutes,
                qualityRate = draft.qualityRate,
                finishingRate = draft.finishingRate,
                actionRate = draft.actionRate
            )
            if (result.isSuccess) {
                TurboNotificationManager.postSilenceResetNotification(
                    context = getApplication(),
                    remainingFormatted = formatMmSs(121)
                )
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        activeBlock = null,
                        evaluationDraft = null,
                        silenceRemainingSeconds = 121,
                        showEarlyExitDialog = false,
                        destination = TurboDestination.SilenceReset
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to save block score."
                    )
                }
            }
        }
    }

    // --- Screen 5: 121-Second Silence Reset ---

    fun setEarlyExitDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showEarlyExitDialog = visible) }
    }

    fun confirmExitSilenceEarly() {
        TurboNotificationManager.cancelSilenceReset(getApplication())
        _uiState.update {
            it.copy(
                showEarlyExitDialog = false,
                destination = TurboDestination.Dashboard,
                statusToast = "Silence reset ended early"
            )
        }
    }

    // --- Screen 6 & 7: 3-Hour Incoming Call & 7-Point Check-In ---

    fun triggerIncomingAuditCall(nodeIndex: Int = _uiState.value.dailyLog.activeNodeIndex()) {
        val clamped = nodeIndex.coerceIn(1, 7)
        val spec = DailySevenPointCatalog.forNode(clamped)
        TurboNotificationManager.postAuditNotification(getApplication(), spec.shortTitle)
        _uiState.update {
            it.copy(
                pendingAuditNodeIndex = clamped,
                destination = TurboDestination.IncomingCallAudit
            )
        }
    }

    fun answerAuditCall() {
        val node = _uiState.value.pendingAuditNodeIndex.coerceIn(1, 7)
        _uiState.update {
            it.copy(
                checkInSession = CheckInSession(
                    nodeIndex = node,
                    startedAtLabel = currentTimeHhMm(),
                    elapsedSeconds = 0,
                    currentStep = 0,
                    answers = listOf("", "", "", "")
                ),
                destination = TurboDestination.SevenPointCheckIn
            )
        }
    }

    fun updateCurrentCheckInAnswer(text: String) {
        _uiState.update { state ->
            val session = state.checkInSession ?: return@update state
            val updated = session.answers.toMutableList()
            updated[session.currentStep] = text.take(2000)
            state.copy(checkInSession = session.copy(answers = updated))
        }
    }

    fun prevCheckInStep() {
        _uiState.update { state ->
            val session = state.checkInSession ?: return@update state
            if (session.currentStep > 0) {
                state.copy(checkInSession = session.copy(currentStep = session.currentStep - 1))
            } else {
                state
            }
        }
    }

    fun nextOrSubmitCheckIn() {
        val session = _uiState.value.checkInSession ?: return
        if (session.currentStep < 3) {
            _uiState.update {
                it.copy(checkInSession = session.copy(currentStep = session.currentStep + 1))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.recordCheckIn(
                dateKey = _uiState.value.dateKey,
                nodeIndex = session.nodeIndex,
                creativeIq = session.answers[0],
                emotionEq = session.answers[1],
                bestThinking = session.answers[2],
                dailySevenPoint = session.answers[3],
                durationSeconds = session.elapsedSeconds
            )
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(
                        isBusy = false,
                        checkInSession = null,
                        destination = TurboDestination.Dashboard,
                        statusToast = "Time ${session.nodeIndex} logged (+5 pts)"
                    )
                } else {
                    state.copy(
                        isBusy = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to save check-in."
                    )
                }
            }
        }
    }

    // --- Screen 8: End-of-Day Summary & Day State ---

    fun onDayStateToggleClicked() {
        val currentState = _uiState.value.dailyLog.dayStateEnum()
        if (currentState == DayState.WAKE_UP) {
            _uiState.update { it.copy(destination = TurboDestination.EndOfDaySummary) }
        } else {
            wakeUpDay()
        }
    }

    fun dismissEndOfDaySummary() {
        _uiState.update { it.copy(destination = TurboDestination.Dashboard) }
    }

    fun closeDay() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.updateDayState(_uiState.value.dateKey, DayState.SLEEP)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    destination = TurboDestination.Dashboard,
                    errorMessage = result.exceptionOrNull()?.localizedMessage,
                    statusToast = if (result.isSuccess) "Day closed — Sleep state active" else null
                )
            }
        }
    }

    fun wakeUpDay() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.updateDayState(_uiState.value.dateKey, DayState.WAKE_UP)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    destination = TurboDestination.Dashboard,
                    errorMessage = result.exceptionOrNull()?.localizedMessage,
                    statusToast = if (result.isSuccess) "Wake-Up active — 3-hour audit engine started" else null
                )
            }
        }
    }

    // --- Screen 9: Android Lock-Screen & Notifications ---

    fun openNotificationsCenter() {
        _uiState.update { it.copy(destination = TurboDestination.LockScreenNotifications) }
    }

    fun returnToDashboard() {
        _uiState.update { it.copy(destination = TurboDestination.Dashboard) }
    }

    fun handleNotificationAction(action: String?) {
        when (action) {
            TurboNotificationManager.ACTION_OPEN_AUDIT -> triggerIncomingAuditCall()
            TurboNotificationManager.ACTION_OPEN_TIMER -> {
                if (_uiState.value.activeBlock != null) {
                    _uiState.update { it.copy(destination = TurboDestination.ActiveTimeBlock) }
                } else {
                    openStartBlockSheet()
                }
            }
            TurboNotificationManager.ACTION_FINISH_BLOCK -> {
                if (_uiState.value.activeBlock != null) {
                    finishActiveTimeBlock()
                } else {
                    openStartBlockSheet()
                }
            }
            TurboNotificationManager.ACTION_START_DAY -> {
                wakeUpDay()
                triggerIncomingAuditCall(1)
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, statusToast = null) }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TurboModeViewModel(application, TurboRepository(application)) as T
        }
    }
}

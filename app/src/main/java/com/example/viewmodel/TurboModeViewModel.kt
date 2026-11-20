package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CheckInRecord
import com.example.data.DailyLog
import com.example.data.DayState
import com.example.data.DisplayTaskItem
import com.example.data.PriorityTask
import com.example.data.PriorityTier
import com.example.data.TimeBlockRecord
import com.example.data.TurboDataSource
import com.example.data.TurboRepository
import com.example.data.TestTurboRepository
import com.example.data.buildDisplayTasks
import com.example.data.currentTimeHhMm
import com.example.data.todayDateKey
import com.example.notifications.AuditAlarmScheduler
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
    val remainingSeconds: Int,
    val endEpochMillis: Long
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
    val currentStep: Int = 0, // 0..2 (3 core questions)
    val answers: List<String> = listOf("", "", "")
) {
    val currentAnswerTrimmedLength: Int
        get() = answers.getOrElse(currentStep) { "" }.trim().length

    val isCurrentAnswerValid: Boolean
        get() = currentAnswerTrimmedLength >= TurboRepository.MIN_CHECKIN_ANSWER_LENGTH

    val allAnswersValid: Boolean
        get() = answers.size == 3 && answers.all { it.trim().length >= TurboRepository.MIN_CHECKIN_ANSWER_LENGTH }
}

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
    val todayTimeBlocks: List<TimeBlockRecord> = emptyList(),
    val todayCheckIns: List<CheckInRecord> = emptyList(),
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
    val silenceEndEpochMillis: Long = 0L,
    val showEarlyExitDialog: Boolean = false,
    // 3-hour audit & 3-question check-in
    val pendingAuditNodeIndex: Int = 1,
    val checkInSession: CheckInSession? = null,
    // Modals
    val showAddTaskDialog: Boolean = false,
    val showScoreReport: Boolean = false
)

class TurboModeViewModel(
    application: Application,
    private val repository: TurboDataSource
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TurboUiState())
    val uiState: StateFlow<TurboUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var lastAutoTriggeredAuditEpoch: Long = 0L

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
                repository.observeDailyLog(dateKey),
                repository.observeTodayTimeBlocks(dateKey),
                repository.observeTodayCheckIns(dateKey)
            ) { tasks, log, blocks, checkIns ->
                FirestoreBundle(
                    tasks = tasks,
                    log = log ?: DailyLog(dateKey = dateKey),
                    blocks = blocks,
                    checkIns = checkIns
                )
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
                .collect { bundle ->
                    val display = buildDisplayTasks(bundle.tasks)
                    if (bundle.log.dayStateEnum() == DayState.WAKE_UP) {
                        AuditAlarmScheduler.scheduleNextAudit(
                            context = getApplication(),
                            triggerAtMillis = bundle.log.nextAuditEpochMillis
                        )
                    } else {
                        AuditAlarmScheduler.cancelAudit(getApplication())
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = null,
                            tasks = bundle.tasks,
                            displayTasks = display,
                            dailyLog = bundle.log,
                            todayTimeBlocks = bundle.blocks,
                            todayCheckIns = bundle.checkIns,
                            pendingAuditNodeIndex = bundle.log.nextCheckInSlot()
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
                val now = System.currentTimeMillis()
                val state = _uiState.value

                var updatedBlock = state.activeBlock
                var updatedSilence = state.silenceRemainingSeconds
                var updatedCheckIn = state.checkInSession
                var nextDestination = state.destination
                var nextEval = state.evaluationDraft

                if (state.destination == TurboDestination.ActiveTimeBlock && updatedBlock != null) {
                    val nextRem = ceil((updatedBlock.endEpochMillis - now) / 1000.0).toInt().coerceAtLeast(0)
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
                            tierTitle = updatedBlock.task.sectionTitle,
                            taskTitle = updatedBlock.task.task.title,
                            endEpochMillis = updatedBlock.endEpochMillis,
                            progressPercent = (updatedBlock.progressFraction * 100f).toInt()
                        )
                    }
                }

                if (state.destination == TurboDestination.SilenceReset && state.silenceEndEpochMillis > 0L) {
                    val nextRem = ceil((state.silenceEndEpochMillis - now) / 1000.0).toInt().coerceAtLeast(0)
                    updatedSilence = nextRem
                    if (nextRem == 0) {
                        TurboNotificationManager.cancelSilenceReset(getApplication())
                        nextDestination = TurboDestination.Dashboard
                    }
                }

                if (state.destination == TurboDestination.SevenPointCheckIn && updatedCheckIn != null) {
                    updatedCheckIn = updatedCheckIn.copy(elapsedSeconds = updatedCheckIn.elapsedSeconds + 1)
                }

                // Automatic foreground 3-hour interruption when WAKE_UP is active and countdown elapses
                if (nextDestination == TurboDestination.Dashboard &&
                    !state.isLoading &&
                    state.dailyLog.dayStateEnum() == DayState.WAKE_UP &&
                    state.dailyLog.nextAuditEpochMillis > 0L &&
                    now >= state.dailyLog.nextAuditEpochMillis &&
                    lastAutoTriggeredAuditEpoch != state.dailyLog.nextAuditEpochMillis
                ) {
                    lastAutoTriggeredAuditEpoch = state.dailyLog.nextAuditEpochMillis
                    TurboNotificationManager.postAuditNotification(getApplication())
                    nextDestination = TurboDestination.IncomingCallAudit
                }

                _uiState.update {
                    it.copy(
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

    fun repostActiveNotificationsIfNeeded() {
        val state = _uiState.value
        val active = state.activeBlock
        if (state.destination == TurboDestination.ActiveTimeBlock && active != null) {
            TurboNotificationManager.postActiveBlockNotification(
                context = getApplication(),
                tierTitle = active.task.sectionTitle,
                taskTitle = active.task.task.title,
                endEpochMillis = active.endEpochMillis,
                progressPercent = (active.progressFraction * 100f).toInt()
            )
        } else if (state.destination == TurboDestination.SilenceReset && state.silenceEndEpochMillis > System.currentTimeMillis()) {
            TurboNotificationManager.postSilenceResetNotification(
                context = getApplication(),
                endEpochMillis = state.silenceEndEpochMillis
            )
        }
    }

    // --- Dashboard, Modals & Task Queue ---

    fun setAddTaskDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddTaskDialog = visible) }
    }

    fun setScoreReportVisible(visible: Boolean) {
        _uiState.update { it.copy(showScoreReport = visible) }
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
                    statusToast = if (result.isSuccess) "Added to ${tier.sectionTitle}" else null
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
                    statusToast = if (result.isSuccess) "Loaded sample priority queue" else null
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
        val endMillis = System.currentTimeMillis() + totalSec * 1000L
        val session = ActiveBlockSession(
            task = selected,
            startTimeLabel = state.blockStartTimeLabel,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            endEpochMillis = endMillis
        )
        TurboNotificationManager.postActiveBlockNotification(
            context = getApplication(),
            tierTitle = selected.sectionTitle,
            taskTitle = selected.task.title,
            endEpochMillis = endMillis,
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
                val silenceEndMillis = System.currentTimeMillis() + 121_000L
                TurboNotificationManager.postSilenceResetNotification(
                    context = getApplication(),
                    endEpochMillis = silenceEndMillis
                )
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        activeBlock = null,
                        evaluationDraft = null,
                        silenceRemainingSeconds = 121,
                        silenceEndEpochMillis = silenceEndMillis,
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
                silenceEndEpochMillis = 0L,
                showEarlyExitDialog = false,
                destination = TurboDestination.Dashboard,
                statusToast = "Silence reset ended early"
            )
        }
    }

    // --- Screen 6 & 7: 3-Hour Incoming Call & 3-Question Check-In ---

    fun triggerIncomingAuditCall() {
        val slot = _uiState.value.dailyLog.nextCheckInSlot()
        TurboNotificationManager.postAuditNotification(getApplication())
        _uiState.update {
            it.copy(
                pendingAuditNodeIndex = slot,
                destination = TurboDestination.IncomingCallAudit
            )
        }
    }

    fun answerAuditCall() {
        TurboNotificationManager.cancelAudit(getApplication())
        val slot = _uiState.value.pendingAuditNodeIndex.coerceIn(1, 7)
        _uiState.update {
            it.copy(
                checkInSession = CheckInSession(
                    nodeIndex = slot,
                    startedAtLabel = currentTimeHhMm(),
                    elapsedSeconds = 0,
                    currentStep = 0,
                    answers = listOf("", "", "")
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
        if (!session.isCurrentAnswerValid) {
            _uiState.update {
                it.copy(
                    statusToast = "Write at least ${TurboRepository.MIN_CHECKIN_ANSWER_LENGTH} characters before continuing"
                )
            }
            return
        }
        if (session.currentStep < 2) {
            _uiState.update {
                it.copy(checkInSession = session.copy(currentStep = session.currentStep + 1))
            }
            return
        }
        if (!session.allAnswersValid) {
            _uiState.update {
                it.copy(statusToast = "Complete all 3 questions to log your check-in")
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
                durationSeconds = session.elapsedSeconds
            )
            result.onSuccess { nextAuditEpoch ->
                AuditAlarmScheduler.scheduleNextAudit(getApplication(), nextAuditEpoch)
            }
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(
                        isBusy = false,
                        checkInSession = null,
                        destination = TurboDestination.Dashboard,
                        statusToast = "3-hour audit logged (+5 pts)"
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
            if (result.isSuccess) {
                AuditAlarmScheduler.cancelAudit(getApplication())
            }
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
            result.onSuccess { nextAuditEpoch ->
                AuditAlarmScheduler.scheduleNextAudit(getApplication(), nextAuditEpoch)
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    destination = TurboDestination.Dashboard,
                    errorMessage = result.exceptionOrNull()?.localizedMessage,
                    statusToast = if (result.isSuccess) "Wake-Up active — 3-hour audit scheduled" else null
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
            TurboNotificationManager.ACTION_OPEN_SILENCE -> {
                if (_uiState.value.silenceEndEpochMillis > System.currentTimeMillis()) {
                    _uiState.update { it.copy(destination = TurboDestination.SilenceReset) }
                } else {
                    returnToDashboard()
                }
            }
            TurboNotificationManager.ACTION_START_DAY -> {
                wakeUpDay()
                triggerIncomingAuditCall()
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, statusToast = null) }
    }

    private data class FirestoreBundle(
        val tasks: List<PriorityTask>,
        val log: DailyLog,
        val blocks: List<TimeBlockRecord>,
        val checkIns: List<CheckInRecord>
    )

    class Factory(
        private val application: Application,
        private val isTestMode: Boolean = false
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dataSource: TurboDataSource = if (isTestMode) {
                TestTurboRepository()
            } else {
                TurboRepository(application)
            }
            return TurboModeViewModel(application, dataSource) as T
        }
    }
}

package com.example

import android.app.Application
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthScreen
import com.example.auth.signOutUser
import com.example.notifications.TurboNotificationManager
import com.example.ui.screens.ActiveTimeBlockScreen
import com.example.ui.screens.EndOfDaySummaryOverlay
import com.example.ui.screens.IncomingCallAuditScreen
import com.example.ui.screens.LockScreenNotificationsScreen
import com.example.ui.screens.PostBlockEvaluationScreen
import com.example.ui.screens.SevenPointCheckInScreen
import com.example.ui.screens.SilenceResetScreen
import com.example.ui.screens.StartTimeBlockSheetOverlay
import com.example.ui.screens.TurboDashboardScreen
import com.example.ui.theme.LocalTurboColors
import com.example.ui.theme.TurboModeTheme
import com.example.viewmodel.TurboDestination
import com.example.viewmodel.TurboModeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val pendingNavAction = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        TurboNotificationManager.ensureChannels(this)
        pendingNavAction.value = intent?.getStringExtra(TurboNotificationManager.EXTRA_NAV_ACTION)

        setContent {
            TurboModeTheme {
                TurboRootApp(
                    pendingNavActionFlow = pendingNavAction,
                    onNavActionConsumed = { pendingNavAction.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.getStringExtra(TurboNotificationManager.EXTRA_NAV_ACTION)
        if (!action.isNullOrBlank()) {
            pendingNavAction.value = action
        }
    }
}

@Composable
private fun TurboRootApp(
    pendingNavActionFlow: MutableStateFlow<String?>,
    onNavActionConsumed: () -> Unit
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            AuthenticatedTurboApp(
                currentUserId = uid,
                pendingNavActionFlow = pendingNavActionFlow,
                onNavActionConsumed = onNavActionConsumed,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedTurboApp(
    currentUserId: String,
    pendingNavActionFlow: MutableStateFlow<String?>,
    onNavActionConsumed: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val app = context.applicationContext as Application

    val viewModel: TurboModeViewModel = viewModel(
        key = currentUserId,
        factory = remember(app) { TurboModeViewModel.Factory(app) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingNavAction by pendingNavActionFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val turboColors = LocalTurboColors.current

    LaunchedEffect(pendingNavAction) {
        val action = pendingNavAction
        if (!action.isNullOrBlank()) {
            viewModel.handleNotificationAction(action)
            onNavActionConsumed()
        }
    }

    LaunchedEffect(state.errorMessage, state.statusToast) {
        val message = state.errorMessage ?: state.statusToast
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.destination) {
            TurboDestination.Dashboard,
            TurboDestination.StartBlockSheet,
            TurboDestination.EndOfDaySummary -> {
                TurboDashboardScreen(
                    state = state,
                    onToggleDayState = viewModel::onDayStateToggleClicked,
                    onOpenNotifications = viewModel::openNotificationsCenter,
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope,
                            onSignOutComplete = onSignedOut
                        )
                    },
                    onOpenAddTask = { viewModel.setAddTaskDialogVisible(true) },
                    onDismissAddTask = { viewModel.setAddTaskDialogVisible(false) },
                    onSaveTask = viewModel::addTask,
                    onToggleTaskCompleted = viewModel::toggleTaskCompleted,
                    onDeleteTask = viewModel::deleteTask,
                    onTaskClickedForBlock = { taskId -> viewModel.openStartBlockSheet(taskId) },
                    onStartBlockClicked = { viewModel.openStartBlockSheet(null) },
                    onTriggerAuditNode = { node -> viewModel.triggerIncomingAuditCall(node) },
                    onSeedSampleDay = viewModel::seedSampleDay
                )

                if (state.destination == TurboDestination.StartBlockSheet) {
                    StartTimeBlockSheetOverlay(
                        state = state,
                        onDismiss = viewModel::dismissStartBlockSheet,
                        onSelectTask = viewModel::selectTaskForBlock,
                        onUpdateStartTime = viewModel::updateBlockStartTime,
                        onSelectPresetDuration = viewModel::selectPresetDuration,
                        onEnableCustomDuration = viewModel::enableCustomDuration,
                        onUpdateCustomDuration = viewModel::updateCustomDurationInput,
                        onStartBlock = viewModel::startActiveTimeBlock
                    )
                }

                if (state.destination == TurboDestination.EndOfDaySummary) {
                    EndOfDaySummaryOverlay(
                        dailyLog = state.dailyLog,
                        isBusy = state.isBusy,
                        onDismiss = viewModel::dismissEndOfDaySummary,
                        onCloseDay = viewModel::closeDay
                    )
                }
            }

            TurboDestination.ActiveTimeBlock -> {
                val active = state.activeBlock
                if (active != null) {
                    ActiveTimeBlockScreen(
                        session = active,
                        onFinishBlock = viewModel::finishActiveTimeBlock
                    )
                } else {
                    viewModel.returnToDashboard()
                }
            }

            TurboDestination.PostBlockEvaluation -> {
                val draft = state.evaluationDraft
                if (draft != null) {
                    PostBlockEvaluationScreen(
                        draft = draft,
                        isBusy = state.isBusy,
                        onQualityChange = viewModel::updateQualityRate,
                        onFinishingChange = viewModel::updateFinishingRate,
                        onActionChange = viewModel::updateActionRate,
                        onSubmit = viewModel::submitEvaluationAndStartSilence
                    )
                } else {
                    viewModel.returnToDashboard()
                }
            }

            TurboDestination.SilenceReset -> {
                SilenceResetScreen(
                    remainingSeconds = state.silenceRemainingSeconds,
                    showEarlyExitDialog = state.showEarlyExitDialog,
                    onRequestEarlyExit = { viewModel.setEarlyExitDialogVisible(true) },
                    onDismissEarlyExit = { viewModel.setEarlyExitDialogVisible(false) },
                    onConfirmEarlyExit = viewModel::confirmExitSilenceEarly
                )
            }

            TurboDestination.IncomingCallAudit -> {
                IncomingCallAuditScreen(
                    nodeIndex = state.pendingAuditNodeIndex,
                    onAnswer = viewModel::answerAuditCall
                )
            }

            TurboDestination.SevenPointCheckIn -> {
                val session = state.checkInSession
                if (session != null) {
                    SevenPointCheckInScreen(
                        session = session,
                        isBusy = state.isBusy,
                        onAnswerChange = viewModel::updateCurrentCheckInAnswer,
                        onPreviousStep = viewModel::prevCheckInStep,
                        onNextOrSubmit = viewModel::nextOrSubmitCheckIn
                    )
                } else {
                    viewModel.returnToDashboard()
                }
            }

            TurboDestination.LockScreenNotifications -> {
                LockScreenNotificationsScreen(
                    state = state,
                    onBack = viewModel::returnToDashboard,
                    onAnswerAudit = { viewModel.triggerIncomingAuditCall() },
                    onOpenTimer = {
                        if (state.activeBlock != null) {
                            viewModel.handleNotificationAction(TurboNotificationManager.ACTION_OPEN_TIMER)
                        } else {
                            viewModel.openStartBlockSheet(null)
                        }
                    },
                    onFinishBlock = {
                        viewModel.handleNotificationAction(TurboNotificationManager.ACTION_FINISH_BLOCK)
                    },
                    onStartDay = {
                        viewModel.handleNotificationAction(TurboNotificationManager.ACTION_START_DAY)
                    }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 88.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = turboColors.containerFill,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

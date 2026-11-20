package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.DayState
import com.example.data.DisplayTaskItem
import com.example.data.PriorityTask
import com.example.data.PriorityTier
import com.example.data.formatScoreValue
import com.example.notifications.TurboNotificationManager
import com.example.ui.theme.LocalTurboColors
import com.example.viewmodel.TurboUiState
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun TurboDashboardScreen(
    state: TurboUiState,
    onToggleDayState: () -> Unit,
    onOpenActivityProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSignOut: () -> Unit,
    onOpenScoreReport: () -> Unit,
    onOpenAddTask: () -> Unit,
    onDismissAddTask: () -> Unit,
    onSaveTask: (String, PriorityTier) -> Unit,
    onToggleTaskCompleted: (PriorityTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onTaskClickedForBlock: (String) -> Unit,
    onStartBlockClicked: () -> Unit,
    onSeedSampleDay: () -> Unit,
    onNotificationPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val turboColors = LocalTurboColors.current

    // Proactively request POST_NOTIFICATIONS on Android 13+ so live timers & 3-hour audit alarms appear in shade
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onNotificationPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !TurboNotificationManager.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val groupedByTier = remember(state.displayTasks) {
        PriorityTier.entries.mapNotNull { tier ->
            val items = state.displayTasks.filter { it.tier == tier }
            if (items.isNotEmpty()) tier to items else null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 20.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Airy Top Bar
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isWakeUp = state.dailyLog.dayStateEnum() == DayState.WAKE_UP
                        Surface(
                            onClick = onToggleDayState,
                            shape = CircleShape,
                            color = turboColors.elevatedCard,
                            modifier = Modifier
                                .height(42.dp)
                                .border(1.dp, turboColors.hairlineBorder, CircleShape)
                                .testTag("day_state_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isWakeUp) turboColors.success
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                )
                                Text(
                                    text = if (isWakeUp) stringResource(R.string.wake_up) else stringResource(R.string.sleep),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenActivityProfile,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(turboColors.elevatedCard)
                                .border(1.dp, turboColors.hairlineBorder, CircleShape)
                                .testTag("activity_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = stringResource(R.string.activity_profile_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(turboColors.elevatedCard)
                                .border(1.dp, turboColors.hairlineBorder, CircleShape)
                                .testTag("notifications_center_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.notifications_center_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(turboColors.elevatedCard)
                                .border(1.dp, turboColors.hairlineBorder, CircleShape)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.sign_out_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Hero 0–100 Botanical Sage Scorecard (Tap to open Score Calculation Report)
            item(key = "scorecard") {
                ActiveLazinessScorecard(
                    score = state.dailyLog.totalScore,
                    onClick = onOpenScoreReport
                )
            }

            // Priority Queue Section Header
            item(key = "priority_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.priority_queue),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onOpenAddTask,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(turboColors.elevatedCard)
                            .border(1.dp, turboColors.hairlineBorder, CircleShape)
                            .testTag("open_add_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_task_cd),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (groupedByTier.isEmpty() && !state.isLoading) {
                item(key = "empty_queue") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = turboColors.elevatedCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Define your Most Critical task",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add your top priority deliverable (1.5x) or load a structured sample queue across every tier.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Button(
                                    onClick = onOpenAddTask,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("empty_add_task_button")
                                ) {
                                    Text("Add Task", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = onSeedSampleDay,
                                    enabled = !state.isBusy,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = turboColors.recessedSurface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("load_sample_queue_button")
                                ) {
                                    Text("Load Sample Queue", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            } else {
                // Separate boxed priority sections in strict order:
                // Most Critical (pinned at top when present), Most Priority, Priority, Standard, Delegate, Eliminate
                groupedByTier.forEach { (tier, items) ->
                    item(key = "tier_box_${tier.code}") {
                        PriorityTierBoxSection(
                            tier = tier,
                            items = items,
                            onTaskClick = { item ->
                                if (item.tier.isExecutableInBlock) {
                                    onTaskClickedForBlock(item.task.id)
                                } else {
                                    onToggleTaskCompleted(item.task)
                                }
                            },
                            onToggleComplete = { onToggleTaskCompleted(it.task) },
                            onDeleteTask = { onDeleteTask(it.task.id) }
                        )
                    }
                }
            }
        }

        // Floating Bottom Pill CTA (#1B2E29 fill, #F5FAF1 text)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = onStartBlockClicked,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_turbo_time_block_button")
            ) {
                Text(
                    text = stringResource(R.string.start_turbo_time_block),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (state.showAddTaskDialog) {
        AddPriorityTaskDialog(
            isBusy = state.isBusy,
            onDismiss = onDismissAddTask,
            onSave = onSaveTask
        )
    }
}

@Composable
private fun ActiveLazinessScorecard(
    score: Double,
    onClick: () -> Unit
) {
    val turboColors = LocalTurboColors.current
    val passMark = 50.0
    val scaleMax = 100.0
    val isActiveZone = score >= passMark
    val scoreDisplay = formatScoreValue(score)
    val deltaText = if (isActiveZone) {
        "+${(score - passMark).roundToInt()} pts above Active"
    } else {
        "${ceil(passMark - score).toInt()} pts to Active"
    }
    val progressFraction = (score / scaleMax).toFloat().coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = turboColors.elevatedCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(24.dp))
            .testTag("scorecard_open_report")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.active_vs_laziness_mark),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = if (isActiveZone) turboColors.activeBadgeFill else turboColors.warningContainer
                ) {
                    Text(
                        text = if (isActiveZone) stringResource(R.string.active_zone) else stringResource(R.string.laziness_zone),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActiveZone) turboColors.activeBadgeText else turboColors.onWarningContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = scoreDisplay,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress Bar Track #DCEBD2, Laziness Fill #C94A38, Active Fill #659287, Notch #1B2E29 at 50%
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val fullWidth = maxWidth
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(turboColors.recessedSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(14.dp)
                                .clip(CircleShape)
                                .background(if (isActiveZone) turboColors.success else turboColors.warning)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = fullWidth * 0.5f - 1.dp)
                            .width(2.dp)
                            .height(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    Text(
                        text = "50",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = "100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            // Subtle Footer Cue to Open Calculation Report
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.view_calculation_report),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.view_calculation_report),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PriorityTierBoxSection(
    tier: PriorityTier,
    items: List<DisplayTaskItem>,
    onTaskClick: (DisplayTaskItem) -> Unit,
    onToggleComplete: (DisplayTaskItem) -> Unit,
    onDeleteTask: (DisplayTaskItem) -> Unit
) {
    val turboColors = LocalTurboColors.current
    val isCritical = tier == PriorityTier.S
    val isEliminate = tier == PriorityTier.E
    val isMutedTier = tier == PriorityTier.D || tier == PriorityTier.E

    val containerBg = if (isMutedTier) turboColors.recessedSurface else turboColors.elevatedCard
    val borderColor = if (isCritical) {
        MaterialTheme.colorScheme.primary
    } else {
        turboColors.hairlineBorder
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCritical) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("priority_section_${tier.code}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tier.sectionTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCritical) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tier.isExecutableInBlock) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCritical) turboColors.criticalBadgeFill else turboColors.recessedSurface
                    ) {
                        Text(
                            text = tier.multiplierLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCritical) turboColors.criticalBadgeText else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Clean Task Rows (No A-1 / B-1 Badge Clutter)
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = turboColors.hairlineBorder.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                val isCompleted = item.task.completed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTaskClick(item) }
                        .padding(vertical = 4.dp)
                        .testTag("task_row_${item.task.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onToggleComplete(item) }
                                .testTag("task_toggle_${item.task.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCompleted) turboColors.success else Color.Transparent
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            isCompleted -> turboColors.success
                                            isCritical -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = turboColors.onSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = item.task.title,
                            style = if (isCritical) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            color = if (isCompleted || isMutedTier) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (isCompleted || isEliminate) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onDeleteTask(item) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("task_delete_${item.task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPriorityTaskDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, PriorityTier) -> Unit
) {
    val turboColors = LocalTurboColors.current
    var title by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf(PriorityTier.A) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = turboColors.elevatedCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Priority Task",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(turboColors.recessedSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_cd),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Task Title",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextField(
                        value = title,
                        onValueChange = { title = it.take(160) },
                        placeholder = {
                            Text(
                                text = "Single concrete deliverable",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = turboColors.recessedSurface,
                            unfocusedContainerColor = turboColors.recessedSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_task_title_input")
                    )
                }

                // Clean Executive 2-Column Priority Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Priority Level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val tierRows = remember { PriorityTier.entries.chunked(2) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tierRows.forEach { rowTiers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTiers.forEach { tier ->
                                    val selected = tier == selectedTier
                                    Surface(
                                        onClick = { selectedTier = tier },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 58.dp)
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else turboColors.hairlineBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .testTag("select_tier_${tier.code}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tier.sectionTitle,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (tier.isExecutableInBlock) {
                                                    Text(
                                                        text = tier.multiplierLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                text = tier.selectorSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.9f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = turboColors.recessedSurface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, selectedTier) },
                        enabled = title.isNotBlank() && !isBusy,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("save_task_button")
                    ) {
                        Text("Save Task", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

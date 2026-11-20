package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.DayState
import com.example.data.DisplayTaskItem
import com.example.data.PriorityTask
import com.example.data.PriorityTier
import com.example.data.formatHhMmSs
import com.example.ui.theme.LocalTurboColors
import com.example.viewmodel.TurboUiState
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun TurboDashboardScreen(
    state: TurboUiState,
    onToggleDayState: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSignOut: () -> Unit,
    onOpenAddTask: () -> Unit,
    onDismissAddTask: () -> Unit,
    onSaveTask: (String, PriorityTier) -> Unit,
    onToggleTaskCompleted: (PriorityTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onTaskClickedForBlock: (String) -> Unit,
    onStartBlockClicked: () -> Unit,
    onTriggerAuditNode: (Int) -> Unit,
    onSeedSampleDay: () -> Unit
) {
    val turboColors = LocalTurboColors.current
    var secondaryQueuesExpanded by remember { mutableStateOf(false) }

    val sTask = remember(state.displayTasks) {
        state.displayTasks.firstOrNull { it.task.tierEnum() == PriorityTier.S }
    }
    val aTasks = remember(state.displayTasks) {
        state.displayTasks.filter { it.task.tierEnum() == PriorityTier.A }
    }
    val secondaryTasks = remember(state.displayTasks) {
        state.displayTasks.filter {
            val t = it.task.tierEnum()
            t == PriorityTier.B || t == PriorityTier.C || t == PriorityTier.D || t == PriorityTier.E
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isWakeUp = state.dailyLog.dayStateEnum() == DayState.WAKE_UP
                        Surface(
                            onClick = onToggleDayState,
                            shape = CircleShape,
                            color = turboColors.elevatedCard,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("day_state_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(turboColors.elevatedCard)
                                .testTag("notifications_center_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.notifications_center_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(turboColors.elevatedCard)
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

            // Hero Bento Scorecard (Active vs. Laziness Mark)
            item(key = "scorecard") {
                ActiveLazinessScorecard(
                    score = state.dailyLog.totalScore
                )
            }

            // Minimal 7-Point Cadence Strip
            item(key = "cadence_strip") {
                SevenPointCadenceStrip(
                    completedNodes = state.dailyLog.completedNodes.map { it.toInt() }.toSet(),
                    activeNode = state.dailyLog.activeNodeIndex(),
                    countdownSeconds = state.auditCountdownSeconds,
                    onNodeClick = onTriggerAuditNode
                )
            }

            // Priority Queue Section
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(turboColors.containerFill)
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

            if (state.displayTasks.isEmpty() && !state.isLoading) {
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
                                text = "Define your S-1 deliverable",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Start with one high-leverage S-1 priority (1.5x) or load the Turbo Mode sample queue to explore every tier.",
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
                                    modifier = Modifier.testTag("empty_add_task_button")
                                ) {
                                    Text("Add S-1 Task", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = onSeedSampleDay,
                                    enabled = !state.isBusy,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = turboColors.containerFill,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("load_sample_queue_button")
                                ) {
                                    Text("Load Sample Queue", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            } else {
                // S-1 Hero Tactile Card
                if (sTask != null) {
                    item(key = "s1_${sTask.task.id}") {
                        S1HeroTaskCard(
                            item = sTask,
                            onClick = { onTaskClickedForBlock(sTask.task.id) },
                            onToggleComplete = { onToggleTaskCompleted(sTask.task) }
                        )
                    }
                }

                // A-1 to A-3 Borderless List Rows
                if (aTasks.isNotEmpty()) {
                    item(key = "a_tier_list") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            aTasks.forEach { item ->
                                ATierTaskRow(
                                    item = item,
                                    onClick = { onTaskClickedForBlock(item.task.id) },
                                    onToggleComplete = { onToggleTaskCompleted(item.task) }
                                )
                            }
                        }
                    }
                }

                // Single Ultra-Clean Expandable Drawer Pill for B, C, D & E
                if (secondaryTasks.isNotEmpty()) {
                    item(key = "secondary_queues") {
                        SecondaryQueuesDrawer(
                            items = secondaryTasks,
                            expanded = secondaryQueuesExpanded,
                            onToggleExpanded = { secondaryQueuesExpanded = !secondaryQueuesExpanded },
                            onTaskClick = { item ->
                                if (item.task.tierEnum().isExecutableInBlock) {
                                    onTaskClickedForBlock(item.task.id)
                                } else {
                                    onToggleTaskCompleted(item.task)
                                }
                            },
                            onDeleteTask = onDeleteTask
                        )
                    }
                }
            }
        }

        // Floating Bottom Pill CTA (56dp height, safe-area clearance, no box dock)
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
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
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
private fun ActiveLazinessScorecard(score: Double) {
    val turboColors = LocalTurboColors.current
    val passMark = 50.0
    val scaleMax = 75.0
    val isActiveZone = score >= passMark
    val scoreDisplay = if (score % 1.0 == 0.0) score.roundToInt().toString() else String.format("%.1f", score)
    val deltaText = if (isActiveZone) {
        "+${(score - passMark).roundToInt()} pts above Active"
    } else {
        "${ceil(passMark - score).toInt()} pts to Active"
    }
    val progressFraction = (score / scaleMax).toFloat().coerceIn(0f, 1f)

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
                    text = stringResource(R.string.active_vs_laziness_mark),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = if (isActiveZone) turboColors.success else turboColors.warningContainer
                ) {
                    Text(
                        text = if (isActiveZone) stringResource(R.string.active_zone) else stringResource(R.string.laziness_zone),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActiveZone) turboColors.onSuccess else turboColors.onWarningContainer,
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
                        text = "/ 50",
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

            // Thick Tactile Pill Progress Bar with Crisp Threshold Notch at 50 (66.7%)
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
                            .background(turboColors.containerFill)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(14.dp)
                                .clip(CircleShape)
                                .background(if (isActiveZone) turboColors.success else turboColors.warning)
                        )
                    }
                    // Threshold notch at 66.7% (50 / 75)
                    Box(
                        modifier = Modifier
                            .offset(x = fullWidth * 0.667f - 1.dp)
                            .width(2.dp)
                            .height(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fullWidth = maxWidth
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
                        modifier = Modifier.offset(x = fullWidth * 0.667f - 8.dp)
                    )
                    Text(
                        text = "75",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun SevenPointCadenceStrip(
    completedNodes: Set<Int>,
    activeNode: Int,
    countdownSeconds: Long,
    onNodeClick: (Int) -> Unit
) {
    val turboColors = LocalTurboColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 1..7) {
                val isCompleted = index in completedNodes
                val isActive = index == activeNode && !isCompleted
                val bgColor = when {
                    isCompleted -> MaterialTheme.colorScheme.secondary
                    isActive -> MaterialTheme.colorScheme.primaryContainer
                    else -> turboColors.elevatedCard
                }
                val txtColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onSecondary
                    isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onNodeClick(index) }
                        .testTag("cadence_node_t$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .then(
                                if (isActive) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(if (isActive) 3.dp else 0.dp)
                            .clip(CircleShape)
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T$index",
                            style = MaterialTheme.typography.labelSmall,
                            color = txtColor
                        )
                    }
                }
            }
        }

        Text(
            text = "Next 3-hour audit (Time $activeNode) in ${formatHhMmSs(countdownSeconds)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onNodeClick(activeNode) }
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("next_audit_countdown")
        )
    }
}

@Composable
private fun S1HeroTaskCard(
    item: DisplayTaskItem,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val turboColors = LocalTurboColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = turboColors.elevatedCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(24.dp))
            .testTag("s1_hero_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onToggleComplete,
                    shape = CircleShape,
                    color = if (item.task.completed) turboColors.success else MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (item.task.completed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = turboColors.onSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = item.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.task.completed) turboColors.onSuccess else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = item.multiplierLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ATierTaskRow(
    item: DisplayTaskItem,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val turboColors = LocalTurboColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onToggleComplete,
                shape = CircleShape,
                color = if (item.task.completed) turboColors.success else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = item.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.task.completed) turboColors.onSuccess else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(
                text = item.task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.task.completed) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.multiplierLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecondaryQueuesDrawer(
    items: List<DisplayTaskItem>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTaskClick: (DisplayTaskItem) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val turboColors = LocalTurboColors.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = turboColors.recessedSurface,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(200))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("secondary_queues_drawer_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "B, C, D & E Queues (${items.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse queues" else "Expand queues",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        val tier = item.task.tierEnum()
                        val isEliminate = tier == PriorityTier.E
                        val isMuted = tier == PriorityTier.D || tier == PriorityTier.E || item.task.completed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTaskClick(item) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.badge}: ${item.task.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isEliminate || item.task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.multiplierLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { onDeleteTask(item.task.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete task",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPriorityTaskDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, PriorityTier) -> Unit
) {
    val turboColors = LocalTurboColors.current
    var title by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf(PriorityTier.A) }
    var dropdownExpanded by remember { mutableStateOf(false) }

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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Priority Tier",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        TextField(
                            value = selectedTier.dropdownLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            containerColor = turboColors.elevatedCard
                        ) {
                            PriorityTier.entries.forEach { tier ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tier.dropdownLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedTier = tier
                                        dropdownExpanded = false
                                    }
                                )
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
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, selectedTier) },
                        enabled = title.trim().isNotEmpty() && !isBusy,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("save_add_task_button")
                    ) {
                        Text("Save Task", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

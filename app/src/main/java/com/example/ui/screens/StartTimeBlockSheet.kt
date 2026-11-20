package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PriorityTier
import com.example.ui.theme.LocalTurboColors
import com.example.ui.theme.TurboMonoMedium
import com.example.viewmodel.TurboUiState
import java.util.Calendar
import java.util.Locale

@Composable
fun StartTimeBlockSheetOverlay(
    state: TurboUiState,
    onDismiss: () -> Unit,
    onSelectTask: (String) -> Unit,
    onUpdateStartTime: (String) -> Unit,
    onSelectPresetDuration: (Int) -> Unit,
    onEnableCustomDuration: () -> Unit,
    onUpdateCustomDuration: (String) -> Unit,
    onStartBlock: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val turboColors = LocalTurboColors.current
    val context = LocalContext.current

    val selectableTasks = remember(state.displayTasks) {
        state.displayTasks.filter { it.task.tierEnum().isExecutableInBlock }
    }
    val hasValidSelection = selectableTasks.any { it.task.id == state.selectedTaskIdForBlock }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(turboColors.modalBackdrop)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = turboColors.elevatedCard,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .border(
                    width = 1.dp,
                    color = turboColors.hairlineBorder,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Centered Minimal Grabber Pill
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(turboColors.hairlineBorder)
                    )
                }

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Select Task to Begin",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bind this block to one concrete priority.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(turboColors.containerFill)
                            .testTag("close_start_block_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_cd),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Spacious 56dp Selectable Task Pills
                if (selectableTasks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = turboColors.recessedSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Add at least one S, A, B, or C priority task on the dashboard before starting a block.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectableTasks, key = { it.task.id }) { item ->
                            val isSelected = item.task.id == state.selectedTaskIdForBlock
                            val isS = item.task.tierEnum() == PriorityTier.S
                            Surface(
                                onClick = { onSelectTask(item.task.id) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) turboColors.recessedSurface else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 58.dp)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else turboColors.hairlineBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .testTag("block_task_option_${item.task.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = item.task.title,
                                            style = if (isS) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.sectionTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isS) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.multiplierLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline Start Time Pill + Horizontal Duration Chips
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start Time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            onClick = {
                                val parts = state.blockStartTimeLabel.split(":")
                                val cal = Calendar.getInstance()
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                                val min = parts.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        onUpdateStartTime(String.format(Locale.US, "%02d:%02d", h, m))
                                    },
                                    hour,
                                    min,
                                    true
                                ).show()
                            },
                            shape = CircleShape,
                            color = turboColors.containerFill,
                            modifier = Modifier.testTag("start_time_picker_pill")
                        ) {
                            Text(
                                text = state.blockStartTimeLabel,
                                style = TurboMonoMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val presets = listOf(25, 45, 60, 90)
                        presets.forEach { mins ->
                            val selected = !state.isCustomDuration && state.selectedDurationMinutes == mins
                            DurationChip(
                                label = "${mins}m",
                                selected = selected,
                                onClick = { onSelectPresetDuration(mins) },
                                tag = "duration_chip_${mins}m"
                            )
                        }
                        DurationChip(
                            label = "Custom",
                            selected = state.isCustomDuration,
                            onClick = onEnableCustomDuration,
                            tag = "duration_chip_custom"
                        )
                    }

                    if (state.isCustomDuration) {
                        TextField(
                            value = state.customDurationInput,
                            onValueChange = onUpdateCustomDuration,
                            singleLine = true,
                            label = { Text("Minutes (1-240)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = turboColors.recessedSurface,
                                unfocusedContainerColor = turboColors.recessedSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Floating 56dp Full-Pill CTA
                Button(
                    onClick = onStartBlock,
                    enabled = hasValidSelection,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = turboColors.containerFill,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("confirm_start_block_button")
                ) {
                    Text(
                        text = "Start Block",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val turboColors = LocalTurboColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondary else turboColors.recessedSurface,
        modifier = Modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CheckInRecord
import com.example.data.DailyLog
import com.example.data.TimeBlockRecord
import com.example.data.currentDateSubtitle
import com.example.data.formatScoreValue
import com.example.data.formatTimestampHhMm
import com.example.ui.theme.LocalTurboColors
import com.example.ui.theme.TurboMonoMedium
import java.util.Locale
import kotlin.math.round

@Composable
fun ScoreCalculationReportModal(
    dailyLog: DailyLog,
    timeBlocks: List<TimeBlockRecord>,
    checkIns: List<CheckInRecord>,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val turboColors = LocalTurboColors.current
    val totalScore = dailyLog.totalScore
    val isActiveZone = totalScore >= 50.0
    val focusBlocksTotal = round(timeBlocks.sumOf { it.earnedPoints } * 10.0) / 10.0
    val auditsTotal = checkIns.sumOf { it.pointsAwarded }.toDouble()

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
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item(key = "report_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.score_calculation_report),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentDateSubtitle(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(turboColors.elevatedCard)
                            .testTag("close_score_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_cd),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Reconciled Daily Total Card
            item(key = "report_summary") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = turboColors.elevatedCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                    text = formatScoreValue(totalScore),
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

                        HorizontalDivider(color = turboColors.hairlineBorder.copy(alpha = 0.6f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReconciliationRow(
                                label = "Focus Blocks (${timeBlocks.size})",
                                value = "+${formatScoreValue(focusBlocksTotal)} pts"
                            )
                            ReconciliationRow(
                                label = "3-Hour Audits (${checkIns.size} × 5 pts)",
                                value = "+${formatScoreValue(auditsTotal)} pts"
                            )
                            HorizontalDivider(
                                color = turboColors.hairlineBorder.copy(alpha = 0.45f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            ReconciliationRow(
                                label = "Daily Mark (50 pass mark)",
                                value = "${formatScoreValue(totalScore)} / 100",
                                emphasize = true
                            )
                        }
                    }
                }
            }

            // Focus Blocks Section
            item(key = "blocks_header") {
                Text(
                    text = "Focus Blocks (${timeBlocks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (timeBlocks.isEmpty()) {
                item(key = "blocks_empty") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = turboColors.recessedSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No completed focus blocks logged today yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(timeBlocks, key = { it.id.ifBlank { "${it.startTimeLabel}_${it.taskTitle}_${it.earnedPoints}" } }) { block ->
                    FocusBlockBreakdownCard(block = block)
                }
            }

            // 3-Hour Audits Section
            item(key = "audits_header") {
                Text(
                    text = "3-Hour Audits (${checkIns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (checkIns.isEmpty()) {
                item(key = "audits_empty") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = turboColors.recessedSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No 3-hour audit check-ins logged today yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(checkIns, key = { it.id.ifBlank { "${it.nodeIndex}_${it.durationSeconds}" } }) { checkIn ->
                    AuditBreakdownCard(checkIn = checkIn)
                }
            }

            // Scoring Formula Reference
            item(key = "formula_reference") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = turboColors.recessedSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, turboColors.hairlineBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "How Your Score Is Calculated",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Focus Block = (Quality 1–10 + Finishing 1–5 + Action 1–20) × Priority Multiplier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Multipliers: Most Critical 1.5x · Most Priority 1.0x · Priority 0.75x · Standard 0.5x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "3-Hour Audit = +5 pts when all 3 core questions are answered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pass Mark: 50 / 100 separates Laziness Zone (< 50) from Active Zone (50+).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else TurboMonoMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FocusBlockBreakdownCard(block: TimeBlockRecord) {
    val turboColors = LocalTurboColors.current
    val tier = block.tierEnum()
    val multiplierFormatted = if (block.multiplier % 1.0 == 0.0) {
        String.format(Locale.US, "%.1fx", block.multiplier)
    } else {
        "${block.multiplier}x"
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = turboColors.elevatedCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = block.taskTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tier.sectionTitle} · ${block.durationMinutes}m · ${block.startTimeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "+${formatScoreValue(block.earnedPoints)} pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = turboColors.recessedSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Q(${block.qualityRate}) + F(${block.finishingRate}) + A(${block.actionRate}) = ${block.rawScore}/35 × $multiplierFormatted = +${formatScoreValue(block.earnedPoints)}",
                    style = TurboMonoMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AuditBreakdownCard(checkIn: CheckInRecord) {
    val turboColors = LocalTurboColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = turboColors.elevatedCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3-Hour Audit · ${formatTimestampHhMm(checkIn.createdAt)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "+${checkIn.pointsAwarded} pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (checkIn.creativeIq.isNotBlank()) {
                AuditSnippetRow("Creative IQ", checkIn.creativeIq)
            }
            if (checkIn.emotionEq.isNotBlank()) {
                AuditSnippetRow("Emotion EQ", checkIn.emotionEq)
            }
            if (checkIn.bestThinking.isNotBlank()) {
                AuditSnippetRow("Best Thinking", checkIn.bestThinking)
            }
        }
    }
}

@Composable
private fun AuditSnippetRow(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

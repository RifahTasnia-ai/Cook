package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyLog
import com.example.data.formatScoreValue
import com.example.data.formatTrackedDuration
import com.example.ui.theme.LocalTurboColors
import com.example.viewmodel.TurboUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.round

private data class HeatmapDayCell(
    val dateKey: String,
    val displayDate: String,
    val isToday: Boolean,
    val log: DailyLog?
) {
    val score: Double get() = log?.totalScore ?: 0.0
}

/**
 * Screen 10: Activity Profile — Botanical Sage Daylight 6-step Red-to-Green Activity Heatmap,
 * recessed Selected-Day Inspector Pill, and 2x2 Bento Summary Tiles.
 */
@Composable
fun ActivityProfileScreen(
    state: TurboUiState,
    onSelectDate: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val turboColors = LocalTurboColors.current
    val totalDays = 84 // 12 weeks x 7 days
    val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.US) }

    val dayCells = remember(state.dailyLogsByDate, state.dateKey) {
        val cal = Calendar.getInstance()
        val list = ArrayList<HeatmapDayCell>(totalDays)
        for (offset in (totalDays - 1) downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val key = dateKeyFormat.format(cal.time)
            list.add(
                HeatmapDayCell(
                    dateKey = key,
                    displayDate = displayFormat.format(cal.time),
                    isToday = key == state.dateKey,
                    log = state.dailyLogsByDate[key]
                )
            )
        }
        list
    }

    val weekColumns = remember(dayCells) { dayCells.chunked(7) }
    val selectedCell = remember(dayCells, state.selectedProfileDateKey) {
        dayCells.firstOrNull { it.dateKey == state.selectedProfileDateKey } ?: dayCells.last()
    }

    // Live Summary Metrics
    val activeStreak = remember(dayCells) {
        var streak = 0
        val startIdx = if (dayCells.last().score >= 50.0) dayCells.lastIndex else dayCells.lastIndex - 1
        for (i in startIdx downTo 0) {
            if (dayCells[i].score >= 50.0) streak++ else break
        }
        streak
    }
    val thirtyDayAvg = remember(dayCells) {
        val last30 = dayCells.takeLast(30)
        val logged = last30.filter { it.log != null }
        if (logged.isEmpty()) 0.0 else round((logged.sumOf { it.score } / logged.size) * 10.0) / 10.0
    }
    val activeDaysCount = remember(dayCells) {
        dayCells.count { it.score >= 50.0 }
    }
    val totalTrackedMinutes = remember(state.dailyLogsByDate) {
        state.dailyLogsByDate.values.sumOf { it.trackedMinutes }
    }

    val gridScrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        gridScrollState.scrollTo(gridScrollState.maxValue)
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
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Bar
            item(key = "profile_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(turboColors.elevatedCard)
                                .border(1.dp, turboColors.hairlineBorder, CircleShape)
                                .testTag("profile_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to dashboard",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.activity_profile),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = turboColors.elevatedCard,
                        modifier = Modifier.border(1.dp, turboColors.hairlineBorder, CircleShape)
                    ) {
                        Text(
                            text = "12 Weeks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Elevated #F5FAF1 Activity Heatmap Card + Recessed #E6F2DD Selected-Day Inspector Pill
            item(key = "heatmap_card") {
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
                            Text(
                                text = "Daily Activity Heatmap",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                listOf(
                                    turboColors.heatmapLevel0,
                                    turboColors.heatmapLevel1,
                                    turboColors.heatmapLevel2,
                                    turboColors.heatmapLevel3,
                                    turboColors.heatmapLevel4,
                                    turboColors.heatmapLevel5
                                ).forEach { swatch ->
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(swatch)
                                    )
                                }
                                Text(
                                    text = "90+",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 12-Week (84-Day) Red-to-Green Interactive Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(gridScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekColumns.forEach { week ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    week.forEach { cell ->
                                        val isSelected = cell.dateKey == selectedCell.dateKey
                                        val fillColor = turboColors.heatmapColorForScore(cell.score)
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(fillColor)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.border(
                                                            width = 2.dp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable { onSelectDate(cell.dateKey) }
                                                .testTag("heatmap_day_${cell.dateKey}")
                                        )
                                    }
                                }
                            }
                        }

                        // Recessed #E6F2DD Selected-Day Inspector Pill
                        val selectedScore = selectedCell.score
                        val isActiveDay = selectedScore >= 50.0
                        val badgeLabel = when {
                            selectedScore >= 90.0 -> "Peak Turbo"
                            selectedScore >= 50.0 -> "Active Zone"
                            selectedScore > 0.0 -> "Laziness Zone"
                            else -> "Missed"
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(16.dp))
                                .testTag("selected_day_inspector")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedCell.isToday) "Today · ${selectedCell.displayDate}" else selectedCell.displayDate,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isActiveDay) turboColors.activeBadgeFill else turboColors.warningContainer
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isActiveDay) turboColors.activeBadgeText else turboColors.onWarningContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
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
                                            text = formatScoreValue(selectedScore),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "/ 100",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }

                                    Text(
                                        text = "${formatTrackedDuration(selectedCell.log?.trackedMinutes ?: 0L)} tracked · +${selectedCell.log?.checkInPoints ?: 0L} audit pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2x2 Bento Summary Tiles in #F5FAF1
            item(key = "bento_metrics") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileBentoTile(
                            label = "Active Streak",
                            value = "$activeStreak ${if (activeStreak == 1) "day" else "days"}",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileBentoTile(
                            label = "30-Day Avg",
                            value = "${formatScoreValue(thirtyDayAvg)} / 100",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileBentoTile(
                            label = "Active Days (50+)",
                            value = "$activeDaysCount / $totalDays",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileBentoTile(
                            label = "Total Tracked",
                            value = formatTrackedDuration(totalTrackedMinutes),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6-Step Scale Key Card
            item(key = "scale_legend") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = turboColors.elevatedCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Heatmap Score Scale",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LegendRow(turboColors.heatmapLevel0, "0 pts", "Missed / Inactive")
                            LegendRow(turboColors.heatmapLevel1, "1–29 pts", "Heavy Laziness Zone")
                            LegendRow(turboColors.heatmapLevel2, "30–49 pts", "Near Pass Mark")
                            LegendRow(turboColors.heatmapLevel3, "50–69 pts", "Active Zone Pass")
                            LegendRow(turboColors.heatmapLevel4, "70–89 pts", "Strong Active Day")
                            LegendRow(turboColors.heatmapLevel5, "90+ pts", "Peak Turbo Output")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBentoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val turboColors = LocalTurboColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = turboColors.elevatedCard,
        modifier = modifier.border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LegendRow(
    swatch: Color,
    rangeLabel: String,
    meaning: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(swatch)
            )
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = meaning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

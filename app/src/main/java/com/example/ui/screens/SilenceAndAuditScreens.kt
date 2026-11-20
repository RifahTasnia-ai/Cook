package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.TurboRepository
import com.example.data.formatMmSs
import com.example.ui.theme.LocalTurboColors
import com.example.viewmodel.CheckInSession
import kotlin.math.roundToInt

/**
 * Screen 5: 121-Second Silence Reset — Stillness countdown with early-exit confirmation guard.
 */
@Composable
fun SilenceResetScreen(
    remainingSeconds: Int,
    showEarlyExitDialog: Boolean,
    onRequestEarlyExit: () -> Unit,
    onDismissEarlyExit: () -> Unit,
    onConfirmEarlyExit: () -> Unit
) {
    BackHandler(onBack = onRequestEarlyExit)

    val turboColors = LocalTurboColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Centered Stillness Focal Point
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = formatMmSs(remainingSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("silence_reset_timer")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Text(
                        text = "1. Without phone and any electronic device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "2. Spend alone in silence, like meditation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Subtle Bottom Early-Exit Link
            TextButton(
                onClick = onRequestEarlyExit,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .height(48.dp)
                    .testTag("end_reset_early_button")
            ) {
                Text(
                    text = "End reset early",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showEarlyExitDialog) {
        Dialog(onDismissRequest = onDismissEarlyExit) {
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
                    Text(
                        text = "Exit silence early?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Leaving before 00:00 interrupts your 121-second attention-residue reset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismissEarlyExit,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = turboColors.recessedSurface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("stay_in_silence_button")
                        ) {
                            Text("Stay in Silence", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = onConfirmEarlyExit,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = turboColors.error,
                                contentColor = turboColors.onError
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("confirm_exit_silence_button")
                        ) {
                            Text("Exit Early", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 6: 3-Hour Incoming Call Interruption — Pulsing rings + Slide to Answer only (NO Decline/Snooze).
 */
@Composable
fun IncomingCallAuditScreen(
    onAnswer: () -> Unit
) {
    // Strictly mandatory audit interruption: no decline/dismiss/snooze
    BackHandler(enabled = true) { /* Slide to answer required */ }

    val infiniteTransition = rememberInfiniteTransition(label = "audit_ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MANDATORY CHECK-IN",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Centered Minimal Pulsing Geometric Ring Indicator & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Box(
                    modifier = Modifier.size(144.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "3-Hour Audit",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "3-Hour Turbo Audit",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.three_hour_audit_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Single Bottom Tactile Slide to Answer Control (Strictly NO Decline/Dismiss/Snooze)
            SlideToAnswerControl(
                onAnswered = onAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun SlideToAnswerControl(
    onAnswered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val turboColors = LocalTurboColors.current
    val density = LocalDensity.current
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var answered by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .height(58.dp)
            .clip(CircleShape)
            .background(turboColors.elevatedCard)
            .border(1.dp, turboColors.hairlineBorder, CircleShape)
            .padding(horizontal = 6.dp)
            .testTag("slide_to_answer_track"),
        contentAlignment = Alignment.CenterStart
    ) {
        val thumbSizeDp = 46.dp
        val maxOffsetPx = with(density) { (maxWidth - thumbSizeDp).toPx().coerceAtLeast(1f) }
        val animatedOffset by animateFloatAsState(
            targetValue = dragOffsetPx,
            animationSpec = tween(durationMillis = 120),
            label = "slide_thumb_offset"
        )
        val progress = (dragOffsetPx / maxOffsetPx).coerceIn(0f, 1f)

        Text(
            text = if (answered || progress >= 0.88f) "Answering Audit..." else "Slide to Answer",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = (1f - progress * 0.45f).coerceIn(0.4f, 1f)),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (!answered) {
                            dragOffsetPx = (dragOffsetPx + delta).coerceIn(0f, maxOffsetPx)
                        }
                    },
                    onDragStopped = {
                        if (!answered && dragOffsetPx / maxOffsetPx >= 0.85f) {
                            answered = true
                            dragOffsetPx = maxOffsetPx
                            onAnswered()
                        } else if (!answered) {
                            dragOffsetPx = 0f
                        }
                    }
                )
                .testTag("slide_to_answer_thumb"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Slide to Answer",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Screen 7: 3-Hour Check-In — Strictly validated 3-question reflection flow (Creative IQ, Emotion EQ, Best Thinking).
 */
@Composable
fun SevenPointCheckInScreen(
    session: CheckInSession,
    isBusy: Boolean,
    onAnswerChange: (String) -> Unit,
    onPreviousStep: () -> Unit,
    onNextOrSubmit: () -> Unit
) {
    BackHandler(enabled = true) {
        if (session.currentStep > 0) {
            onPreviousStep()
        }
    }

    val turboColors = LocalTurboColors.current
    val steps = remember {
        listOf(
            "Step 1 of 3 • Creative IQ" to "Did you use Creative IQ?",
            "Step 2 of 3 • Emotion EQ" to "What Emotion EQ?",
            "Step 3 of 3 • Best Thinking" to "What is your best Thinking?"
        )
    }
    val stepIndex = session.currentStep.coerceIn(0, 2)
    val (eyebrow, promptText) = steps[stepIndex]
    val currentText = session.answers.getOrElse(stepIndex) { "" }
    val isFinalStep = stepIndex == 2
    val minLen = TurboRepository.MIN_CHECKIN_ANSWER_LENGTH
    val trimmedLen = session.currentAnswerTrimmedLength
    val canProceed = !isBusy && session.isCurrentAnswerValid && (!isFinalStep || session.allAnswersValid)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Slim Floating Live Status Pill at Top (#27524B)
            Surface(
                shape = CircleShape,
                color = turboColors.containerFill,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = "3-Hour Audit • ${session.startedAtLabel} • ${formatMmSs(session.elapsedSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Conversational Card-Stack Reflection Sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                // Subtle Stacked-Card Depth Peeking Underneath (#122724)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = 10.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(turboColors.recessedSurface)
                )

                // Active Focused Prompt Card (#17322E with 1dp #27524B hairline)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = turboColors.elevatedCard,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 380.dp)
                        .border(1.dp, turboColors.hairlineBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 3-Step Progress Pill Bar
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (idx in 0..2) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (idx <= stepIndex) MaterialTheme.colorScheme.secondary
                                                else turboColors.containerFill
                                            )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = eyebrow,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+5 pts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Single Editorial Prompt + Completely Borderless Writing Canvas
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp)
                            ) {
                                if (currentText.isEmpty()) {
                                    Text(
                                        text = "Write a specific, honest reflection...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                BasicTextField(
                                    value = currentText,
                                    onValueChange = onAnswerChange,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 150.dp)
                                        .testTag("reflection_canvas_input")
                                )
                            }

                            // Quiet Inline Validation Guard
                            Text(
                                text = if (session.isCurrentAnswerValid) {
                                    if (isFinalStep) "All 3 answers complete" else "Answer recorded"
                                } else {
                                    "Write at least $minLen characters to continue ($trimmedLen/$minLen)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (session.isCurrentAnswerValid) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    turboColors.warning
                                },
                                modifier = Modifier.testTag("checkin_validation_hint")
                            )
                        }
                    }
                }
            }

            // Bottom Floating Pill Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onPreviousStep,
                    enabled = stepIndex > 0 && !isBusy,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(turboColors.elevatedCard)
                        .testTag("checkin_prev_step_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous prompt",
                        tint = if (stepIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onNextOrSubmit,
                    enabled = canProceed,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = turboColors.containerFill,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("checkin_next_or_submit_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isFinalStep) "Submit & Log (+5 pts)" else "Next Prompt",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

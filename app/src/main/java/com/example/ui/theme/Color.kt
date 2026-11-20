package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Botanical Sage Daylight — Core Color Hunt Swatches (#659287, #88BDA4, #B1D3B9, #E6F2DD)
val SageDeepAccent = Color(0xFF659287)
val SageMintAccent = Color(0xFF88BDA4)
val SageMistBorder = Color(0xFFB1D3B9)
val SageDewCanvas = Color(0xFFE6F2DD)

// Derived Accessibility Companions (>10:1 WCAG AAA contrast with Deep Botanical Pine Ink)
val SageElevatedCard = Color(0xFFF5FAF1)
val SageRecessedInset = Color(0xFFDCEBD2)
val SagePineInk = Color(0xFF1B2E29)
val SageSecondaryInk = Color(0xFF47665E)
val SageDarkTrack = Color(0xFF2B453E)

// Semantic Status Tokens
val SageLazinessText = Color(0xFFA63A2B)
val SageLazinessFill = Color(0xFFF4DDD8)
val SageLazinessBar = Color(0xFFC94A38)
val SageActiveBadgeFill = Color(0xFF88BDA4)
val SageActiveBadgeText = Color(0xFF1B2E29)
val SageCriticalFill = Color(0xFF1B2E29)
val SageCriticalText = Color(0xFFE6F2DD)

// Activity Heatmap 6-Step Red-to-Green Scale
val HeatmapLevel0Missed = Color(0xFFC94A38)       // 0 pts
val HeatmapLevel1HeavyLazy = Color(0xFFDC7A68)    // 1 - 29 pts
val HeatmapLevel2NearPass = Color(0xFFE8B4A8)     // 30 - 49 pts
val HeatmapLevel3ActivePass = Color(0xFFB1D3B9)   // 50 - 69 pts
val HeatmapLevel4StrongActive = Color(0xFF88BDA4) // 70 - 89 pts
val HeatmapLevel5PeakTurbo = Color(0xFF2E6B53)    // 90+ pts

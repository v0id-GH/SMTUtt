package com.smtutt.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Data structure for VS Code syntax and UI palette
data class AppColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // Time badge
    val timeText: Color,
    val timeBg: Color,
    val timeBorder: Color,

    // Week badges
    val upperWeekText: Color,
    val upperWeekBg: Color,
    val lowerWeekText: Color,
    val lowerWeekBg: Color,

    // Lesson badges (VS Code syntax styles)
    val lectureText: Color,
    val lectureBg: Color,
    val lectureBorder: Color,

    val practiceText: Color,
    val practiceBg: Color,
    val practiceBorder: Color,

    val labText: Color,
    val labBg: Color,
    val labBorder: Color,

    val otherText: Color,
    val otherBg: Color,
    val otherBorder: Color,

    val subgroupText: Color,
    val subgroupBg: Color,
    val subgroupBorder: Color
)

// VS Code Dark Modern / Dark+ Palette
val VSCodeDarkAppColors = AppColors(
    isDark = true,
    bg = Color(0xFF1E1E1E),            // VS Code editor background
    surface = Color(0xFF252526),       // VS Code side bar & card background
    surfaceElevated = Color(0xFF2D2D2D),// VS Code dropdown / active tab
    border = Color(0xFF383838),        // VS Code line separator
    textPrimary = Color(0xFFE2E2E2),   // Crisp light text
    textSecondary = Color(0xFF9E9E9E), // Muted editor text
    textMuted = Color(0xFF757575),
    primary = Color(0xFF3794FF),       // VS Code bright blue
    primaryContainer = Color(0xFF0E639C),
    onPrimaryContainer = Color(0xFFFFFFFF),

    timeText = Color(0xFF4FC1FF),
    timeBg = Color(0xFF162A3D),
    timeBorder = Color(0xFF264F78),

    upperWeekText = Color(0xFF4FC1FF),
    upperWeekBg = Color(0xFF162A3D),
    lowerWeekText = Color(0xFF4EC9B0),
    lowerWeekBg = Color(0xFF1A332C),

    lectureText = Color(0xFF4FC1FF),   // Syntax cyan/blue
    lectureBg = Color(0xFF162A3D),
    lectureBorder = Color(0xFF264F78),

    practiceText = Color(0xFF6A9955),  // Syntax comment green
    practiceBg = Color(0xFF1E2D22),
    practiceBorder = Color(0xFF2E4E36),

    labText = Color(0xFFCE9178),       // Syntax string orange/peach
    labBg = Color(0xFF33251E),
    labBorder = Color(0xFF5E3A25),

    otherText = Color(0xFFC586C0),     // Syntax keyword purple
    otherBg = Color(0xFF2E1F33),
    otherBorder = Color(0xFF56315F),

    subgroupText = Color(0xFFD4D4D4),
    subgroupBg = Color(0xFF2D2D2D),
    subgroupBorder = Color(0xFF3C3C3C)
)

// VS Code Light Modern / Light+ Palette
val VSCodeLightAppColors = AppColors(
    isDark = false,
    bg = Color(0xFFF3F3F3),            // VS Code light editor side
    surface = Color(0xFFFFFFFF),       // Pure white card
    surfaceElevated = Color(0xFFF8F8F8),
    border = Color(0xFFE5E5E5),        // VS Code light border
    textPrimary = Color(0xFF1E1E1E),   // VS Code dark text
    textSecondary = Color(0xFF616161), // Readable secondary text
    textMuted = Color(0xFF8A8A8A),
    primary = Color(0xFF007ACC),       // VS Code classic blue
    primaryContainer = Color(0xFFDCEBFA),
    onPrimaryContainer = Color(0xFF00447A),

    timeText = Color(0xFF005FB8),
    timeBg = Color(0xFFEBF3FB),
    timeBorder = Color(0xFFC4DCF3),

    upperWeekText = Color(0xFF005FB8),
    upperWeekBg = Color(0xFFEBF3FB),
    lowerWeekText = Color(0xFF107C10),
    lowerWeekBg = Color(0xFFEDF8ED),

    lectureText = Color(0xFF005FB8),   // Syntax blue
    lectureBg = Color(0xFFEBF3FB),
    lectureBorder = Color(0xFFC4DCF3),

    practiceText = Color(0xFF107C10),  // Syntax green
    practiceBg = Color(0xFFEDF8ED),
    practiceBorder = Color(0xFFBCE3BC),

    labText = Color(0xFFA83800),       // Syntax orange/rust
    labBg = Color(0xFFFDF1EB),
    labBorder = Color(0xFFF6CEB9),

    otherText = Color(0xFF881798),     // Syntax purple
    otherBg = Color(0xFFF8EEFA),
    otherBorder = Color(0xFFE4C3E9),

    subgroupText = Color(0xFF424242),
    subgroupBg = Color(0xFFF0F0F0),
    subgroupBorder = Color(0xFFE0E0E0)
)

val LocalAppColors = staticCompositionLocalOf {
    VSCodeLightAppColors
}

// Backward compatibility constants
val NavyPrimary = Color(0xFF007ACC)
val NavyDark = Color(0xFF00447A)
val NavyLight = Color(0xFFDCEBFA)
val SeaTeal = Color(0xFF107C10)
val BackgroundLight = Color(0xFFF3F3F3)
val SurfaceLight = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1E1E1E)
val TextSecondary = Color(0xFF616161)
val TextMuted = Color(0xFF8A8A8A)

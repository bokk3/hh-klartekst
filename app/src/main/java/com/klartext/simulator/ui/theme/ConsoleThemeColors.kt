package com.klartext.simulator.ui.theme

import androidx.compose.ui.graphics.Color
import com.klartext.simulator.model.ConsoleTheme

/**
 * Palette configuration for the two operating panel themes:
 * 1. CLASSIC_TNC640: Authentic HEIDENHAIN TE 640 industrial hardware console (light metallic grey casing,
 *    ivory numpad keys with dark text, dark graphite contour keys with orange legends, blue-grey cycle keys,
 *    rich machine green ENT, warning crimson NO ENT, and brushed aluminum rotary dials).
 * 2. HYPERMODERN: Sleek cyber dark slate CNC cockpit with glowing cyan and amber accents.
 */
data class ConsoleColors(
    val theme: ConsoleTheme,
    val isClassic: Boolean,
    val casingBackground: Color,
    val casingBorder: Color,
    val headerBackground: Color,
    val headerTextColor: Color,
    val headerBadgeBackground: Color,
    val headerBadgeTextColor: Color,
    val sectionBackground: Color,
    val sectionBorder: Color,
    val sectionTitleColor: Color,
    val numpadKeyBackground: Color,
    val numpadKeyText: Color,
    val numpadKeyBorder: Color,
    val deleteKeyBackground: Color,
    val deleteKeyText: Color,
    val contourKeyBackground: Color,
    val contourKeyText: Color,
    val cycleKeyBackground: Color,
    val cycleKeyText: Color,
    val axisKeyBackground: Color,
    val axisKeyBorder: Color,
    val axisKeyTextXy: Color,
    val axisKeyTextZ: Color,
    val entKeyBackground: Color,
    val entKeyText: Color,
    val noEntKeyBackground: Color,
    val noEntKeyText: Color,
    val ncStartBackground: Color,
    val ncStartText: Color,
    val ncStopBackground: Color,
    val ncStopText: Color,
    val singleBlockActive: Color,
    val singleBlockInactive: Color,
    val singleBlockActiveText: Color,
    val singleBlockInactiveText: Color,
    val dialCardBackground: Color,
    val dialCardBorder: Color,
    val dialTrackColor: Color,
    val dialKnobColors: List<Color>,
    val dialPointerColor: Color,
    val dialTickColor: Color,
    val dialTitleColor: Color,
    val dialPillBackground: Color,
    val dialPillText: Color,
    val softkeyBackground: Color,
    val softkeyText: Color,
    val softkeyBorder: Color
)

val ClassicTnc640ConsoleColors = ConsoleColors(
    theme = ConsoleTheme.CLASSIC_TNC640,
    isClassic = true,
    casingBackground = Color(0xFFC7CED6),        // Traditional HEIDENHAIN TE 640 light metallic grey (RAL 7035)
    casingBorder = Color(0xFF98A2AE),            // Machined bezel edge
    headerBackground = Color(0xFF2E3842),        // Dark graphite TE 640 top strip
    headerTextColor = Color(0xFFFFFFFF),        // High-contrast white logo text
    headerBadgeBackground = Color(0xFFFF9100),   // Heidenhain orange badge
    headerBadgeTextColor = Color(0xFF1B2228),
    sectionBackground = Color(0xFFB8C2CC),       // Recessed tray for key blocks
    sectionBorder = Color(0xFFA2ACB8),
    sectionTitleColor = Color(0xFF1E293B),       // Clear dark slate section headings
    numpadKeyBackground = Color(0xFFF8FAFC),     // Ivory / pure off-white numeric keys
    numpadKeyText = Color(0xFF0F172A),           // Bold dark ink text
    numpadKeyBorder = Color(0xFF94A3B8),         // Keycap 3D border
    deleteKeyBackground = Color(0xFFE2E8F0),     // CE/DEL key
    deleteKeyText = Color(0xFFDC2626),           // Red DEL label
    contourKeyBackground = Color(0xFF1E2832),    // Dark graphite keys for L, C, CR, CC, RND, CHF
    contourKeyText = Color(0xFFFF9100),          // Iconic Heidenhain orange contour symbols
    cycleKeyBackground = Color(0xFF243447),      // Blue-grey keys for APPR, DEP, CYCL DEF, TOOL CALL
    cycleKeyText = Color(0xFF67E8F9),            // Crisp cyan/white legend
    axisKeyBackground = Color(0xFFE2E8F0),       // Light grey axis buttons
    axisKeyBorder = Color(0xFF94A3B8),
    axisKeyTextXy = Color(0xFF0F172A),           // Dark text for X/Y/IV
    axisKeyTextZ = Color(0xFF16A34A),            // Distinct Green for Z
    entKeyBackground = Color(0xFF15803D),        // Rich industrial machine green ENT
    entKeyText = Color(0xFFFFFFFF),
    noEntKeyBackground = Color(0xFFB91C1C),      // Warning safety crimson red NO ENT
    noEntKeyText = Color(0xFFFFFFFF),
    ncStartBackground = Color(0xFF16A34A),       // Prominent NC START green
    ncStartText = Color(0xFFFFFFFF),
    ncStopBackground = Color(0xFFDC2626),        // Prominent NC STOP red
    ncStopText = Color(0xFFFFFFFF),
    singleBlockActive = Color(0xFFD97706),
    singleBlockInactive = Color(0xFF475569),
    singleBlockActiveText = Color(0xFFFFFFFF),
    singleBlockInactiveText = Color(0xFFCBD5E1),
    dialCardBackground = Color(0xFFD3DBE3),      // Brushed aluminum faceplate
    dialCardBorder = Color(0xFFA0ABB7),
    dialTrackColor = Color(0xFF94A3B8),
    dialKnobColors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A)),
    dialPointerColor = Color(0xFFFF9100),
    dialTickColor = Color(0xFF334155),
    dialTitleColor = Color(0xFF1E293B),
    dialPillBackground = Color(0xFFE2E8F0),
    dialPillText = Color(0xFF0F172A),
    softkeyBackground = Color(0xFFCBD5E1),
    softkeyText = Color(0xFF0F172A),
    softkeyBorder = Color(0xFF94A3B8)
)

val HypermodernConsoleColors = ConsoleColors(
    theme = ConsoleTheme.HYPERMODERN,
    isClassic = false,
    casingBackground = Color(0xFF141A20),        // Dark carbon / cyber background
    casingBorder = Color(0xFF2C3942),
    headerBackground = Color(0xFF1B242C),
    headerTextColor = Color(0xFFFF9100),
    headerBadgeBackground = Color(0xFF00E5FF),
    headerBadgeTextColor = Color(0xFF101418),
    sectionBackground = Color(0xFF1E2830),
    sectionBorder = Color(0xFF263238),
    sectionTitleColor = Color(0xFF78909C),
    numpadKeyBackground = Color(0xFF2C3942),     // Dark slate numpad keys
    numpadKeyText = Color(0xFFECEFF1),
    numpadKeyBorder = Color(0xFF37474F),
    deleteKeyBackground = Color(0xFF2C3942),
    deleteKeyText = Color(0xFFEF4444),
    contourKeyBackground = Color(0xFF263238),
    contourKeyText = Color(0xFFFF9100),
    cycleKeyBackground = Color(0xFF263238),
    cycleKeyText = Color(0xFF00E5FF),
    axisKeyBackground = Color(0xFF2C3942),
    axisKeyBorder = Color(0xFF37474F),
    axisKeyTextXy = Color(0xFFFF9100),
    axisKeyTextZ = Color(0xFF00E676),
    entKeyBackground = Color(0xFF1B5E20),
    entKeyText = Color(0xFFFFFFFF),
    noEntKeyBackground = Color(0xFFB71C1C),
    noEntKeyText = Color(0xFFFFFFFF),
    ncStartBackground = Color(0xFF2E7D32),
    ncStartText = Color(0xFFFFFFFF),
    ncStopBackground = Color(0xFFC62828),
    ncStopText = Color(0xFFFFFFFF),
    singleBlockActive = Color(0xFFE65100),
    singleBlockInactive = Color(0xFF263238),
    singleBlockActiveText = Color(0xFFFFFFFF),
    singleBlockInactiveText = Color(0xFF90A4AE),
    dialCardBackground = Color(0xFF1B2228),
    dialCardBorder = Color(0xFF2C3844),
    dialTrackColor = Color(0xFF2C3942),
    dialKnobColors = listOf(Color(0xFF37474F), Color(0xFF1E2830), Color(0xFF10171D)),
    dialPointerColor = Color(0xFFFF9100),
    dialTickColor = Color(0xFF78909C),
    dialTitleColor = Color(0xFF90A4AE),
    dialPillBackground = Color(0xFF263238),
    dialPillText = Color(0xFFECEFF1),
    softkeyBackground = Color(0xFF1E2830),
    softkeyText = Color(0xFFECEFF1),
    softkeyBorder = Color(0xFF263238)
)

fun getConsoleColors(theme: ConsoleTheme): ConsoleColors {
    return when (theme) {
        ConsoleTheme.CLASSIC_TNC640 -> ClassicTnc640ConsoleColors
        ConsoleTheme.HYPERMODERN -> HypermodernConsoleColors
    }
}

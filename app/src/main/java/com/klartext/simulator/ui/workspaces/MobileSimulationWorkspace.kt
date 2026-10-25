package com.klartext.simulator.ui.workspaces

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.SimulationUiState
import com.klartext.simulator.model.ViewMode
import com.klartext.simulator.ui.components.RotaryOverrideDial
import com.klartext.simulator.ui.components.SimulationCanvas
import com.klartext.simulator.ui.designsystem.*
import com.klartext.simulator.ui.theme.*

/**
 * Tab 1: Mobile 3D Simulation & Machine Execution Workspace.
 * Dedicated exclusively to viewing the 3D/2D workpiece and running the CNC machine
 * (NC START, NC STOP, Single Block, Step, Timeline Scrubber, Feed/Spindle Overrides).
 */
@Composable
fun MobileSimulationWorkspace(
    uiState: SimulationUiState,
    onViewModeChange: (ViewMode) -> Unit,
    onToggleSectionPlane: () -> Unit,
    onToggleToolHolder: () -> Unit,
    onNcStart: () -> Unit,
    onNcStop: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onToggleSingleBlock: () -> Unit,
    onReset: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onFeedOverrideChange: (Int) -> Unit,
    onSpindleOverrideChange: (Int) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onJumpToProgramming: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = getConsoleColors(uiState.consoleTheme)

    if (isLandscape) {
        // === LANDSCAPE PHONE: 65% HERO 3D CANVAS LEFT | 35% EXECUTION & OVERRIDES RIGHT ===
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left: DRO + Full-Height Simulation Canvas + View Mode Strip
            Column(
                modifier = Modifier
                    .weight(1.75f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactDroBar(
                    state = uiState.machineState,
                    diagnostics = uiState.diagnostics,
                    feedOverride = uiState.feedOverride,
                    spindleOverride = uiState.spindleOverride,
                    onOpenDiagnostics = onOpenDiagnostics
                )

                if (uiState.collisionAlertMessage != null) {
                    CollisionAlertBanner(message = uiState.collisionAlertMessage)
                }

                SimulationCanvas(
                    state = uiState,
                    currentStepIndex = uiState.machineState.currentStepIndex,
                    onToggleSectionPlane = onToggleSectionPlane,
                    onToggleToolHolder = onToggleToolHolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                )

                ViewModeChipStrip(
                    activeMode = uiState.viewMode,
                    onViewModeChange = onViewModeChange
                )
            }

            // Right: Dedicated Machine Execution & Override Console
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .background(colors.casingBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.casingBorder, RoundedCornerShape(8.dp))
                    .padding(6.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.headerBackground, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MACHINE UITVOER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.headerTextColor
                    )
                    Text(
                        text = if (uiState.isSingleBlockActive) "[ENKEL BLOK]" else "[CONTINU]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isSingleBlockActive) Color(0xFFFFD54F) else CncDroGreen
                    )
                }

                // NC START & NC STOP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CncKeyCap(
                        label = "NC START",
                        icon = Icons.Default.PlayArrow,
                        containerColor = colors.ncStartBackground,
                        textColor = colors.ncStartText,
                        height = 38.dp,
                        fontSize = 11.sp,
                        cornerRadius = 5.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onNcStart
                    )
                    CncKeyCap(
                        label = "NC STOP",
                        icon = Icons.Default.Pause,
                        containerColor = colors.ncStopBackground,
                        textColor = colors.ncStopText,
                        height = 38.dp,
                        fontSize = 11.sp,
                        cornerRadius = 5.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onNcStop
                    )
                }

                // Step Back / Step Forward / Single Block / Reset / Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    CncKeyCap(
                        label = "",
                        icon = Icons.Default.SkipPrevious,
                        containerColor = CncSurfaceDark,
                        textColor = CncTextPrimary,
                        borderColor = CncBorder,
                        height = 28.dp,
                        enabled = uiState.machineState.currentStepIndex > 0,
                        modifier = Modifier.weight(0.8f),
                        onClick = onStepBackward
                    )
                    CncKeyCap(
                        label = "",
                        icon = Icons.Default.SkipNext,
                        containerColor = CncSurfaceDark,
                        textColor = CncTextPrimary,
                        borderColor = CncBorder,
                        height = 28.dp,
                        enabled = uiState.machineState.currentStepIndex < uiState.segments.size,
                        modifier = Modifier.weight(0.8f),
                        onClick = onStepForward
                    )
                    CncKeyCap(
                        label = "S-BLK",
                        containerColor = if (uiState.isSingleBlockActive) colors.singleBlockActive else colors.singleBlockInactive,
                        textColor = if (uiState.isSingleBlockActive) colors.singleBlockActiveText else colors.singleBlockInactiveText,
                        height = 28.dp,
                        fontSize = 9.5.sp,
                        modifier = Modifier.weight(1.1f),
                        onClick = onToggleSingleBlock
                    )
                    SpeedDropdownKey(
                        currentSpeed = uiState.playbackSpeed,
                        onSpeedChange = onSpeedChange,
                        modifier = Modifier.weight(0.95f)
                    )
                    CncKeyCap(
                        label = "",
                        icon = Icons.Default.Replay,
                        containerColor = colors.sectionBackground,
                        textColor = colors.contourKeyText,
                        borderColor = colors.sectionBorder,
                        height = 28.dp,
                        modifier = Modifier.weight(0.8f),
                        onClick = onReset
                    )
                }

                // Timeline Scrubber
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CncPanelBackground, RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    CncTimelineScrubber(
                        currentStep = uiState.machineState.currentStepIndex,
                        totalSteps = uiState.segments.size,
                        onSeek = onSeek
                    )
                }

                // Dual Rotary Override Dials
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RotaryOverrideDial(
                        title = "FEED %",
                        valuePercent = uiState.feedOverride,
                        onValueChange = onFeedOverrideChange,
                        accentColor = CncAccentOrange,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    RotaryOverrideDial(
                        title = "SPINDLE %",
                        valuePercent = uiState.spindleOverride,
                        onValueChange = onSpindleOverrideChange,
                        accentColor = CncFeedCyan,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Active Code Line Preview Banner
                LiveActiveCodeBanner(
                    blocks = uiState.blocks,
                    activeLineNumber = uiState.machineState.activeLineNumber,
                    onJumpToProgramming = onJumpToProgramming
                )
            }
        }
    } else {
        // === PORTRAIT PHONE: MAXIMIZED 3D CANVAS TOP | MACHINE EXECUTION DECK BOTTOM ===
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Compact DRO Header
            CompactDroBar(
                state = uiState.machineState,
                diagnostics = uiState.diagnostics,
                feedOverride = uiState.feedOverride,
                spindleOverride = uiState.spindleOverride,
                onOpenDiagnostics = onOpenDiagnostics
            )

            if (uiState.collisionAlertMessage != null) {
                CollisionAlertBanner(message = uiState.collisionAlertMessage)
            }

            // 2. Adaptive View Mode Strip (never clipped!)
            ViewModeChipStrip(
                activeMode = uiState.viewMode,
                onViewModeChange = onViewModeChange
            )

            // 3. Large Hero Simulation Canvas
            SimulationCanvas(
                state = uiState,
                currentStepIndex = uiState.machineState.currentStepIndex,
                onToggleSectionPlane = onToggleSectionPlane,
                onToggleToolHolder = onToggleToolHolder,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            )

            // 4. Live Active Klartext Code Line Ticker (tap to jump to Code & Keypad tab)
            LiveActiveCodeBanner(
                blocks = uiState.blocks,
                activeLineNumber = uiState.machineState.activeLineNumber,
                onJumpToProgramming = onJumpToProgramming
            )

            // 5. Machine Execution & Override Deck
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.casingBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.casingBorder, RoundedCornerShape(8.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Scrubber + Speed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CncPanelBackground, RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CncTimelineScrubber(
                        currentStep = uiState.machineState.currentStepIndex,
                        totalSteps = uiState.segments.size,
                        onSeek = onSeek,
                        modifier = Modifier.weight(1f)
                    )
                    SpeedDropdownKey(
                        currentSpeed = uiState.playbackSpeed,
                        onSpeedChange = onSpeedChange,
                        modifier = Modifier.width(52.dp)
                    )
                }

                // Compact Feed & Spindle Override Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactOverridePill(
                        label = "FEED",
                        percent = uiState.feedOverride,
                        accentColor = CncAccentOrange,
                        colors = colors,
                        onChange = onFeedOverrideChange,
                        modifier = Modifier.weight(1f)
                    )
                    CompactOverridePill(
                        label = "SPIL",
                        percent = uiState.spindleOverride,
                        accentColor = CncFeedCyan,
                        colors = colors,
                        onChange = onSpindleOverrideChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Big Thumb-Friendly Machine Execution Buttons (NC START / NC STOP / STEP / S-BLK / RESET)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CncKeyCap(
                        label = "NC START",
                        icon = Icons.Default.PlayArrow,
                        containerColor = colors.ncStartBackground,
                        textColor = colors.ncStartText,
                        height = 38.dp,
                        fontSize = 11.sp,
                        cornerRadius = 5.dp,
                        modifier = Modifier.weight(1.4f),
                        onClick = onNcStart
                    )
                    CncKeyCap(
                        label = "NC STOP",
                        icon = Icons.Default.Pause,
                        containerColor = colors.ncStopBackground,
                        textColor = colors.ncStopText,
                        height = 38.dp,
                        fontSize = 11.sp,
                        cornerRadius = 5.dp,
                        modifier = Modifier.weight(1.3f),
                        onClick = onNcStop
                    )
                    CncKeyCap(
                        label = "",
                        icon = Icons.Default.SkipPrevious,
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 38.dp,
                        enabled = uiState.machineState.currentStepIndex > 0,
                        modifier = Modifier.weight(0.65f),
                        onClick = onStepBackward
                    )
                    CncKeyCap(
                        label = "",
                        icon = Icons.Default.SkipNext,
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 38.dp,
                        enabled = uiState.machineState.currentStepIndex < uiState.segments.size,
                        modifier = Modifier.weight(0.65f),
                        onClick = onStepForward
                    )
                    CncKeyCap(
                        label = "S-BLK",
                        containerColor = if (uiState.isSingleBlockActive) colors.singleBlockActive else colors.singleBlockInactive,
                        textColor = if (uiState.isSingleBlockActive) colors.singleBlockActiveText else colors.singleBlockInactiveText,
                        height = 38.dp,
                        fontSize = 9.5.sp,
                        modifier = Modifier.weight(0.85f),
                        onClick = onToggleSingleBlock
                    )
                    CncKeyCap(
                        label = "",
                        icon = Icons.Default.Replay,
                        containerColor = colors.sectionBackground,
                        textColor = colors.contourKeyText,
                        borderColor = colors.sectionBorder,
                        height = 38.dp,
                        modifier = Modifier.weight(0.65f),
                        onClick = onReset
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedDropdownKey(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        CncKeyCap(
            label = "${currentSpeed}x",
            containerColor = CncSurfaceDark,
            textColor = CncAccentOrange,
            borderColor = CncBorder,
            height = 26.dp,
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CncSurfaceDark)
        ) {
            listOf(0.25f, 0.5f, 1.0f, 2.0f, 5.0f, 10.0f).forEach { spd ->
                DropdownMenuItem(
                    text = { Text("${spd}x", color = CncTextPrimary, fontSize = 12.sp) },
                    onClick = {
                        onSpeedChange(spd)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactOverridePill(
    label: String,
    percent: Int,
    accentColor: Color,
    colors: ConsoleColors,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(colors.dialCardBackground, RoundedCornerShape(5.dp))
            .border(1.dp, colors.dialCardBorder, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label $percent%",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = accentColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CncKeyCap(
                label = "-10",
                containerColor = colors.axisKeyBackground,
                textColor = colors.numpadKeyText,
                borderColor = colors.axisKeyBorder,
                height = 22.dp,
                fontSize = 8.5.sp,
                modifier = Modifier.width(28.dp),
                onClick = { onChange((percent - 10).coerceIn(0, 150)) }
            )
            CncKeyCap(
                label = "100",
                containerColor = colors.entKeyBackground,
                textColor = Color.White,
                height = 22.dp,
                fontSize = 8.5.sp,
                modifier = Modifier.width(28.dp),
                onClick = { onChange(100) }
            )
            CncKeyCap(
                label = "+10",
                containerColor = colors.axisKeyBackground,
                textColor = colors.numpadKeyText,
                borderColor = colors.axisKeyBorder,
                height = 22.dp,
                fontSize = 8.5.sp,
                modifier = Modifier.width(28.dp),
                onClick = { onChange((percent + 10).coerceIn(0, 150)) }
            )
        }
    }
}

package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.SimulationUiState
import com.klartext.simulator.model.ViewMode
import com.klartext.simulator.ui.designsystem.CncKeyCap
import com.klartext.simulator.ui.designsystem.CncSoftkey
import com.klartext.simulator.ui.designsystem.CncTimelineScrubber
import com.klartext.simulator.ui.theme.*

/**
 * Tablet & Large-Screen Twin-Screen Layout for HEIDENHAIN TNC 640 Simulator
 * Left: Display Unit (DRO, 3D Orbit / 2.5D Canvas, Klartekst Code Viewer, Scrubber, Softkeys)
 * Right: Full HEIDENHAIN TE 640 Operating Panel with Dual Rotary Override Dials
 */
@Composable
fun TwinScreenConsoleView(
    uiState: SimulationUiState,
    onLineClick: (Int) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onToggleContour: () -> Unit,
    onToggleSectionPlane: () -> Unit,
    onToggleToolHolder: () -> Unit,
    onToggleConsoleTheme: () -> Unit,
    onFeedOverrideChange: (Int) -> Unit,
    onSpindleOverrideChange: (Int) -> Unit,
    onNcStart: () -> Unit,
    onNcStop: () -> Unit,
    onToggleSingleBlock: () -> Unit,
    onReset: () -> Unit,
    onSeek: (Int) -> Unit = {},
    onStartCommand: (String) -> Unit,
    onDialogChar: (String) -> Unit,
    onDialogEnt: () -> Unit,
    onDialogNoEnt: () -> Unit,
    onDialogDel: () -> Unit,
    onDialogEnd: () -> Unit,
    onNavigateLine: (Int) -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenToolTable: () -> Unit,
    onOpenQParams: () -> Unit,
    onOpenImportExport: () -> Unit,
    onCopyLine: () -> Unit,
    onPasteLine: () -> Unit,
    onDeleteLine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = getConsoleColors(uiState.consoleTheme)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // === LEFT PANEL: DISPLAY UNIT (16:9 BEELDSCHERM) ===
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Digital Readout (DRO)
            DigitalReadout(
                state = uiState.machineState,
                diagnostics = uiState.diagnostics,
                onOpenDiagnostics = onOpenDiagnostics,
                feedOverride = uiState.feedOverride,
                spindleOverride = uiState.spindleOverride,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Simulation Canvas & Code Viewer Side-by-side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SimulationCanvas(
                    state = uiState,
                    currentStepIndex = uiState.machineState.currentStepIndex,
                    onToggleSectionPlane = onToggleSectionPlane,
                    onToggleToolHolder = onToggleToolHolder,
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .clipToBounds()
                )

                CodeViewer(
                    blocks = uiState.blocks,
                    activeLineNumber = uiState.machineState.activeLineNumber,
                    diagnostics = uiState.diagnostics,
                    onLineClick = onLineClick,
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                )
            }

            // 3. View Mode Toolbar & Timeline Scrubber
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CncPanelBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, CncBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(
                        ViewMode.TOP_XY to "XY",
                        ViewMode.FRONT_XZ to "XZ",
                        ViewMode.SIDE_YZ to "YZ",
                        ViewMode.MATERIAL_25D to "MAT",
                        ViewMode.ISOMETRIC to "ISO",
                        ViewMode.ORBIT_3D to "3D",
                        ViewMode.SECTION_CUT to "SNEDE"
                    ).forEach { (mode, label) ->
                        val isSelected = uiState.viewMode == mode
                        CncKeyCap(
                            label = label,
                            containerColor = if (isSelected) CncAccentOrange else CncSurfaceDark,
                            textColor = if (isSelected) CncDarkBackground else CncTextSecondary,
                            borderColor = if (isSelected) CncAccentOrange else CncBorder,
                            height = 24.dp,
                            fontSize = 9.sp,
                            cornerRadius = 3.dp,
                            modifier = Modifier.widthIn(min = 32.dp),
                            onClick = { onViewModeChange(mode) }
                        )
                    }
                }

                CncTimelineScrubber(
                    currentStep = uiState.machineState.currentStepIndex,
                    totalSteps = uiState.segments.size,
                    onSeek = onSeek,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Authentic Bottom Softkeys Row (Zero-overlap CncSoftkey)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.casingBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.casingBorder, RoundedCornerShape(6.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                CncSoftkey("KOPIËER", colors, Modifier.weight(1f), height = 26.dp) { onCopyLine() }
                CncSoftkey("PLAK", colors, Modifier.weight(1f), height = 26.dp) { onPasteLine() }
                CncSoftkey("DEL", colors, Modifier.weight(0.9f), height = 26.dp) { onDeleteLine() }
                CncSoftkey("TOOLS", colors, Modifier.weight(1f), height = 26.dp) { onOpenToolTable() }
                CncSoftkey("CALC", colors, Modifier.weight(0.9f), height = 26.dp) { onOpenCalculator() }
                CncSoftkey("Q-PAR", colors, Modifier.weight(1f), height = 26.dp) { onOpenQParams() }
                CncSoftkey("DXF/ISO", colors, Modifier.weight(1.1f), height = 26.dp) { onOpenImportExport() }
                CncSoftkey(
                    label = if (uiState.diagnostics.isNotEmpty()) "FOUTEN (${uiState.diagnostics.size})" else "STATUS",
                    colors = colors,
                    modifier = Modifier.weight(1.2f),
                    height = 26.dp,
                    isAlert = uiState.diagnostics.isNotEmpty()
                ) { onOpenDiagnostics() }
            }
        }

        // === RIGHT PANEL: HEIDENHAIN TE 640 OPERATING PANEL ===
        HeidenhainTe640Console(
            activeDialog = uiState.activeDialog,
            feedOverride = uiState.feedOverride,
            spindleOverride = uiState.spindleOverride,
            isPlaying = uiState.isPlaying,
            isSingleBlockActive = uiState.isSingleBlockActive,
            consoleTheme = uiState.consoleTheme,
            onToggleConsoleTheme = onToggleConsoleTheme,
            onFeedOverrideChange = onFeedOverrideChange,
            onSpindleOverrideChange = onSpindleOverrideChange,
            onNcStart = onNcStart,
            onNcStop = onNcStop,
            onToggleSingleBlock = onToggleSingleBlock,
            onReset = onReset,
            onStartCommand = onStartCommand,
            onDialogChar = onDialogChar,
            onDialogEnt = onDialogEnt,
            onDialogNoEnt = onDialogNoEnt,
            onDialogDel = onDialogDel,
            onDialogEnd = onDialogEnd,
            onNavigateLine = onNavigateLine,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight()
        )
    }
}

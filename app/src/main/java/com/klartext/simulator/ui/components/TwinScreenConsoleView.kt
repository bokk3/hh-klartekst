package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.SimulationUiState
import com.klartext.simulator.model.ViewMode
import com.klartext.simulator.ui.theme.*

/**
 * Tablet & Landscape Twin-Screen Layout for HEIDENHAIN TNC 640 Simulator
 * Left: Display Unit (DRO, 3D Orbit / 2.5D Canvas, Klartekst Code Viewer, Softkeys)
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
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Simulation Canvas & Code Viewer Side-by-side or stacked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Simulation Graphics Canvas
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

                // Klartekst Code Viewer
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

            // 3. View Mode Toolbar & Scrub Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CncPanelBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, CncBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Mode Selectors
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        ViewMode.TOP_XY to "XY",
                        ViewMode.FRONT_XZ to "XZ",
                        ViewMode.SIDE_YZ to "YZ",
                        ViewMode.MATERIAL_25D to "MAT",
                        ViewMode.ISOMETRIC to "ISO",
                        ViewMode.ORBIT_3D to "3D",
                        ViewMode.SECTION_CUT to "SNIJD"
                    ).forEach { (mode, label) ->
                        val isSelected = uiState.viewMode == mode
                        Button(
                            onClick = { onViewModeChange(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CncAccentOrange else CncSurfaceDark
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier
                                .height(24.dp)
                                .defaultMinSize(minWidth = 26.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = if (isSelected) CncDarkBackground else CncTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "Regel ${uiState.machineState.activeLineNumber} | Stap ${uiState.machineState.currentStepIndex}/${uiState.segments.size}",
                    fontSize = 10.sp,
                    color = CncDroGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. Authentic Bottom Softkeys Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.casingBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.casingBorder, RoundedCornerShape(6.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TwinSoftkey("KOPIEER", colors, Modifier.weight(1f)) { onCopyLine() }
                TwinSoftkey("PLAK", colors, Modifier.weight(1f)) { onPasteLine() }
                TwinSoftkey("DEL", colors, Modifier.weight(0.9f)) { onDeleteLine() }
                TwinSoftkey("TOOLS", colors, Modifier.weight(1f)) { onOpenToolTable() }
                TwinSoftkey("CALC", colors, Modifier.weight(0.9f)) { onOpenCalculator() }
                TwinSoftkey("Q-PAR", colors, Modifier.weight(1f)) { onOpenQParams() }
                TwinSoftkey("DXF/ISO", colors, Modifier.weight(1.1f)) { onOpenImportExport() }
                TwinSoftkey(
                    if (uiState.diagnostics.isNotEmpty()) "FOUTEN (${uiState.diagnostics.size})" else "STATUS",
                    colors,
                    Modifier.weight(1.2f),
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

@Composable
private fun TwinSoftkey(
    text: String,
    colors: ConsoleColors,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isAlert) CncRapidOrange else colors.softkeyBackground
        ),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
            .height(26.dp)
            .border(0.8.dp, if (isAlert) CncRapidOrange else colors.softkeyBorder, RoundedCornerShape(3.dp))
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = if (isAlert) Color.White else colors.softkeyText,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

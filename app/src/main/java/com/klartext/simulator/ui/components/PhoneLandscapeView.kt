package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.*
import com.klartext.simulator.ui.theme.*
import java.util.Locale

enum class PhoneSideTab(val label: String) {
    CODE("CODE"),
    CONSOLE("BEDIENING"),
    OVERRIDES("OVERRIDES")
}

/**
 * Phone Landscape View for HEIDENHAIN TNC 640 Simulator
 * Tailored specifically for smartphones in landscape mode:
 * - Left pane (62% width): Prominent, maximized Simulation Canvas with compact DRO HUD.
 * - Right pane (38% width): Machine execution controls (NC START / NC STOP) + Tabbed Deck
 *   switching between Code Viewer, Full Console Keypad, and Override Dials.
 * Solves crowding and ensures the simulation is large, clear, and immersive!
 */
@Composable
fun PhoneLandscapeView(
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
    var activeTab by remember { mutableStateOf(PhoneSideTab.CODE) }
    val colors = getConsoleColors(uiState.consoleTheme)

    // Automatically switch to CONSOLE tab if a Klartekst dialogue begins
    LaunchedEffect(uiState.activeDialog != null) {
        if (uiState.activeDialog != null) {
            activeTab = PhoneSideTab.CONSOLE
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // === LEFT PANE: MAXIMIZED SIMULATION CANVAS & DRO (62% WIDTH) ===
        Column(
            modifier = Modifier
                .weight(1.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // 1. Compact DRO Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.sectionBackground, RoundedCornerShape(4.dp))
                    .border(1.dp, colors.sectionBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Axis coordinates
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactAxisValue("X", uiState.machineState.x)
                    CompactAxisValue("Y", uiState.machineState.y)
                    CompactAxisValue("Z", uiState.machineState.z, isZ = true)
                }

                // Active tool & feed indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "F:${uiState.machineState.feedRate.toInt()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CncAccentCyan
                    )
                    Text(
                        text = "S:${uiState.machineState.spindleSpeed.toInt()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CncDroGreen
                    )
                    Text(
                        text = uiState.machineState.toolName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.contourKeyText
                    )
                    if (uiState.machineState.radiusComp != RadiusComp.R0) {
                        Text(
                            text = uiState.machineState.radiusComp.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            }

            // 2. Large Simulation Canvas (Hero View - Takes full available space!)
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

            // 3. View Mode Toolbar & Status Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CncPanelBackground, RoundedCornerShape(4.dp))
                    .border(1.dp, CncBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier
                                .height(22.dp)
                                .defaultMinSize(minWidth = 24.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                color = if (isSelected) CncDarkBackground else CncTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "R${uiState.machineState.activeLineNumber} | Stap ${uiState.machineState.currentStepIndex}/${uiState.segments.size}",
                    fontSize = 9.5.sp,
                    color = CncDroGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // === RIGHT PANE: THUMB-FRIENDLY CONTROLS & TABBED DECK (38% WIDTH) ===
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // 1. Prominent Machine Execution Bar (NC START, NC STOP, SINGLE, RESET)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NC START
                Button(
                    onClick = onNcStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.ncStartBackground,
                        contentColor = colors.ncStartText
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(34.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "NC START", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("START", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // NC STOP
                Button(
                    onClick = onNcStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.ncStopBackground,
                        contentColor = colors.ncStopText
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(34.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Pause, contentDescription = "NC STOP", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // SINGLE BLOCK
                Button(
                    onClick = onToggleSingleBlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isSingleBlockActive) colors.singleBlockActive else colors.singleBlockInactive,
                        contentColor = if (uiState.isSingleBlockActive) colors.singleBlockActiveText else colors.singleBlockInactiveText
                    ),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1.0f)
                        .height(34.dp)
                ) {
                    Text(
                        text = "ENKEL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // RESET
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(34.dp)
                        .background(colors.sectionBackground, RoundedCornerShape(4.dp))
                        .border(1.dp, colors.sectionBorder, RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        Icons.Default.Replay,
                        contentDescription = "Reset",
                        tint = colors.contourKeyText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Tab Navigation Switcher ([CODE] | [BEDIENING] | [OVERRIDES])
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.headerBackground, RoundedCornerShape(4.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PhoneSideTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Button(
                        onClick = { activeTab = tab },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) colors.headerBadgeBackground else Color.Transparent,
                            contentColor = if (isSelected) colors.headerBadgeTextColor else colors.headerTextColor
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    PhoneSideTab.CODE -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            CodeViewer(
                                blocks = uiState.blocks,
                                activeLineNumber = uiState.machineState.activeLineNumber,
                                diagnostics = uiState.diagnostics,
                                onLineClick = onLineClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            // Compact Softkeys Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.casingBackground, RoundedCornerShape(4.dp))
                                    .border(1.dp, colors.casingBorder, RoundedCornerShape(4.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                PhoneMiniSoftkey("KOPIEER", colors, Modifier.weight(1f)) { onCopyLine() }
                                PhoneMiniSoftkey("PLAK", colors, Modifier.weight(1f)) { onPasteLine() }
                                PhoneMiniSoftkey("DEL", colors, Modifier.weight(0.9f)) { onDeleteLine() }
                                PhoneMiniSoftkey("TOOLS", colors, Modifier.weight(1f)) { onOpenToolTable() }
                                PhoneMiniSoftkey("CALC", colors, Modifier.weight(0.9f)) { onOpenCalculator() }
                                PhoneMiniSoftkey(
                                    if (uiState.diagnostics.isNotEmpty()) "FOUT (${uiState.diagnostics.size})" else "STATUS",
                                    colors,
                                    Modifier.weight(1.1f),
                                    isAlert = uiState.diagnostics.isNotEmpty()
                                ) { onOpenDiagnostics() }
                            }
                        }
                    }

                    PhoneSideTab.CONSOLE -> {
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
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    PhoneSideTab.OVERRIDES -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.casingBackground, RoundedCornerShape(6.dp))
                                .border(1.dp, colors.casingBorder, RoundedCornerShape(6.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RotaryOverrideDial(
                                title = "VOEDING % (FEED)",
                                valuePercent = uiState.feedOverride,
                                onValueChange = onFeedOverrideChange,
                                accentColor = CncAccentOrange,
                                colors = colors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            RotaryOverrideDial(
                                title = "SPILSNELHEID % (SPINDLE)",
                                valuePercent = uiState.spindleOverride,
                                onValueChange = onSpindleOverrideChange,
                                accentColor = CncFeedCyan,
                                colors = colors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAxisValue(
    axis: String,
    value: Float,
    isZ: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$axis:",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isZ) CncDroGreen else CncAccentOrange,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(1.dp))
        Text(
            text = String.format(Locale.US, "%+7.2f", value),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isZ) CncDroGreen else Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PhoneMiniSoftkey(
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
        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 0.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
            .height(24.dp)
            .border(0.8.dp, if (isAlert) CncRapidOrange else colors.softkeyBorder, RoundedCornerShape(3.dp))
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            color = if (isAlert) Color.White else colors.softkeyText,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

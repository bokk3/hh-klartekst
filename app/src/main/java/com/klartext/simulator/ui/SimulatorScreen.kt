package com.klartext.simulator.ui

import android.content.res.Configuration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.*
import com.klartext.simulator.sample.SamplePrograms
import com.klartext.simulator.ui.components.*
import com.klartext.simulator.ui.designsystem.CncKeyCap
import com.klartext.simulator.ui.theme.*
import com.klartext.simulator.ui.workspaces.MobileProgrammingWorkspace
import com.klartext.simulator.ui.workspaces.MobileSimulationWorkspace
import com.klartext.simulator.ui.workspaces.MobileToolsWorkspace

@Composable
fun SimulatorScreen(
    viewModel: SimulatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var programMenuExpanded by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = configuration.smallestScreenWidthDp >= 600

    // Tablets default to Twin-Screen in landscape; phones default to the dedicated Workspace tabs
    val isTwinScreen = uiState.isConsoleTwinScreen ?: (isTablet && screenWidthDp >= 840)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CncDarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // === 1. COMPACT ANDROID 17 EDGE-TO-EDGE HEADER BAR ===
        CompactCncTopBar(
            uiState = uiState,
            isLandscape = isLandscape,
            isTwinScreen = isTwinScreen,
            programMenuExpanded = programMenuExpanded,
            onOpenProgramMenu = { programMenuExpanded = true },
            onDismissProgramMenu = { programMenuExpanded = false },
            onSelectProgram = { name, code ->
                viewModel.loadProgram(name, code)
                programMenuExpanded = false
            },
            onSelectMobileTab = { tab -> viewModel.setMobileTab(tab) },
            onToggleConsoleTheme = { viewModel.toggleConsoleTheme() },
            onToggleTwinScreen = { viewModel.toggleConsoleTwinScreen() },
            onOpenToolTable = { viewModel.openToolTable() },
            onOpenImportExport = { viewModel.openImportExport() }
        )

        // === 2. MAIN ADAPTIVE CONTENT AREA ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isTwinScreen) {
                TwinScreenConsoleView(
                    uiState = uiState,
                    onLineClick = { line -> viewModel.seekToLine(line) },
                    onViewModeChange = { mode -> viewModel.setViewMode(mode) },
                    onToggleContour = { viewModel.toggleProgrammedContour() },
                    onToggleSectionPlane = { viewModel.toggleSectionPlane() },
                    onToggleToolHolder = { viewModel.toggleToolHolder() },
                    onToggleConsoleTheme = { viewModel.toggleConsoleTheme() },
                    onFeedOverrideChange = { ov -> viewModel.setFeedOverride(ov) },
                    onSpindleOverrideChange = { ov -> viewModel.setSpindleOverride(ov) },
                    onNcStart = { viewModel.ncStart() },
                    onNcStop = { viewModel.ncStop() },
                    onToggleSingleBlock = { viewModel.toggleSingleBlock() },
                    onReset = { viewModel.reset() },
                    onSeek = { step -> viewModel.seekToStep(step) },
                    onStartCommand = { cmd -> viewModel.startCommand(cmd) },
                    onDialogChar = { ch -> viewModel.onDialogChar(ch) },
                    onDialogEnt = { viewModel.onDialogEnt() },
                    onDialogNoEnt = { viewModel.onDialogNoEnt() },
                    onDialogDel = { viewModel.onDialogDel() },
                    onDialogEnd = { viewModel.onDialogEnd() },
                    onNavigateLine = { delta -> viewModel.navigateCodeLine(delta) },
                    onOpenCalculator = { viewModel.openCalculator() },
                    onOpenDiagnostics = { viewModel.openDiagnostics() },
                    onOpenToolTable = { viewModel.openToolTable() },
                    onOpenQParams = { viewModel.openQParamDialog() },
                    onOpenImportExport = { viewModel.openImportExport() },
                    onCopyLine = { viewModel.copyCurrentLine() },
                    onPasteLine = { viewModel.pasteLine() },
                    onDeleteLine = { viewModel.deleteCurrentLine() }
                )
            } else {
                when (uiState.activeMobileTab) {
                    MobileWorkspaceTab.SIMULATION -> {
                        MobileSimulationWorkspace(
                            uiState = uiState,
                            onViewModeChange = { mode -> viewModel.setViewMode(mode) },
                            onToggleSectionPlane = { viewModel.toggleSectionPlane() },
                            onToggleToolHolder = { viewModel.toggleToolHolder() },
                            onNcStart = { viewModel.ncStart() },
                            onNcStop = { viewModel.ncStop() },
                            onStepForward = { viewModel.stepForward() },
                            onStepBackward = { viewModel.stepBackward() },
                            onToggleSingleBlock = { viewModel.toggleSingleBlock() },
                            onReset = { viewModel.reset() },
                            onSeek = { step -> viewModel.seekToStep(step) },
                            onSpeedChange = { spd -> viewModel.setPlaybackSpeed(spd) },
                            onFeedOverrideChange = { ov -> viewModel.setFeedOverride(ov) },
                            onSpindleOverrideChange = { ov -> viewModel.setSpindleOverride(ov) },
                            onOpenDiagnostics = { viewModel.openDiagnostics() },
                            onJumpToProgramming = { viewModel.setMobileTab(MobileWorkspaceTab.PROGRAMMING) }
                        )
                    }

                    MobileWorkspaceTab.PROGRAMMING -> {
                        MobileProgrammingWorkspace(
                            uiState = uiState,
                            onLineClick = { line -> viewModel.seekToLine(line) },
                            onNavigateLine = { delta -> viewModel.navigateCodeLine(delta) },
                            onStartCommand = { cmd -> viewModel.startCommand(cmd) },
                            onDialogChar = { ch -> viewModel.onDialogChar(ch) },
                            onDialogEnt = { viewModel.onDialogEnt() },
                            onDialogNoEnt = { viewModel.onDialogNoEnt() },
                            onDialogDel = { viewModel.onDialogDel() },
                            onDialogEnd = { viewModel.onDialogEnd() },
                            onCopyLine = { viewModel.copyCurrentLine() },
                            onPasteLine = { viewModel.pasteLine() },
                            onDeleteLine = { viewModel.deleteCurrentLine() },
                            onTogglePortraitKeypad = { viewModel.togglePortraitKeypad() },
                            onJumpToSimulation = { viewModel.setMobileTab(MobileWorkspaceTab.SIMULATION) }
                        )
                    }

                    MobileWorkspaceTab.TOOLS -> {
                        MobileToolsWorkspace(
                            uiState = uiState,
                            onOpenToolTable = { viewModel.openToolTable() },
                            onOpenCalculator = { viewModel.openCalculator() },
                            onOpenQParams = { viewModel.openQParamDialog() },
                            onOpenImportExport = { viewModel.openImportExport() },
                            onOpenDiagnostics = { viewModel.openDiagnostics() },
                            onToggleConsoleTheme = { viewModel.toggleConsoleTheme() },
                            onToggleTwinScreen = { viewModel.toggleConsoleTwinScreen() },
                            onLoadProgram = { name, code ->
                                viewModel.loadProgram(name, code)
                                viewModel.setMobileTab(MobileWorkspaceTab.SIMULATION)
                            }
                        )
                    }
                }
            }
        }

        // === 3. PORTRAIT BOTTOM WORKSPACE NAVIGATION BAR (WHEN NOT IN TWIN-SCREEN OR LANDSCAPE) ===
        if (!isTwinScreen && !isLandscape) {
            MobileBottomWorkspaceBar(
                activeTab = uiState.activeMobileTab,
                diagnosticsCount = uiState.diagnostics.size,
                hasActiveDialog = uiState.activeDialog != null,
                onSelectTab = { tab -> viewModel.setMobileTab(tab) }
            )
        }
    }

    // Modal Dialogs
    if (uiState.showCalculator) {
        CuttingCalculatorDialog(onDismiss = { viewModel.closeCalculator() })
    }

    if (uiState.showDiagnosticsSheet) {
        DiagnosticsDialog(
            diagnostics = uiState.diagnostics,
            onSelectLine = { line -> viewModel.seekToLine(line) },
            onDismiss = { viewModel.closeDiagnostics() }
        )
    }

    if (uiState.showToolTableDialog) {
        ToolTableDialog(
            activePreset = uiState.machinePreset,
            onSelectPreset = { preset -> viewModel.setMachinePreset(preset) },
            onSelectTool = { tool -> viewModel.selectActiveTool(tool) },
            onInsertToolCall = { tool -> viewModel.insertToolCall(tool) },
            onDismiss = { viewModel.closeToolTable() }
        )
    }

    if (uiState.showQParamDialog) {
        QParameterMonitorDialog(
            qParameters = uiState.qParameters,
            onDismiss = { viewModel.closeQParamDialog() }
        )
    }

    if (uiState.showImportExportDialog) {
        ImportExportDialog(
            currentKlartext = uiState.rawCode,
            currentProgramName = uiState.programName,
            onLoadConvertedProgram = { name, code -> viewModel.loadProgram(name, code) },
            onDismiss = { viewModel.closeImportExport() }
        )
    }
}

@Composable
private fun CompactCncTopBar(
    uiState: SimulationUiState,
    isLandscape: Boolean,
    isTwinScreen: Boolean,
    programMenuExpanded: Boolean,
    onOpenProgramMenu: () -> Unit,
    onDismissProgramMenu: () -> Unit,
    onSelectProgram: (String, String) -> Unit,
    onSelectMobileTab: (MobileWorkspaceTab) -> Unit,
    onToggleConsoleTheme: () -> Unit,
    onToggleTwinScreen: () -> Unit,
    onOpenToolTable: () -> Unit,
    onOpenImportExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CncPanelBackground)
            .border(0.8.dp, CncBorder)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Title & Active Program Name
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "TNC 640",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CncAccentOrange,
                    maxLines = 1
                )
                Text(
                    text = uiState.programName,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CncDroGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // In Landscape (when not Twin-Screen), show the 3 Workspace Tabs inline in the TopBar to save vertical height!
            if (isLandscape && !isTwinScreen) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkspaceTabPill(
                        label = "1. 3D SIM & UITVOER",
                        icon = Icons.Default.PlayCircle,
                        isSelected = uiState.activeMobileTab == MobileWorkspaceTab.SIMULATION,
                        onClick = { onSelectMobileTab(MobileWorkspaceTab.SIMULATION) }
                    )
                    WorkspaceTabPill(
                        label = "2. PROGRAMMEREN (CODE + KNOPPEN)",
                        icon = Icons.Default.Terminal,
                        isSelected = uiState.activeMobileTab == MobileWorkspaceTab.PROGRAMMING,
                        hasBadge = uiState.activeDialog != null,
                        onClick = { onSelectMobileTab(MobileWorkspaceTab.PROGRAMMING) }
                    )
                    WorkspaceTabPill(
                        label = "3. TOOLS & PGM",
                        icon = Icons.Default.BuildCircle,
                        isSelected = uiState.activeMobileTab == MobileWorkspaceTab.TOOLS,
                        onClick = { onSelectMobileTab(MobileWorkspaceTab.TOOLS) }
                    )
                }
            }
        }

        // Right: Quick Action Buttons (Theme, Twin-Screen, Tools, Program Picker)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CncKeyCap(
                label = if (uiState.consoleTheme == ConsoleTheme.CLASSIC_TNC640) "TE640" else "DARK",
                icon = Icons.Default.Palette,
                containerColor = CncSurfaceDark,
                textColor = if (uiState.consoleTheme == ConsoleTheme.CLASSIC_TNC640) Color(0xFFFF9100) else Color(0xFF00E5FF),
                borderColor = CncBorder,
                height = 28.dp,
                fontSize = 8.5.sp,
                modifier = Modifier.widthIn(min = 52.dp),
                onClick = onToggleConsoleTheme
            )

            CncKeyCap(
                label = if (isTwinScreen) "TWIN" else "SPLIT",
                icon = if (isTwinScreen) Icons.Default.ViewSidebar else Icons.Default.Dvr,
                containerColor = if (isTwinScreen) Color(0xFF1B5E20) else CncSurfaceDark,
                textColor = if (isTwinScreen) CncDroGreen else CncTextSecondary,
                borderColor = if (isTwinScreen) CncDroGreen else CncBorder,
                height = 28.dp,
                fontSize = 8.5.sp,
                modifier = Modifier.widthIn(min = 50.dp),
                onClick = onToggleTwinScreen
            )

            CncKeyCap(
                label = "",
                icon = Icons.Default.Build,
                containerColor = CncSurfaceDark,
                textColor = CncAccentOrange,
                borderColor = CncBorder,
                height = 28.dp,
                modifier = Modifier.width(30.dp),
                onClick = onOpenToolTable
            )

            Box {
                CncKeyCap(
                    label = "PGM",
                    icon = Icons.Default.FolderOpen,
                    containerColor = CncAccentOrange,
                    textColor = CncDarkBackground,
                    height = 28.dp,
                    fontSize = 9.sp,
                    modifier = Modifier.widthIn(min = 52.dp),
                    onClick = onOpenProgramMenu
                )
                DropdownMenu(
                    expanded = programMenuExpanded,
                    onDismissRequest = onDismissProgramMenu,
                    modifier = Modifier.background(CncSurfaceDark)
                ) {
                    SamplePrograms.all.forEach { (name, code) ->
                        val isErrorDemo = name.startsWith("FOUTDEMO")
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = name,
                                    color = if (isErrorDemo) CncRapidOrange else CncTextPrimary,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = { onSelectProgram(name, code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    CncKeyCap(
        label = if (hasBadge) "$label •" else label,
        icon = icon,
        containerColor = if (isSelected) CncAccentOrange else CncSurfaceDark,
        textColor = if (isSelected) CncDarkBackground else CncTextPrimary,
        borderColor = if (isSelected) CncAccentOrange else CncBorder,
        height = 28.dp,
        fontSize = 9.sp,
        cornerRadius = 5.dp,
        modifier = Modifier.padding(horizontal = 1.dp),
        onClick = onClick
    )
}

@Composable
private fun MobileBottomWorkspaceBar(
    activeTab: MobileWorkspaceTab,
    diagnosticsCount: Int,
    hasActiveDialog: Boolean,
    onSelectTab: (MobileWorkspaceTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CncPanelBackground)
            .border(0.8.dp, CncBorder)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CncKeyCap(
            label = "3D SIM & UITVOER",
            icon = Icons.Default.PlayCircle,
            containerColor = if (activeTab == MobileWorkspaceTab.SIMULATION) CncAccentOrange else CncSurfaceDark,
            textColor = if (activeTab == MobileWorkspaceTab.SIMULATION) CncDarkBackground else CncTextPrimary,
            borderColor = if (activeTab == MobileWorkspaceTab.SIMULATION) CncAccentOrange else CncBorder,
            height = 36.dp,
            fontSize = 10.sp,
            cornerRadius = 6.dp,
            modifier = Modifier.weight(1.1f),
            onClick = { onSelectTab(MobileWorkspaceTab.SIMULATION) }
        )

        CncKeyCap(
            label = if (hasActiveDialog) "PROGRAMMEREN ●" else "PROGRAMMEREN",
            icon = Icons.Default.Terminal,
            containerColor = if (activeTab == MobileWorkspaceTab.PROGRAMMING) CncAccentOrange else CncSurfaceDark,
            textColor = if (activeTab == MobileWorkspaceTab.PROGRAMMING) CncDarkBackground else CncTextPrimary,
            borderColor = if (activeTab == MobileWorkspaceTab.PROGRAMMING) CncAccentOrange else CncBorder,
            height = 36.dp,
            fontSize = 10.sp,
            cornerRadius = 6.dp,
            modifier = Modifier.weight(1.1f),
            onClick = { onSelectTab(MobileWorkspaceTab.PROGRAMMING) }
        )

        val toolsLabel = if (diagnosticsCount > 0) "TOOLS ($diagnosticsCount)" else "TOOLS & PGM"
        CncKeyCap(
            label = toolsLabel,
            icon = Icons.Default.BuildCircle,
            containerColor = if (activeTab == MobileWorkspaceTab.TOOLS) CncAccentOrange else CncSurfaceDark,
            textColor = if (activeTab == MobileWorkspaceTab.TOOLS) CncDarkBackground else CncTextPrimary,
            borderColor = if (activeTab == MobileWorkspaceTab.TOOLS) CncAccentOrange else CncBorder,
            height = 36.dp,
            fontSize = 10.sp,
            cornerRadius = 6.dp,
            modifier = Modifier.weight(0.95f),
            onClick = { onSelectTab(MobileWorkspaceTab.TOOLS) }
        )
    }
}

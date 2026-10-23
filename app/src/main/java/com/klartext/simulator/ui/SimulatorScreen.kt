package com.klartext.simulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.sample.SamplePrograms
import com.klartext.simulator.ui.components.*
import com.klartext.simulator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    viewModel: SimulatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var programMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "HEIDENHAIN TNC 640 SIMULATOR",
                            fontSize = 13.sp,
                            color = CncAccentOrange
                        )
                        Text(
                            text = uiState.programName,
                            fontSize = 10.sp,
                            color = CncTextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openImportExport() }) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "DXF & ISO Import/Export",
                            tint = CncAccentOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.openToolTable() }) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Gereedschapstabel",
                            tint = CncAccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box {
                        IconButton(onClick = { programMenuExpanded = true }) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Programma's",
                                tint = CncAccentOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = programMenuExpanded,
                            onDismissRequest = { programMenuExpanded = false },
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
                                    onClick = {
                                        viewModel.loadProgram(name, code)
                                        programMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CncDarkBackground
                )
            )
        },
        containerColor = CncDarkBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Digital Readout (DRO) & Warning Alerts
            DigitalReadout(
                state = uiState.machineState,
                diagnostics = uiState.diagnostics,
                onOpenDiagnostics = { viewModel.openDiagnostics() },
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Simulation Canvas (2D Dual path & 2.5D material removal)
            SimulationCanvas(
                state = uiState,
                currentStepIndex = uiState.machineState.currentStepIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
            )

            // 3. Klartext Code Viewer (TNC blue selection bar + syntax highlighting)
            CodeViewer(
                blocks = uiState.blocks,
                activeLineNumber = uiState.machineState.activeLineNumber,
                diagnostics = uiState.diagnostics,
                onLineClick = { line -> viewModel.seekToLine(line) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
            )

            // 4. Control Panel (Play, Pause, Single-Block step, scrub slider, speed, view switcher)
            ControlPanel(
                isPlaying = uiState.isPlaying,
                currentStep = uiState.machineState.currentStepIndex,
                totalSteps = uiState.segments.size,
                playbackSpeed = uiState.playbackSpeed,
                viewMode = uiState.viewMode,
                showContour = uiState.showProgrammedContour,
                onPlayPause = { viewModel.togglePlayPause() },
                onStepForward = { viewModel.stepForward() },
                onStepBack = { viewModel.stepBackward() },
                onReset = { viewModel.reset() },
                onSeek = { step -> viewModel.seekToStep(step) },
                onSpeedChange = { spd -> viewModel.setPlaybackSpeed(spd) },
                onViewModeChange = { mode -> viewModel.setViewMode(mode) },
                onToggleContour = { viewModel.toggleProgrammedContour() },
                modifier = Modifier.fillMaxWidth()
            )

            // 5. TNC 640 Keypad & Interactive Dialog Session (ENT / NO ENT buttons + softkeys)
            TncKeypad(
                activeDialog = uiState.activeDialog,
                diagnosticsCount = uiState.diagnostics.size,
                onStartCommand = { cmd -> viewModel.startCommand(cmd) },
                onDialogChar = { ch -> viewModel.onDialogChar(ch) },
                onDialogEnt = { viewModel.onDialogEnt() },
                onDialogNoEnt = { viewModel.onDialogNoEnt() },
                onDialogDel = { viewModel.onDialogDel() },
                onDialogEnd = { viewModel.onDialogEnd() },
                onOpenCalculator = { viewModel.openCalculator() },
                onOpenDiagnostics = { viewModel.openDiagnostics() },
                onOpenToolTable = { viewModel.openToolTable() },
                onOpenImportExport = { viewModel.openImportExport() },
                onCopyLine = { viewModel.copyCurrentLine() },
                onPasteLine = { viewModel.pasteLine() },
                onDeleteLine = { viewModel.deleteCurrentLine() },
                modifier = Modifier.fillMaxWidth()
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
        ToolTableDialog(onDismiss = { viewModel.closeToolTable() })
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

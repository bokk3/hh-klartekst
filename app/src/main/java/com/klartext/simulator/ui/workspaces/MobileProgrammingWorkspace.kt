package com.klartext.simulator.ui.workspaces

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.ActiveDialogSession
import com.klartext.simulator.model.SimulationUiState
import com.klartext.simulator.ui.components.CodeViewer
import com.klartext.simulator.ui.designsystem.CncKeyCap
import com.klartext.simulator.ui.designsystem.CncSoftkey
import com.klartext.simulator.ui.theme.*

/**
 * Tab 2: Dedicated Klartext Programming Workspace.
 * - Landscape: Code Viewer on the Left (52%) + Full TNC 640 Programming Keypad on the Right (48%).
 * - Portrait: Code Viewer on Top + Collapsible CNC Programming Keypad Dock at the Bottom.
 */
@Composable
fun MobileProgrammingWorkspace(
    uiState: SimulationUiState,
    onLineClick: (Int) -> Unit,
    onNavigateLine: (Int) -> Unit,
    onStartCommand: (String) -> Unit,
    onDialogChar: (String) -> Unit,
    onDialogEnt: () -> Unit,
    onDialogNoEnt: () -> Unit,
    onDialogDel: () -> Unit,
    onDialogEnd: () -> Unit,
    onCopyLine: () -> Unit,
    onPasteLine: () -> Unit,
    onDeleteLine: () -> Unit,
    onTogglePortraitKeypad: () -> Unit,
    onJumpToSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val colors = getConsoleColors(uiState.consoleTheme)
    val isKeypadVisible = uiState.isPortraitKeypadExpanded || uiState.activeDialog != null

    if (isLandscape) {
        // === LANDSCAPE: CODE LEFT (52%) | TNC 640 KEYPAD RIGHT (48%) ===
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left Column: Edit Toolbar + Active Dialog Banner + Code Viewer
            Column(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CodeEditorToolbar(
                    colors = colors,
                    hasClipboard = uiState.clipboardLine != null,
                    isPortrait = false,
                    isKeypadExpanded = true,
                    onCopyLine = onCopyLine,
                    onPasteLine = onPasteLine,
                    onDeleteLine = onDeleteLine,
                    onNavigateUp = { onNavigateLine(-1) },
                    onNavigateDown = { onNavigateLine(1) },
                    onToggleKeypad = {},
                    onJumpToSimulation = onJumpToSimulation
                )

                if (uiState.activeDialog != null) {
                    HeidenhainDialogBanner(activeDialog = uiState.activeDialog)
                }

                CodeViewer(
                    blocks = uiState.blocks,
                    activeLineNumber = uiState.machineState.activeLineNumber,
                    diagnostics = uiState.diagnostics,
                    onLineClick = onLineClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // Right Column: Full Tactile TE 640 Programming & Dialog Keypad
            TncProgrammingDeck(
                activeDialog = uiState.activeDialog,
                colors = colors,
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
    } else {
        // === PORTRAIT: CODE VIEWER TOP | COLLAPSIBLE CNC KEYPAD BOTTOM ===
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CodeEditorToolbar(
                colors = colors,
                hasClipboard = uiState.clipboardLine != null,
                isPortrait = true,
                isKeypadExpanded = isKeypadVisible,
                onCopyLine = onCopyLine,
                onPasteLine = onPasteLine,
                onDeleteLine = onDeleteLine,
                onNavigateUp = { onNavigateLine(-1) },
                onNavigateDown = { onNavigateLine(1) },
                onToggleKeypad = onTogglePortraitKeypad,
                onJumpToSimulation = onJumpToSimulation
            )

            if (uiState.activeDialog != null) {
                HeidenhainDialogBanner(activeDialog = uiState.activeDialog)
            }

            CodeViewer(
                blocks = uiState.blocks,
                activeLineNumber = uiState.machineState.activeLineNumber,
                diagnostics = uiState.diagnostics,
                onLineClick = onLineClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            AnimatedVisibility(
                visible = isKeypadVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TncProgrammingDeck(
                    activeDialog = uiState.activeDialog,
                    colors = colors,
                    onStartCommand = onStartCommand,
                    onDialogChar = onDialogChar,
                    onDialogEnt = onDialogEnt,
                    onDialogNoEnt = onDialogNoEnt,
                    onDialogDel = onDialogDel,
                    onDialogEnd = onDialogEnd,
                    onNavigateLine = onNavigateLine,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CodeEditorToolbar(
    colors: ConsoleColors,
    hasClipboard: Boolean,
    isPortrait: Boolean,
    isKeypadExpanded: Boolean,
    onCopyLine: () -> Unit,
    onPasteLine: () -> Unit,
    onDeleteLine: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onToggleKeypad: () -> Unit,
    onJumpToSimulation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.casingBackground, RoundedCornerShape(6.dp))
            .border(1.dp, colors.casingBorder, RoundedCornerShape(6.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CncSoftkey(
            label = "KOPIËER",
            colors = colors,
            height = 28.dp,
            modifier = Modifier.weight(1f),
            onClick = onCopyLine
        )
        CncSoftkey(
            label = "PLAK",
            colors = colors,
            height = 28.dp,
            isSelected = hasClipboard,
            modifier = Modifier.weight(0.9f),
            onClick = onPasteLine
        )
        CncKeyCap(
            label = "DEL REGEL",
            containerColor = colors.deleteKeyBackground,
            textColor = colors.deleteKeyText,
            borderColor = colors.numpadKeyBorder,
            height = 28.dp,
            fontSize = 9.sp,
            modifier = Modifier.weight(1.1f),
            onClick = onDeleteLine
        )
        CncKeyCap(
            label = "",
            icon = Icons.Default.KeyboardArrowUp,
            containerColor = colors.axisKeyBackground,
            textColor = colors.numpadKeyText,
            borderColor = colors.axisKeyBorder,
            height = 28.dp,
            modifier = Modifier.weight(0.6f),
            onClick = onNavigateUp
        )
        CncKeyCap(
            label = "",
            icon = Icons.Default.KeyboardArrowDown,
            containerColor = colors.axisKeyBackground,
            textColor = colors.numpadKeyText,
            borderColor = colors.axisKeyBorder,
            height = 28.dp,
            modifier = Modifier.weight(0.6f),
            onClick = onNavigateDown
        )
        if (isPortrait) {
            CncKeyCap(
                label = if (isKeypadExpanded) "TOETSEN ▼" else "TOETSEN ▲",
                containerColor = colors.cycleKeyBackground,
                textColor = colors.cycleKeyText,
                height = 28.dp,
                fontSize = 8.5.sp,
                modifier = Modifier.weight(1.2f),
                onClick = onToggleKeypad
            )
        }
        CncKeyCap(
            label = "SIM ▶",
            containerColor = colors.ncStartBackground,
            textColor = colors.ncStartText,
            height = 28.dp,
            fontSize = 9.sp,
            modifier = Modifier.weight(0.9f),
            onClick = onJumpToSimulation
        )
    }
}

@Composable
fun HeidenhainDialogBanner(
    activeDialog: ActiveDialogSession,
    modifier: Modifier = Modifier
) {
    val step = activeDialog.currentStep
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D47A1), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF42A5F5), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TNC DIALOOG: ${activeDialog.commandTitle}",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.5.sp
            )
            Text(
                text = "Stap ${activeDialog.currentStepIndex + 1}/${activeDialog.steps.size} • ENT=OK | NO ENT=Sla over",
                color = Color(0xFFFFD54F),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${step?.promptLabel ?: ""} ${activeDialog.currentInput}▌",
                color = CncDroGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = activeDialog.accumulatedTokens.joinToString(" "),
                color = Color(0xFFBBDEFB),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Full HEIDENHAIN TE 640 Programming & Dialog Keypad Deck.
 * Uses CncKeyCap everywhere to guarantee zero overlapping borders.
 */
@Composable
fun TncProgrammingDeck(
    activeDialog: ActiveDialogSession?,
    colors: ConsoleColors,
    onStartCommand: (String) -> Unit,
    onDialogChar: (String) -> Unit,
    onDialogEnt: () -> Unit,
    onDialogNoEnt: () -> Unit,
    onDialogDel: () -> Unit,
    onDialogEnd: () -> Unit,
    onNavigateLine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(colors.casingBackground, RoundedCornerShape(8.dp))
            .border(1.dp, colors.casingBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. CONTOUR & CYCLE COMMAND KEYS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.sectionBackground, RoundedCornerShape(5.dp))
                .border(1.dp, colors.sectionBorder, RoundedCornerShape(5.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "BAAN- & CYCLUSTOETSEN",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.sectionTitleColor
            )
            // Contour Row: L, LP, CC, C, CR, CT, RND, CHF
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("L", "CC", "C", "CR", "CT", "RND", "CHF").forEach { cmd ->
                    CncKeyCap(
                        label = cmd,
                        containerColor = colors.contourKeyBackground,
                        textColor = colors.contourKeyText,
                        height = 29.dp,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartCommand(cmd) }
                    )
                }
            }
            // Cycle & Subprogram Row: APPR, DEP, CYCL DEF, CYCL CALL, TOOL CALL, LBL SET, CALL LBL
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("APPR", "DEP", "CYCL DEF", "CYCL CALL", "TOOL CALL", "LBL SET", "CALL LBL").forEach { cmd ->
                    CncKeyCap(
                        label = cmd,
                        containerColor = colors.cycleKeyBackground,
                        textColor = colors.cycleKeyText,
                        height = 29.dp,
                        fontSize = 8.5.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartCommand(cmd) }
                    )
                }
            }
        }

        // 2. AXIS & DIALOG TOKEN SHORTCUT KEYS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.sectionBackground, RoundedCornerShape(5.dp))
                .border(1.dp, colors.sectionBorder, RoundedCornerShape(5.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("X", "Y", "Z", "IX", "IY", "IZ", "RL", "RR", "R0", "FMAX").forEach { token ->
                    val isZ = token.contains("Z")
                    val isComp = token in listOf("RL", "RR", "R0", "FMAX")
                    CncKeyCap(
                        label = token,
                        containerColor = if (isComp) colors.contourKeyBackground else colors.axisKeyBackground,
                        textColor = when {
                            isComp -> colors.contourKeyText
                            isZ -> colors.axisKeyTextZ
                            else -> colors.axisKeyTextXy
                        },
                        borderColor = colors.axisKeyBorder,
                        height = 26.dp,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (activeDialog != null) {
                                onDialogChar(token)
                            }
                        }
                    )
                }
            }
        }

        // 3. NUMERIC KEYPAD & ENT / NO ENT CONFIRMATION BLOCK
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Left: 4x4 Numeric Grid
            Column(
                modifier = Modifier
                    .weight(1.45f)
                    .background(colors.sectionBackground, RoundedCornerShape(5.dp))
                    .border(1.dp, colors.sectionBorder, RoundedCornerShape(5.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("7", "8", "9").forEach { k ->
                        CncKeyCap(
                            label = k,
                            containerColor = colors.numpadKeyBackground,
                            textColor = colors.numpadKeyText,
                            borderColor = colors.numpadKeyBorder,
                            height = 29.dp,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            onClick = { onDialogChar(k) }
                        )
                    }
                    CncKeyCap(
                        label = "CE",
                        containerColor = colors.deleteKeyBackground,
                        textColor = colors.deleteKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 29.dp,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        onClick = onDialogDel
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("4", "5", "6").forEach { k ->
                        CncKeyCap(
                            label = k,
                            containerColor = colors.numpadKeyBackground,
                            textColor = colors.numpadKeyText,
                            borderColor = colors.numpadKeyBorder,
                            height = 29.dp,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            onClick = { onDialogChar(k) }
                        )
                    }
                    CncKeyCap(
                        label = "DEL",
                        containerColor = colors.deleteKeyBackground,
                        textColor = colors.deleteKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 29.dp,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        onClick = onDialogDel
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("1", "2", "3").forEach { k ->
                        CncKeyCap(
                            label = k,
                            containerColor = colors.numpadKeyBackground,
                            textColor = colors.numpadKeyText,
                            borderColor = colors.numpadKeyBorder,
                            height = 29.dp,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            onClick = { onDialogChar(k) }
                        )
                    }
                    CncKeyCap(
                        label = "+/-",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 29.dp,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { onDialogChar("-") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    CncKeyCap(
                        label = "0",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 29.dp,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { onDialogChar("0") }
                    )
                    CncKeyCap(
                        label = ".",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 29.dp,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { onDialogChar(".") }
                    )
                    CncKeyCap(
                        label = "Q",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.contourKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 29.dp,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { onDialogChar("Q") }
                    )
                    CncKeyCap(
                        label = "END",
                        containerColor = colors.cycleKeyBackground,
                        textColor = Color.White,
                        borderColor = colors.numpadKeyBorder,
                        height = 29.dp,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        onClick = onDialogEnd
                    )
                }
            }

            // Right: Cursor Up/Down + NO ENT + ENT
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .background(colors.sectionBackground, RoundedCornerShape(5.dp))
                    .border(1.dp, colors.sectionBorder, RoundedCornerShape(5.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    CncKeyCap(
                        label = "▲",
                        containerColor = colors.axisKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.axisKeyBorder,
                        height = 29.dp,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateLine(-1) }
                    )
                    CncKeyCap(
                        label = "▼",
                        containerColor = colors.axisKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.axisKeyBorder,
                        height = 29.dp,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateLine(1) }
                    )
                }

                CncKeyCap(
                    label = "NO ENT",
                    containerColor = colors.noEntKeyBackground,
                    textColor = colors.noEntKeyText,
                    height = 38.dp,
                    fontSize = 11.sp,
                    cornerRadius = 5.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDialogNoEnt
                )

                CncKeyCap(
                    label = "ENT",
                    containerColor = colors.entKeyBackground,
                    textColor = colors.entKeyText,
                    height = 48.dp,
                    fontSize = 13.sp,
                    cornerRadius = 5.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDialogEnt
                )
            }
        }
    }
}

package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.ActiveDialogSession
import com.klartext.simulator.model.ConsoleTheme
import com.klartext.simulator.ui.theme.*

/**
 * Authentic HEIDENHAIN TE 640 Operating Panel & Console
 * Features:
 * - 2 Themes: CLASSIC_TNC640 (Authentic industrial hardware) & HYPERMODERN (Cyber CNC)
 * - Dual Rotary Potentiometers: Feed Rate & Spindle Speed (0% - 150%)
 * - NC START (Green), NC STOP (Red), SINGLE BLOCK mode
 * - Axis keys (X, Y, Z, IV)
 * - Klartekst Dialogue keys (L, C, CR, CT, CC, RND, CHF, APPR, DEP, CYCL DEF, TOOL CALL, LBL, Q)
 * - Numeric Keypad with authentic keycap colors (Ivory in Classic) & CE/DEL
 * - Dialog ENT, NO ENT, END, cursor navigation
 */
@Composable
fun HeidenhainTe640Console(
    activeDialog: ActiveDialogSession?,
    feedOverride: Int,
    spindleOverride: Int,
    isPlaying: Boolean,
    isSingleBlockActive: Boolean,
    consoleTheme: ConsoleTheme = ConsoleTheme.CLASSIC_TNC640,
    onToggleConsoleTheme: () -> Unit = {},
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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colors = getConsoleColors(consoleTheme)

    Column(
        modifier = modifier
            .background(colors.casingBackground, RoundedCornerShape(8.dp))
            .border(1.5.dp, colors.casingBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- CONSOLE HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.headerBackground, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isPlaying) CncDroGreen else Color(0xFF78909C), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HEIDENHAIN TE 640",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerTextColor
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // One-click Theme Toggle Badge
                Button(
                    onClick = onToggleConsoleTheme,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.headerBadgeBackground,
                        contentColor = colors.headerBadgeTextColor
                    ),
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        text = if (colors.isClassic) "TNC 640 KLASSIEK" else "HYPERMODERN",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Single / Continuous Mode Indicator
                Text(
                    text = if (isSingleBlockActive) "[ENKELE REGEL]" else "[CONTINU]",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSingleBlockActive) Color(0xFFFFD54F) else CncDroGreen
                )
            }
        }

        // --- 1. DUAL ROTARY OVERRIDE POTENTIOMETERS (0% - 150%) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RotaryOverrideDial(
                title = "FEED %",
                valuePercent = feedOverride,
                onValueChange = onFeedOverrideChange,
                accentColor = CncAccentOrange,
                colors = colors,
                modifier = Modifier.weight(1f)
            )
            RotaryOverrideDial(
                title = "SPINDLE %",
                valuePercent = spindleOverride,
                onValueChange = onSpindleOverrideChange,
                accentColor = CncFeedCyan,
                colors = colors,
                modifier = Modifier.weight(1f)
            )
        }

        // --- 2. MACHINE EXECUTION CONTROL (NC START, NC STOP, SINGLE BLOCK, RESET) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NC START (Authentic Green Button)
            Button(
                onClick = onNcStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.ncStartBackground,
                    contentColor = colors.ncStartText
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "NC START", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("NC START", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // NC STOP (Authentic Red Button)
            Button(
                onClick = onNcStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.ncStopBackground,
                    contentColor = colors.ncStopText
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Pause, contentDescription = "NC STOP", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("NC STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // SINGLE BLOCK TOGGLE
            Button(
                onClick = onToggleSingleBlock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSingleBlockActive) colors.singleBlockActive else colors.singleBlockInactive,
                    contentColor = if (isSingleBlockActive) colors.singleBlockActiveText else colors.singleBlockInactiveText
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(38.dp)
            ) {
                Text(
                    text = "SINGLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // RESET
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(38.dp)
                    .background(colors.sectionBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.sectionBorder, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    Icons.Default.Replay,
                    contentDescription = "Reset",
                    tint = colors.contourKeyText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // --- 3. ACTIVE DIALOG STRIP (IF DIALOG RUNNING) ---
        if (activeDialog != null) {
            val step = activeDialog.currentStep
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D47A1), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF42A5F5), RoundedCornerShape(6.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DIALOOG: ${activeDialog.commandTitle}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${activeDialog.currentStepIndex + 1}/${activeDialog.steps.size}",
                        color = Color(0xFFBBDEFB),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${step?.promptLabel ?: ""} ${activeDialog.currentInput}",
                    color = CncDroGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- 4. KLARTEKST PROGRAMMING KEYS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.sectionBackground, RoundedCornerShape(6.dp))
                .border(1.dp, colors.sectionBorder, RoundedCornerShape(6.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "PROGRAMMEERTOETSEN",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = colors.sectionTitleColor
            )

            // Linear & Arcs Row (L, C, CR, CT, CC, RND, CHF) - Iconic Orange Contour keys
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("L", "C", "CR", "CT", "CC", "RND", "CHF").forEach { cmd ->
                    ConsoleCmdKey(
                        text = cmd,
                        containerColor = colors.contourKeyBackground,
                        textColor = colors.contourKeyText,
                        modifier = Modifier.weight(1f)
                    ) { onStartCommand(cmd) }
                }
            }

            // Cycles, Tool, Labels Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("APPR", "DEP", "CYCL DEF", "CYCL CALL", "TOOL CALL", "LBL SET", "CALL LBL").forEach { cmd ->
                    ConsoleCmdKey(
                        text = cmd,
                        containerColor = colors.cycleKeyBackground,
                        textColor = colors.cycleKeyText,
                        modifier = Modifier.weight(1f)
                    ) { onStartCommand(cmd) }
                }
            }
        }

        // --- 5. AXIS KEYS & JOG NUDGE ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.sectionBackground, RoundedCornerShape(6.dp))
                .border(1.dp, colors.sectionBorder, RoundedCornerShape(6.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "AS-TOETSEN & RICHTING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = colors.sectionTitleColor
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("X+", "X-", "Y+", "Y-", "Z+", "Z-", "IV+", "IV-").forEach { axis ->
                    Button(
                        onClick = {
                            if (activeDialog != null) {
                                onDialogChar(axis.take(1))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.axisKeyBackground),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .border(0.8.dp, colors.axisKeyBorder, RoundedCornerShape(3.dp))
                    ) {
                        Text(
                            text = axis,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (axis.startsWith("Z")) colors.axisKeyTextZ else colors.axisKeyTextXy
                        )
                    }
                }
            }
        }

        // --- 6. NUMERIC KEYPAD, NAVIGATION & CONFIRMATION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Numeric Block (Left)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .background(colors.sectionBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.sectionBorder, RoundedCornerShape(6.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("7", "8", "9").forEach { k ->
                        NumpadKey(
                            label = k,
                            containerColor = colors.numpadKeyBackground,
                            textColor = colors.numpadKeyText,
                            borderColor = colors.numpadKeyBorder,
                            modifier = Modifier.weight(1f)
                        ) { onDialogChar(k) }
                    }
                    NumpadKey(
                        label = "CE",
                        containerColor = colors.deleteKeyBackground,
                        textColor = colors.deleteKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogDel() }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("4", "5", "6").forEach { k ->
                        NumpadKey(
                            label = k,
                            containerColor = colors.numpadKeyBackground,
                            textColor = colors.numpadKeyText,
                            borderColor = colors.numpadKeyBorder,
                            modifier = Modifier.weight(1f)
                        ) { onDialogChar(k) }
                    }
                    NumpadKey(
                        label = "DEL",
                        containerColor = colors.deleteKeyBackground,
                        textColor = colors.deleteKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogDel() }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("1", "2", "3").forEach { k ->
                        NumpadKey(
                            label = k,
                            containerColor = colors.numpadKeyBackground,
                            textColor = colors.numpadKeyText,
                            borderColor = colors.numpadKeyBorder,
                            modifier = Modifier.weight(1f)
                        ) { onDialogChar(k) }
                    }
                    NumpadKey(
                        label = "+/-",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogChar("-") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    NumpadKey(
                        label = "0",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogChar("0") }
                    NumpadKey(
                        label = ".",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogChar(".") }
                    NumpadKey(
                        label = "Q",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.contourKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogChar("Q") }
                    NumpadKey(
                        label = "END",
                        containerColor = colors.cycleKeyBackground,
                        textColor = Color.White,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogEnd() }
                }
            }

            // Navigation & Big ENT / NO ENT (Right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.sectionBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.sectionBorder, RoundedCornerShape(6.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Line Navigation (Up / Down)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onNavigateLine(-1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(colors.axisKeyBackground, RoundedCornerShape(3.dp))
                            .border(0.8.dp, colors.axisKeyBorder, RoundedCornerShape(3.dp))
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Regel Omhoog", tint = colors.numpadKeyText)
                    }
                    IconButton(
                        onClick = { onNavigateLine(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(colors.axisKeyBackground, RoundedCornerShape(3.dp))
                            .border(0.8.dp, colors.axisKeyBorder, RoundedCornerShape(3.dp))
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Regel Omlaag", tint = colors.numpadKeyText)
                    }
                }

                // Authentic Warning Red NO ENT Key (Preserves modal RL/RR!)
                Button(
                    onClick = onDialogNoEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.noEntKeyBackground),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Text(
                        text = "NO ENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.noEntKeyText
                    )
                }

                // Authentic Machine Green ENT Key
                Button(
                    onClick = onDialogEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.entKeyBackground),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = "ENT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.entKeyText
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleCmdKey(
    text: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier.height(28.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun NumpadKey(
    label: String,
    containerColor: Color,
    textColor: Color,
    borderColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
            .height(28.dp)
            .then(if (borderColor != null) Modifier.border(0.8.dp, borderColor, RoundedCornerShape(3.dp)) else Modifier)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

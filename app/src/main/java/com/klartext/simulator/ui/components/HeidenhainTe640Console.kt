package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
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
import com.klartext.simulator.ui.designsystem.CncKeyCap
import com.klartext.simulator.ui.theme.*

/**
 * Authentic HEIDENHAIN TE 640 Operating Panel & Console.
 * Uses CncKeyCap to ensure pixel-accurate industrial keycaps with zero Material 3
 * minimum touch-target border overflow.
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
        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                CncKeyCap(
                    label = if (colors.isClassic) "TNC 640 KLASSIEK" else "HYPERMODERN",
                    containerColor = colors.headerBadgeBackground,
                    textColor = colors.headerBadgeTextColor,
                    height = 20.dp,
                    fontSize = 8.5.sp,
                    cornerRadius = 3.dp,
                    modifier = Modifier.width(102.dp),
                    onClick = onToggleConsoleTheme
                )

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
            CncKeyCap(
                label = "NC START",
                icon = Icons.Default.PlayArrow,
                containerColor = colors.ncStartBackground,
                textColor = colors.ncStartText,
                height = 36.dp,
                fontSize = 11.sp,
                cornerRadius = 5.dp,
                modifier = Modifier.weight(1.3f),
                onClick = onNcStart
            )
            CncKeyCap(
                label = "NC STOP",
                icon = Icons.Default.Pause,
                containerColor = colors.ncStopBackground,
                textColor = colors.ncStopText,
                height = 36.dp,
                fontSize = 11.sp,
                cornerRadius = 5.dp,
                modifier = Modifier.weight(1.3f),
                onClick = onNcStop
            )
            CncKeyCap(
                label = "SINGLE",
                containerColor = if (isSingleBlockActive) colors.singleBlockActive else colors.singleBlockInactive,
                textColor = if (isSingleBlockActive) colors.singleBlockActiveText else colors.singleBlockInactiveText,
                height = 36.dp,
                fontSize = 10.sp,
                cornerRadius = 5.dp,
                modifier = Modifier.weight(1.0f),
                onClick = onToggleSingleBlock
            )
            CncKeyCap(
                label = "",
                icon = Icons.Default.Replay,
                containerColor = colors.sectionBackground,
                textColor = colors.contourKeyText,
                borderColor = colors.sectionBorder,
                height = 36.dp,
                cornerRadius = 5.dp,
                modifier = Modifier.width(38.dp),
                onClick = onReset
            )
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
                    text = "${step?.promptLabel ?: ""} ${activeDialog.currentInput}▌",
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

            // Linear & Arcs Row (L, C, CR, CT, CC, RND, CHF)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("L", "C", "CR", "CT", "CC", "RND", "CHF").forEach { cmd ->
                    CncKeyCap(
                        label = cmd,
                        containerColor = colors.contourKeyBackground,
                        textColor = colors.contourKeyText,
                        height = 27.dp,
                        fontSize = 9.5.sp,
                        cornerRadius = 3.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartCommand(cmd) }
                    )
                }
            }

            // Cycles, Tool, Labels Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("APPR", "DEP", "CYCL DEF", "CYCL CALL", "TOOL CALL", "LBL SET", "CALL LBL").forEach { cmd ->
                    CncKeyCap(
                        label = cmd,
                        containerColor = colors.cycleKeyBackground,
                        textColor = colors.cycleKeyText,
                        height = 27.dp,
                        fontSize = 8.5.sp,
                        cornerRadius = 3.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartCommand(cmd) }
                    )
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
                    CncKeyCap(
                        label = axis,
                        containerColor = colors.axisKeyBackground,
                        textColor = if (axis.startsWith("Z")) colors.axisKeyTextZ else colors.axisKeyTextXy,
                        borderColor = colors.axisKeyBorder,
                        height = 25.dp,
                        fontSize = 9.sp,
                        cornerRadius = 3.dp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (activeDialog != null) {
                                onDialogChar(axis.take(1))
                            }
                        }
                    )
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
                    .weight(1.35f)
                    .background(colors.sectionBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.sectionBorder, RoundedCornerShape(6.dp))
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
                            height = 27.dp,
                            fontSize = 11.sp,
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
                        height = 27.dp,
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
                            height = 27.dp,
                            fontSize = 11.sp,
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
                        height = 27.dp,
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
                            height = 27.dp,
                            fontSize = 11.sp,
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
                        height = 27.dp,
                        fontSize = 10.5.sp,
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
                        height = 27.dp,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { onDialogChar("0") }
                    )
                    CncKeyCap(
                        label = ".",
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 27.dp,
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
                        height = 27.dp,
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
                        height = 27.dp,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        onClick = onDialogEnd
                    )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    CncKeyCap(
                        label = "▲",
                        containerColor = colors.axisKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.axisKeyBorder,
                        height = 27.dp,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateLine(-1) }
                    )
                    CncKeyCap(
                        label = "▼",
                        containerColor = colors.axisKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.axisKeyBorder,
                        height = 27.dp,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateLine(1) }
                    )
                }

                CncKeyCap(
                    label = "NO ENT",
                    containerColor = colors.noEntKeyBackground,
                    textColor = colors.noEntKeyText,
                    height = 36.dp,
                    fontSize = 11.sp,
                    cornerRadius = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDialogNoEnt
                )

                CncKeyCap(
                    label = "ENT",
                    containerColor = colors.entKeyBackground,
                    textColor = colors.entKeyText,
                    height = 44.dp,
                    fontSize = 13.sp,
                    cornerRadius = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDialogEnt
                )
            }
        }
    }
}

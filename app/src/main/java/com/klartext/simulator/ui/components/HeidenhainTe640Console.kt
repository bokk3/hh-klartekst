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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.ActiveDialogSession
import com.klartext.simulator.ui.theme.*

/**
 * Authentic HEIDENHAIN TE 640 Operating Panel & Console
 * Features:
 * - Dual Rotary Potentiometers: Feed Rate & Spindle Speed (0% - 150%)
 * - NC START (Green), NC STOP (Red), SINGLE BLOCK mode
 * - Axis keys (X, Y, Z, IV)
 * - Klartekst Dialogue keys (L, C, CR, CT, CC, RND, CHF, APPR, DEP, CYCL DEF, TOOL CALL, LBL, Q)
 * - Numeric Keypad with signs & CE/DEL
 * - Dialog ENT, NO ENT, END, cursor navigation
 */
@Composable
fun HeidenhainTe640Console(
    activeDialog: ActiveDialogSession?,
    feedOverride: Int,
    spindleOverride: Int,
    isPlaying: Boolean,
    isSingleBlockActive: Boolean,
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

    Column(
        modifier = modifier
            .background(Color(0xFF141A20), RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFF2C3942), RoundedCornerShape(8.dp))
            .padding(6.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- CONSOLE HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B242C), RoundedCornerShape(4.dp))
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
                    text = "HEIDENHAIN TE 640 CONSOLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CncAccentOrange
                )
            }
            Text(
                text = if (isSingleBlockActive) "[ENKELE REGEL]" else "[CONTINU]",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSingleBlockActive) Color(0xFFFFD54F) else CncDroGreen
            )
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
                modifier = Modifier.weight(1f)
            )
            RotaryOverrideDial(
                title = "SPINDLE %",
                valuePercent = spindleOverride,
                onValueChange = onSpindleOverrideChange,
                accentColor = CncFeedCyan,
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
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
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
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White
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
                    containerColor = if (isSingleBlockActive) Color(0xFFE65100) else Color(0xFF263238),
                    contentColor = if (isSingleBlockActive) Color.White else CncTextSecondary
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
                    .background(Color(0xFF263238), RoundedCornerShape(6.dp))
            ) {
                Icon(
                    Icons.Default.Replay,
                    contentDescription = "Reset",
                    tint = CncAccentOrange,
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
                .background(Color(0xFF1E2830), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF263238), RoundedCornerShape(6.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "PROGRAMMEERTOETSEN",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C)
            )

            // Linear & Arcs Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("L", "C", "CR", "CT", "CC", "RND", "CHF").forEach { cmd ->
                    ConsoleCmdKey(cmd, Modifier.weight(1f)) { onStartCommand(cmd) }
                }
            }

            // Cycles, Tool, Labels Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("APPR", "DEP", "CYCL DEF", "CYCL CALL", "TOOL CALL", "LBL SET", "CALL LBL").forEach { cmd ->
                    ConsoleCmdKey(cmd, Modifier.weight(1f)) { onStartCommand(cmd) }
                }
            }
        }

        // --- 5. AXIS KEYS & JOG NUDGE ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2830), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF263238), RoundedCornerShape(6.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "AS-TOETSEN & RICHTING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("X+", "X-", "Y+", "Y-", "Z+", "Z-", "IV+", "IV-").forEach { axis ->
                    Button(
                        onClick = {
                            if (activeDialog != null) {
                                onDialogChar(axis.take(1))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3942)),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    ) {
                        Text(
                            text = axis,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (axis.startsWith("Z")) CncDroGreen else CncAccentOrange
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
                    .background(Color(0xFF1E2830), RoundedCornerShape(6.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("7", "8", "9").forEach { k ->
                        NumpadKey(k, Modifier.weight(1f)) { onDialogChar(k) }
                    }
                    NumpadKey("CE", Modifier.weight(1f), color = Color(0xFFD32F2F)) {
                        onDialogDel()
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("4", "5", "6").forEach { k ->
                        NumpadKey(k, Modifier.weight(1f)) { onDialogChar(k) }
                    }
                    NumpadKey("DEL", Modifier.weight(1f), color = Color(0xFFD32F2F)) {
                        onDialogDel()
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("1", "2", "3").forEach { k ->
                        NumpadKey(k, Modifier.weight(1f)) { onDialogChar(k) }
                    }
                    NumpadKey("+/-", Modifier.weight(1f)) {
                        onDialogChar("-")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    NumpadKey("0", Modifier.weight(1f)) { onDialogChar("0") }
                    NumpadKey(".", Modifier.weight(1f)) { onDialogChar(".") }
                    NumpadKey("Q", Modifier.weight(1f), color = CncAccentOrange) { onDialogChar("Q") }
                    NumpadKey("END", Modifier.weight(1f), color = Color(0xFF37474F)) { onDialogEnd() }
                }
            }

            // Navigation & Big ENT / NO ENT (Right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1E2830), RoundedCornerShape(6.dp))
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
                            .background(Color(0xFF2C3942), RoundedCornerShape(3.dp))
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Regel Omhoog", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onNavigateLine(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(Color(0xFF2C3942), RoundedCornerShape(3.dp))
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Regel Omlaag", tint = Color.White)
                    }
                }

                // Authentic Red NO ENT Key (Preserves modal RL/RR!)
                Button(
                    onClick = onDialogNoEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
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
                        color = Color.White
                    )
                }

                // Authentic Green ENT Key
                Button(
                    onClick = onDialogEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
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
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleCmdKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier.height(28.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = CncAccentOrange,
            maxLines = 1
        )
    }
}

@Composable
private fun NumpadKey(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = CncTextPrimary,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3942)),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier.height(28.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

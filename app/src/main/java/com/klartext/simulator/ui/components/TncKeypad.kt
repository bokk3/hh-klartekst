package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun TncKeypad(
    activeDialog: ActiveDialogSession?,
    diagnosticsCount: Int,
    consoleTheme: ConsoleTheme = ConsoleTheme.CLASSIC_TNC640,
    onStartCommand: (String) -> Unit,
    onDialogChar: (String) -> Unit,
    onDialogEnt: () -> Unit,
    onDialogNoEnt: () -> Unit,
    onDialogDel: () -> Unit,
    onDialogEnd: () -> Unit,
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
    val colors = getConsoleColors(consoleTheme)

    Column(
        modifier = modifier
            .background(colors.casingBackground, RoundedCornerShape(8.dp))
            .border(1.dp, colors.casingBorder, RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Active Dialog Prompt (Heidenhain Blue Dialog Bar)
        if (activeDialog != null) {
            val step = activeDialog.currentStep
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D47A1), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF42A5F5), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TNC DIALOOG: ${activeDialog.commandTitle}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Stap ${activeDialog.currentStepIndex + 1}/${activeDialog.steps.size}",
                        color = Color(0xFFBBDEFB),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${step?.promptLabel ?: ""} ${activeDialog.currentInput}",
                        color = CncDroGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ENT = Akkoord | NO ENT = Overslaan",
                        color = Color(0xFFFFD54F),
                        fontSize = 10.sp
                    )
                }
            }

            // Keypad numbers & ENT / NO ENT row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("7", "8", "9", "+", "DEL").forEach { key ->
                    KeypadButton(
                        label = key,
                        containerColor = if (key == "DEL") colors.deleteKeyBackground else colors.numpadKeyBackground,
                        textColor = if (key == "DEL") colors.deleteKeyText else colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (key == "DEL") onDialogDel() else onDialogChar(key)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("4", "5", "6", "-", "END").forEach { key ->
                    KeypadButton(
                        label = key,
                        containerColor = if (key == "END") colors.cycleKeyBackground else colors.numpadKeyBackground,
                        textColor = if (key == "END") Color.White else colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (key == "END") onDialogEnd() else onDialogChar(key)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("1", "2", "3", "0", ".").forEach { key ->
                    KeypadButton(
                        label = key,
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        modifier = Modifier.weight(1f)
                    ) { onDialogChar(key) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // NO ENT button (Crucial for TNC 640 modal RL/RR preservation!)
                Button(
                    onClick = onDialogNoEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.noEntKeyBackground),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("NO ENT", color = colors.noEntKeyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // ENT button
                Button(
                    onClick = onDialogEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.entKeyBackground),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ENT", color = colors.entKeyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // TNC Programming Keys Row 1 (Contour keys - L, CC, C, CR, CT, RND, CHF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("L", "CC", "C", "CR", "CT", "RND", "CHF").forEach { cmd ->
                    TncCmdButton(
                        label = cmd,
                        containerColor = colors.contourKeyBackground,
                        textColor = colors.contourKeyText,
                        modifier = Modifier.weight(1f)
                    ) { onStartCommand(cmd) }
                }
            }

            // TNC Programming Keys Row 2 (Cycle & Tool keys)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("APPR", "DEP", "TOOL CALL", "CYCL DEF", "LBL SET", "CALL LBL").forEach { cmd ->
                    TncCmdButton(
                        label = cmd,
                        containerColor = colors.cycleKeyBackground,
                        textColor = colors.cycleKeyText,
                        modifier = Modifier.weight(1f)
                    ) { onStartCommand(cmd) }
                }
            }
        }

        // TNC Bottom Softkeys Bar (Authentic TNC 640 function softkeys)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SoftkeyButton("KOPIËER", colors, Modifier.weight(1f)) { onCopyLine() }
            SoftkeyButton("PLAK", colors, Modifier.weight(1f)) { onPasteLine() }
            SoftkeyButton("DEL REGEL", colors, Modifier.weight(1f)) { onDeleteLine() }
            SoftkeyButton("TOOLS", colors, Modifier.weight(1f)) { onOpenToolTable() }
            SoftkeyButton("CALC", colors, Modifier.weight(1f)) { onOpenCalculator() }
            SoftkeyButton("Q-PAR", colors, Modifier.weight(1f)) { onOpenQParams() }
            SoftkeyButton("DXF/ISO", colors, Modifier.weight(1.1f)) { onOpenImportExport() }
            SoftkeyButton(
                if (diagnosticsCount > 0) "FOUTEN ($diagnosticsCount)" else "MELDINGEN",
                colors,
                Modifier.weight(1.3f),
                isAlert = diagnosticsCount > 0
            ) { onOpenDiagnostics() }
        }
    }
}

@Composable
private fun KeypadButton(
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
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .height(30.dp)
            .then(if (borderColor != null) Modifier.border(0.8.dp, borderColor, RoundedCornerShape(4.dp)) else Modifier)
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

@Composable
private fun TncCmdButton(
    label: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(32.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SoftkeyButton(
    label: String,
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
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
            .height(28.dp)
            .border(0.8.dp, if (isAlert) CncRapidOrange else colors.softkeyBorder, RoundedCornerShape(3.dp))
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (isAlert) Color.White else colors.softkeyText,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

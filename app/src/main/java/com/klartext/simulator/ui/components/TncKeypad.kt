package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.klartext.simulator.ui.designsystem.CncSoftkey
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
                        text = "${step?.promptLabel ?: ""} ${activeDialog.currentInput}▌",
                        color = CncDroGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ENT = OK | NO ENT = Sla over",
                        color = Color(0xFFFFD54F),
                        fontSize = 10.sp
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("7", "8", "9", "+", "DEL").forEach { key ->
                    CncKeyCap(
                        label = key,
                        containerColor = if (key == "DEL") colors.deleteKeyBackground else colors.numpadKeyBackground,
                        textColor = if (key == "DEL") colors.deleteKeyText else colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 28.dp,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { if (key == "DEL") onDialogDel() else onDialogChar(key) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("4", "5", "6", "-", "END").forEach { key ->
                    CncKeyCap(
                        label = key,
                        containerColor = if (key == "END") colors.cycleKeyBackground else colors.numpadKeyBackground,
                        textColor = if (key == "END") Color.White else colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 28.dp,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { if (key == "END") onDialogEnd() else onDialogChar(key) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("1", "2", "3", "0", ".").forEach { key ->
                    CncKeyCap(
                        label = key,
                        containerColor = colors.numpadKeyBackground,
                        textColor = colors.numpadKeyText,
                        borderColor = colors.numpadKeyBorder,
                        height = 28.dp,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        onClick = { onDialogChar(key) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CncKeyCap(
                    label = "NO ENT",
                    containerColor = colors.noEntKeyBackground,
                    textColor = colors.noEntKeyText,
                    height = 32.dp,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    onClick = onDialogNoEnt
                )
                CncKeyCap(
                    label = "ENT",
                    containerColor = colors.entKeyBackground,
                    textColor = colors.entKeyText,
                    height = 32.dp,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    onClick = onDialogEnt
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("L", "CC", "C", "CR", "CT", "RND", "CHF").forEach { cmd ->
                    CncKeyCap(
                        label = cmd,
                        containerColor = colors.contourKeyBackground,
                        textColor = colors.contourKeyText,
                        height = 30.dp,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartCommand(cmd) }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("APPR", "DEP", "TOOL CALL", "CYCL DEF", "LBL SET", "CALL LBL").forEach { cmd ->
                    CncKeyCap(
                        label = cmd,
                        containerColor = colors.cycleKeyBackground,
                        textColor = colors.cycleKeyText,
                        height = 30.dp,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartCommand(cmd) }
                    )
                }
            }
        }

        // TNC Bottom Softkeys Bar (Using zero-overflow CncSoftkey)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            CncSoftkey("KOPIËER", colors, Modifier.weight(1f)) { onCopyLine() }
            CncSoftkey("PLAK", colors, Modifier.weight(1f)) { onPasteLine() }
            CncSoftkey("DEL", colors, Modifier.weight(0.9f)) { onDeleteLine() }
            CncSoftkey("TOOLS", colors, Modifier.weight(1f)) { onOpenToolTable() }
            CncSoftkey("CALC", colors, Modifier.weight(0.9f)) { onOpenCalculator() }
            CncSoftkey("Q-PAR", colors, Modifier.weight(1f)) { onOpenQParams() }
            CncSoftkey("DXF/ISO", colors, Modifier.weight(1.1f)) { onOpenImportExport() }
            CncSoftkey(
                label = if (diagnosticsCount > 0) "FOUT ($diagnosticsCount)" else "STATUS",
                colors = colors,
                modifier = Modifier.weight(1.2f),
                isAlert = diagnosticsCount > 0
            ) { onOpenDiagnostics() }
        }
    }
}

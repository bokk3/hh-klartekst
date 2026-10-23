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
import com.klartext.simulator.ui.theme.*

@Composable
fun TncKeypad(
    activeDialog: ActiveDialogSession?,
    diagnosticsCount: Int,
    onStartCommand: (String) -> Unit,
    onDialogChar: (String) -> Unit,
    onDialogEnt: () -> Unit,
    onDialogNoEnt: () -> Unit,
    onDialogDel: () -> Unit,
    onDialogEnd: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenToolTable: () -> Unit,
    onOpenImportExport: () -> Unit,
    onCopyLine: () -> Unit,
    onPasteLine: () -> Unit,
    onDeleteLine: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CncPanelBackground, RoundedCornerShape(8.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(8.dp))
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
                    KeypadButton(key, modifier = Modifier.weight(1f)) {
                        if (key == "DEL") onDialogDel() else onDialogChar(key)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("4", "5", "6", "-", "END").forEach { key ->
                    KeypadButton(key, modifier = Modifier.weight(1f)) {
                        if (key == "END") onDialogEnd() else onDialogChar(key)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("1", "2", "3", "0", ".").forEach { key ->
                    KeypadButton(key, modifier = Modifier.weight(1f)) { onDialogChar(key) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // NO ENT button (Crucial for TNC 640 modal RL/RR preservation!)
                Button(
                    onClick = onDialogNoEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("NO ENT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // ENT button
                Button(
                    onClick = onDialogEnt,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ENT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // TNC Programming Keys Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("L", "CC", "C", "CR", "CT", "RND", "CHF").forEach { cmd ->
                    TncCmdButton(cmd, modifier = Modifier.weight(1f)) { onStartCommand(cmd) }
                }
            }

            // TNC Programming Keys Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("APPR", "DEP", "TOOL CALL", "CYCL DEF", "LBL SET", "CALL LBL").forEach { cmd ->
                    TncCmdButton(cmd, modifier = Modifier.weight(1f)) { onStartCommand(cmd) }
                }
            }
        }

        // TNC Bottom Softkeys Bar (Authentic TNC 640 function softkeys)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SoftkeyButton("KOPIËER", Modifier.weight(1f)) { onCopyLine() }
            SoftkeyButton("PLAK", Modifier.weight(1f)) { onPasteLine() }
            SoftkeyButton("DEL REGEL", Modifier.weight(1f)) { onDeleteLine() }
            SoftkeyButton("TOOLS", Modifier.weight(1f)) { onOpenToolTable() }
            SoftkeyButton("CALC", Modifier.weight(1f)) { onOpenCalculator() }
            SoftkeyButton("DXF/ISO", Modifier.weight(1.1f)) { onOpenImportExport() }
            SoftkeyButton(
                if (diagnosticsCount > 0) "FOUTEN ($diagnosticsCount)" else "MELDINGEN",
                Modifier.weight(1.3f),
                isAlert = diagnosticsCount > 0
            ) { onOpenDiagnostics() }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = CncSurfaceDark),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(30.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = CncTextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TncCmdButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(32.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = CncAccentOrange,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SoftkeyButton(
    label: String,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isAlert) CncRapidOrange else Color(0xFF1E2830)
        ),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier.height(28.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (isAlert) Color.White else CncTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.klartext.simulator.model.DiagnosticSeverity
import com.klartext.simulator.model.TncDiagnostic
import com.klartext.simulator.ui.theme.*

@Composable
fun DiagnosticsDialog(
    diagnostics: List<TncDiagnostic>,
    onSelectLine: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = CncPanelBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TNC 640 MELDINGEN (${diagnostics.size})",
                        color = CncAccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Sluiten", tint = CncTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (diagnostics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Geen fouten gevonden! Programma voldoet aan TNC 640 syntax.",
                            color = CncDroGreen,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(diagnostics) { diag ->
                            val (badgeColor, badgeIcon) = when (diag.severity) {
                                DiagnosticSeverity.ERROR -> Pair(CncRapidOrange, Icons.Default.Error)
                                DiagnosticSeverity.STUDENT_HINT -> Pair(CncAccentOrange, Icons.Default.Info)
                                DiagnosticSeverity.WARNING -> Pair(Color(0xFFFFD600), Icons.Default.Warning)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CncSurfaceDark, RoundedCornerShape(8.dp))
                                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectLine(diag.lineNumber)
                                        onDismiss()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = diag.title,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Regel ${diag.lineNumber}",
                                            color = CncTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = diag.message,
                                        color = CncTextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

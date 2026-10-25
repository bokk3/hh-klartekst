package com.klartext.simulator.ui.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.ConsoleTheme
import com.klartext.simulator.model.DiagnosticSeverity
import com.klartext.simulator.model.SimulationUiState
import com.klartext.simulator.sample.SamplePrograms
import com.klartext.simulator.ui.theme.*

/**
 * Tab 3: CNC Workshop Tools, Tables, Diagnostics & Curriculum Hub.
 * Eliminates the need for 8 cramped softkeys on mobile screens by providing
 * spacious, touch-friendly cards for all TNC 640 utilities and sample programs.
 */
@Composable
fun MobileToolsWorkspace(
    uiState: SimulationUiState,
    onOpenToolTable: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenQParams: () -> Unit,
    onOpenImportExport: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onToggleConsoleTheme: () -> Unit,
    onToggleTwinScreen: () -> Unit,
    onLoadProgram: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val errorCount = uiState.diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warnCount = uiState.diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section 1: TNC 640 Werkplaats Utilities
        Text(
            text = "TNC 640 WERKPLAATS & TABELLEN",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CncAccentOrange
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkshopToolCard(
                title = "GEREEDSCHAPSTABEL",
                subtitle = "Actief: ${uiState.machineState.toolName} (T${uiState.machineState.activeTool})",
                badge = "TOOL.T",
                icon = Icons.Default.Build,
                accentColor = CncAccentOrange,
                modifier = Modifier.weight(1f),
                onClick = onOpenToolTable
            )
            WorkshopToolCard(
                title = "VERSPANINGSCALCULATOR",
                subtitle = "Toerental S, Voeding F, Tap M3-M16",
                badge = "CALC",
                icon = Icons.Default.Calculate,
                accentColor = CncFeedCyan,
                modifier = Modifier.weight(1f),
                onClick = onOpenCalculator
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkshopToolCard(
                title = "Q-PARAMETERS",
                subtitle = "${uiState.qParameters.size} actieve Q-variabelen",
                badge = "Q0..Q199",
                icon = Icons.Default.Functions,
                accentColor = CncDroGreen,
                modifier = Modifier.weight(1f),
                onClick = onOpenQParams
            )
            WorkshopToolCard(
                title = "DXF & ISO CONVERTER",
                subtitle = "AutoCAD DXF & G-Code import/export",
                badge = "CAD / ISO",
                icon = Icons.Default.SwapHoriz,
                accentColor = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f),
                onClick = onOpenImportExport
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val diagColor = when {
                errorCount > 0 -> Color(0xFFEF5350)
                warnCount > 0 -> Color(0xFFFFA726)
                else -> CncDroGreen
            }
            WorkshopToolCard(
                title = "DIAGNOSTIEK & STATUS",
                subtitle = if (uiState.diagnostics.isEmpty()) "Geen botsingen of fouten" else "$errorCount fouten, $warnCount waarschuwingen",
                badge = if (uiState.diagnostics.isEmpty()) "OK" else "${uiState.diagnostics.size} MELDINGEN",
                icon = Icons.Default.NotificationsActive,
                accentColor = diagColor,
                modifier = Modifier.weight(1f),
                onClick = onOpenDiagnostics
            )
            WorkshopToolCard(
                title = "CONSOLE THEMA",
                subtitle = if (uiState.consoleTheme == ConsoleTheme.CLASSIC_TNC640) "Klassiek HEIDENHAIN TE 640" else "Hypermodern Dark Cockpit",
                badge = "WISSEL",
                icon = Icons.Default.Palette,
                accentColor = Color(0xFFFFCA28),
                modifier = Modifier.weight(1f),
                onClick = onToggleConsoleTheme
            )
        }

        // Section 2: Officiële HEIDENHAIN Praktijkoefeningen & Voorbeeldprogramma's
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OPLEIDINGSCURRICULUM & PROGRAMMA'S (.H)",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CncAccentOrange
            )
            Text(
                text = "Tik om te laden",
                fontSize = 9.5.sp,
                color = CncTextSecondary
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            SamplePrograms.all.forEach { (name, code) ->
                val isSelected = uiState.programName == name
                val isCurriculum = name.startsWith("OEFENING")
                val isErrorDemo = name.startsWith("FOUTDEMO")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF0D2847) else CncPanelBackground)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> CncAccentOrange
                                isCurriculum -> CncFeedCyan.copy(alpha = 0.5f)
                                isErrorDemo -> CncRapidOrange.copy(alpha = 0.5f)
                                else -> CncBorder
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onLoadProgram(name, code) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isCurriculum -> Icons.Default.School
                                isErrorDemo -> Icons.Default.Warning
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = when {
                                isSelected -> CncAccentOrange
                                isCurriculum -> CncFeedCyan
                                isErrorDemo -> CncRapidOrange
                                else -> CncTextSecondary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = name,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.White else CncTextPrimary
                            )
                            Text(
                                text = "${code.lines().size} regels • ${if (isCurriculum) "Officiële HEIDENHAIN Praktijkopdracht" else "CNC Voorbeeldprogramma"}",
                                fontSize = 9.5.sp,
                                color = CncTextSecondary
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .background(CncAccentOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIEF",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CncDarkBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkshopToolCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CncPanelBackground)
            .border(1.dp, CncBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(4.dp))
                    .border(0.8.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CncTextPrimary
        )
        Text(
            text = subtitle,
            fontSize = 9.5.sp,
            color = CncTextSecondary,
            maxLines = 2
        )
    }
}

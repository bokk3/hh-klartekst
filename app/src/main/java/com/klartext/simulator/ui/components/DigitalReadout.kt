package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.MachineState
import com.klartext.simulator.model.TncDiagnostic
import com.klartext.simulator.ui.theme.*
import java.util.Locale

@Composable
fun DigitalReadout(
    state: MachineState,
    diagnostics: List<TncDiagnostic>,
    onOpenDiagnostics: () -> Unit,
    feedOverride: Int = 100,
    spindleOverride: Int = 100,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CncPanelBackground, RoundedCornerShape(8.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // TNC Diagnostic Warning Banner if errors / student hints are active
        if (diagnostics.isNotEmpty()) {
            val hasErrors = diagnostics.any { it.severity == com.klartext.simulator.model.DiagnosticSeverity.ERROR }
            val bannerColor = if (hasErrors) CncRapidOrange else CncAccentOrange

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerColor.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                    .border(1.dp, bannerColor, RoundedCornerShape(4.dp))
                    .clickable { onOpenDiagnostics() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = bannerColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = diagnostics.first().message,
                        color = bannerColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${diagnostics.size} melding(en)",
                    color = bannerColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // CNC Axis Coordinates (DRO X, Y, Z)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CoordinateItem(label = "X", value = state.x)
            CoordinateItem(label = "Y", value = state.y)
            CoordinateItem(label = "Z", value = state.z)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CNC Status Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val feedSuffix = if (feedOverride == 0) " (HOLD)" else if (feedOverride != 100) " ($feedOverride%)" else ""
            val spindleSuffix = if (spindleOverride != 100) " ($spindleOverride%)" else ""

            InfoTag(
                label = "VOEDING",
                value = if (state.isRapid) "FMAX$feedSuffix" else "${String.format(Locale.US, "%.0f", state.feedRate)}$feedSuffix",
                color = if (feedOverride == 0) CncRapidOrange else if (state.isRapid) CncRapidOrange else CncFeedCyan
            )

            InfoTag(
                label = "SPIL",
                value = if (state.spindleOn) "${String.format(Locale.US, "%.0f RPM", state.spindleSpeed)}$spindleSuffix" else "STOP",
                color = if (state.spindleOn) CncDroGreen else CncTextSecondary
            )

            InfoTag(
                label = "ACTIEF TOOL",
                value = "${state.toolName} (R${String.format(Locale.US, "%.1f", state.effectiveRadius)})",
                color = CncAccentOrange
            )

            InfoTag(
                label = "CORRECTIE",
                value = "${state.radiusComp.name} (DR${String.format(Locale.US, "%+.1f", state.dr)})",
                color = if (state.radiusComp != com.klartext.simulator.model.RadiusComp.R0) CncDroGreen else CncTextPrimary
            )
        }
    }
}

@Composable
private fun CoordinateItem(
    label: String,
    value: Float
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            color = CncAccentOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = String.format(Locale.US, "%+8.3f", value),
            color = CncDroGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun InfoTag(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 8.sp,
            color = CncTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

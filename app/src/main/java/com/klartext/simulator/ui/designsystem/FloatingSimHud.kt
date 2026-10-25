package com.klartext.simulator.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.*
import com.klartext.simulator.ui.theme.*
import java.util.Locale

/**
 * Persistent high-visibility collision alert banner.
 * Displayed when tool shank or rapid move collides with workpiece material.
 */
@Composable
fun CollisionAlertBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFB71C1C))
            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = "Collision Warning",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = message,
            color = Color.White,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact, high-contrast Digital Readout (DRO) bar designed for mobile workspaces
 * to maximize 3D/2D canvas area while keeping X, Y, Z, F, S, Tool, and Comp legible.
 */
@Composable
fun CompactDroBar(
    state: MachineState,
    diagnostics: List<TncDiagnostic>,
    feedOverride: Int = 100,
    spindleOverride: Int = 100,
    onOpenDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val errorCount = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warnCount = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CncPanelBackground.copy(alpha = 0.96f), RoundedCornerShape(6.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Row 1: X, Y, Z Coordinates + Diagnostics Alert Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DroAxisItem("X", state.x, CncAccentOrange)
                DroAxisItem("Y", state.y, CncAccentOrange)
                DroAxisItem("Z", state.z, CncDroGreen)
            }

            if (errorCount > 0 || warnCount > 0) {
                val badgeColor = if (errorCount > 0) Color(0xFFD32F2F) else Color(0xFFF57C00)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .clickable { onOpenDiagnostics() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Diagnostiek",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (errorCount > 0) "$errorCount FOUT" else "$warnCount WAARSCH",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Row 2: Feed, Spindle, Active Tool, Compensation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val feedText = if (state.isRapid) "FMAX" else "F${state.feedRate.toInt()} (${feedOverride}%)"
            val feedColor = if (state.isRapid) CncRapidOrange else CncFeedCyan
            Text(
                text = feedText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = feedColor
            )

            Text(
                text = if (state.spindleSpeed > 0f) "S${state.spindleSpeed.toInt()} (${spindleOverride}%)" else "SPIL STOP",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (state.spindleSpeed > 0f) CncDroGreen else CncTextSecondary
            )

            Text(
                text = "${state.toolName} (R${String.format(Locale.US, "%.1f", state.effectiveRadius)})",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CncAccentOrange
            )

            val compColor = when (state.radiusComp) {
                RadiusComp.RL -> Color(0xFF00E676)
                RadiusComp.RR -> Color(0xFFFF5252)
                RadiusComp.R0 -> CncTextSecondary
            }
            Text(
                text = state.radiusComp.name,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = compColor
            )
        }
    }
}

@Composable
private fun DroAxisItem(axis: String, value: Float, axisColor: Color) {
    val sign = if (value >= 0f) "+" else ""
    val formatted = String.format(Locale.US, "%s%.3f", sign, value)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$axis:",
            color = axisColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = formatted,
            color = CncDroGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Full-width adaptive View Mode selector strip (XY, XZ, YZ, MAT, ISO, 3D, SNIJD).
 * Never truncates or clips buttons off-screen on narrow phones.
 */
@Composable
fun ViewModeChipStrip(
    activeMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        ViewMode.TOP_XY to "XY (2D)",
        ViewMode.FRONT_XZ to "XZ",
        ViewMode.SIDE_YZ to "YZ",
        ViewMode.MATERIAL_25D to "MAT 2.5D",
        ViewMode.ISOMETRIC to "ISO",
        ViewMode.ORBIT_3D to "3D ORBIT",
        ViewMode.SECTION_CUT to "SNEDE"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(CncPanelBackground, RoundedCornerShape(6.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = activeMode == mode
            CncKeyCap(
                label = label,
                containerColor = if (isSelected) CncAccentOrange else CncSurfaceDark,
                textColor = if (isSelected) CncDarkBackground else CncTextPrimary,
                borderColor = if (isSelected) CncAccentOrange else CncBorder,
                height = 24.dp,
                fontSize = 9.5.sp,
                cornerRadius = 4.dp,
                modifier = Modifier.widthIn(min = 42.dp),
                onClick = { onViewModeChange(mode) }
            )
        }
    }
}

/**
 * Live Active Code Line banner displayed on the Simulation tab.
 * Shows the currently executing Klartext block and lets the user jump directly
 * to the Programming (Code + Keypad) tab with a single tap.
 */
@Composable
fun LiveActiveCodeBanner(
    blocks: List<KlartextBlock>,
    activeLineNumber: Int,
    onJumpToProgramming: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentBlock = blocks.firstOrNull { it.lineNumber == activeLineNumber } ?: blocks.firstOrNull()
    val rawLineText = currentBlock?.rawText ?: "0 BEGIN PGM MM"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0D2847))
            .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(6.dp))
            .clickable { onJumpToProgramming() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF1976D2), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "R$activeLineNumber",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Text(
                text = rawLineText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
            modifier = Modifier
                .background(CncSurfaceDark, RoundedCornerShape(4.dp))
                .border(0.8.dp, CncAccentOrange, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                Icons.Default.Code,
                contentDescription = "Open Code Editor",
                tint = CncAccentOrange,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "CODE",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CncAccentOrange
            )
        }
    }
}

package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.ViewMode
import com.klartext.simulator.ui.theme.*

@Composable
fun ControlPanel(
    isPlaying: Boolean,
    currentStep: Int,
    totalSteps: Int,
    playbackSpeed: Float,
    viewMode: ViewMode,
    showContour: Boolean,
    onPlayPause: () -> Unit,
    onStepForward: () -> Unit,
    onStepBack: () -> Unit,
    onReset: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onToggleContour: () -> Unit,
    isSingleBlockActive: Boolean = false,
    onToggleSingleBlock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CncPanelBackground, RoundedCornerShape(8.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        // Scrub Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentStep.coerceAtLeast(0)} / $totalSteps",
                fontSize = 10.sp,
                color = CncTextSecondary,
                modifier = Modifier.width(55.dp)
            )
            Slider(
                value = currentStep.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..totalSteps.coerceAtLeast(1).toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = CncAccentOrange,
                    activeTrackColor = CncAccentOrange,
                    inactiveTrackColor = CncBorder
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playback buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onReset, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Replay, contentDescription = "Reset", tint = CncTextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onStepBack, enabled = currentStep > 0, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Stap Terug", tint = CncTextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onPlayPause,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CncAccentOrange),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pauze" else "Start",
                        tint = CncDarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onStepForward, enabled = currentStep < totalSteps, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Stap Vooruit", tint = CncTextPrimary, modifier = Modifier.size(18.dp))
                }
                Button(
                    onClick = onToggleSingleBlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSingleBlockActive) Color(0xFFE65100) else CncSurfaceDark
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "S-BLK",
                        fontSize = 9.sp,
                        color = if (isSingleBlockActive) Color.White else CncTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Speed multiplier selector
            var speedExpanded by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { speedExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CncSurfaceDark),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = "${playbackSpeed}x", fontSize = 11.sp, color = CncTextPrimary)
                }
                DropdownMenu(
                    expanded = speedExpanded,
                    onDismissRequest = { speedExpanded = false },
                    modifier = Modifier.background(CncSurfaceDark)
                ) {
                    listOf(0.25f, 0.5f, 1.0f, 2.0f, 5.0f, 10.0f).forEach { spd ->
                        DropdownMenuItem(
                            text = { Text("${spd}x", color = CncTextPrimary) },
                            onClick = {
                                onSpeedChange(spd)
                                speedExpanded = false
                            }
                        )
                    }
                }
            }

            // View mode toggles: XY, XZ, YZ, MAT (2.5D Material), ISO, 3D (Orbit), SNIJD (Section)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                    ViewMode.TOP_XY to "XY",
                    ViewMode.FRONT_XZ to "XZ",
                    ViewMode.SIDE_YZ to "YZ",
                    ViewMode.MATERIAL_25D to "MAT",
                    ViewMode.ISOMETRIC to "ISO",
                    ViewMode.ORBIT_3D to "3D",
                    ViewMode.SECTION_CUT to "SNIJD"
                ).forEach { (mode, label) ->
                    val isSelected = viewMode == mode
                    Button(
                        onClick = { onViewModeChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) CncAccentOrange else CncSurfaceDark
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (isSelected) CncDarkBackground else CncTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.ViewMode
import com.klartext.simulator.ui.designsystem.CncKeyCap
import com.klartext.simulator.ui.designsystem.CncTimelineScrubber
import com.klartext.simulator.ui.designsystem.ViewModeChipStrip
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
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Custom Timeline Scrubber (no vertical bar glitch)
        CncTimelineScrubber(
            currentStep = currentStep,
            totalSteps = totalSteps,
            onSeek = onSeek
        )

        // Adaptive View Mode Strip
        ViewModeChipStrip(
            activeMode = viewMode,
            onViewModeChange = onViewModeChange
        )

        // Playback Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CncKeyCap(
                label = "",
                icon = Icons.Default.Replay,
                containerColor = CncSurfaceDark,
                textColor = CncTextPrimary,
                borderColor = CncBorder,
                height = 30.dp,
                modifier = Modifier.weight(0.8f),
                onClick = onReset
            )
            CncKeyCap(
                label = "",
                icon = Icons.Default.SkipPrevious,
                containerColor = CncSurfaceDark,
                textColor = CncTextPrimary,
                borderColor = CncBorder,
                height = 30.dp,
                enabled = currentStep > 0,
                modifier = Modifier.weight(0.8f),
                onClick = onStepBack
            )
            CncKeyCap(
                label = if (isPlaying) "PAUZE" else "START",
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                containerColor = CncAccentOrange,
                textColor = CncDarkBackground,
                height = 30.dp,
                fontSize = 10.sp,
                modifier = Modifier.weight(1.4f),
                onClick = onPlayPause
            )
            CncKeyCap(
                label = "",
                icon = Icons.Default.SkipNext,
                containerColor = CncSurfaceDark,
                textColor = CncTextPrimary,
                borderColor = CncBorder,
                height = 30.dp,
                enabled = currentStep < totalSteps,
                modifier = Modifier.weight(0.8f),
                onClick = onStepForward
            )
            CncKeyCap(
                label = "S-BLK",
                containerColor = if (isSingleBlockActive) Color(0xFFE65100) else CncSurfaceDark,
                textColor = if (isSingleBlockActive) Color.White else CncTextSecondary,
                borderColor = CncBorder,
                height = 30.dp,
                fontSize = 9.5.sp,
                modifier = Modifier.weight(1.0f),
                onClick = onToggleSingleBlock
            )

            var speedExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(0.9f)) {
                CncKeyCap(
                    label = "${playbackSpeed}x",
                    containerColor = CncSurfaceDark,
                    textColor = CncAccentOrange,
                    borderColor = CncBorder,
                    height = 30.dp,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { speedExpanded = true }
                )
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
        }
    }
}

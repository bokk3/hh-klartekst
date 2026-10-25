package com.klartext.simulator.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.ui.theme.*
import kotlin.math.roundToInt

/**
 * Custom CNC Timeline Scrubber that replaces Material 3's tall vertical-bar Slider
 * with a sleek, industrial horizontal progress track and precision circular playhead.
 */
@Composable
fun CncTimelineScrubber(
    currentStep: Int,
    totalSteps: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = CncAccentOrange
) {
    val safeTotal = totalSteps.coerceAtLeast(1)
    val clampedCurrent = currentStep.coerceIn(0, safeTotal)
    val fraction = if (totalSteps > 0) clampedCurrent.toFloat() / safeTotal.toFloat() else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Step counter pill
        Box(
            modifier = Modifier
                .background(CncSurfaceDark, RoundedCornerShape(4.dp))
                .border(0.8.dp, CncBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$clampedCurrent / $totalSteps",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CncDroGreen
            )
        }

        // Interactive Custom Scrubber Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(safeTotal) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val pad = 8.dp.toPx()
                        val usableW = (w - 2 * pad).coerceAtLeast(1f)
                        val rel = ((offset.x - pad) / usableW).coerceIn(0f, 1f)
                        onSeek((rel * totalSteps).roundToInt())
                    }
                }
                .pointerInput(safeTotal) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val pad = 8.dp.toPx()
                        val usableW = (w - 2 * pad).coerceAtLeast(1f)
                        val rel = ((change.position.x - pad) / usableW).coerceIn(0f, 1f)
                        onSeek((rel * totalSteps).roundToInt())
                    }
                }
        ) {
            val pad = 8.dp.toPx()
            val trackHeight = 6.dp.toPx()
            val usableWidth = (size.width - 2 * pad).coerceAtLeast(1f)
            val centerY = size.height / 2f
            val trackTop = centerY - trackHeight / 2f

            // 1. Inactive Background Track
            drawRoundRect(
                color = CncSurfaceDark,
                topLeft = Offset(pad, trackTop),
                size = Size(usableWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            // 2. Active Progress Track
            val activeWidth = usableWidth * fraction
            if (activeWidth > 0f) {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(pad, trackTop),
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )
            }

            // 3. Playhead Thumb (Outer Halo + Inner Core)
            val thumbX = pad + activeWidth
            drawCircle(
                color = accentColor.copy(alpha = 0.28f),
                radius = 9.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = accentColor,
                radius = 5.5.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = CncDarkBackground,
                radius = 2.2.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
        }
    }
}

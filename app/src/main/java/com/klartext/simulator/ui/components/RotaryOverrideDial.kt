package com.klartext.simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.ui.theme.*
import kotlin.math.*

/**
 * Authentic HEIDENHAIN TE 640 Rotary Potentiometer (0% - 150%)
 * for Feed Rate (Voeding) and Spindle Speed (Spil) override.
 */
@Composable
fun RotaryOverrideDial(
    title: String,
    valuePercent: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color = CncAccentOrange,
    colors: ConsoleColors? = null,
    modifier: Modifier = Modifier
) {
    // 0% -> -135 deg, 100% -> +45 deg, 150% -> +135 deg
    // Total sweep = 270 deg for 150% (1.8 deg per 1%)
    val startAngleDeg = -135f
    val sweepTotalDeg = 270f
    val currentAngleDeg = startAngleDeg + (valuePercent.coerceIn(0, 150) / 150f) * sweepTotalDeg

    val cardBg = colors?.dialCardBackground ?: CncSurfaceDark
    val cardBorderColor = colors?.dialCardBorder ?: CncBorder
    val titleCol = colors?.dialTitleColor ?: CncTextSecondary
    val trackCol = colors?.dialTrackColor ?: Color(0xFF2C3942)
    val knobGradColors = colors?.dialKnobColors ?: listOf(Color(0xFF37474F), Color(0xFF1E2830), Color(0xFF10171D))
    val needleCol = colors?.dialPointerColor ?: accentColor
    val tickCol = colors?.dialTickColor ?: Color(0xFF78909C)

    Column(
        modifier = modifier
            .background(cardBg, RoundedCornerShape(8.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Title & Value Readout Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = titleCol
            )
            Text(
                text = "$valuePercent%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (valuePercent == 100) CncDroGreen else if (valuePercent > 100) CncAccentOrange else Color(0xFFFFD54F)
            )
        }

        // Circular Rotary Knob with Touch-Drag
        Box(
            modifier = Modifier
                .size(76.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touch = change.position
                        val dx = touch.x - center.x
                        val dy = touch.y - center.y
                        var angleDeg = (atan2(dy, dx) * 180f / PI.toFloat()) - 90f // 0 deg is at top (12 o'clock)
                        while (angleDeg < -180f) angleDeg += 360f
                        while (angleDeg > 180f) angleDeg -= 360f

                        // Map angle from -135..+135 to 0..150%
                        val clampedAngle = angleDeg.coerceIn(-135f, 135f)
                        val pct = (((clampedAngle + 135f) / sweepTotalDeg) * 150f).roundToInt().coerceIn(0, 150)
                        onValueChange(pct)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 6f
                val innerRadius = radius * 0.72f

                // Outer bezel scale arc
                drawArc(
                    color = trackCol,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active arc fill up to current angle
                val activeSweep = (valuePercent.coerceIn(0, 150) / 150f) * sweepTotalDeg
                drawArc(
                    brush = Brush.sweepGradient(
                        0.375f to Color(0xFF388E3C),
                        0.75f to CncAccentOrange,
                        1.0f to CncRapidOrange
                    ),
                    startAngle = 135f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Tick marks (0, 20, 40, 60, 80, 100, 120, 140, 150%)
                val tickPcts = listOf(0, 20, 40, 60, 80, 100, 120, 140, 150)
                tickPcts.forEach { tickPct ->
                    val tAngle = (startAngleDeg + (tickPct / 150f) * sweepTotalDeg) * (PI.toFloat() / 180f)
                    val rad = tAngle + (PI.toFloat() / 2f)
                    val is100 = tickPct == 100
                    val tickLen = if (is100) 7.dp.toPx() else 4.dp.toPx()
                    val p1 = Offset(center.x + (radius + 2f) * cos(rad), center.y + (radius + 2f) * sin(rad))
                    val p2 = Offset(center.x + (radius + 2f + tickLen) * cos(rad), center.y + (radius + 2f + tickLen) * sin(rad))
                    drawLine(
                        color = if (is100) CncDroGreen else tickCol,
                        start = p1,
                        end = p2,
                        strokeWidth = if (is100) 2.5.dp.toPx() else 1.2.dp.toPx()
                    )
                }

                // Inner Knob Body
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = knobGradColors,
                        center = center,
                        radius = innerRadius
                    ),
                    radius = innerRadius,
                    center = center
                )

                // Bezel ring around knob
                drawCircle(
                    color = cardBorderColor,
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Pointer notch / needle on the knob
                val pointerRad = (currentAngleDeg + 90f) * (PI.toFloat() / 180f)
                val pStart = Offset(center.x + innerRadius * 0.25f * cos(pointerRad), center.y + innerRadius * 0.25f * sin(pointerRad))
                val pEnd = Offset(center.x + innerRadius * 0.95f * cos(pointerRad), center.y + innerRadius * 0.95f * sin(pointerRad))
                drawLine(
                    color = needleCol,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center screw cap
                drawCircle(
                    color = knobGradColors.last(),
                    radius = innerRadius * 0.25f,
                    center = center
                )
                drawCircle(
                    color = cardBorderColor,
                    radius = innerRadius * 0.25f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Quick Preset Buttons (-10%, 100% Reset, +10%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallPillButton("-10", colors = colors) {
                onValueChange((valuePercent - 10).coerceIn(0, 150))
            }
            SmallPillButton(
                "100%",
                isHighlight = valuePercent == 100,
                colors = colors
            ) {
                onValueChange(100)
            }
            SmallPillButton("+10", colors = colors) {
                onValueChange((valuePercent + 10).coerceIn(0, 150))
            }
        }
    }
}

@Composable
private fun SmallPillButton(
    text: String,
    isHighlight: Boolean = false,
    colors: ConsoleColors? = null,
    onClick: () -> Unit
) {
    val pillBg = if (isHighlight) Color(0xFF1B5E20) else (colors?.dialPillBackground ?: Color(0xFF263238))
    val pillTxt = if (isHighlight) CncDroGreen else (colors?.dialPillText ?: CncTextPrimary)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = pillBg
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        shape = RoundedCornerShape(3.dp),
        modifier = Modifier
            .height(20.dp)
            .defaultMinSize(minWidth = 24.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = pillTxt
        )
    }
}

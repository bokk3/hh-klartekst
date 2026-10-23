package com.klartext.simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.klartext.simulator.model.*
import com.klartext.simulator.ui.theme.*
import kotlin.math.*

@Composable
fun SimulationCanvas(
    state: SimulationUiState,
    currentStepIndex: Int,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(2.2f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(CncDarkBackground, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.4f, 20f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + offsetX, size.height / 2f + offsetY)

            // 1. Draw Grid
            if (state.showGrid) {
                drawGrid(center, scale)
            }

            // 2. 2.5D Material Removal View
            if (state.viewMode == ViewMode.MATERIAL_25D && state.heightfield != null) {
                drawMaterialRemoval25D(state.heightfield, center, scale)
            } else if (state.viewMode == ViewMode.ISOMETRIC && state.heightfield != null) {
                drawIsometricHeightfield(state.heightfield, center, scale)
            } else {
                // Stock outline for technical views
                drawStock(state.stock, state.viewMode, center, scale)
            }

            // 3. Workpiece Datum Symbol at X0 Y0 (Authentic CNC Datum circle)
            drawWorkpieceDatum(state.viewMode, center, scale)

            // 4. Dual Path Rendering: Programmed Contour (Yellow/White)
            if (state.showProgrammedContour && state.viewMode != ViewMode.MATERIAL_25D) {
                drawProgrammedContour(state.segments, currentStepIndex, state.viewMode, center, scale)
            }

            // 5. Equidistant Cutter Center Path
            val executedCount = currentStepIndex.coerceIn(0, state.segments.size)
            val segmentsToDraw = state.segments.take(executedCount)
            for (seg in segmentsToDraw) {
                drawSegment(seg, state.viewMode, center, scale, isExecuted = true)
            }

            // Draw future segments translucent
            val futureSegments = state.segments.drop(executedCount)
            for (seg in futureSegments) {
                drawSegment(seg, state.viewMode, center, scale, isExecuted = false)
            }

            // 6. Active Tool Footprint (D = 2 * Reff) and Yellow Crosshair
            val currentPos = Point3D(state.machineState.x, state.machineState.y, state.machineState.z)
            drawTool(
                pos = currentPos,
                radius = state.machineState.effectiveRadius,
                viewMode = state.viewMode,
                center = center,
                scale = scale,
                showRadius = state.showToolRadius
            )
        }
    }
}

private fun DrawScope.drawGrid(center: Offset, scale: Float) {
    val step = 10f * scale
    // Major axes
    drawLine(CncBorder, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1.5f)
    drawLine(CncBorder, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.5f)

    if (step >= 8f) {
        var x = center.x % step
        while (x < size.width) {
            drawLine(CncGridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = center.y % step
        while (y < size.height) {
            drawLine(CncGridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}

private fun DrawScope.drawWorkpieceDatum(mode: ViewMode, center: Offset, scale: Float) {
    val datumPt = projectPoint(Point3D(0f, 0f, 0f), mode, center, scale)
    val r = 7f

    drawCircle(color = CncAccentOrange, radius = r, center = datumPt, style = Stroke(width = 1.5f))
    // Fill opposite quadrants
    drawArc(
        color = CncAccentOrange,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = Offset(datumPt.x - r, datumPt.y - r),
        size = Size(2 * r, 2 * r)
    )
    drawArc(
        color = CncAccentOrange,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = Offset(datumPt.x - r, datumPt.y - r),
        size = Size(2 * r, 2 * r)
    )
}

private fun projectPoint(p: Point3D, mode: ViewMode, center: Offset, scale: Float): Offset {
    return when (mode) {
        ViewMode.TOP_XY, ViewMode.MATERIAL_25D -> Offset(center.x + p.x * scale, center.y - p.y * scale)
        ViewMode.FRONT_XZ -> Offset(center.x + p.x * scale, center.y - p.z * scale)
        ViewMode.SIDE_YZ -> Offset(center.x + p.y * scale, center.y - p.z * scale)
        ViewMode.ISOMETRIC -> {
            val isoX = (p.x - p.y) * 0.866025f // cos(30°)
            val isoY = (p.x + p.y) * 0.5f - p.z // sin(30°) - z
            Offset(center.x + isoX * scale, center.y + isoY * scale)
        }
    }
}

private fun DrawScope.drawStock(stock: StockBox, mode: ViewMode, center: Offset, scale: Float) {
    val pMin = projectPoint(stock.min, mode, center, scale)
    val pMax = projectPoint(stock.max, mode, center, scale)

    val left = min(pMin.x, pMax.x)
    val top = min(pMin.y, pMax.y)
    val w = abs(pMax.x - pMin.x)
    val h = abs(pMax.y - pMin.y)

    drawRect(color = CncStockGrey, topLeft = Offset(left, top), size = Size(w, h))
    drawRect(color = CncBorder, topLeft = Offset(left, top), size = Size(w, h), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawProgrammedContour(
    segments: List<ToolpathSegment>,
    currentStepIndex: Int,
    mode: ViewMode,
    center: Offset,
    scale: Float
) {
    val yellowContour = Color(0xFFFFEB3B).copy(alpha = 0.6f)
    val stroke = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))

    for (seg in segments) {
        if (seg.moveType == MoveType.RAPID) continue
        val pStart = projectPoint(seg.programmedStart, mode, center, scale)
        val pEnd = projectPoint(seg.programmedEnd, mode, center, scale)
        if (pStart.x != pEnd.x || pStart.y != pEnd.y) {
            drawLine(
                color = yellowContour,
                start = pStart,
                end = pEnd,
                strokeWidth = 1.2f,
                pathEffect = stroke.pathEffect
            )
        }
    }
}

private fun DrawScope.drawSegment(
    seg: ToolpathSegment,
    mode: ViewMode,
    center: Offset,
    scale: Float,
    isExecuted: Boolean
) {
    val baseColor = when (seg.moveType) {
        MoveType.RAPID -> CncRapidOrange
        MoveType.FEED -> CncFeedCyan
        MoveType.PLUNGE -> CncPlungePurple
    }
    val color = if (isExecuted) baseColor else baseColor.copy(alpha = 0.25f)
    val strokeWidth = if (isExecuted) 2.5f else 1.2f
    val pathEffect = if (seg.moveType == MoveType.RAPID) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null

    when (seg) {
        is ToolpathSegment.Linear -> {
            val pStart = projectPoint(seg.start, mode, center, scale)
            val pEnd = projectPoint(seg.end, mode, center, scale)
            drawLine(
                color = color,
                start = pStart,
                end = pEnd,
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }

        is ToolpathSegment.Arc -> {
            val steps = 20
            val startAngle = atan2(seg.start.y - seg.center.y, seg.start.x - seg.center.x)
            var endAngle = atan2(seg.end.y - seg.center.y, seg.end.x - seg.center.x)

            if (seg.direction == Direction.CW && endAngle >= startAngle - 1e-4f) {
                endAngle -= (2.0 * Math.PI).toFloat()
            } else if (seg.direction == Direction.CCW && endAngle <= startAngle + 1e-4f) {
                endAngle += (2.0 * Math.PI).toFloat()
            }

            val path = Path()
            val p0 = projectPoint(seg.start, mode, center, scale)
            path.moveTo(p0.x, p0.y)

            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val angle = startAngle + t * (endAngle - startAngle)
                val ax = seg.center.x + seg.radius * cos(angle)
                val ay = seg.center.y + seg.radius * sin(angle)
                val az = seg.start.z + t * (seg.end.z - seg.start.z)
                val pt = projectPoint(Point3D(ax, ay, az), mode, center, scale)
                path.lineTo(pt.x, pt.y)
            }

            drawPath(path = path, color = color, style = Stroke(width = strokeWidth, pathEffect = pathEffect))
        }
    }
}

private fun DrawScope.drawMaterialRemoval25D(
    grid: ZMapHeightfield,
    center: Offset,
    scale: Float
) {
    val cellW = (grid.maxX - grid.minX) / grid.cols * scale
    val cellH = (grid.maxY - grid.minY) / grid.rows * scale

    for (row in 0 until grid.rows) {
        val yVal = grid.minY + row * ((grid.maxY - grid.minY) / grid.rows)
        for (col in 0 until grid.cols) {
            val xVal = grid.minX + col * ((grid.maxX - grid.minX) / grid.cols)
            val z = grid.getZ(col, row)

            // Authentic TNC 640 depth coloring
            val color = when {
                z >= grid.maxZ - 0.05f -> Color(0xFF455A64)       // Uncut raw metallic grey
                z >= -0.5f -> Color(0xFFE67E22)                    // Faced surface (TNC Orange)
                z >= -10.0f -> Color(0xFFF1C40F)                   // Pocket depth (Golden Yellow)
                z >= -20.0f -> Color(0xFF2ECC71)                   // Deep pocket / slot (Green)
                else -> Color(0xFF2980B9)                          // Deep hole / thread (Blue)
            }

            val pt = projectPoint(Point3D(xVal, yVal, z), ViewMode.TOP_XY, center, scale)
            drawRect(
                color = color,
                topLeft = Offset(pt.x, pt.y - cellH),
                size = Size(cellW + 0.5f, cellH + 0.5f)
            )
        }
    }
}

private fun DrawScope.drawIsometricHeightfield(
    grid: ZMapHeightfield,
    center: Offset,
    scale: Float
) {
    val colStep = max(1, grid.cols / 40)
    val rowStep = max(1, grid.rows / 40)

    for (row in 0 until grid.rows - rowStep step rowStep) {
        for (col in 0 until grid.cols - colStep step colStep) {
            val x1 = grid.minX + col * ((grid.maxX - grid.minX) / grid.cols)
            val y1 = grid.minY + row * ((grid.maxY - grid.minY) / grid.rows)
            val z = grid.getZ(col, row)

            val p1 = projectPoint(Point3D(x1, y1, z), ViewMode.ISOMETRIC, center, scale)

            val color = when {
                z >= grid.maxZ - 0.05f -> Color(0xFF546E7A)
                z >= -0.5f -> Color(0xFFE67E22)
                z >= -10.0f -> Color(0xFFF1C40F)
                z >= -20.0f -> Color(0xFF2ECC71)
                else -> Color(0xFF2980B9)
            }

            drawCircle(color = color, radius = 2.5f * scale.coerceAtLeast(0.8f), center = p1)
        }
    }
}

private fun DrawScope.drawTool(
    pos: Point3D,
    radius: Float,
    viewMode: ViewMode,
    center: Offset,
    scale: Float,
    showRadius: Boolean
) {
    val screenPos = projectPoint(pos, viewMode, center, scale)

    // Center yellow crosshair dot
    drawCircle(color = CncToolYellow, radius = 3.5f, center = screenPos)

    // Tool footprint (true effective diameter circle)
    if (showRadius && radius > 0.1f && (viewMode == ViewMode.TOP_XY || viewMode == ViewMode.MATERIAL_25D)) {
        val pixRadius = radius * scale
        drawCircle(color = CncToolYellow.copy(alpha = 0.2f), radius = pixRadius, center = screenPos)
        drawCircle(color = CncToolYellow, radius = pixRadius, center = screenPos, style = Stroke(width = 1.5f))
    }

    // Crosshair reticle
    val arm = 10f
    drawLine(CncToolYellow, Offset(screenPos.x - arm, screenPos.y), Offset(screenPos.x + arm, screenPos.y), strokeWidth = 1.2f)
    drawLine(CncToolYellow, Offset(screenPos.x, screenPos.y - arm), Offset(screenPos.x, screenPos.y + arm), strokeWidth = 1.2f)
}

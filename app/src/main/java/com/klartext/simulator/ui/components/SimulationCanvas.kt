package com.klartext.simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.*
import com.klartext.simulator.ui.theme.*
import kotlin.math.*

@Composable
fun SimulationCanvas(
    state: SimulationUiState,
    currentStepIndex: Int,
    modifier: Modifier = Modifier,
    onToggleSectionPlane: (() -> Unit)? = null,
    onToggleToolHolder: (() -> Unit)? = null
) {
    var scale by remember { mutableFloatStateOf(2.2f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var orbitYaw by remember { mutableFloatStateOf(state.orbitYaw) }
    var orbitPitch by remember { mutableFloatStateOf(state.orbitPitch) }

    Box(
        modifier = modifier
            .background(CncDarkBackground, RoundedCornerShape(8.dp))
            .pointerInput(state.viewMode) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.4f, 25f)
                    if (state.viewMode == ViewMode.ORBIT_3D) {
                        orbitYaw = (orbitYaw + pan.x * 0.45f) % 360f
                        orbitPitch = (orbitPitch - pan.y * 0.45f).coerceIn(5f, 85f)
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + offsetX, size.height / 2f + offsetY)

            // 1. Draw Grid (for 2D views)
            if (state.showGrid && state.viewMode != ViewMode.ORBIT_3D && state.viewMode != ViewMode.SECTION_CUT) {
                drawGrid(center, scale)
            }

            // 2. Workpiece Stock or Material Display
            when (state.viewMode) {
                ViewMode.MATERIAL_25D -> {
                    if (state.heightfield != null) {
                        drawMaterialRemoval25D(state.heightfield, center, scale)
                    } else {
                        drawStock2D(state.stock, state.viewMode, center, scale)
                    }
                }
                ViewMode.ISOMETRIC -> {
                    drawStock3D(state.stock, state.viewMode, center, scale, yaw = 45f, pitch = 30f)
                    if (state.heightfield != null) {
                        drawIsometricHeightfield(state.heightfield, center, scale)
                    }
                }
                ViewMode.ORBIT_3D -> {
                    drawStock3D(state.stock, state.viewMode, center, scale, yaw = orbitYaw, pitch = orbitPitch)
                }
                ViewMode.SECTION_CUT -> {
                    drawSectionCut(state, center, scale)
                }
                else -> {
                    drawStock2D(state.stock, state.viewMode, center, scale)
                }
            }

            // 3. Workpiece Datum Symbol at X0 Y0
            if (state.viewMode != ViewMode.SECTION_CUT) {
                drawWorkpieceDatum(state.viewMode, center, scale, orbitYaw, orbitPitch)
            }

            // 4. Programmed Contour (Yellow Dashed)
            if (state.showProgrammedContour && state.viewMode != ViewMode.MATERIAL_25D && state.viewMode != ViewMode.SECTION_CUT) {
                drawProgrammedContour(state.segments, currentStepIndex, state.viewMode, center, scale, orbitYaw, orbitPitch)
            }

            // 5. Equidistant Cutter Center Path
            if (state.viewMode != ViewMode.SECTION_CUT) {
                val executedCount = currentStepIndex.coerceIn(0, state.segments.size)
                val segmentsToDraw = state.segments.take(executedCount)
                for (seg in segmentsToDraw) {
                    drawSegment(seg, state.viewMode, center, scale, orbitYaw, orbitPitch, isExecuted = true)
                }

                val futureSegments = state.segments.drop(executedCount)
                for (seg in futureSegments) {
                    drawSegment(seg, state.viewMode, center, scale, orbitYaw, orbitPitch, isExecuted = false)
                }
            }

            // 6. Active Tool Assembly (Flute + SK40 Holder + Crosshair)
            val currentPos = Point3D(state.machineState.x, state.machineState.y, state.machineState.z)
            val activeTool = ToolTable.defaultTools.firstOrNull { it.number == state.machineState.activeTool }
                ?: ToolTable.defaultTools[3]

            drawToolAssembly(
                pos = currentPos,
                tool = activeTool,
                effectiveRadius = state.machineState.effectiveRadius,
                viewMode = state.viewMode,
                center = center,
                scale = scale,
                yaw = orbitYaw,
                pitch = orbitPitch,
                showHolder = state.showToolHolder
            )
        }

        // Overlay Badges & Quick Action Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (state.viewMode == ViewMode.ORBIT_3D) {
                Surface(
                    color = CncPanelBackground.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, CncAccentCyan, RoundedCornerShape(4.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "3D Orbit: ${orbitYaw.toInt()}° / ${orbitPitch.toInt()}°",
                            color = CncAccentCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { orbitYaw = 45f; orbitPitch = 35f },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CncSurfaceDark)
                        ) {
                            Text("Reset", fontSize = 9.sp, color = CncTextPrimary)
                        }
                    }
                }
            }

            if (state.viewMode == ViewMode.SECTION_CUT && onToggleSectionPlane != null) {
                Button(
                    onClick = onToggleSectionPlane,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CncAccentOrange)
                ) {
                    Text(
                        text = "Vlak: ${state.sectionPlane.name.replace("_AXIS", "")}",
                        fontSize = 10.sp,
                        color = CncDarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun projectPoint(
    p: Point3D,
    mode: ViewMode,
    center: Offset,
    scale: Float,
    yawDeg: Float = 45f,
    pitchDeg: Float = 35f
): Offset {
    return when (mode) {
        ViewMode.TOP_XY, ViewMode.MATERIAL_25D -> Offset(center.x + p.x * scale, center.y - p.y * scale)
        ViewMode.FRONT_XZ, ViewMode.SECTION_CUT -> Offset(center.x + p.x * scale, center.y - p.z * scale)
        ViewMode.SIDE_YZ -> Offset(center.x + p.y * scale, center.y - p.z * scale)
        ViewMode.ISOMETRIC -> {
            val isoX = (p.x - p.y) * 0.866025f // cos(30°)
            val isoY = (p.x + p.y) * 0.5f - p.z // sin(30°) - z
            Offset(center.x + isoX * scale, center.y + isoY * scale)
        }
        ViewMode.ORBIT_3D -> {
            val yawRad = Math.toRadians(yawDeg.toDouble())
            val pitchRad = Math.toRadians(pitchDeg.toDouble())
            val cosY = cos(yawRad).toFloat()
            val sinY = sin(yawRad).toFloat()
            val cosP = cos(pitchRad).toFloat()
            val sinP = sin(pitchRad).toFloat()

            // Rotate around Z (yaw)
            val rx = p.x * cosY - p.y * sinY
            val ry = p.x * sinY + p.y * cosY
            val rz = p.z

            // Tilt around X (pitch)
            val sx = rx
            val sy = ry * sinP - rz * cosP

            Offset(center.x + sx * scale, center.y - sy * scale)
        }
    }
}

private fun DrawScope.drawGrid(center: Offset, scale: Float) {
    val step = 10f * scale
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

private fun DrawScope.drawWorkpieceDatum(
    mode: ViewMode,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float
) {
    val datumPt = projectPoint(Point3D(0f, 0f, 0f), mode, center, scale, yaw, pitch)
    val r = 7f

    drawCircle(color = CncAccentOrange, radius = r, center = datumPt, style = Stroke(width = 1.5f))
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

private fun DrawScope.drawStock2D(stock: StockBox, mode: ViewMode, center: Offset, scale: Float) {
    val pMin = projectPoint(stock.min, mode, center, scale)
    val pMax = projectPoint(stock.max, mode, center, scale)

    val left = min(pMin.x, pMax.x)
    val top = min(pMin.y, pMax.y)
    val w = abs(pMax.x - pMin.x)
    val h = abs(pMax.y - pMin.y)

    drawRect(color = CncStockGrey, topLeft = Offset(left, top), size = Size(w, h))
    drawRect(color = CncBorder, topLeft = Offset(left, top), size = Size(w, h), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawStock3D(
    stock: StockBox,
    mode: ViewMode,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float
) {
    val x0 = stock.min.x; val x1 = stock.max.x
    val y0 = stock.min.y; val y1 = stock.max.y
    val z0 = stock.min.z; val z1 = stock.max.z

    val corners = listOf(
        Point3D(x0, y0, z0), // 0: min X, min Y, min Z
        Point3D(x1, y0, z0), // 1: max X, min Y, min Z
        Point3D(x1, y1, z0), // 2: max X, max Y, min Z
        Point3D(x0, y1, z0), // 3: min X, max Y, min Z
        Point3D(x0, y0, z1), // 4: min X, min Y, max Z (top)
        Point3D(x1, y0, z1), // 5: max X, min Y, max Z (top)
        Point3D(x1, y1, z1), // 6: max X, max Y, max Z (top)
        Point3D(x0, y1, z1)  // 7: min X, max Y, max Z (top)
    ).map { projectPoint(it, mode, center, scale, yaw, pitch) }

    // Bottom face
    val bottomEdges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
    for ((i, j) in bottomEdges) {
        drawLine(CncBorder.copy(alpha = 0.4f), corners[i], corners[j], strokeWidth = 1f)
    }

    // Vertical pillars
    val verticalEdges = listOf(0 to 4, 1 to 5, 2 to 6, 3 to 7)
    for ((i, j) in verticalEdges) {
        drawLine(CncBorder.copy(alpha = 0.6f), corners[i], corners[j], strokeWidth = 1.2f)
    }

    // Top face
    val topEdges = listOf(4 to 5, 5 to 6, 6 to 7, 7 to 4)
    for ((i, j) in topEdges) {
        drawLine(CncAccentOrange.copy(alpha = 0.85f), corners[i], corners[j], strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawProgrammedContour(
    segments: List<ToolpathSegment>,
    currentStepIndex: Int,
    mode: ViewMode,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float
) {
    val yellowContour = Color(0xFFFFEB3B).copy(alpha = 0.6f)
    val stroke = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))

    for (seg in segments) {
        if (seg.moveType == MoveType.RAPID) continue
        val pStart = projectPoint(seg.programmedStart, mode, center, scale, yaw, pitch)
        val pEnd = projectPoint(seg.programmedEnd, mode, center, scale, yaw, pitch)
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
    yaw: Float,
    pitch: Float,
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
            val pStart = projectPoint(seg.start, mode, center, scale, yaw, pitch)
            val pEnd = projectPoint(seg.end, mode, center, scale, yaw, pitch)
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
            val p0 = projectPoint(seg.start, mode, center, scale, yaw, pitch)
            path.moveTo(p0.x, p0.y)

            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val angle = startAngle + t * (endAngle - startAngle)
                val ax = seg.center.x + seg.radius * cos(angle)
                val ay = seg.center.y + seg.radius * sin(angle)
                val az = seg.start.z + t * (seg.end.z - seg.start.z)
                val pt = projectPoint(Point3D(ax, ay, az), mode, center, scale, yaw, pitch)
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

            val color = when {
                z >= grid.maxZ - 0.05f -> Color(0xFF455A64)       // Raw metallic grey
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

private fun DrawScope.drawSectionCut(
    state: SimulationUiState,
    center: Offset,
    scale: Float
) {
    val stock = state.stock
    val isYPlane = state.sectionPlane == SectionPlane.Y_AXIS
    val pMin = stock.min
    val pMax = stock.max

    val axisMin = if (isYPlane) pMin.x else pMin.y
    val axisMax = if (isYPlane) pMax.x else pMax.y
    val zMin = pMin.z
    val zMax = pMax.z

    val screenOrigin = Offset(center.x + axisMin * scale, center.y - zMax * scale)
    val widthPx = (axisMax - axisMin) * scale
    val heightPx = abs(zMin - zMax) * scale

    // 1. Raw stock cross section solid base
    drawRect(
        color = Color(0xFF263238),
        topLeft = screenOrigin,
        size = Size(widthPx, heightPx)
    )

    // 2. Hatch solid material with 45° diagonal lines (authentic technical section view)
    val hatchSpacing = 14f
    val hatchColor = Color(0xFF37474F)
    var d = -heightPx
    while (d < widthPx + heightPx) {
        val x1 = (screenOrigin.x + d).coerceIn(screenOrigin.x, screenOrigin.x + widthPx)
        val y1 = (screenOrigin.y + d - (x1 - screenOrigin.x)).coerceIn(screenOrigin.y, screenOrigin.y + heightPx)
        val y2 = (screenOrigin.y + heightPx).coerceAtMost(screenOrigin.y + d)
        val x2 = (screenOrigin.x + d - (y2 - screenOrigin.y)).coerceIn(screenOrigin.x, screenOrigin.x + widthPx)
        if (abs(x1 - x2) > 1f || abs(y1 - y2) > 1f) {
            drawLine(hatchColor, Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.2f)
        }
        d += hatchSpacing
    }

    // 3. Clear machined profile using Z-Map heightfield
    val hf = state.heightfield
    if (hf != null) {
        val path = Path()
        val fixedCoord = if (isYPlane) (pMin.y + pMax.y) / 2f else (pMin.x + pMax.x) / 2f
        val steps = 80
        val startPt = Offset(center.x + axisMin * scale, center.y - zMax * scale)
        path.moveTo(startPt.x, startPt.y)

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val currentAxisVal = axisMin + t * (axisMax - axisMin)
            val col = (((currentAxisVal - hf.minX) / (hf.maxX - hf.minX)) * (hf.cols - 1)).toInt().coerceIn(0, hf.cols - 1)
            val row = (((fixedCoord - hf.minY) / (hf.maxY - hf.minY)) * (hf.rows - 1)).toInt().coerceIn(0, hf.rows - 1)
            val depthZ = if (isYPlane) hf.getZ(col, row) else hf.getZ(row, col)

            val screenX = center.x + currentAxisVal * scale
            val screenY = center.y - depthZ * scale
            path.lineTo(screenX, screenY)
        }

        path.lineTo(center.x + axisMax * scale, center.y - zMax * scale)
        path.close()

        drawPath(path, color = CncDarkBackground)
        drawPath(path, color = CncFeedCyan, style = Stroke(width = 2f))
    }

    // 4. Outer stock border
    drawRect(
        color = CncAccentOrange,
        topLeft = screenOrigin,
        size = Size(widthPx, heightPx),
        style = Stroke(width = 1.5f)
    )

    // 5. Depth reference lines and annotations (Z0, Z-10, Z-20...)
    var zTick = 0f
    while (zTick >= zMin) {
        val tickY = center.y - zTick * scale
        drawLine(
            color = CncBorder.copy(alpha = 0.5f),
            start = Offset(screenOrigin.x - 10f, tickY),
            end = Offset(screenOrigin.x + widthPx + 10f, tickY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
        zTick -= 10f
    }
}

private fun DrawScope.drawToolAssembly(
    pos: Point3D,
    tool: TncTool,
    effectiveRadius: Float,
    viewMode: ViewMode,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float,
    showHolder: Boolean
) {
    val tipScreen = projectPoint(pos, viewMode, center, scale, yaw, pitch)

    // Center yellow crosshair dot
    drawCircle(color = CncToolYellow, radius = 3.5f, center = tipScreen)

    // Crosshair reticle
    val arm = 10f
    drawLine(CncToolYellow, Offset(tipScreen.x - arm, tipScreen.y), Offset(tipScreen.x + arm, tipScreen.y), strokeWidth = 1.2f)
    drawLine(CncToolYellow, Offset(tipScreen.x, tipScreen.y - arm), Offset(tipScreen.x, tipScreen.y + arm), strokeWidth = 1.2f)

    if (viewMode == ViewMode.TOP_XY || viewMode == ViewMode.MATERIAL_25D) {
        val pixRadius = effectiveRadius * scale
        drawCircle(color = CncToolYellow.copy(alpha = 0.2f), radius = pixRadius, center = tipScreen)
        drawCircle(color = CncToolYellow, radius = pixRadius, center = tipScreen, style = Stroke(width = 1.5f))

        if (showHolder) {
            val colletPixR = (tool.holder.colletDiameter / 2f) * scale
            drawCircle(
                color = Color(0xFF607D8B).copy(alpha = 0.35f),
                radius = colletPixR,
                center = tipScreen,
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
            )
        }
    } else {
        // Vertical assembly: Cutter flute + SK40 Holder
        val stickOut = tool.stickOut
        val fluteEnd = Point3D(pos.x, pos.y, pos.z + stickOut)
        val fluteTopScreen = projectPoint(fluteEnd, viewMode, center, scale, yaw, pitch)

        // Cutter flute cylinder
        drawLine(
            color = Color(0xFFFFD54F),
            start = tipScreen,
            end = fluteTopScreen,
            strokeWidth = (effectiveRadius * 2f * scale).coerceAtLeast(3f)
        )

        if (showHolder) {
            // 1. Collet nut (ER32 nut)
            val nutTop = Point3D(pos.x, pos.y, pos.z + stickOut + tool.holder.colletLength)
            val nutTopScreen = projectPoint(nutTop, viewMode, center, scale, yaw, pitch)
            drawLine(
                color = Color(0xFF37474F),
                start = fluteTopScreen,
                end = nutTopScreen,
                strokeWidth = (tool.holder.colletDiameter * scale).coerceAtLeast(6f)
            )

            // 2. Collet chuck body
            val chuckTop = Point3D(pos.x, pos.y, pos.z + stickOut + tool.holder.colletLength + 25f)
            val chuckTopScreen = projectPoint(chuckTop, viewMode, center, scale, yaw, pitch)
            drawLine(
                color = Color(0xFF455A64),
                start = nutTopScreen,
                end = chuckTopScreen,
                strokeWidth = ((tool.holder.colletDiameter - 6f) * scale).coerceAtLeast(5f)
            )

            // 3. SK40 V-Flange
            val flangeTop = Point3D(pos.x, pos.y, pos.z + stickOut + tool.holder.colletLength + 38f)
            val flangeTopScreen = projectPoint(flangeTop, viewMode, center, scale, yaw, pitch)
            drawLine(
                color = Color(0xFF78909C),
                start = chuckTopScreen,
                end = flangeTopScreen,
                strokeWidth = (tool.holder.flangeDiameter * scale).coerceAtLeast(8f)
            )
        }
    }
}

package com.klartext.simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

/**
 * Authentic HEIDENHAIN TNC 640 3D & 2.5D Simulation Canvas
 * Features:
 * - Authentic TNC 640 Color Palette (Olive stock, Terracotta face milling, Golden amber pockets, Emerald deep pockets, Cobalt bores)
 * - Free 3D Orbit Camera with 1-finger drag, pinch-zoom, and Pan mode
 * - Step-by-step 3D Solid Milled Workpiece Simulation with directional normal shading
 * - 5-Axis Machine Vise Clamping under the workpiece
 * - Dynamic Cutter Engagement & Spark Glow Indicator
 * - 3D Orientation Gizmo (X=Red, Y=Green, Z=Blue)
 * - Quick-View Presets (ISO, XY, XZ, YZ, FIT, PAN/DRAAI)
 */
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
    var isPanMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(CncDarkBackground, RoundedCornerShape(8.dp))
            .pointerInput(state.viewMode, isPanMode) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.4f, 25f)
                    if (state.viewMode == ViewMode.ORBIT_3D) {
                        if (isPanMode) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            orbitYaw = (orbitYaw + pan.x * 0.45f) % 360f
                            if (orbitYaw < 0f) orbitYaw += 360f
                            orbitPitch = (orbitPitch - pan.y * 0.45f).coerceIn(5f, 88f)
                        }
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
                    if (state.heightfield != null) {
                        drawOrbit3DMaterialRemoval(
                            grid = state.heightfield,
                            stock = state.stock,
                            center = center,
                            scale = scale,
                            yaw = 45f,
                            pitch = 30f,
                            toolPos = Point3D(state.machineState.x, state.machineState.y, state.machineState.z),
                            effectiveRadius = state.machineState.effectiveRadius
                        )
                    } else {
                        drawStock3D(state.stock, state.viewMode, center, scale, yaw = 45f, pitch = 30f)
                    }
                }
                ViewMode.ORBIT_3D -> {
                    if (state.heightfield != null) {
                        drawOrbit3DMaterialRemoval(
                            grid = state.heightfield,
                            stock = state.stock,
                            center = center,
                            scale = scale,
                            yaw = orbitYaw,
                            pitch = orbitPitch,
                            toolPos = Point3D(state.machineState.x, state.machineState.y, state.machineState.z),
                            effectiveRadius = state.machineState.effectiveRadius
                        )
                    } else {
                        drawStock3D(state.stock, state.viewMode, center, scale, yaw = orbitYaw, pitch = orbitPitch)
                    }
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

            // 6. Active Tool Assembly (Flute + HSK-A63/BT40/SK40 Holder + Engagement Spark)
            val currentPos = Point3D(state.machineState.x, state.machineState.y, state.machineState.z)
            val activeTool = ToolTable.defaultTools.firstOrNull { it.number == state.machineState.activeTool }
                ?: ToolTable.defaultTools.firstOrNull() ?: ToolTable.standardTools[2]

            drawToolAssembly(
                pos = currentPos,
                tool = activeTool,
                effectiveRadius = state.machineState.effectiveRadius,
                viewMode = state.viewMode,
                center = center,
                scale = scale,
                yaw = orbitYaw,
                pitch = orbitPitch,
                showHolder = state.showToolHolder,
                stock = state.stock,
                isRapid = state.machineState.isRapid
            )

            // 7. 3D Orientation Gizmo (Triad in bottom-left corner)
            if (state.viewMode == ViewMode.ORBIT_3D || state.viewMode == ViewMode.ISOMETRIC) {
                val effectiveYaw = if (state.viewMode == ViewMode.ISOMETRIC) 45f else orbitYaw
                val effectivePitch = if (state.viewMode == ViewMode.ISOMETRIC) 30f else orbitPitch
                drawOrientationGizmo3D(effectiveYaw, effectivePitch)
            }
        }

        // Overlay Quick-View Controls & Presets
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (state.viewMode == ViewMode.ORBIT_3D) {
                // Angle badge & Quick presets row
                Surface(
                    color = CncPanelBackground.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, CncAccentCyan, RoundedCornerShape(4.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "3D ${orbitYaw.toInt()}°/${orbitPitch.toInt()}°",
                                color = CncAccentCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            // Pan vs Rotate Mode Switch
                            Button(
                                onClick = { isPanMode = !isPanMode },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPanMode) CncAccentOrange else CncSurfaceDark
                                )
                            ) {
                                Text(
                                    text = if (isPanMode) "PAN" else "DRAAI",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPanMode) CncDarkBackground else CncTextPrimary
                                )
                            }
                        }

                        // Presets: ISO, XY, XZ, YZ, FIT
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CanvasPresetBtn("ISO") { orbitYaw = 45f; orbitPitch = 35f }
                            CanvasPresetBtn("XY") { orbitYaw = 0f; orbitPitch = 88f }
                            CanvasPresetBtn("XZ") { orbitYaw = 0f; orbitPitch = 10f }
                            CanvasPresetBtn("YZ") { orbitYaw = 90f; orbitPitch = 10f }
                            CanvasPresetBtn("FIT") {
                                offsetX = 0f
                                offsetY = 0f
                                scale = 2.2f
                            }
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

@Composable
private fun CanvasPresetBtn(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
        modifier = Modifier
            .height(18.dp)
            .defaultMinSize(minWidth = 22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
        shape = RoundedCornerShape(2.dp)
    ) {
        Text(text = label, fontSize = 8.sp, color = CncTextPrimary, fontWeight = FontWeight.Bold)
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

    drawRect(color = Color(0xFF5A6B5C), topLeft = Offset(left, top), size = Size(w, h))
    drawRect(color = Color(0xFF8B9E8D), topLeft = Offset(left, top), size = Size(w, h), style = Stroke(width = 1.5f))
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

    // Top face (Authentic Heidenhain Olive)
    val topEdges = listOf(4 to 5, 5 to 6, 6 to 7, 7 to 4)
    for ((i, j) in topEdges) {
        drawLine(Color(0xFF8B9E8D), corners[i], corners[j], strokeWidth = 1.5f)
    }
}

/**
 * Renders the solid workpiece with step-by-step material removal in 3D Orbit,
 * using authentic HEIDENHAIN TNC 640 depth colors, Painter's algorithm, and directional shading.
 */
private fun DrawScope.drawOrbit3DMaterialRemoval(
    grid: ZMapHeightfield,
    stock: StockBox,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float,
    toolPos: Point3D?,
    effectiveRadius: Float
) {
    // 1. Draw 5-Axis Machine Vise Clamping Base under stock
    drawMachineVise3D(stock, center, scale, yaw, pitch)

    // 2. Traversal steps for solid quads
    val colStep = max(1, grid.cols / 36)
    val rowStep = max(1, grid.rows / 36)

    // Back-to-front rendering order based on camera view direction
    val yawRad = Math.toRadians(yaw.toDouble())
    val cosY = cos(yawRad).toFloat()
    val sinY = sin(yawRad).toFloat()

    val rowProgression = if (cosY >= 0f) {
        (grid.rows - rowStep - 1 downTo 0 step rowStep)
    } else {
        (0 until grid.rows - rowStep step rowStep)
    }

    val colProgression = if (sinY >= 0f) {
        (0 until grid.cols - colStep step colStep)
    } else {
        (grid.cols - colStep - 1 downTo 0 step colStep)
    }

    val stockZMax = stock.max.z
    val stockZMin = stock.min.z

    val cellW = (grid.maxX - grid.minX) / grid.cols
    val cellH = (grid.maxY - grid.minY) / grid.rows

    // 3. Render 3D solid milled mesh
    for (row in rowProgression) {
        val y0 = grid.minY + row * cellH
        val y1 = grid.minY + (row + rowStep) * cellH

        for (col in colProgression) {
            val x0 = grid.minX + col * cellW
            val x1 = grid.minX + (col + colStep) * cellW

            val z00 = grid.getZ(col, row)
            val z10 = grid.getZ((col + colStep).coerceAtMost(grid.cols - 1), row)
            val z11 = grid.getZ((col + colStep).coerceAtMost(grid.cols - 1), (row + rowStep).coerceAtMost(grid.rows - 1))
            val z01 = grid.getZ(col, (row + rowStep).coerceAtMost(grid.rows - 1))

            val zAvg = (z00 + z10 + z11 + z01) * 0.25f

            // Project 4 corners
            val p00 = projectPoint(Point3D(x0, y0, z00), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
            val p10 = projectPoint(Point3D(x1, y0, z10), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
            val p11 = projectPoint(Point3D(x1, y1, z11), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
            val p01 = projectPoint(Point3D(x0, y1, z01), ViewMode.ORBIT_3D, center, scale, yaw, pitch)

            // Authentic TNC 640 Base Color based on milled depth
            val baseColor = when {
                zAvg >= stockZMax - 0.05f -> Color(0xFF5A6B5C) // Uncut raw stock (Heidenhain Olive)
                zAvg >= -0.5f -> Color(0xFFD35400)             // Faced surface (TNC Terracotta Orange)
                zAvg >= -10.0f -> Color(0xFFF1C40F)            // Pocket floor (TNC Golden Amber)
                zAvg >= -25.0f -> Color(0xFF27AE60)            // Deep pockets / slots (TNC Emerald Green)
                else -> Color(0xFF1F618D)                      // Deep bores / thread (TNC Cobalt Blue)
            }

            // Directional normal shading factor
            val dzX = (z10 - z00)
            val dzY = (z01 - z00)
            val slope = abs(dzX) + abs(dzY)
            val shade = (1.0f - (slope * 0.12f).coerceIn(-0.35f, 0.4f)).coerceIn(0.6f, 1.25f)

            val shadedColor = Color(
                red = (baseColor.red * shade).coerceIn(0f, 1f),
                green = (baseColor.green * shade).coerceIn(0f, 1f),
                blue = (baseColor.blue * shade).coerceIn(0f, 1f),
                alpha = 1f
            )

            val quadPath = Path().apply {
                moveTo(p00.x, p00.y)
                lineTo(p10.x, p10.y)
                lineTo(p11.x, p11.y)
                lineTo(p01.x, p01.y)
                close()
            }

            drawPath(quadPath, color = shadedColor)
        }
    }

    // 4. Draw outer boundary vertical side walls of the stock down to stockZMin
    drawStockWalls3D(grid, stock, center, scale, yaw, pitch)

    // 5. Workpiece outer wireframe accent for crisp technical look
    drawStockEdges3D(stock, center, scale, yaw, pitch)
}

/**
 * Solid vertical side walls of the workpiece from Zmin up to surface height
 */
private fun DrawScope.drawStockWalls3D(
    grid: ZMapHeightfield,
    stock: StockBox,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float
) {
    val wallColorFront = Color(0xFF38463D)
    val wallColorSide = Color(0xFF2C3730)
    val zMin = stock.min.z

    val cornersBottom = listOf(
        Point3D(stock.min.x, stock.min.y, zMin),
        Point3D(stock.max.x, stock.min.y, zMin),
        Point3D(stock.max.x, stock.max.y, zMin),
        Point3D(stock.min.x, stock.max.y, zMin)
    ).map { projectPoint(it, ViewMode.ORBIT_3D, center, scale, yaw, pitch) }

    // Bottom solid plate
    val bottomPath = Path().apply {
        moveTo(cornersBottom[0].x, cornersBottom[0].y)
        lineTo(cornersBottom[1].x, cornersBottom[1].y)
        lineTo(cornersBottom[2].x, cornersBottom[2].y)
        lineTo(cornersBottom[3].x, cornersBottom[3].y)
        close()
    }
    drawPath(bottomPath, color = Color(0xFF1E2621))

    // Front Wall (Y = minY)
    val frontP0 = projectPoint(Point3D(stock.min.x, stock.min.y, zMin), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val frontP1 = projectPoint(Point3D(stock.max.x, stock.min.y, zMin), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val frontP2 = projectPoint(Point3D(stock.max.x, stock.min.y, grid.getZ(grid.cols - 1, 0)), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val frontP3 = projectPoint(Point3D(stock.min.x, stock.min.y, grid.getZ(0, 0)), ViewMode.ORBIT_3D, center, scale, yaw, pitch)

    val frontWallPath = Path().apply {
        moveTo(frontP0.x, frontP0.y)
        lineTo(frontP1.x, frontP1.y)
        lineTo(frontP2.x, frontP2.y)
        lineTo(frontP3.x, frontP3.y)
        close()
    }
    drawPath(frontWallPath, color = wallColorFront)

    // Right Wall (X = maxX)
    val rightP0 = projectPoint(Point3D(stock.max.x, stock.min.y, zMin), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val rightP1 = projectPoint(Point3D(stock.max.x, stock.max.y, zMin), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val rightP2 = projectPoint(Point3D(stock.max.x, stock.max.y, grid.getZ(grid.cols - 1, grid.rows - 1)), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val rightP3 = projectPoint(Point3D(stock.max.x, stock.min.y, grid.getZ(grid.cols - 1, 0)), ViewMode.ORBIT_3D, center, scale, yaw, pitch)

    val rightWallPath = Path().apply {
        moveTo(rightP0.x, rightP0.y)
        lineTo(rightP1.x, rightP1.y)
        lineTo(rightP2.x, rightP2.y)
        lineTo(rightP3.x, rightP3.y)
        close()
    }
    drawPath(rightWallPath, color = wallColorSide)
}

/**
 * 5-Axis Machine Vise (Lang Makro-Grip style) clamping the workpiece base
 */
private fun DrawScope.drawMachineVise3D(
    stock: StockBox,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float
) {
    val zViseTop = stock.min.z
    val zViseBottom = stock.min.z - 25f
    val viseOverhang = 8f

    val vx0 = stock.min.x - viseOverhang
    val vx1 = stock.max.x + viseOverhang
    val vy0 = stock.min.y - 4f
    val vy1 = stock.max.y + 4f

    val b0 = projectPoint(Point3D(vx0, vy0, zViseBottom), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val b1 = projectPoint(Point3D(vx1, vy0, zViseBottom), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val b2 = projectPoint(Point3D(vx1, vy1, zViseBottom), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val b3 = projectPoint(Point3D(vx0, vy1, zViseBottom), ViewMode.ORBIT_3D, center, scale, yaw, pitch)

    val t0 = projectPoint(Point3D(vx0, vy0, zViseTop), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val t1 = projectPoint(Point3D(vx1, vy0, zViseTop), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val t2 = projectPoint(Point3D(vx1, vy1, zViseTop), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val t3 = projectPoint(Point3D(vx0, vy1, zViseTop), ViewMode.ORBIT_3D, center, scale, yaw, pitch)

    // Vise body (Dark anthracite cast iron)
    val viseFront = Path().apply {
        moveTo(b0.x, b0.y); lineTo(b1.x, b1.y); lineTo(t1.x, t1.y); lineTo(t0.x, t0.y); close()
    }
    drawPath(viseFront, color = Color(0xFF1E2830))

    val viseSide = Path().apply {
        moveTo(b1.x, b1.y); lineTo(b2.x, b2.y); lineTo(t2.x, t2.y); lineTo(t1.x, t1.y); close()
    }
    drawPath(viseSide, color = Color(0xFF161F26))

    // Hardened ground steel clamping jaw strips
    drawLine(Color(0xFF90A4AE), t0, t1, strokeWidth = 2.5f)
    drawLine(Color(0xFF90A4AE), t1, t2, strokeWidth = 2.5f)
}

private fun DrawScope.drawStockEdges3D(
    stock: StockBox,
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float
) {
    val x0 = stock.min.x; val x1 = stock.max.x
    val y0 = stock.min.y; val y1 = stock.max.y
    val z0 = stock.min.z; val z1 = stock.max.z

    val c0 = projectPoint(Point3D(x0, y0, z1), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val c1 = projectPoint(Point3D(x1, y0, z1), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val c2 = projectPoint(Point3D(x1, y1, z1), ViewMode.ORBIT_3D, center, scale, yaw, pitch)
    val c3 = projectPoint(Point3D(x0, y1, z1), ViewMode.ORBIT_3D, center, scale, yaw, pitch)

    val edgeColor = Color(0xFF8B9E8D).copy(alpha = 0.6f)
    drawLine(edgeColor, c0, c1, strokeWidth = 1f)
    drawLine(edgeColor, c1, c2, strokeWidth = 1f)
    drawLine(edgeColor, c2, c3, strokeWidth = 1f)
    drawLine(edgeColor, c3, c0, strokeWidth = 1f)
}

/**
 * Rotating 3D Orientation Gizmo (Triad) in the bottom-left corner
 */
private fun DrawScope.drawOrientationGizmo3D(yaw: Float, pitch: Float) {
    val gizmoCenter = Offset(45.dp.toPx(), size.height - 45.dp.toPx())
    val r = 26.dp.toPx()

    // Disc backdrop
    drawCircle(color = Color(0xFF1B242C), radius = r + 4f, center = gizmoCenter)
    drawCircle(color = Color(0xFF37474F), radius = r + 4f, center = gizmoCenter, style = Stroke(width = 1f))

    val axisLen = r * 0.85f

    // X Axis (Red)
    val pX = projectPoint(Point3D(axisLen, 0f, 0f), ViewMode.ORBIT_3D, gizmoCenter, 1f, yaw, pitch)
    drawLine(Color(0xFFE53935), gizmoCenter, pX, strokeWidth = 2.5f)
    drawCircle(Color(0xFFE53935), radius = 3f, center = pX)

    // Y Axis (Green)
    val pY = projectPoint(Point3D(0f, axisLen, 0f), ViewMode.ORBIT_3D, gizmoCenter, 1f, yaw, pitch)
    drawLine(Color(0xFF43A047), gizmoCenter, pY, strokeWidth = 2.5f)
    drawCircle(Color(0xFF43A047), radius = 3f, center = pY)

    // Z Axis (Blue)
    val pZ = projectPoint(Point3D(0f, 0f, axisLen), ViewMode.ORBIT_3D, gizmoCenter, 1f, yaw, pitch)
    drawLine(Color(0xFF1E88E5), gizmoCenter, pZ, strokeWidth = 2.5f)
    drawCircle(Color(0xFF1E88E5), radius = 3f, center = pZ)
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
        MoveType.RAPID -> Color(0xFFE53935)
        MoveType.FEED -> Color(0xFF00E5FF)
        MoveType.PLUNGE -> Color(0xFFE040FB)
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
                z >= grid.maxZ - 0.05f -> Color(0xFF5A6B5C)       // Uncut raw stock
                z >= -0.5f -> Color(0xFFD35400)                    // Faced surface (TNC Orange)
                z >= -10.0f -> Color(0xFFF1C40F)                   // Pocket depth (Golden Yellow)
                z >= -25.0f -> Color(0xFF27AE60)                   // Deep pocket / slot (Green)
                else -> Color(0xFF1F618D)                          // Deep hole / thread (Blue)
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
    showHolder: Boolean,
    stock: StockBox,
    isRapid: Boolean
) {
    val tipScreen = projectPoint(pos, viewMode, center, scale, yaw, pitch)

    // Center yellow crosshair dot
    drawCircle(color = CncToolYellow, radius = 3.5f, center = tipScreen)

    // Dynamic cutting engagement spark glow
    val isCutting = !isRapid && pos.z <= stock.max.z && pos.z >= stock.min.z &&
            pos.x >= stock.min.x && pos.x <= stock.max.x && pos.y >= stock.min.y && pos.y <= stock.max.y

    if (isCutting) {
        drawCircle(
            color = Color(0xFFFF9800).copy(alpha = 0.45f),
            radius = (effectiveRadius * scale * 1.5f).coerceIn(8f, 35f),
            center = tipScreen
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 4f,
            center = tipScreen
        )
    }

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
        // Vertical assembly: Cutter flute + HSK/BT40/SK40 Holder
        val stickOut = tool.stickOut
        val fluteEnd = Point3D(pos.x, pos.y, pos.z + stickOut)
        val fluteTopScreen = projectPoint(fluteEnd, viewMode, center, scale, yaw, pitch)

        // Cutter flute cylinder
        val fluteColor = when (tool.geometryType) {
            ToolGeometryType.BALL_NOSE, ToolGeometryType.TORUS_MILL -> Color(0xFF81C784)
            ToolGeometryType.SPOT_DRILL_90, ToolGeometryType.TWIST_DRILL -> Color(0xFF64B5F6)
            ToolGeometryType.TAP, ToolGeometryType.THREAD_MILL -> Color(0xFFBA68C8)
            else -> Color(0xFFFFD54F)
        }

        drawLine(
            color = fluteColor,
            start = tipScreen,
            end = fluteTopScreen,
            strokeWidth = (effectiveRadius * 2f * scale).coerceAtLeast(3f)
        )

        if (showHolder) {
            // 1. Collet nut (ER32 / HSK-A63 collet)
            val nutTop = Point3D(pos.x, pos.y, pos.z + stickOut + tool.holder.colletLength)
            val nutTopScreen = projectPoint(nutTop, viewMode, center, scale, yaw, pitch)
            drawLine(
                color = Color(0xFF37474F),
                start = fluteTopScreen,
                end = nutTopScreen,
                strokeWidth = (tool.holder.colletDiameter * scale).coerceAtLeast(6f)
            )

            // 2. Chuck body
            val chuckTop = Point3D(pos.x, pos.y, pos.z + stickOut + tool.holder.colletLength + 25f)
            val chuckTopScreen = projectPoint(chuckTop, viewMode, center, scale, yaw, pitch)
            drawLine(
                color = Color(0xFF455A64),
                start = nutTopScreen,
                end = chuckTopScreen,
                strokeWidth = ((tool.holder.colletDiameter - 6f) * scale).coerceAtLeast(5f)
            )

            // 3. Holder V-Flange (HSK-A63 / BT40 / SK40)
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

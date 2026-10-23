package com.klartext.simulator.parser

import com.klartext.simulator.model.*
import java.util.Locale
import kotlin.math.*

class KlartextInterpreter {

    fun interpret(blocks: List<KlartextBlock>): InterpretationResult {
        var currentX = 0f
        var currentY = 0f
        var currentZ = 100f

        var datumX = 0f
        var datumY = 0f
        var datumZ = 0f

        var activeTool: TncTool = ToolTable.defaultTools[3] // Default FREES10
        var activeDl = 0f
        var activeDr = 0f
        var effectiveRadius = activeTool.radius

        var currentFeed = 500f
        var currentSpeed = 3000f
        var spindleOn = false
        var activeRadiusComp = RadiusComp.R0

        var circleCenterX = 0f
        var circleCenterY = 0f

        var lastTangent = Point2D(1f, 0f)

        var stockMin = Point3D(0f, 0f, -25f)
        var stockMax = Point3D(120f, 120f, 0f)

        val customToolDefs = mutableMapOf<Int, Pair<Float, Float>>() // toolNum -> (L, R)
        val labelMapByNumber = mutableMapOf<Int, Int>()
        val labelMapByName = mutableMapOf<String, Int>()
        val qParams = mutableMapOf<Int, Float>()

        var activeCycle: KlartextBlock.CycleDef? = null
        val diagnostics = mutableListOf<TncDiagnostic>()

        // Pre-scan for BLK FORM, TOOL DEF, and LBL definitions
        for ((idx, block) in blocks.withIndex()) {
            when (block) {
                is KlartextBlock.LabelSet -> {
                    if (block.labelNumber != 0) {
                        labelMapByNumber[block.labelNumber] = idx
                        labelMapByName[block.labelName.lowercase()] = idx
                    }
                }
                is KlartextBlock.ToolDef -> {
                    customToolDefs[block.toolNumber] = Pair(block.length, block.radius)
                }
                is KlartextBlock.BlkForm -> {
                    if (block.subIndex == 1) stockMin = block.point
                    if (block.subIndex == 2) stockMax = block.point
                }
                else -> Unit
            }
        }

        val segments = mutableListOf<ToolpathSegment>()
        val callStack = mutableListOf<CallFrame>()
        var pc = 0
        var loopBudget = 15000

        fun updateModalComp(explicit: RadiusComp?, lineNum: Int, isCuttingMoveInXY: Boolean) {
            if (explicit != null) {
                if (explicit == RadiusComp.R0 &&
                    (activeRadiusComp == RadiusComp.RL || activeRadiusComp == RadiusComp.RR) &&
                    isCuttingMoveInXY
                ) {
                    diagnostics.add(
                        TncDiagnostic(
                            lineNumber = lineNum,
                            code = TncErrorCode.R0_CANCELS_MODAL_COMPENSATION,
                            severity = DiagnosticSeverity.STUDENT_HINT,
                            title = "R0 schakelt ${activeRadiusComp.name} uit!",
                            message = "Let op: R0 schakelt ${activeRadiusComp.name} uit op regel $lineNum! Gebruik NO ENT om ${activeRadiusComp.name} actief te houden."
                        )
                    )
                }
                activeRadiusComp = explicit
            }
        }

        fun applyOffset2D(pt: Point2D, tangent: Point2D, comp: RadiusComp, rEff: Float): Point2D {
            if (rEff <= 0.0001f || comp == RadiusComp.R0) return pt
            val normal = when (comp) {
                RadiusComp.RL -> tangent.leftNormal()
                RadiusComp.RR -> tangent.rightNormal()
                RadiusComp.R0 -> Point2D(0f, 0f)
            }
            return pt + normal * rEff
        }

        fun checkHolderCollision(lineNum: Int, x: Float, y: Float, z: Float) {
            val collisionZ = stockMax.z - activeTool.stickOut
            if (z < collisionZ) {
                val colletR = activeTool.holder.colletDiameter / 2f
                val inStock = x in (stockMin.x - colletR)..(stockMax.x + colletR) &&
                              y in (stockMin.y - colletR)..(stockMax.y + colletR)
                if (inStock) {
                    diagnostics.add(
                        TncDiagnostic(
                            lineNumber = lineNum,
                            severity = DiagnosticSeverity.ERROR,
                            code = TncErrorCode.TOOL_HOLDER_COLLISION,
                            title = "KOL-BEWAKING: Houder botst met werkstuk",
                            message = "Fout op regel $lineNum: Gereedschapshouder (${activeTool.holder.type}, Ø${activeTool.holder.colletDiameter.toInt()} mm) raakt werkstuk (Z=${formatNum(z)} mm overschrijdt uitsteeklengte ${formatNum(activeTool.stickOut)} mm)"
                        )
                    )
                }
            }
        }

        fun addLinearSegment(
            lineNum: Int,
            progStart: Point3D,
            progEnd: Point3D,
            moveType: MoveType,
            feed: Float,
            comp: RadiusComp
        ) {
            val dir2D = Point2D(progEnd.x - progStart.x, progEnd.y - progStart.y)
            val distXY = hypot(dir2D.x, dir2D.y)
            val tangent = if (distXY > 1e-4f) dir2D.normalized() else lastTangent
            if (distXY > 1e-4f) lastTangent = tangent

            val equiStart2D = if (distXY > 1e-4f && moveType != MoveType.RAPID) {
                applyOffset2D(progStart.to2D(), tangent, comp, effectiveRadius)
            } else {
                val prevEnd = segments.lastOrNull()?.end
                if (prevEnd != null && comp != RadiusComp.R0 && distXY <= 1e-4f) {
                    Point2D(prevEnd.x, prevEnd.y)
                } else {
                    progStart.to2D()
                }
            }

            val equiEnd2D = if (distXY > 1e-4f && moveType != MoveType.RAPID) {
                applyOffset2D(progEnd.to2D(), tangent, comp, effectiveRadius)
            } else if (comp != RadiusComp.R0 && distXY <= 1e-4f) {
                equiStart2D
            } else {
                progEnd.to2D()
            }

            // Connect gracefully if previous segment ended at a slightly different offset point
            val prevEnd = segments.lastOrNull()?.end
            val actualStart = if (prevEnd != null && comp != RadiusComp.R0 && distXY > 1e-4f &&
                hypot(prevEnd.x - equiStart2D.x, prevEnd.y - equiStart2D.y) in 0.01f..(effectiveRadius * 3f)
            ) {
                segments.add(
                    ToolpathSegment.Linear(
                        lineNumber = lineNum,
                        start = prevEnd,
                        end = Point3D(equiStart2D.x, equiStart2D.y, progStart.z),
                        programmedStart = progStart,
                        programmedEnd = progStart,
                        moveType = MoveType.FEED,
                        feedRate = feed,
                        spindleSpeed = currentSpeed,
                        toolNumber = activeTool.number,
                        toolName = activeTool.name,
                        toolRadius = activeTool.radius,
                        effectiveRadius = effectiveRadius,
                        dl = activeDl,
                        dr = activeDr,
                        toolGeometry = activeTool.geometryType,
                        pointAngleDeg = activeTool.pointAngleDeg,
                        radiusComp = comp
                    )
                )
                Point3D(equiStart2D.x, equiStart2D.y, progStart.z)
            } else {
                Point3D(equiStart2D.x, equiStart2D.y, progStart.z)
            }

            val actualEnd = Point3D(equiEnd2D.x, equiEnd2D.y, progEnd.z)

            if (actualStart.distanceTo(actualEnd) > 0.0001f || progStart.distanceTo(progEnd) > 0.0001f) {
                if (moveType != MoveType.RAPID || actualEnd.z < stockMax.z) {
                    checkHolderCollision(lineNum, actualEnd.x, actualEnd.y, actualEnd.z)
                }
                segments.add(
                    ToolpathSegment.Linear(
                        lineNumber = lineNum,
                        start = actualStart,
                        end = actualEnd,
                        programmedStart = progStart,
                        programmedEnd = progEnd,
                        moveType = moveType,
                        feedRate = feed,
                        spindleSpeed = currentSpeed,
                        toolNumber = activeTool.number,
                        toolName = activeTool.name,
                        toolRadius = activeTool.radius,
                        effectiveRadius = effectiveRadius,
                        dl = activeDl,
                        dr = activeDr,
                        toolGeometry = activeTool.geometryType,
                        pointAngleDeg = activeTool.pointAngleDeg,
                        radiusComp = comp
                    )
                )
            }
        }

        fun addArcSegment(
            lineNum: Int,
            progStart: Point3D,
            progEnd: Point3D,
            center: Point2D,
            progRadius: Float,
            direction: Direction,
            moveType: MoveType,
            feed: Float,
            comp: RadiusComp
        ) {
            // Determine if tool is inside or outside curvature
            // For CCW (DR+): left normal points TOWARD center (inside), right normal points AWAY (outside)
            // For CW (DR-): left normal points AWAY from center (outside), right normal points TOWARD (inside)
            val isInsideArc = (comp == RadiusComp.RL && direction == Direction.CCW) ||
                    (comp == RadiusComp.RR && direction == Direction.CW)
            val isOutsideArc = (comp == RadiusComp.RL && direction == Direction.CW) ||
                    (comp == RadiusComp.RR && direction == Direction.CCW)

            if (isInsideArc && effectiveRadius > progRadius + 0.005f) {
                diagnostics.add(
                    TncDiagnostic(
                        lineNumber = lineNum,
                        code = TncErrorCode.TOOL_RADIUS_TOO_LARGE,
                        severity = DiagnosticSeverity.ERROR,
                        title = "Gereedschapsradius te groot",
                        message = "Fout op regel $lineNum: Gereedschapsradius te groot (R_eff=${formatNum(effectiveRadius)} mm > boog R=${formatNum(progRadius)} mm)"
                    )
                )
            }

            val equiRadius = when {
                isInsideArc -> (progRadius - effectiveRadius).coerceAtLeast(0.1f)
                isOutsideArc -> progRadius + effectiveRadius
                else -> progRadius
            }

            val startAngle = atan2(progStart.y - center.y, progStart.x - center.x)
            val endAngle = atan2(progEnd.y - center.y, progEnd.x - center.x)

            val equiStart = Point3D(
                center.x + equiRadius * cos(startAngle),
                center.y + equiRadius * sin(startAngle),
                progStart.z
            )
            val equiEnd = Point3D(
                center.x + equiRadius * cos(endAngle),
                center.y + equiRadius * sin(endAngle),
                progEnd.z
            )

            // Update outgoing tangent at endAngle
            lastTangent = if (direction == Direction.CCW) {
                Point2D(-sin(endAngle), cos(endAngle))
            } else {
                Point2D(sin(endAngle), -cos(endAngle))
            }

            checkHolderCollision(lineNum, equiEnd.x, equiEnd.y, equiEnd.z)

            segments.add(
                ToolpathSegment.Arc(
                    lineNumber = lineNum,
                    start = equiStart,
                    end = equiEnd,
                    programmedStart = progStart,
                    programmedEnd = progEnd,
                    moveType = moveType,
                    feedRate = feed,
                    spindleSpeed = currentSpeed,
                    toolNumber = activeTool.number,
                    toolName = activeTool.name,
                    toolRadius = activeTool.radius,
                    effectiveRadius = effectiveRadius,
                    dl = activeDl,
                    dr = activeDr,
                    toolGeometry = activeTool.geometryType,
                    pointAngleDeg = activeTool.pointAngleDeg,
                    radiusComp = comp,
                    center = center,
                    radius = equiRadius,
                    programmedRadius = progRadius,
                    direction = direction
                )
            )
        }

        fun resolveCoord(raw: String?, staticVal: Float?, qMap: Map<Int, Float>): Float? {
            if (raw != null) {
                if (raw.contains("Q", ignoreCase = true)) {
                    return QExpressionEvaluator.evaluate(raw, qMap)
                } else if (staticVal == null) {
                    return raw.toFloatOrNull()
                }
            }
            return staticVal
        }

        fun peekNextTarget2D(fromPc: Int, curX: Float, curY: Float): Point2D? {
            for (k in (fromPc + 1) until blocks.size) {
                when (val nb = blocks[k]) {
                    is KlartextBlock.Linear -> {
                        val rx = resolveCoord(nb.rawX, nb.x, qParams)
                        val ry = resolveCoord(nb.rawY, nb.y, qParams)
                        val nx = if (nb.isIncrementalX) curX + (rx ?: 0f) else (rx?.plus(datumX) ?: curX)
                        val ny = if (nb.isIncrementalY) curY + (ry ?: 0f) else (ry?.plus(datumY) ?: curY)
                        if (hypot(nx - curX, ny - curY) > 1e-4f) return Point2D(nx, ny)
                    }
                    is KlartextBlock.CircleRadius -> {
                        val rx = resolveCoord(nb.rawX, nb.x, qParams)
                        val ry = resolveCoord(nb.rawY, nb.y, qParams)
                        val nx = if (nb.isIncrementalX) curX + (rx ?: 0f) else (rx?.plus(datumX) ?: curX)
                        val ny = if (nb.isIncrementalY) curY + (ry ?: 0f) else (ry?.plus(datumY) ?: curY)
                        return Point2D(nx, ny)
                    }
                    is KlartextBlock.Circle -> {
                        val rx = resolveCoord(nb.rawX, nb.x, qParams)
                        val ry = resolveCoord(nb.rawY, nb.y, qParams)
                        val nx = if (nb.isIncrementalX) curX + (rx ?: 0f) else (rx?.plus(datumX) ?: curX)
                        val ny = if (nb.isIncrementalY) curY + (ry ?: 0f) else (ry?.plus(datumY) ?: curY)
                        return Point2D(nx, ny)
                    }
                    is KlartextBlock.CircleTangential -> {
                        val rx = resolveCoord(nb.rawX, nb.x, qParams)
                        val ry = resolveCoord(nb.rawY, nb.y, qParams)
                        val nx = if (nb.isIncrementalX) curX + (rx ?: 0f) else (rx?.plus(datumX) ?: curX)
                        val ny = if (nb.isIncrementalY) curY + (ry ?: 0f) else (ry?.plus(datumY) ?: curY)
                        return Point2D(nx, ny)
                    }
                    is KlartextBlock.Depart -> {
                        val nx = nb.x?.plus(datumX) ?: (curX + lastTangent.x * (nb.len ?: 10f))
                        val ny = nb.y?.plus(datumY) ?: (curY + lastTangent.y * (nb.len ?: 10f))
                        return Point2D(nx, ny)
                    }
                    is KlartextBlock.Comment -> continue
                    else -> break
                }
            }
            return null
        }

        while (pc in blocks.indices && loopBudget-- > 0) {
            val block = blocks[pc]

            when (block) {
                is KlartextBlock.ToolCall -> {
                    activeTool = ToolTable.resolve(block.toolIdentifier)
                    val custom = customToolDefs[activeTool.number]
                    if (custom != null && custom.second > 0f) {
                        activeTool = activeTool.copy(diameter = custom.second * 2f, length = custom.first)
                    }
                    activeDl = block.dl
                    activeDr = block.dr
                    effectiveRadius = (activeTool.radius + activeDr).coerceAtLeast(0.1f)
                    block.spindleSpeed?.let { currentSpeed = it; spindleOn = true }
                    block.feedRate?.let { currentFeed = it }
                }

                is KlartextBlock.CircleCenter -> {
                    val rx = resolveCoord(block.rawX, block.x, qParams)
                    val ry = resolveCoord(block.rawY, block.y, qParams)
                    circleCenterX = when {
                        rx == null -> currentX
                        block.isIncrementalX || block.isIncremental -> currentX + rx
                        else -> rx + datumX
                    }
                    circleCenterY = when {
                        ry == null -> currentY
                        block.isIncrementalY || block.isIncremental -> currentY + ry
                        else -> ry + datumY
                    }
                }

                is KlartextBlock.Approach -> {
                    activeRadiusComp = block.comp
                    block.feedRate?.let { currentFeed = it }
                    val targetX = block.x + datumX
                    val targetY = block.y + datumY
                    val targetZ = block.z?.plus(datumZ) ?: currentZ

                    // Determine contour direction from next block
                    val nextPt = peekNextTarget2D(pc, targetX, targetY)
                    val contourDir = if (nextPt != null && nextPt.distanceTo(Point2D(targetX, targetY)) > 1e-4f) {
                        (nextPt - Point2D(targetX, targetY)).normalized()
                    } else {
                        Point2D(targetX - currentX, targetY - currentY).normalized()
                    }
                    lastTangent = contourDir

                    val progStart = Point3D(currentX, currentY, currentZ)
                    val progEnd = Point3D(targetX, targetY, targetZ)
                    addLinearSegment(block.lineNumber, progStart, progEnd, MoveType.FEED, currentFeed, activeRadiusComp)

                    currentX = targetX
                    currentY = targetY
                    currentZ = targetZ
                }

                is KlartextBlock.Depart -> {
                    block.feedRate?.let { currentFeed = it }
                    val len = block.len ?: block.radius ?: 10f
                    val targetX = block.x?.plus(datumX) ?: (currentX + lastTangent.x * len)
                    val targetY = block.y?.plus(datumY) ?: (currentY + lastTangent.y * len)
                    val targetZ = block.z?.plus(datumZ) ?: currentZ

                    val progStart = Point3D(currentX, currentY, currentZ)
                    val progEnd = Point3D(targetX, targetY, targetZ)
                    // Departure cancels radius compensation at the end of the depart move
                    activeRadiusComp = RadiusComp.R0
                    addLinearSegment(block.lineNumber, progStart, progEnd, MoveType.FEED, currentFeed, RadiusComp.R0)

                    currentX = targetX
                    currentY = targetY
                    currentZ = targetZ
                    if (block.mCodes.contains(5) || block.mCodes.contains(30)) spindleOn = false
                }

                is KlartextBlock.Linear -> {
                    val rx = resolveCoord(block.rawX, block.x, qParams)
                    val ry = resolveCoord(block.rawY, block.y, qParams)
                    val rz = resolveCoord(block.rawZ, block.z, qParams)
                    val rFeed = resolveCoord(block.rawFeed, block.feedRate, qParams)

                    val targetX = when {
                        rx == null -> currentX
                        block.isIncrementalX -> currentX + rx
                        else -> rx + datumX
                    }
                    val targetY = when {
                        ry == null -> currentY
                        block.isIncrementalY -> currentY + ry
                        else -> ry + datumY
                    }
                    val targetZ = when {
                        rz == null -> currentZ
                        block.isIncrementalZ -> currentZ + rz
                        else -> rz + datumZ
                    }

                    val movesXY = hypot(targetX - currentX, targetY - currentY) > 1e-4f
                    val isCuttingXY = movesXY && !block.isRapid && currentZ <= stockMax.z + 0.5f
                    updateModalComp(block.explicitComp, block.lineNumber, isCuttingXY)

                    rFeed?.let { currentFeed = it }
                    if (block.mCodes.contains(3) || block.mCodes.contains(4)) spindleOn = true
                    if (block.mCodes.contains(5) || block.mCodes.contains(30)) spindleOn = false

                    val moveType = when {
                        block.isRapid -> MoveType.RAPID
                        targetZ < currentZ && !movesXY && currentZ <= stockMax.z + 5f -> MoveType.PLUNGE
                        else -> MoveType.FEED
                    }

                    val progStart = Point3D(currentX, currentY, currentZ)
                    val progEnd = Point3D(targetX, targetY, targetZ)
                    val feed = if (block.isRapid) 99999f else (rFeed ?: currentFeed)

                    addLinearSegment(block.lineNumber, progStart, progEnd, moveType, feed, activeRadiusComp)

                    currentX = targetX
                    currentY = targetY
                    currentZ = targetZ

                    // Execute active cycle if M99 is on this block
                    val curCycle = activeCycle
                    if (block.mCodes.contains(99) && curCycle != null) {
                        currentZ = executeFixedCycle(
                            cycle = curCycle,
                            callLine = block.lineNumber,
                            cx = currentX,
                            cy = currentY,
                            cz = currentZ,
                            datumZ = datumZ,
                            stockMin = stockMin,
                            stockMax = stockMax,
                            tool = activeTool,
                            effectiveRadius = effectiveRadius,
                            dl = activeDl,
                            dr = activeDr,
                            feed = currentFeed,
                            speed = currentSpeed,
                            segments = segments,
                            diagnostics = diagnostics
                        )
                    }
                }

                is KlartextBlock.LinearPolar -> {
                    val rPR = resolveCoord(block.rawPR, block.pr, qParams) ?: block.pr
                    val rPA = resolveCoord(block.rawPA, block.pa, qParams) ?: block.pa
                    val rZ = resolveCoord(block.rawZ, block.z, qParams)
                    val angleRad = Math.toRadians(rPA.toDouble()).toFloat()
                    val targetX = circleCenterX + rPR * cos(angleRad)
                    val targetY = circleCenterY + rPR * sin(angleRad)
                    val targetZ = rZ?.plus(datumZ) ?: currentZ

                    val movesXY = hypot(targetX - currentX, targetY - currentY) > 1e-4f
                    updateModalComp(block.explicitComp, block.lineNumber, movesXY && !block.isRapid)
                    block.feedRate?.let { currentFeed = it }

                    val moveType = if (block.isRapid) MoveType.RAPID else MoveType.FEED
                    val feed = if (block.isRapid) 99999f else (block.feedRate ?: currentFeed)

                    addLinearSegment(
                        block.lineNumber,
                        Point3D(currentX, currentY, currentZ),
                        Point3D(targetX, targetY, targetZ),
                        moveType,
                        feed,
                        activeRadiusComp
                    )
                    currentX = targetX
                    currentY = targetY
                    currentZ = targetZ

                    val curCycPolar = activeCycle
                    if (block.mCodes.contains(99) && curCycPolar != null) {
                        currentZ = executeFixedCycle(
                            curCycPolar, block.lineNumber, currentX, currentY, currentZ, datumZ,
                            stockMin, stockMax, activeTool, effectiveRadius, activeDl, activeDr,
                            currentFeed, currentSpeed, segments, diagnostics
                        )
                    }
                }

                is KlartextBlock.Circle -> {
                    updateModalComp(block.explicitComp, block.lineNumber, true)
                    val start = Point3D(currentX, currentY, currentZ)
                    val rx = resolveCoord(block.rawX, block.x, qParams)
                    val ry = resolveCoord(block.rawY, block.y, qParams)
                    val rz = resolveCoord(block.rawZ, block.z, qParams)
                    val targetX = when {
                        rx == null -> currentX
                        block.isIncrementalX -> currentX + rx
                        else -> rx + datumX
                    }
                    val targetY = when {
                        ry == null -> currentY
                        block.isIncrementalY -> currentY + ry
                        else -> ry + datumY
                    }
                    val targetZ = when {
                        rz == null -> currentZ
                        block.isIncrementalZ -> currentZ + rz
                        else -> rz + datumZ
                    }
                    val end = Point3D(targetX, targetY, targetZ)
                    val center = Point2D(circleCenterX, circleCenterY)
                    val rStart = hypot(start.x - center.x, start.y - center.y)
                    val rEnd = hypot(end.x - center.x, end.y - center.y)

                    // Validate circle endpoint match (tolerance 0.01 mm, or 0.05 mm for educational rounding)
                    if (abs(rStart - rEnd) > 0.02f) {
                        diagnostics.add(
                            TncDiagnostic(
                                lineNumber = block.lineNumber,
                                code = TncErrorCode.CIRCLE_ENDPOINT_MISMATCH,
                                severity = DiagnosticSeverity.ERROR,
                                title = "Cirkel-eindpunt foutief",
                                message = "Fout op regel ${block.lineNumber}: Cirkel-eindpunt ligt niet op dezelfde straal (R_start=${formatNum(rStart)} mm, R_eind=${formatNum(rEnd)} mm)"
                            )
                        )
                    }

                    block.feedRate?.let { currentFeed = it }
                    val moveType = if (block.isRapid) MoveType.RAPID else MoveType.FEED
                    val feed = if (block.isRapid) 99999f else (block.feedRate ?: currentFeed)

                    addArcSegment(
                        lineNum = block.lineNumber,
                        progStart = start,
                        progEnd = end,
                        center = center,
                        progRadius = rStart,
                        direction = block.direction,
                        moveType = moveType,
                        feed = feed,
                        comp = activeRadiusComp
                    )
                    currentX = targetX
                    currentY = targetY
                    currentZ = targetZ
                }

                is KlartextBlock.CirclePolar -> {
                    updateModalComp(block.explicitComp, block.lineNumber, true)
                    val start = Point3D(currentX, currentY, currentZ)
                    val center = Point2D(circleCenterX, circleCenterY)
                    val r = hypot(start.x - center.x, start.y - center.y)
                    val rPA = resolveCoord(block.rawPA, block.pa, qParams) ?: block.pa
                    val rZ = resolveCoord(block.rawZ, block.z, qParams)
                    val angleRad = Math.toRadians(rPA.toDouble()).toFloat()
                    val targetX = center.x + r * cos(angleRad)
                    val targetY = center.y + r * sin(angleRad)
                    val targetZ = rZ?.plus(datumZ) ?: currentZ
                    val end = Point3D(targetX, targetY, targetZ)

                    block.feedRate?.let { currentFeed = it }
                    addArcSegment(
                        lineNum = block.lineNumber,
                        progStart = start,
                        progEnd = end,
                        center = center,
                        progRadius = r,
                        direction = block.direction,
                        moveType = MoveType.FEED,
                        feed = block.feedRate ?: currentFeed,
                        comp = activeRadiusComp
                    )
                    currentX = targetX
                    currentY = targetY
                    currentZ = targetZ
                }

                is KlartextBlock.CircleRadius -> {
                    updateModalComp(block.explicitComp, block.lineNumber, true)
                    val start = Point3D(currentX, currentY, currentZ)
                    val rx = resolveCoord(block.rawX, block.x, qParams)
                    val ry = resolveCoord(block.rawY, block.y, qParams)
                    val rr = resolveCoord(block.rawRadius, block.radius, qParams) ?: block.radius
                    val targetX = if (block.isIncrementalX) currentX + (rx ?: 0f) else (rx?.plus(datumX) ?: currentX)
                    val targetY = if (block.isIncrementalY) currentY + (ry ?: 0f) else (ry?.plus(datumY) ?: currentY)
                    val end = Point3D(targetX, targetY, currentZ)
                    val r = abs(rr)
                    val longArc = rr < 0f

                    val center = calculateArcCenter(
                        Point2D(start.x, start.y),
                        Point2D(targetX, targetY),
                        r,
                        isCw = block.direction == Direction.CW,
                        longArc = longArc
                    )

                    block.feedRate?.let { currentFeed = it }
                    val moveType = if (block.isRapid) MoveType.RAPID else MoveType.FEED
                    val feed = if (block.isRapid) 99999f else (block.feedRate ?: currentFeed)

                    addArcSegment(
                        lineNum = block.lineNumber,
                        progStart = start,
                        progEnd = end,
                        center = center,
                        progRadius = r,
                        direction = block.direction,
                        moveType = moveType,
                        feed = feed,
                        comp = activeRadiusComp
                    )
                    currentX = targetX
                    currentY = targetY
                }

                is KlartextBlock.CircleTangential -> {
                    updateModalComp(block.explicitComp, block.lineNumber, true)
                    val start = Point3D(currentX, currentY, currentZ)
                    val rx = resolveCoord(block.rawX, block.x, qParams)
                    val ry = resolveCoord(block.rawY, block.y, qParams)
                    val rz = resolveCoord(block.rawZ, block.z, qParams)
                    val targetX = if (block.isIncrementalX) currentX + (rx ?: 0f) else (rx?.plus(datumX) ?: currentX)
                    val targetY = if (block.isIncrementalY) currentY + (ry ?: 0f) else (ry?.plus(datumY) ?: currentY)
                    val targetZ = rz?.plus(datumZ) ?: currentZ
                    val end = Point3D(targetX, targetY, targetZ)

                    val p1 = Point2D(start.x, start.y)
                    val p2 = Point2D(targetX, targetY)
                    val dVec = p2 - p1
                    val normal = lastTangent.leftNormal()
                    val denom = 2f * (dVec.x * normal.x + dVec.y * normal.y)

                    block.feedRate?.let { currentFeed = it }
                    val feed = block.feedRate ?: currentFeed

                    if (abs(denom) > 1e-4f) {
                        val s = (dVec.x * dVec.x + dVec.y * dVec.y) / denom
                        val center = p1 + normal * s
                        val r = abs(s)
                        val dir = if (s > 0f) Direction.CCW else Direction.CW

                        addArcSegment(
                            lineNum = block.lineNumber,
                            progStart = start,
                            progEnd = end,
                            center = center,
                            progRadius = r,
                            direction = dir,
                            moveType = MoveType.FEED,
                            feed = feed,
                            comp = activeRadiusComp
                        )
                    } else {
                        addLinearSegment(block.lineNumber, start, end, MoveType.FEED, feed, activeRadiusComp)
                    }

                    currentX = targetX
                    currentY = targetY
                    currentZ = targetZ
                }

                is KlartextBlock.CornerRounding -> {
                    val rRnd = block.radius
                    val nextPt = peekNextTarget2D(pc, currentX, currentY)
                    val u1 = lastTangent.normalized()
                    val u2 = if (nextPt != null && nextPt.distanceTo(Point2D(currentX, currentY)) > 1e-4f) {
                        (nextPt - Point2D(currentX, currentY)).normalized()
                    } else {
                        Point2D(-u1.y, u1.x)
                    }

                    // Cross product u1 x u2: > 0 means left turn (CCW), < 0 means right turn (CW)
                    val cross = u1.x * u2.y - u1.y * u2.x
                    val isInsideCorner = (activeRadiusComp == RadiusComp.RL && cross > 0.01f) ||
                            (activeRadiusComp == RadiusComp.RR && cross < -0.01f) ||
                            (activeRadiusComp != RadiusComp.R0 && nextPt == null && effectiveRadius > rRnd)

                    if (isInsideCorner && effectiveRadius > rRnd + 0.005f) {
                        diagnostics.add(
                            TncDiagnostic(
                                lineNumber = block.lineNumber,
                                code = TncErrorCode.TOOL_RADIUS_TOO_LARGE,
                                severity = DiagnosticSeverity.ERROR,
                                title = "Gereedschapsradius te groot",
                                message = "Fout op regel ${block.lineNumber}: Gereedschapsradius te groot (R_eff=${formatNum(effectiveRadius)} mm > RND R${formatNum(rRnd)} mm)"
                            )
                        )
                    }

                    // Geometrically trim the corner and insert the rounding arc if there is a previous linear segment
                    val lastSegIdx = segments.lastIndex
                    val prevSeg = segments.getOrNull(lastSegIdx)
                    if (prevSeg is ToolpathSegment.Linear && abs(cross) > 0.01f) {
                        val dot = (-u1.x * u2.x + -u1.y * u2.y).coerceIn(-0.999f, 0.999f)
                        val halfAngle = acos(dot) / 2f
                        val trimDist = (rRnd / tan(halfAngle)).coerceIn(0.5f, rRnd * 2f)

                        val cornerPt = Point2D(currentX, currentY)
                        val segLen = prevSeg.programmedStart.to2D().distanceTo(cornerPt)
                        if (segLen > trimDist + 0.1f) {
                            val arcStart2D = cornerPt - u1 * trimDist
                            val arcEnd2D = cornerPt + u2 * trimDist
                            val dir = if (cross > 0f) Direction.CCW else Direction.CW
                            val centerNormal = if (dir == Direction.CCW) u1.leftNormal() else u1.rightNormal()
                            val center = arcStart2D + centerNormal * rRnd

                            // Shorten previous segment to arcStart2D
                            segments.removeAt(lastSegIdx)
                            addLinearSegment(
                                prevSeg.lineNumber,
                                prevSeg.programmedStart,
                                Point3D(arcStart2D.x, arcStart2D.y, currentZ),
                                prevSeg.moveType,
                                prevSeg.feedRate,
                                activeRadiusComp
                            )

                            // Insert RND arc
                            addArcSegment(
                                lineNum = block.lineNumber,
                                progStart = Point3D(arcStart2D.x, arcStart2D.y, currentZ),
                                progEnd = Point3D(arcEnd2D.x, arcEnd2D.y, currentZ),
                                center = center,
                                progRadius = rRnd,
                                direction = dir,
                                moveType = MoveType.FEED,
                                feed = block.feedRate ?: currentFeed,
                                comp = activeRadiusComp
                            )
                            currentX = arcEnd2D.x
                            currentY = arcEnd2D.y
                        }
                    }
                }

                is KlartextBlock.CornerChamfer -> {
                    val chfLen = block.length
                    val nextPt = peekNextTarget2D(pc, currentX, currentY)
                    val u1 = lastTangent.normalized()
                    val u2 = if (nextPt != null && nextPt.distanceTo(Point2D(currentX, currentY)) > 1e-4f) {
                        (nextPt - Point2D(currentX, currentY)).normalized()
                    } else {
                        Point2D(-u1.y, u1.x)
                    }

                    val lastSegIdx = segments.lastIndex
                    val prevSeg = segments.getOrNull(lastSegIdx)
                    val cornerPt = Point2D(currentX, currentY)
                    if (prevSeg is ToolpathSegment.Linear && prevSeg.programmedStart.to2D().distanceTo(cornerPt) > chfLen + 0.1f) {
                        val chfStart2D = cornerPt - u1 * chfLen
                        val chfEnd2D = cornerPt + u2 * chfLen

                        segments.removeAt(lastSegIdx)
                        addLinearSegment(
                            prevSeg.lineNumber,
                            prevSeg.programmedStart,
                            Point3D(chfStart2D.x, chfStart2D.y, currentZ),
                            prevSeg.moveType,
                            prevSeg.feedRate,
                            activeRadiusComp
                        )
                        addLinearSegment(
                            block.lineNumber,
                            Point3D(chfStart2D.x, chfStart2D.y, currentZ),
                            Point3D(chfEnd2D.x, chfEnd2D.y, currentZ),
                            MoveType.FEED,
                            block.feedRate ?: currentFeed,
                            activeRadiusComp
                        )
                        currentX = chfEnd2D.x
                        currentY = chfEnd2D.y
                    }
                }

                is KlartextBlock.CycleDef -> {
                    if (block.cycleNumber == 7) {
                        if (block.subNumber == 0) {
                            // Next 7.1, 7.2, 7.3 lines will set datum
                        } else {
                            block.datumX?.let { datumX = if (block.isIncDatum) datumX + it else it }
                            block.datumY?.let { datumY = if (block.isIncDatum) datumY + it else it }
                            block.datumZ?.let { datumZ = if (block.isIncDatum) datumZ + it else it }
                        }
                    } else {
                        activeCycle = block
                        // Also pre-validate Cycle 206 / 208 immediately upon definition if parameters are invalid
                        validateCycleDefinition(block, activeTool, effectiveRadius, currentSpeed, diagnostics)
                    }
                }

                is KlartextBlock.CycleCall -> {
                    val curCycle = activeCycle
                    if (curCycle != null) {
                        currentZ = executeFixedCycle(
                            cycle = curCycle,
                            callLine = block.lineNumber,
                            cx = currentX,
                            cy = currentY,
                            cz = currentZ,
                            datumZ = datumZ,
                            stockMin = stockMin,
                            stockMax = stockMax,
                            tool = activeTool,
                            effectiveRadius = effectiveRadius,
                            dl = activeDl,
                            dr = activeDr,
                            feed = currentFeed,
                            speed = currentSpeed,
                            segments = segments,
                            diagnostics = diagnostics
                        )
                    }
                }

                is KlartextBlock.LabelCall -> {
                    val targetPc = labelMapByName[block.labelName.lowercase()]
                        ?: labelMapByNumber[block.labelNumber]
                    if (targetPc != null) {
                        val remainingReps = (block.repeatCount - 1).coerceAtLeast(0)
                        callStack.add(
                            CallFrame(
                                returnPc = pc,
                                labelPc = targetPc,
                                remainingReps = remainingReps
                            )
                        )
                        pc = targetPc
                    }
                }

                is KlartextBlock.LabelSet -> {
                    if ((block.labelNumber == 0 || block.labelName == "0") && callStack.isNotEmpty()) {
                        val frame = callStack.last()
                        if (frame.remainingReps > 0) {
                            callStack[callStack.lastIndex] = frame.copy(remainingReps = frame.remainingReps - 1)
                            pc = frame.labelPc
                        } else {
                            val popped = callStack.removeAt(callStack.lastIndex)
                            pc = popped.returnPc
                        }
                    }
                }

                is KlartextBlock.QParamAssignment -> {
                    val res = QExpressionEvaluator.evaluate(block.expression, qParams)
                    qParams[block.targetQ] = res
                }

                is KlartextBlock.QParamConditionalJump -> {
                    val val1 = QExpressionEvaluator.evaluate(block.operand1, qParams)
                    val val2 = QExpressionEvaluator.evaluate(block.operand2, qParams)
                    val conditionMet = when (block.condition.uppercase(Locale.ROOT)) {
                        "EQU" -> abs(val1 - val2) < 1e-4f
                        "NE" -> abs(val1 - val2) >= 1e-4f
                        "GT" -> (val1 - val2) > 1e-4f
                        "LT" -> (val2 - val1) > 1e-4f
                        else -> false
                    }
                    if (conditionMet) {
                        val targetPc = labelMapByName[block.targetLabelName.lowercase(Locale.ROOT)]
                            ?: labelMapByNumber[block.targetLabelNum]
                        if (targetPc != null) {
                            pc = targetPc
                        } else {
                            diagnostics.add(
                                TncDiagnostic(
                                    lineNumber = block.lineNumber,
                                    severity = DiagnosticSeverity.ERROR,
                                    code = TncErrorCode.LABEL_NOT_FOUND,
                                    title = "Label niet gevonden",
                                    message = "Fout op regel ${block.lineNumber}: Label '${block.targetLabelName}' niet gevonden voor sprong"
                                )
                            )
                        }
                    }
                }

                is KlartextBlock.QParamDef -> {
                    val v1 = if (block.isQ1) (qParams[block.value1.toInt()] ?: 0f) else block.value1
                    val v2 = if (block.isQ2) (qParams[block.value2.toInt()] ?: 0f) else block.value2
                    val res = when (block.fnType) {
                        1 -> v1 + v2
                        2 -> v1 - v2
                        3 -> v1 * v2
                        4 -> if (abs(v2) > 1e-6f) v1 / v2 else 0f
                        else -> v1
                    }
                    qParams[block.targetQ] = res
                }

                else -> Unit
            }

            pc++
        }

        return InterpretationResult(
            segments = segments,
            diagnostics = diagnostics.distinctBy { "${it.lineNumber}_${it.code}" },
            stock = StockBox(stockMin, stockMax),
            qParameters = qParams,
            finalState = MachineState(
                x = currentX,
                y = currentY,
                z = currentZ,
                feedRate = currentFeed,
                spindleSpeed = currentSpeed,
                activeTool = activeTool.number,
                toolName = activeTool.name,
                toolRadius = activeTool.radius,
                effectiveRadius = effectiveRadius,
                toolLength = activeTool.length,
                dl = activeDl,
                dr = activeDr,
                radiusComp = activeRadiusComp,
                spindleOn = spindleOn,
                totalSteps = segments.size,
                datumShift = Point3D(datumX, datumY, datumZ)
            )
        )
    }

    private fun validateCycleDefinition(
        cycle: KlartextBlock.CycleDef,
        tool: TncTool,
        effectiveRadius: Float,
        spindleSpeed: Float,
        diagnostics: MutableList<TncDiagnostic>
    ) {
        when (cycle.cycleNumber) {
            208 -> {
                val q335 = cycle.qParams[335] ?: 0f // Nominale diameter
                val q342 = cycle.qParams[342] ?: 0f // Voorboordiameter
                val dEff = 2f * effectiveRadius
                if (q342 == 0f && q335 > 2f * dEff + 0.01f) {
                    diagnostics.add(
                        TncDiagnostic(
                            lineNumber = cycle.lineNumber,
                            code = TncErrorCode.BORE_MILLING_DIAMETER_TOO_LARGE,
                            severity = DiagnosticSeverity.ERROR,
                            title = "Te grote diameter boring",
                            message = "Fout op regel ${cycle.lineNumber}: Te grote diameter boring (Q335=${formatNum(q335)} mm > 2×D_eff=${formatNum(2f * dEff)} mm bij Q342=0). Tip: stel voorboordiameter Q342 in op de bestaande gatdiameter."
                        )
                    )
                }
            }
            206, 207 -> {
                val q206 = cycle.qParams[206] // Geprogrammeerde tapvoeding
                val pitch = cycle.qParams[239] ?: tool.pitchMm ?: 1.25f
                if (q206 != null && spindleSpeed > 0f) {
                    val expectedFeed = spindleSpeed * pitch
                    if (abs(q206 - expectedFeed) > 1.5f) {
                        diagnostics.add(
                            TncDiagnostic(
                                lineNumber = cycle.lineNumber,
                                code = TncErrorCode.TAP_FEEDRATE_MISMATCH,
                                severity = DiagnosticSeverity.WARNING,
                                title = "Tap-voeding komt niet overeen met S × spoed",
                                message = "Waarschuwing op regel ${cycle.lineNumber}: Geprogrammeerde voeding Q206=${formatNum(q206)} mm/min wijkt af van S(${formatNum(spindleSpeed)}) × spoed(${formatNum(pitch)}) = ${formatNum(expectedFeed)} mm/min."
                            )
                        )
                    }
                }
            }
        }
    }

    private fun executeFixedCycle(
        cycle: KlartextBlock.CycleDef,
        callLine: Int,
        cx: Float,
        cy: Float,
        cz: Float,
        datumZ: Float,
        stockMin: Point3D,
        stockMax: Point3D,
        tool: TncTool,
        effectiveRadius: Float,
        dl: Float,
        dr: Float,
        feed: Float,
        speed: Float,
        segments: MutableList<ToolpathSegment>,
        diagnostics: MutableList<TncDiagnostic>
    ): Float {
        validateCycleDefinition(cycle, tool, effectiveRadius, speed, diagnostics)

        val q200 = cycle.qParams[200] ?: 2f
        val q201 = cycle.qParams[201] ?: -10f
        val q203 = (cycle.qParams[203] ?: 0f) + datumZ
        val q204 = cycle.qParams[204] ?: 50f
        val q206 = cycle.qParams[206] ?: feed
        val q207 = cycle.qParams[207] ?: feed

        val safeZ = q203 + q200
        val retractZ = max(safeZ, q203 + q204)
        val targetBottomZ = q203 + q201

        fun addMove(p1: Point3D, p2: Point3D, type: MoveType, f: Float) {
            if (p1.distanceTo(p2) > 1e-4f) {
                segments.add(
                    ToolpathSegment.Linear(
                        lineNumber = callLine,
                        start = p1,
                        end = p2,
                        moveType = type,
                        feedRate = f,
                        spindleSpeed = speed,
                        toolNumber = tool.number,
                        toolName = tool.name,
                        toolRadius = tool.radius,
                        effectiveRadius = effectiveRadius,
                        dl = dl,
                        dr = dr,
                        toolGeometry = tool.geometryType,
                        pointAngleDeg = tool.pointAngleDeg
                    )
                )
            }
        }

        when (cycle.cycleNumber) {
            240, 200, 206, 207 -> {
                // Centering, Drilling, Tapping
                val depthZ = if (cycle.cycleNumber == 240 && cycle.qParams.containsKey(343) && (cycle.qParams[343] ?: 0f) > 0f) {
                    q203 - (cycle.qParams[343]!! / 2f)
                } else {
                    targetBottomZ
                }
                addMove(Point3D(cx, cy, cz), Point3D(cx, cy, safeZ), MoveType.RAPID, 99999f)
                addMove(Point3D(cx, cy, safeZ), Point3D(cx, cy, depthZ), MoveType.PLUNGE, q206)
                addMove(Point3D(cx, cy, depthZ), Point3D(cx, cy, retractZ), MoveType.RAPID, 99999f)
                return retractZ
            }

            208 -> {
                // Bore milling (Boorfrezen)
                val q335 = cycle.qParams[335] ?: (effectiveRadius * 3f)
                val q334 = (cycle.qParams[334] ?: 1.5f).coerceAtLeast(0.2f)
                val orbitRadius = ((q335 / 2f) - effectiveRadius).coerceAtLeast(0.2f)

                addMove(Point3D(cx, cy, cz), Point3D(cx, cy, safeZ), MoveType.RAPID, 99999f)
                addMove(Point3D(cx, cy, safeZ), Point3D(cx + orbitRadius, cy, q203), MoveType.FEED, q206)

                var zCur = q203
                while (zCur > targetBottomZ + 1e-3f) {
                    val zNext = max(targetBottomZ, zCur - q334)
                    val zMid = (zCur + zNext) / 2f
                    // Two 180-deg arcs per helical revolution
                    segments.add(
                        ToolpathSegment.Arc(
                            lineNumber = callLine,
                            start = Point3D(cx + orbitRadius, cy, zCur),
                            end = Point3D(cx - orbitRadius, cy, zMid),
                            moveType = MoveType.FEED,
                            feedRate = q206,
                            spindleSpeed = speed,
                            toolNumber = tool.number,
                            toolName = tool.name,
                            toolRadius = tool.radius,
                            effectiveRadius = effectiveRadius,
                            dl = dl,
                            dr = dr,
                            toolGeometry = tool.geometryType,
                            pointAngleDeg = tool.pointAngleDeg,
                            center = Point2D(cx, cy),
                            radius = orbitRadius,
                            programmedRadius = q335 / 2f,
                            direction = Direction.CCW
                        )
                    )
                    segments.add(
                        ToolpathSegment.Arc(
                            lineNumber = callLine,
                            start = Point3D(cx - orbitRadius, cy, zMid),
                            end = Point3D(cx + orbitRadius, cy, zNext),
                            moveType = MoveType.FEED,
                            feedRate = q206,
                            spindleSpeed = speed,
                            toolNumber = tool.number,
                            toolName = tool.name,
                            toolRadius = tool.radius,
                            effectiveRadius = effectiveRadius,
                            dl = dl,
                            dr = dr,
                            toolGeometry = tool.geometryType,
                            pointAngleDeg = tool.pointAngleDeg,
                            center = Point2D(cx, cy),
                            radius = orbitRadius,
                            programmedRadius = q335 / 2f,
                            direction = Direction.CCW
                        )
                    )
                    zCur = zNext
                }
                addMove(Point3D(cx + orbitRadius, cy, targetBottomZ), Point3D(cx, cy, targetBottomZ), MoveType.FEED, q206)
                addMove(Point3D(cx, cy, targetBottomZ), Point3D(cx, cy, retractZ), MoveType.RAPID, 99999f)
                return retractZ
            }

            251 -> {
                // Rectangular pocket (Rechthoekige kamer)
                val lengthX = cycle.qParams[218] ?: 40f
                val widthY = cycle.qParams[219] ?: 30f
                val stepDown = (cycle.qParams[202] ?: abs(q201)).coerceAtLeast(1f)
                val maxRx = (lengthX / 2f - effectiveRadius).coerceAtLeast(0.5f)
                val maxRy = (widthY / 2f - effectiveRadius).coerceAtLeast(0.5f)
                val stepOver = (effectiveRadius * 1.3f).coerceAtLeast(1f)

                addMove(Point3D(cx, cy, cz), Point3D(cx, cy, safeZ), MoveType.RAPID, 99999f)

                var zLevel = q203 - stepDown
                while (true) {
                    val zCut = max(targetBottomZ, zLevel)
                    addMove(Point3D(cx, cy, if (zCut == q203 - stepDown) safeZ else zCut + stepDown), Point3D(cx, cy, zCut), MoveType.PLUNGE, q206)

                    var factor = 0.35f
                    var prevPt = Point3D(cx, cy, zCut)
                    while (true) {
                        val rx = maxRx * factor
                        val ry = maxRy * factor
                        val p1 = Point3D(cx + rx, cy - ry, zCut)
                        val p2 = Point3D(cx + rx, cy + ry, zCut)
                        val p3 = Point3D(cx - rx, cy + ry, zCut)
                        val p4 = Point3D(cx - rx, cy - ry, zCut)
                        addMove(prevPt, p1, MoveType.FEED, q207)
                        addMove(p1, p2, MoveType.FEED, q207)
                        addMove(p2, p3, MoveType.FEED, q207)
                        addMove(p3, p4, MoveType.FEED, q207)
                        addMove(p4, p1, MoveType.FEED, q207)
                        prevPt = p1
                        if (factor >= 0.999f) break
                        val stepFactor = stepOver / max(maxRx, maxRy).coerceAtLeast(1f)
                        factor = (factor + stepFactor.coerceAtLeast(0.25f)).coerceAtMost(1f)
                    }
                    addMove(prevPt, Point3D(cx, cy, zCut), MoveType.FEED, q207)

                    if (zCut <= targetBottomZ + 1e-3f) break
                    zLevel -= stepDown
                }
                addMove(Point3D(cx, cy, targetBottomZ), Point3D(cx, cy, retractZ), MoveType.RAPID, 99999f)
                return retractZ
            }

            252 -> {
                // Circular pocket (Ronde kamer)
                val diameter = cycle.qParams[223] ?: 40f
                val maxOrbit = ((diameter / 2f) - effectiveRadius).coerceAtLeast(0.5f)
                val stepDown = (cycle.qParams[202] ?: abs(q201)).coerceAtLeast(1f)
                val stepOver = (effectiveRadius * 1.3f).coerceAtLeast(1f)

                addMove(Point3D(cx, cy, cz), Point3D(cx, cy, safeZ), MoveType.RAPID, 99999f)
                addMove(Point3D(cx, cy, safeZ), Point3D(cx, cy, targetBottomZ), MoveType.PLUNGE, q206)

                var rCur = min(stepOver, maxOrbit)
                var prevPt = Point3D(cx, cy, targetBottomZ)
                while (true) {
                    val startCircle = Point3D(cx + rCur, cy, targetBottomZ)
                    val midCircle = Point3D(cx - rCur, cy, targetBottomZ)
                    addMove(prevPt, startCircle, MoveType.FEED, q207)
                    segments.add(
                        ToolpathSegment.Arc(
                            lineNumber = callLine,
                            start = startCircle,
                            end = midCircle,
                            moveType = MoveType.FEED,
                            feedRate = q207,
                            spindleSpeed = speed,
                            toolNumber = tool.number,
                            toolName = tool.name,
                            toolRadius = tool.radius,
                            effectiveRadius = effectiveRadius,
                            dl = dl,
                            dr = dr,
                            toolGeometry = tool.geometryType,
                            pointAngleDeg = tool.pointAngleDeg,
                            center = Point2D(cx, cy),
                            radius = rCur,
                            direction = Direction.CCW
                        )
                    )
                    segments.add(
                        ToolpathSegment.Arc(
                            lineNumber = callLine,
                            start = midCircle,
                            end = startCircle,
                            moveType = MoveType.FEED,
                            feedRate = q207,
                            spindleSpeed = speed,
                            toolNumber = tool.number,
                            toolName = tool.name,
                            toolRadius = tool.radius,
                            effectiveRadius = effectiveRadius,
                            dl = dl,
                            dr = dr,
                            toolGeometry = tool.geometryType,
                            pointAngleDeg = tool.pointAngleDeg,
                            center = Point2D(cx, cy),
                            radius = rCur,
                            direction = Direction.CCW
                        )
                    )
                    prevPt = startCircle
                    if (rCur >= maxOrbit - 1e-3f) break
                    rCur = (rCur + stepOver).coerceAtMost(maxOrbit)
                }
                addMove(prevPt, Point3D(cx, cy, targetBottomZ), MoveType.FEED, q207)
                addMove(Point3D(cx, cy, targetBottomZ), Point3D(cx, cy, retractZ), MoveType.RAPID, 99999f)
                return retractZ
            }

            253 -> {
                // Slot milling (Sleuffrezen)
                val slotLen = cycle.qParams[218] ?: 40f
                val slotWidth = cycle.qParams[219] ?: 12f
                val angleDeg = cycle.qParams[374] ?: 0f
                val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                val halfTravel = ((slotLen - slotWidth) / 2f).coerceAtLeast(1f)
                val sideOffset = ((slotWidth / 2f) - effectiveRadius).coerceAtLeast(0f)

                val ux = cos(angleRad)
                val uy = sin(angleRad)
                val nx = -uy
                val ny = ux

                val pA = Point3D(cx - ux * halfTravel, cy - uy * halfTravel, targetBottomZ)
                val pB = Point3D(cx + ux * halfTravel, cy + uy * halfTravel, targetBottomZ)

                addMove(Point3D(cx, cy, cz), Point3D(pA.x, pA.y, safeZ), MoveType.RAPID, 99999f)
                addMove(Point3D(pA.x, pA.y, safeZ), pA, MoveType.PLUNGE, q206)
                addMove(pA, pB, MoveType.FEED, q207)

                if (sideOffset > 0.2f) {
                    val pB1 = Point3D(pB.x + nx * sideOffset, pB.y + ny * sideOffset, targetBottomZ)
                    val pA1 = Point3D(pA.x + nx * sideOffset, pA.y + ny * sideOffset, targetBottomZ)
                    val pA2 = Point3D(pA.x - nx * sideOffset, pA.y - ny * sideOffset, targetBottomZ)
                    val pB2 = Point3D(pB.x - nx * sideOffset, pB.y - ny * sideOffset, targetBottomZ)
                    addMove(pB, pB1, MoveType.FEED, q207)
                    addMove(pB1, pA1, MoveType.FEED, q207)
                    addMove(pA1, pA2, MoveType.FEED, q207)
                    addMove(pA2, pB2, MoveType.FEED, q207)
                    addMove(pB2, pB, MoveType.FEED, q207)
                }
                addMove(pB, Point3D(pB.x, pB.y, retractZ), MoveType.RAPID, 99999f)
                return retractZ
            }

            else -> {
                addMove(Point3D(cx, cy, cz), Point3D(cx, cy, targetBottomZ), MoveType.PLUNGE, q206)
                addMove(Point3D(cx, cy, targetBottomZ), Point3D(cx, cy, retractZ), MoveType.RAPID, 99999f)
                return retractZ
            }
        }
    }

    private data class CallFrame(
        val returnPc: Int,
        val labelPc: Int,
        val remainingReps: Int
    )

    private fun calculateArcCenter(
        p1: Point2D,
        p2: Point2D,
        radius: Float,
        isCw: Boolean,
        longArc: Boolean = false
    ): Point2D {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val d = hypot(dx, dy)
        if (d < 1e-5f || d > 2f * radius) {
            return Point2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
        }
        val mx = (p1.x + p2.x) / 2f
        val my = (p1.y + p2.y) / 2f
        val h = sqrt(max(0f, radius * radius - (d / 2f) * (d / 2f)))

        val ux = -dy / d
        val uy = dx / d
        var sign = if (isCw) -1f else 1f
        if (longArc) sign = -sign
        return Point2D(mx + sign * ux * h, my + sign * uy * h)
    }

    private fun formatNum(v: Float): String =
        if (abs(v - v.roundToInt()) < 0.005f) v.roundToInt().toString()
        else String.format(java.util.Locale.US, "%.2f", v)
}

data class InterpretationResult(
    val segments: List<ToolpathSegment>,
    val diagnostics: List<TncDiagnostic>,
    val stock: StockBox,
    val qParameters: Map<Int, Float> = emptyMap(),
    val finalState: MachineState
)

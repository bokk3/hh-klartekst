package com.klartext.simulator.parser

import com.klartext.simulator.model.*
import kotlin.math.*

object ZMapSimulator {

    /**
     * Generates a 2.5D heightmap over the stock bounding box up to [maxStepIndex].
     * Grid resolution is ~110x110 cells for smooth real-time rendering on Android.
     */
    fun computeHeightfield(
        stock: StockBox,
        segments: List<ToolpathSegment>,
        maxStepIndex: Int,
        gridResolution: Int = 100
    ): ZMapHeightfield {
        val width = stock.width
        val height = stock.height
        val aspect = (width / height).coerceIn(0.4f, 2.5f)

        val cols = if (aspect >= 1f) gridResolution else (gridResolution * aspect).roundToInt().coerceAtLeast(40)
        val rows = if (aspect <= 1f) gridResolution else (gridResolution / aspect).roundToInt().coerceAtLeast(40)

        val minX = stock.min.x
        val minY = stock.min.y
        val maxX = stock.max.x
        val maxY = stock.max.y
        val minZ = stock.min.z
        val maxZ = stock.max.z

        val heights = FloatArray(cols * rows) { maxZ }

        val cellW = (maxX - minX) / cols
        val cellH = (maxY - minY) / rows
        val stepLen = min(cellW, cellH) * 0.65f

        val count = maxStepIndex.coerceIn(0, segments.size)
        for (i in 0 until count) {
            val seg = segments[i]
            if (seg.moveType == MoveType.RAPID) continue

            // Quick reject if entire segment is above stock top
            val minSegZ = min(seg.start.z, seg.end.z) + seg.dl
            if (minSegZ >= maxZ) continue

            when (seg) {
                is ToolpathSegment.Linear -> {
                    carveLinear(
                        seg = seg,
                        heights = heights,
                        cols = cols,
                        rows = rows,
                        minX = minX,
                        minY = minY,
                        cellW = cellW,
                        cellH = cellH,
                        stepLen = stepLen,
                        stockMinZ = minZ
                    )
                }
                is ToolpathSegment.Arc -> {
                    carveArc(
                        seg = seg,
                        heights = heights,
                        cols = cols,
                        rows = rows,
                        minX = minX,
                        minY = minY,
                        cellW = cellW,
                        cellH = cellH,
                        stepLen = stepLen,
                        stockMinZ = minZ
                    )
                }
            }
        }

        return ZMapHeightfield(
            cols = cols,
            rows = rows,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ,
            heights = heights
        )
    }

    private fun carveLinear(
        seg: ToolpathSegment,
        heights: FloatArray,
        cols: Int,
        rows: Int,
        minX: Float,
        minY: Float,
        cellW: Float,
        cellH: Float,
        stepLen: Float,
        stockMinZ: Float
    ) {
        val distXY = hypot(seg.end.x - seg.start.x, seg.end.y - seg.start.y)
        val steps = max(1, (distXY / stepLen).ceilToInt())
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val cx = seg.start.x + t * (seg.end.x - seg.start.x)
            val cy = seg.start.y + t * (seg.end.y - seg.start.y)
            val cz = seg.start.z + t * (seg.end.z - seg.start.z)
            stampToolAt(
                cx = cx,
                cy = cy,
                zProg = cz,
                toolRadius = seg.toolRadius,
                dl = seg.dl,
                geometry = seg.toolGeometry,
                heights = heights,
                cols = cols,
                rows = rows,
                minX = minX,
                minY = minY,
                cellW = cellW,
                cellH = cellH,
                stockMinZ = stockMinZ
            )
        }
    }

    private fun carveArc(
        seg: ToolpathSegment.Arc,
        heights: FloatArray,
        cols: Int,
        rows: Int,
        minX: Float,
        minY: Float,
        cellW: Float,
        cellH: Float,
        stepLen: Float,
        stockMinZ: Float
    ) {
        val startAngle = atan2(seg.start.y - seg.center.y, seg.start.x - seg.center.x)
        var endAngle = atan2(seg.end.y - seg.center.y, seg.end.x - seg.center.x)

        if (seg.direction == Direction.CW && endAngle >= startAngle - 1e-4f) {
            endAngle -= (2.0 * Math.PI).toFloat()
        } else if (seg.direction == Direction.CCW && endAngle <= startAngle + 1e-4f) {
            endAngle += (2.0 * Math.PI).toFloat()
        }

        val arcLen = abs(endAngle - startAngle) * seg.radius
        val steps = max(12, (arcLen / stepLen).ceilToInt())

        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val angle = startAngle + t * (endAngle - startAngle)
            val cx = seg.center.x + seg.radius * cos(angle)
            val cy = seg.center.y + seg.radius * sin(angle)
            val cz = seg.start.z + t * (seg.end.z - seg.start.z)
            stampToolAt(
                cx = cx,
                cy = cy,
                zProg = cz,
                toolRadius = seg.toolRadius,
                dl = seg.dl,
                geometry = seg.toolGeometry,
                heights = heights,
                cols = cols,
                rows = rows,
                minX = minX,
                minY = minY,
                cellW = cellW,
                cellH = cellH,
                stockMinZ = stockMinZ
            )
        }
    }

    private fun stampToolAt(
        cx: Float,
        cy: Float,
        zProg: Float,
        toolRadius: Float,
        dl: Float,
        geometry: ToolGeometryType,
        heights: FloatArray,
        cols: Int,
        rows: Int,
        minX: Float,
        minY: Float,
        cellW: Float,
        cellH: Float,
        stockMinZ: Float
    ) {
        val rTool = toolRadius.coerceAtLeast(0.5f)
        val zTipPhysical = zProg + dl

        val minCol = ((cx - rTool - minX) / cellW).floorToInt().coerceAtLeast(0)
        val maxCol = ((cx + rTool - minX) / cellW).ceilToInt().coerceAtMost(cols - 1)
        val minRow = ((cy - rTool - minY) / cellH).floorToInt().coerceAtLeast(0)
        val maxRow = ((cy + rTool - minY) / cellH).ceilToInt().coerceAtMost(rows - 1)

        val rSq = rTool * rTool

        for (row in minRow..maxRow) {
            val cellY = minY + (row + 0.5f) * cellH
            val dy = cellY - cy
            val dySq = dy * dy
            val rowOffset = row * cols

            for (col in minCol..maxCol) {
                val cellX = minX + (col + 0.5f) * cellW
                val dx = cellX - cx
                val dSq = dx * dx + dySq
                if (dSq <= rSq) {
                    val zCut = when (geometry) {
                        ToolGeometryType.CHAMFER_MILL_45,
                        ToolGeometryType.SPOT_DRILL_90 -> {
                            // 90-deg conical tip (45 deg per side): Z(r) = zTipPhysical + r
                            val r = sqrt(dSq)
                            zTipPhysical + r
                        }
                        else -> {
                            // Flat cylindrical endmill / drill / tap
                            zTipPhysical
                        }
                    }.coerceAtLeast(stockMinZ - 5f)

                    val idx = rowOffset + col
                    if (zCut < heights[idx]) {
                        heights[idx] = zCut
                    }
                }
            }
        }
    }

    private fun Float.ceilToInt(): Int = ceil(this).toInt()
    private fun Float.floorToInt(): Int = floor(this).toInt()
}

package com.klartext.simulator.parser

import com.klartext.simulator.model.Point2D
import com.klartext.simulator.model.RadiusComp
import java.util.Locale
import kotlin.math.*

sealed class DxfEntity {
    data class Line(
        val start: Point2D,
        val end: Point2D
    ) : DxfEntity()

    data class Arc(
        val center: Point2D,
        val radius: Float,
        val startAngleDeg: Float,
        val endAngleDeg: Float
    ) : DxfEntity() {
        val startPoint: Point2D
            get() {
                val rad = Math.toRadians(startAngleDeg.toDouble()).toFloat()
                return Point2D(center.x + radius * cos(rad), center.y + radius * sin(rad))
            }
        val endPoint: Point2D
            get() {
                val rad = Math.toRadians(endAngleDeg.toDouble()).toFloat()
                return Point2D(center.x + radius * cos(rad), center.y + radius * sin(rad))
            }
    }

    data class Circle(
        val center: Point2D,
        val radius: Float
    ) : DxfEntity()
}

object DxfParser {

    /**
     * Parses the ENTITIES section of a 2D ASCII DXF file.
     */
    fun parse(dxfContent: String): List<DxfEntity> {
        val lines = dxfContent.lines().map { it.trim() }
        val entities = mutableListOf<DxfEntity>()

        var inEntities = false
        var i = 0

        while (i < lines.size) {
            val code = lines[i]
            val value = lines.getOrNull(i + 1) ?: ""

            if (code == "2" && value.equals("ENTITIES", ignoreCase = true)) {
                inEntities = true
                i += 2
                continue
            }
            if (code == "0" && (value.equals("ENDSEC", ignoreCase = true) || value.equals("EOF", ignoreCase = true))) {
                if (inEntities) break
            }

            if (inEntities && code == "0") {
                when (value.uppercase(Locale.ROOT)) {
                    "LINE" -> {
                        val (lineEntity, nextIdx) = parseLine(lines, i + 2)
                        if (lineEntity != null) entities.add(lineEntity)
                        i = nextIdx
                        continue
                    }
                    "ARC" -> {
                        val (arcEntity, nextIdx) = parseArc(lines, i + 2)
                        if (arcEntity != null) entities.add(arcEntity)
                        i = nextIdx
                        continue
                    }
                    "CIRCLE" -> {
                        val (circleEntity, nextIdx) = parseCircle(lines, i + 2)
                        if (circleEntity != null) entities.add(circleEntity)
                        i = nextIdx
                        continue
                    }
                    "LWPOLYLINE" -> {
                        val (polyEntities, nextIdx) = parseLwPolyline(lines, i + 2)
                        entities.addAll(polyEntities)
                        i = nextIdx
                        continue
                    }
                }
            }
            i += 2
        }

        return entities
    }

    private fun parseLine(lines: List<String>, startIdx: Int): Pair<DxfEntity.Line?, Int> {
        var x1 = 0f; var y1 = 0f; var x2 = 0f; var y2 = 0f
        var i = startIdx
        while (i < lines.size && lines[i] != "0") {
            val code = lines[i]
            val value = lines.getOrNull(i + 1) ?: ""
            when (code) {
                "10" -> x1 = value.toFloatOrNull() ?: x1
                "20" -> y1 = value.toFloatOrNull() ?: y1
                "11" -> x2 = value.toFloatOrNull() ?: x2
                "21" -> y2 = value.toFloatOrNull() ?: y2
            }
            i += 2
        }
        return Pair(DxfEntity.Line(Point2D(x1, y1), Point2D(x2, y2)), i)
    }

    private fun parseArc(lines: List<String>, startIdx: Int): Pair<DxfEntity.Arc?, Int> {
        var cx = 0f; var cy = 0f; var r = 0f
        var startAngle = 0f; var endAngle = 360f
        var i = startIdx
        while (i < lines.size && lines[i] != "0") {
            val code = lines[i]
            val value = lines.getOrNull(i + 1) ?: ""
            when (code) {
                "10" -> cx = value.toFloatOrNull() ?: cx
                "20" -> cy = value.toFloatOrNull() ?: cy
                "40" -> r = value.toFloatOrNull() ?: r
                "50" -> startAngle = value.toFloatOrNull() ?: startAngle
                "51" -> endAngle = value.toFloatOrNull() ?: endAngle
            }
            i += 2
        }
        return Pair(DxfEntity.Arc(Point2D(cx, cy), r, startAngle, endAngle), i)
    }

    private fun parseCircle(lines: List<String>, startIdx: Int): Pair<DxfEntity.Circle?, Int> {
        var cx = 0f; var cy = 0f; var r = 0f
        var i = startIdx
        while (i < lines.size && lines[i] != "0") {
            val code = lines[i]
            val value = lines.getOrNull(i + 1) ?: ""
            when (code) {
                "10" -> cx = value.toFloatOrNull() ?: cx
                "20" -> cy = value.toFloatOrNull() ?: cy
                "40" -> r = value.toFloatOrNull() ?: r
            }
            i += 2
        }
        return Pair(DxfEntity.Circle(Point2D(cx, cy), r), i)
    }

    private fun parseLwPolyline(lines: List<String>, startIdx: Int): Pair<List<DxfEntity>, Int> {
        val points = mutableListOf<Point2D>()
        var isClosed = false
        var currentX: Float? = null
        var i = startIdx

        while (i < lines.size && lines[i] != "0") {
            val code = lines[i]
            val value = lines.getOrNull(i + 1) ?: ""
            when (code) {
                "70" -> {
                    val flags = value.toIntOrNull() ?: 0
                    isClosed = (flags and 1) == 1
                }
                "10" -> currentX = value.toFloatOrNull()
                "20" -> {
                    val currentY = value.toFloatOrNull()
                    if (currentX != null && currentY != null) {
                        points.add(Point2D(currentX, currentY))
                        currentX = null
                    }
                }
            }
            i += 2
        }

        val entities = mutableListOf<DxfEntity>()
        for (idx in 0 until points.size - 1) {
            entities.add(DxfEntity.Line(points[idx], points[idx + 1]))
        }
        if (isClosed && points.size > 2) {
            entities.add(DxfEntity.Line(points.last(), points.first()))
        }
        return Pair(entities, i)
    }
}

object DxfToKlartextConverter {

    data class ConversionConfig(
        val programName: String = "CAD_IMPORT",
        val toolName: String = "FREES16",
        val spindleSpeed: Int = 3500,
        val feedRate: Int = 1000,
        val plungeFeed: Int = 400,
        val cuttingDepth: Float = -10.0f,
        val safeZ: Float = 50.0f,
        val comp: RadiusComp = RadiusComp.RL,
        val leadRadius: Float = 10.0f
    )

    /**
     * Converts DXF entities into a fully formatted HEIDENHAIN Klartekst program.
     */
    fun convert(entities: List<DxfEntity>, config: ConversionConfig): String {
        if (entities.isEmpty()) return "; GEEN 2D ELEMENTEN GEVONDEN IN DXF"

        // 1. Calculate workpiece blank (BLK FORM) bounds
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE

        fun updateBounds(p: Point2D) {
            minX = min(minX, p.x); minY = min(minY, p.y)
            maxX = max(maxX, p.x); maxY = max(maxY, p.y)
        }

        for (e in entities) {
            when (e) {
                is DxfEntity.Line -> { updateBounds(e.start); updateBounds(e.end) }
                is DxfEntity.Arc -> {
                    updateBounds(e.startPoint); updateBounds(e.endPoint)
                    updateBounds(Point2D(e.center.x - e.radius, e.center.y))
                    updateBounds(Point2D(e.center.x + e.radius, e.center.y))
                    updateBounds(Point2D(e.center.x, e.center.y - e.radius))
                    updateBounds(Point2D(e.center.x, e.center.y + e.radius))
                }
                is DxfEntity.Circle -> {
                    updateBounds(Point2D(e.center.x - e.radius, e.center.y - e.radius))
                    updateBounds(Point2D(e.center.x + e.radius, e.center.y + e.radius))
                }
            }
        }

        // Add 5mm padding to stock
        val stockMinX = (floor((minX - 5f) / 5f) * 5f)
        val stockMinY = (floor((minY - 5f) / 5f) * 5f)
        val stockMaxX = (ceil((maxX + 5f) / 5f) * 5f)
        val stockMaxY = (ceil((maxY + 5f) / 5f) * 5f)
        val stockMinZ = (floor((config.cuttingDepth - 10f) / 5f) * 5f)

        // 2. Chain entities into continuous contours
        val chained = chainEntities(entities)

        val sb = StringBuilder()
        var lineNum = 0

        fun emit(cmd: String) {
            sb.append("$lineNum $cmd\n")
            lineNum++
        }

        emit("BEGIN PGM ${config.programName.uppercase()} MM")
        emit("; --- DXF CAD IMPORT ---")
        emit(String.format(Locale.US, "BLK FORM 0.1 Z X%+1.1f Y%+1.1f Z%+1.1f", stockMinX, stockMinY, stockMinZ))
        emit(String.format(Locale.US, "BLK FORM 0.2 X%+1.1f Y%+1.1f Z+0", stockMaxX, stockMaxY))
        emit("TOOL CALL \"${config.toolName}\" Z S${config.spindleSpeed} F${config.feedRate}")
        emit(String.format(Locale.US, "L Z%+1.1f R0 FMAX", config.safeZ))

        for (chain in chained) {
            val firstEntity = chain.firstOrNull() ?: continue
            val startPt = when (firstEntity) {
                is DxfEntity.Line -> firstEntity.start
                is DxfEntity.Arc -> firstEntity.startPoint
                is DxfEntity.Circle -> Point2D(firstEntity.center.x + firstEntity.radius, firstEntity.center.y)
            }

            // Lead-in approach vector
            val leadVector = Point2D(-config.leadRadius * 1.5f, -config.leadRadius * 1.5f)
            val preStart = startPt + leadVector

            emit("; --- CONTOUR ---")
            emit(String.format(Locale.US, "L X%+1.3f Y%+1.3f R0 FMAX", preStart.x, preStart.y))
            emit("L Z+2 R0 FMAX")
            emit(String.format(Locale.US, "L Z%+1.3f R0 F%d M3", config.cuttingDepth, config.plungeFeed))
            emit(String.format(Locale.US, "APPR LCT X%+1.3f Y%+1.3f R%1.1f %s F%d",
                startPt.x, startPt.y, config.leadRadius, config.comp.name, config.feedRate))

            var currentPoint = startPt
            for (elem in chain) {
                when (elem) {
                    is DxfEntity.Line -> {
                        emit(String.format(Locale.US, "L X%+1.3f Y%+1.3f", elem.end.x, elem.end.y))
                        currentPoint = elem.end
                    }
                    is DxfEntity.Arc -> {
                        // In DXF angles are CCW (DR+)
                        val dirStr = "DR+"
                        emit(String.format(Locale.US, "CR X%+1.3f Y%+1.3f R%+1.3f %s",
                            elem.endPoint.x, elem.endPoint.y, elem.radius, dirStr))
                        currentPoint = elem.endPoint
                    }
                    is DxfEntity.Circle -> {
                        emit(String.format(Locale.US, "CC X%+1.3f Y%+1.3f", elem.center.x, elem.center.y))
                        emit("C X+0 Y+0 DR+ RL")
                    }
                }
            }

            emit(String.format(Locale.US, "DEP LCT X%+1.3f Y%+1.3f R%1.1f F%d",
                preStart.x, preStart.y, config.leadRadius, config.feedRate))
            emit(String.format(Locale.US, "L Z%+1.1f R0 FMAX", config.safeZ))
        }

        emit(String.format(Locale.US, "L Z%+1.1f R0 FMAX M30", config.safeZ))
        emit("END PGM ${config.programName.uppercase()} MM")

        return sb.toString().trimEnd()
    }

    /**
     * Chains unsorted lines and arcs into sequential connected contours.
     */
    private fun chainEntities(entities: List<DxfEntity>): List<List<DxfEntity>> {
        val remaining = entities.toMutableList()
        val chains = mutableListOf<List<DxfEntity>>()

        while (remaining.isNotEmpty()) {
            val currentChain = mutableListOf<DxfEntity>()
            var current = remaining.removeAt(0)
            currentChain.add(current)

            var endPt = getEndPoint(current)

            while (remaining.isNotEmpty()) {
                val nextIdx = remaining.indexOfFirst { cand ->
                    val startCand = getStartPoint(cand)
                    val endCand = getEndPoint(cand)
                    hypot(candStartDist(cand, endPt).x, candStartDist(cand, endPt).y) < 0.05f ||
                            hypot(candEndDist(cand, endPt).x, candEndDist(cand, endPt).y) < 0.05f
                }

                if (nextIdx >= 0) {
                    val nextEntity = remaining.removeAt(nextIdx)
                    val distStart = hypot(getStartPoint(nextEntity).x - endPt.x, getStartPoint(nextEntity).y - endPt.y)
                    val distEnd = hypot(getEndPoint(nextEntity).x - endPt.x, getEndPoint(nextEntity).y - endPt.y)

                    val oriented = if (distEnd < distStart && nextEntity is DxfEntity.Line) {
                        // Reverse line direction
                        DxfEntity.Line(nextEntity.end, nextEntity.start)
                    } else {
                        nextEntity
                    }

                    currentChain.add(oriented)
                    endPt = getEndPoint(oriented)
                } else {
                    break
                }
            }
            chains.add(currentChain)
        }

        return chains
    }

    private fun getStartPoint(e: DxfEntity): Point2D = when (e) {
        is DxfEntity.Line -> e.start
        is DxfEntity.Arc -> e.startPoint
        is DxfEntity.Circle -> Point2D(e.center.x + e.radius, e.center.y)
    }

    private fun getEndPoint(e: DxfEntity): Point2D = when (e) {
        is DxfEntity.Line -> e.end
        is DxfEntity.Arc -> e.endPoint
        is DxfEntity.Circle -> Point2D(e.center.x + e.radius, e.center.y)
    }

    private fun candStartDist(cand: DxfEntity, pt: Point2D): Point2D {
        val sp = getStartPoint(cand)
        return Point2D(sp.x - pt.x, sp.y - pt.y)
    }

    private fun candEndDist(cand: DxfEntity, pt: Point2D): Point2D {
        val ep = getEndPoint(cand)
        return Point2D(ep.x - pt.x, ep.y - pt.y)
    }
}

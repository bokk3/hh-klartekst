package com.klartext.simulator.model

import kotlin.math.*

data class Point3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    fun distanceTo(other: Point3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun to2D(): Point2D = Point2D(x, y)
}

data class Point2D(
    val x: Float = 0f,
    val y: Float = 0f
) {
    fun distanceTo(other: Point2D): Float = hypot(x - other.x, y - other.y)
    operator fun plus(other: Point2D) = Point2D(x + other.x, y + other.y)
    operator fun minus(other: Point2D) = Point2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Point2D(x * scalar, y * scalar)

    fun normalized(): Point2D {
        val len = hypot(x, y)
        return if (len > 1e-6f) Point2D(x / len, y / len) else Point2D(0f, 0f)
    }

    /** Left normal (-y, x) for RL */
    fun leftNormal(): Point2D {
        val n = normalized()
        return Point2D(-n.y, n.x)
    }

    /** Right normal (y, -x) for RR */
    fun rightNormal(): Point2D {
        val n = normalized()
        return Point2D(n.y, -n.x)
    }
}

data class StockBox(
    val min: Point3D = Point3D(0f, 0f, -25f),
    val max: Point3D = Point3D(120f, 120f, 0f)
) {
    val width: Float get() = (max.x - min.x).coerceAtLeast(1f)
    val height: Float get() = (max.y - min.y).coerceAtLeast(1f)
    val depth: Float get() = (max.z - min.z).coerceAtLeast(1f)
    val centerX: Float get() = (min.x + max.x) / 2f
    val centerY: Float get() = (min.y + max.y) / 2f
}

enum class MoveType {
    RAPID,   // FMAX
    FEED,    // F...
    PLUNGE   // Z movement down into stock
}

enum class Direction {
    CW,  // DR- (Clockwise)
    CCW  // DR+ (Counter Clockwise)
}

enum class DiagnosticSeverity {
    ERROR,        // Blokkeert/stopt uitvoering op TNC 640
    STUDENT_HINT, // Educatieve hint (bijv. R0 vs NO ENT valkuil)
    WARNING       // Waarschuwing (bijv. Tap-voeding S * spoed)
}

enum class TncErrorCode {
    TOOL_RADIUS_TOO_LARGE,
    BORE_MILLING_DIAMETER_TOO_LARGE,
    CIRCLE_ENDPOINT_MISMATCH,
    R0_CANCELS_MODAL_COMPENSATION,
    TAP_FEEDRATE_MISMATCH,
    LABEL_NOT_FOUND,
    SYNTAX_ERROR,
    TOOL_HOLDER_COLLISION
}

data class TncDiagnostic(
    val lineNumber: Int,
    val code: TncErrorCode,
    val severity: DiagnosticSeverity,
    val title: String,
    val message: String
)

sealed class ToolpathSegment {
    abstract val lineNumber: Int
    abstract val start: Point3D             // Equidistant cutter center start
    abstract val end: Point3D               // Equidistant cutter center end
    abstract val programmedStart: Point3D   // Workpiece contour start
    abstract val programmedEnd: Point3D     // Workpiece contour end
    abstract val moveType: MoveType
    abstract val feedRate: Float
    abstract val spindleSpeed: Float
    abstract val toolNumber: Int
    abstract val toolName: String
    abstract val toolRadius: Float          // Nominal R_tool
    abstract val effectiveRadius: Float     // R_eff = max(0, R_tool + DR)
    abstract val dl: Float
    abstract val dr: Float
    abstract val toolGeometry: ToolGeometryType
    abstract val pointAngleDeg: Float
    abstract val radiusComp: RadiusComp

    data class Linear(
        override val lineNumber: Int,
        override val start: Point3D,
        override val end: Point3D,
        override val programmedStart: Point3D = start,
        override val programmedEnd: Point3D = end,
        override val moveType: MoveType,
        override val feedRate: Float,
        override val spindleSpeed: Float = 3000f,
        override val toolNumber: Int,
        override val toolName: String = "T$toolNumber",
        override val toolRadius: Float,
        override val effectiveRadius: Float = toolRadius,
        override val dl: Float = 0f,
        override val dr: Float = 0f,
        override val toolGeometry: ToolGeometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
        override val pointAngleDeg: Float = 180f,
        override val radiusComp: RadiusComp = RadiusComp.R0
    ) : ToolpathSegment()

    data class Arc(
        override val lineNumber: Int,
        override val start: Point3D,
        override val end: Point3D,
        override val programmedStart: Point3D = start,
        override val programmedEnd: Point3D = end,
        override val moveType: MoveType,
        override val feedRate: Float,
        override val spindleSpeed: Float = 3000f,
        override val toolNumber: Int,
        override val toolName: String = "T$toolNumber",
        override val toolRadius: Float,
        override val effectiveRadius: Float = toolRadius,
        override val dl: Float = 0f,
        override val dr: Float = 0f,
        override val toolGeometry: ToolGeometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
        override val pointAngleDeg: Float = 180f,
        override val radiusComp: RadiusComp = RadiusComp.R0,
        val center: Point2D,
        val radius: Float,                  // Equidistant cutter path radius
        val programmedRadius: Float = radius,
        val direction: Direction
    ) : ToolpathSegment()
}

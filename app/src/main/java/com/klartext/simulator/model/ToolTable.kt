package com.klartext.simulator.model

import java.util.Locale

enum class ToolGeometryType {
    CYLINDRICAL_ENDMILL, // Vlakke cilinderfrees (90° wand, vlakke bodem)
    CHAMFER_MILL_45,     // 45° Kantenfrees / afschuinfrees (90° punt)
    SPOT_DRILL_90,       // 90° Centerboor
    TWIST_DRILL,         // Spiraalboor / VHM boor
    TAP                  // Schroefdraadtap (met spoed)
}

data class TncTool(
    val number: Int,
    val name: String,
    val diameter: Float,
    val length: Float = 100f,
    val geometryType: ToolGeometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
    val pointAngleDeg: Float = 180f,
    val pitchMm: Float? = null,
    val teeth: Int = 4,
    val description: String = ""
) {
    val radius: Float get() = diameter / 2f
}

object ToolTable {

    val defaultTools: List<TncTool> = listOf(
        TncTool(
            number = 1,
            name = "VLAKFREES63",
            diameter = 63.0f,
            length = 120f,
            geometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
            teeth = 6,
            description = "Vlakfrees Ø63 mm (6 snijkanten)"
        ),
        TncTool(
            number = 2,
            name = "FREES25",
            diameter = 25.0f,
            length = 100f,
            geometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
            teeth = 4,
            description = "Schachtfrees Ø25 mm (R=12.5 mm)"
        ),
        TncTool(
            number = 3,
            name = "FREES16",
            diameter = 16.0f,
            length = 90f,
            geometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
            teeth = 4,
            description = "VHM Schachtfrees Ø16 mm (R=8.0 mm)"
        ),
        TncTool(
            number = 4,
            name = "FREES10",
            diameter = 10.0f,
            length = 80f,
            geometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
            teeth = 4,
            description = "VHM Schachtfrees Ø10 mm (R=5.0 mm)"
        ),
        TncTool(
            number = 5,
            name = "FREES8",
            diameter = 8.0f,
            length = 75f,
            geometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
            teeth = 4,
            description = "VHM Schachtfrees Ø8 mm (R=4.0 mm)"
        ),
        TncTool(
            number = 6,
            name = "FREES6",
            diameter = 6.0f,
            length = 65f,
            geometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
            teeth = 3,
            description = "VHM Schachtfrees Ø6 mm (R=3.0 mm)"
        ),
        TncTool(
            number = 7,
            name = "CENTERBOOR",
            diameter = 12.0f,
            length = 70f,
            geometryType = ToolGeometryType.SPOT_DRILL_90,
            pointAngleDeg = 90f,
            teeth = 2,
            description = "NC Centerboor 90° Ø12 mm"
        ),
        TncTool(
            number = 8,
            name = "BOOR6",
            diameter = 6.0f,
            length = 85f,
            geometryType = ToolGeometryType.TWIST_DRILL,
            pointAngleDeg = 118f,
            teeth = 2,
            description = "Spiraalboor Ø6.0 mm"
        ),
        TncTool(
            number = 9,
            name = "VHMBOOR6.8",
            diameter = 6.8f,
            length = 85f,
            geometryType = ToolGeometryType.TWIST_DRILL,
            pointAngleDeg = 140f,
            teeth = 2,
            description = "VHM Kerngatboor Ø6.8 mm (voor M8)"
        ),
        TncTool(
            number = 10,
            name = "TAPM8",
            diameter = 8.0f,
            length = 90f,
            geometryType = ToolGeometryType.TAP,
            pitchMm = 1.25f,
            teeth = 3,
            description = "Machinetap M8 × 1.25 mm"
        ),
        TncTool(
            number = 11,
            name = "KANTENFREES6",
            diameter = 6.0f,
            length = 60f,
            geometryType = ToolGeometryType.CHAMFER_MILL_45,
            pointAngleDeg = 90f,
            teeth = 4,
            description = "Kantenfrees 45° (90° punt) Ø6 mm"
        )
    )

    fun findByNumber(number: Int): TncTool? =
        defaultTools.firstOrNull { it.number == number }

    fun findByName(name: String): TncTool? {
        val clean = name.trim().removeSurrounding("\"").uppercase(Locale.ROOT)
        return defaultTools.firstOrNull { it.name.uppercase(Locale.ROOT) == clean }
            ?: defaultTools.firstOrNull { clean.contains(it.name.uppercase(Locale.ROOT)) }
    }

    fun resolve(identifier: String): TncTool {
        val clean = identifier.trim().removeSurrounding("\"")
        clean.toIntOrNull()?.let { num ->
            findByNumber(num)?.let { return it }
        }
        findByName(clean)?.let { return it }
        return defaultTools[3] // Default to FREES10 if unknown
    }
}

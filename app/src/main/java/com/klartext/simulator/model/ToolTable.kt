package com.klartext.simulator.model

import java.util.Locale
import kotlin.math.*

enum class ToolGeometryType {
    CYLINDRICAL_ENDMILL, // Vlakke cilinderfrees (90° wand, vlakke bodem)
    TORUS_MILL,          // Torische frees / hoekradiusfrees (vlakke bodem + corner radius R)
    BALL_NOSE,           // Volradius bolfrees voor 3D vormfrezen
    FACE_MILL,           // Wisselplaat-vlakfrees (b.v. Ø63mm)
    CHAMFER_MILL_45,     // 45° Kantenfrees / afschuinfrees (90° punt)
    SPOT_DRILL_90,       // 90° NC-centerboor
    TWIST_DRILL,         // Spiraalboor / VHM boor
    TAP,                 // Schroefdraadtap (met spoed)
    THREAD_MILL,         // Draadfrees (helixinterpolatie)
    TOUCH_PROBE          // 3D taster (Heidenhain TS 740 / Renishaw OMP60)
}

enum class MachinePreset(val displayName: String, val holderStandard: String, val pocketCount: Int) {
    DMU_50("DMG MORI DMU 50 (5-Assig)", "HSK-A63", 30),
    DVF_5000("DN Solutions DVF 5000 (5-Assig)", "BT40 / BBT40", 40),
    STANDARD_WORKSHOP("Standaard Werkplaats", "SK40", 11)
}

data class ToolHolder(
    val type: String = "HSK-A63",
    val colletDiameter: Float = 50f, // mm
    val colletLength: Float = 30f,   // mm
    val flangeDiameter: Float = 63f, // mm
    val gaugeLength: Float = 60f     // mm
)

data class TncTool(
    val number: Int,
    val name: String,
    val diameter: Float,
    val length: Float = 100f,
    val geometryType: ToolGeometryType = ToolGeometryType.CYLINDRICAL_ENDMILL,
    val cornerRadius: Float = 0f,
    val pointAngleDeg: Float = 180f,
    val pitchMm: Float? = null,
    val teeth: Int = 4,
    val cuttingSpeedVc: Float = 160f, // m/min
    val feedPerToothFz: Float = 0.08f, // mm/tooth
    val description: String = "",
    val holder: ToolHolder = ToolHolder()
) {
    val radius: Float get() = diameter / 2f
    val stickOut: Float get() = (length * 0.55f).coerceIn(35f, 85f)

    /**
     * Berekent het aanbevolen spiltoerental: N = (Vc * 1000) / (pi * D)
     */
    val recommendedRpm: Int get() {
        if (diameter <= 0.1f) return 1000
        return ((cuttingSpeedVc * 1000f) / (PI.toFloat() * diameter)).roundToInt().coerceIn(100, 20000)
    }

    /**
     * Berekent de aanbevolen voeding: F = teeth * fz * N
     */
    val recommendedFeed: Int get() {
        return (teeth * feedPerToothFz * recommendedRpm).roundToInt().coerceIn(20, 15000)
    }
}

object ToolTable {

    // --- 1. DMG MORI DMU 50 (HSK-A63, 30 POCKETS) ---
    val dmu50Tools: List<TncTool> = listOf(
        TncTool(1, "VLAKFREES63", 63.0f, 120f, ToolGeometryType.FACE_MILL, teeth = 5, cuttingSpeedVc = 240f, feedPerToothFz = 0.18f, description = "Wisselplaat-vlakfrees Ø63mm (5 wisselplaten)", holder = ToolHolder("HSK-A63", 63f, 25f, 63f, 40f)),
        TncTool(2, "FREES32_WPL", 32.0f, 130f, ToolGeometryType.TORUS_MILL, cornerRadius = 2.0f, teeth = 3, cuttingSpeedVc = 200f, feedPerToothFz = 0.15f, description = "Wisselplaat-hoekfrees Ø32 R2mm (Voorfrezen)", holder = ToolHolder("HSK-A63", 44f, 30f, 63f, 50f)),
        TncTool(3, "FREES20_VHM", 20.0f, 105f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, cuttingSpeedVc = 160f, feedPerToothFz = 0.10f, description = "VHM HPC Schachtfrees Ø20mm (Z=4)", holder = ToolHolder("HSK-A63", 50f, 32f, 63f, 60f)),
        TncTool(4, "FREES16_VHM", 16.0f, 95f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, cuttingSpeedVc = 160f, feedPerToothFz = 0.08f, description = "VHM Schachtfrees Ø16mm (Z=4)", holder = ToolHolder("HSK-A63", 42f, 30f, 63f, 55f)),
        TncTool(5, "FREES12_VHM", 12.0f, 85f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, cuttingSpeedVc = 170f, feedPerToothFz = 0.07f, description = "VHM Schachtfrees Ø12mm (Z=4)", holder = ToolHolder("HSK-A63", 35f, 28f, 63f, 50f)),
        TncTool(6, "FREES10_VHM", 10.0f, 80f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, cuttingSpeedVc = 170f, feedPerToothFz = 0.06f, description = "VHM Schachtfrees Ø10mm (Z=4)", holder = ToolHolder("HSK-A63", 30f, 26f, 63f, 50f)),
        TncTool(7, "FREES8_VHM", 8.0f, 75f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, cuttingSpeedVc = 180f, feedPerToothFz = 0.05f, description = "VHM Schachtfrees Ø8mm (Z=4)", holder = ToolHolder("HSK-A63", 26f, 24f, 63f, 45f)),
        TncTool(8, "FREES6_VHM", 6.0f, 65f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 3, cuttingSpeedVc = 180f, feedPerToothFz = 0.04f, description = "VHM Schachtfrees Ø6mm (Z=3)", holder = ToolHolder("HSK-A63", 22f, 22f, 63f, 45f)),
        TncTool(9, "BOLFREES12", 12.0f, 90f, ToolGeometryType.BALL_NOSE, cornerRadius = 6.0f, teeth = 2, cuttingSpeedVc = 200f, feedPerToothFz = 0.08f, description = "VHM Volradius-bolfrees Ø12 R6mm (3D Vormfrezen)", holder = ToolHolder("HSK-A63", 35f, 28f, 63f, 55f)),
        TncTool(10, "BOLFREES8", 8.0f, 80f, ToolGeometryType.BALL_NOSE, cornerRadius = 4.0f, teeth = 2, cuttingSpeedVc = 220f, feedPerToothFz = 0.06f, description = "VHM Volradius-bolfrees Ø8 R4mm (3D Contouren)", holder = ToolHolder("HSK-A63", 26f, 24f, 63f, 50f)),
        TncTool(11, "BOLFREES6", 6.0f, 70f, ToolGeometryType.BALL_NOSE, cornerRadius = 3.0f, teeth = 2, cuttingSpeedVc = 220f, feedPerToothFz = 0.04f, description = "VHM Volradius-bolfrees Ø6 R3mm (3D Fijnbewerken)", holder = ToolHolder("HSK-A63", 22f, 22f, 63f, 45f)),
        TncTool(12, "TORUS16_R2", 16.0f, 95f, ToolGeometryType.TORUS_MILL, cornerRadius = 2.0f, teeth = 4, cuttingSpeedVc = 170f, feedPerToothFz = 0.08f, description = "VHM Torusfrees Ø16 R2mm (Kamers/Wanden)", holder = ToolHolder("HSK-A63", 42f, 30f, 63f, 55f)),
        TncTool(13, "TORUS10_R1", 10.0f, 80f, ToolGeometryType.TORUS_MILL, cornerRadius = 1.0f, teeth = 4, cuttingSpeedVc = 170f, feedPerToothFz = 0.06f, description = "VHM Torusfrees Ø10 R1mm (Inwendige radii)", holder = ToolHolder("HSK-A63", 30f, 26f, 63f, 50f)),
        TncTool(14, "CENTERBOOR12", 12.0f, 70f, ToolGeometryType.SPOT_DRILL_90, pointAngleDeg = 90f, teeth = 2, cuttingSpeedVc = 100f, feedPerToothFz = 0.06f, description = "NC Centerboor 90° Ø12mm", holder = ToolHolder("HSK-A63", 35f, 25f, 63f, 45f)),
        TncTool(15, "VHMBOOR3.3", 3.3f, 65f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 80f, feedPerToothFz = 0.04f, description = "VHM Kerngatboor Ø3.3mm (voor M4)", holder = ToolHolder("HSK-A63", 22f, 22f, 63f, 40f)),
        TncTool(16, "VHMBOOR4.2", 4.2f, 70f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 85f, feedPerToothFz = 0.05f, description = "VHM Kerngatboor Ø4.2mm (voor M5)", holder = ToolHolder("HSK-A63", 22f, 22f, 63f, 45f)),
        TncTool(17, "VHMBOOR5.0", 5.0f, 75f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 90f, feedPerToothFz = 0.06f, description = "VHM Kerngatboor Ø5.0mm (voor M6)", holder = ToolHolder("HSK-A63", 26f, 24f, 63f, 45f)),
        TncTool(18, "VHMBOOR6.8", 6.8f, 85f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 95f, feedPerToothFz = 0.08f, description = "VHM Kerngatboor Ø6.8mm (voor M8)", holder = ToolHolder("HSK-A63", 26f, 24f, 63f, 50f)),
        TncTool(19, "VHMBOOR8.5", 8.5f, 90f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 100f, feedPerToothFz = 0.10f, description = "VHM Kerngatboor Ø8.5mm (voor M10)", holder = ToolHolder("HSK-A63", 30f, 26f, 63f, 50f)),
        TncTool(20, "VHMBOOR10.2", 10.2f, 95f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 100f, feedPerToothFz = 0.12f, description = "VHM Kerngatboor Ø10.2mm (voor M12)", holder = ToolHolder("HSK-A63", 30f, 26f, 63f, 55f)),
        TncTool(21, "TAP_M4", 4.0f, 70f, ToolGeometryType.TAP, pitchMm = 0.70f, teeth = 3, cuttingSpeedVc = 25f, feedPerToothFz = 0.70f, description = "Synchroontap M4 (Spoed 0.7mm)", holder = ToolHolder("HSK-A63", 22f, 22f, 63f, 45f)),
        TncTool(22, "TAP_M5", 5.0f, 75f, ToolGeometryType.TAP, pitchMm = 0.80f, teeth = 3, cuttingSpeedVc = 25f, feedPerToothFz = 0.80f, description = "Synchroontap M5 (Spoed 0.8mm)", holder = ToolHolder("HSK-A63", 22f, 22f, 63f, 45f)),
        TncTool(23, "TAP_M6", 6.0f, 80f, ToolGeometryType.TAP, pitchMm = 1.00f, teeth = 3, cuttingSpeedVc = 25f, feedPerToothFz = 1.00f, description = "Synchroontap M6 (Spoed 1.0mm)", holder = ToolHolder("HSK-A63", 26f, 24f, 63f, 50f)),
        TncTool(24, "TAP_M8", 8.0f, 85f, ToolGeometryType.TAP, pitchMm = 1.25f, teeth = 3, cuttingSpeedVc = 25f, feedPerToothFz = 1.25f, description = "Synchroontap M8 (Spoed 1.25mm)", holder = ToolHolder("HSK-A63", 26f, 24f, 63f, 50f)),
        TncTool(25, "TAP_M10", 10.0f, 90f, ToolGeometryType.TAP, pitchMm = 1.50f, teeth = 3, cuttingSpeedVc = 25f, feedPerToothFz = 1.50f, description = "Synchroontap M10 (Spoed 1.5mm)", holder = ToolHolder("HSK-A63", 30f, 26f, 63f, 55f)),
        TncTool(26, "TAP_M12", 12.0f, 95f, ToolGeometryType.TAP, pitchMm = 1.75f, teeth = 3, cuttingSpeedVc = 25f, feedPerToothFz = 1.75f, description = "Synchroontap M12 (Spoed 1.75mm)", holder = ToolHolder("HSK-A63", 35f, 28f, 63f, 55f)),
        TncTool(27, "KANTENFREES12", 12.0f, 75f, ToolGeometryType.CHAMFER_MILL_45, pointAngleDeg = 90f, teeth = 4, cuttingSpeedVc = 180f, feedPerToothFz = 0.06f, description = "Kantenfrees 45° Ø12mm (Ontbramen/Afschuinen)", holder = ToolHolder("HSK-A63", 35f, 28f, 63f, 50f)),
        TncTool(28, "KANTENFREES6", 6.0f, 60f, ToolGeometryType.CHAMFER_MILL_45, pointAngleDeg = 90f, teeth = 3, cuttingSpeedVc = 180f, feedPerToothFz = 0.04f, description = "Kantenfrees 45° Ø6mm (Precisie-afschuining)", holder = ToolHolder("HSK-A63", 22f, 22f, 63f, 45f)),
        TncTool(29, "DRAADFREES_M6_M12", 9.5f, 80f, ToolGeometryType.THREAD_MILL, teeth = 3, cuttingSpeedVc = 140f, feedPerToothFz = 0.04f, description = "VHM Draadfrees voor M6-M12 helixinterpolatie", holder = ToolHolder("HSK-A63", 30f, 26f, 63f, 50f)),
        TncTool(30, "3D_TASTER_TS740", 6.0f, 100f, ToolGeometryType.TOUCH_PROBE, teeth = 1, cuttingSpeedVc = 0f, feedPerToothFz = 0f, description = "Heidenhain TS 740 Werkstuktaster (Robijnkogel Ø6mm)", holder = ToolHolder("HSK-A63", 50f, 35f, 63f, 65f))
    )

    // --- 2. DN SOLUTIONS DVF 5000 (BT40, 40 POCKETS) ---
    val dvf5000Tools: List<TncTool> = dmu50Tools.map { tool ->
        tool.copy(holder = ToolHolder("BT40", tool.holder.colletDiameter, tool.holder.colletLength, 63f, tool.holder.gaugeLength))
    } + listOf(
        TncTool(31, "FREES4_VHM", 4.0f, 60f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 3, cuttingSpeedVc = 180f, feedPerToothFz = 0.03f, description = "VHM Micro Schachtfrees Ø4mm (BT40)", holder = ToolHolder("BT40", 20f, 20f, 63f, 45f)),
        TncTool(32, "FREES3_VHM", 3.0f, 55f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 3, cuttingSpeedVc = 180f, feedPerToothFz = 0.02f, description = "VHM Micro Schachtfrees Ø3mm (BT40)", holder = ToolHolder("BT40", 20f, 20f, 63f, 45f)),
        TncTool(33, "BOLFREES4", 4.0f, 60f, ToolGeometryType.BALL_NOSE, cornerRadius = 2.0f, teeth = 2, cuttingSpeedVc = 200f, feedPerToothFz = 0.03f, description = "VHM Micro Bolfrees Ø4 R2mm (BT40)", holder = ToolHolder("BT40", 20f, 20f, 63f, 45f)),
        TncTool(34, "RUWFREES16", 16.0f, 95f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, cuttingSpeedVc = 140f, feedPerToothFz = 0.12f, description = "VHM Schrob/Ruwfrees Ø16mm met spaanbrekers", holder = ToolHolder("BT40", 42f, 30f, 63f, 55f)),
        TncTool(35, "VHMBOOR14.0", 14.0f, 110f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 110f, feedPerToothFz = 0.15f, description = "VHM Boor Ø14.0mm met interne koeling", holder = ToolHolder("BT40", 35f, 28f, 63f, 60f)),
        TncTool(36, "VHMBOOR16.0", 16.0f, 115f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, cuttingSpeedVc = 110f, feedPerToothFz = 0.16f, description = "VHM Boor Ø16.0mm met interne koeling", holder = ToolHolder("BT40", 42f, 30f, 63f, 60f)),
        TncTool(37, "VERZINKBOOR16", 16.0f, 80f, ToolGeometryType.SPOT_DRILL_90, pointAngleDeg = 90f, teeth = 3, cuttingSpeedVc = 90f, feedPerToothFz = 0.08f, description = "90° Drie-snijder Verzinkfrees Ø16mm", holder = ToolHolder("BT40", 35f, 28f, 63f, 50f)),
        TncTool(38, "SCHIJFFREES", 60.0f, 70f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 8, cuttingSpeedVc = 150f, feedPerToothFz = 0.08f, description = "Groeffrees / Zaagfrees Ø60 x 3mm", holder = ToolHolder("BT40", 50f, 30f, 63f, 50f)),
        TncTool(39, "GRAVEERSTIFT60", 6.0f, 60f, ToolGeometryType.SPOT_DRILL_90, pointAngleDeg = 60f, teeth = 1, cuttingSpeedVc = 200f, feedPerToothFz = 0.02f, description = "VHM Graveerfrees 60° (Tekst en logo's)", holder = ToolHolder("BT40", 22f, 22f, 63f, 45f)),
        TncTool(40, "REINISHAW_OMP60", 6.0f, 105f, ToolGeometryType.TOUCH_PROBE, teeth = 1, cuttingSpeedVc = 0f, feedPerToothFz = 0f, description = "Renishaw OMP60 Optische Taster (BT40)", holder = ToolHolder("BT40", 50f, 35f, 63f, 65f))
    )

    // --- 3. STANDAARD WERKPLAATS (SK40, 11 TOOLS - VOOR TERUGWAARTSE COMPATIBILITEIT) ---
    val standardTools: List<TncTool> = listOf(
        TncTool(1, "VLAKFREES63", 63.0f, 120f, ToolGeometryType.FACE_MILL, teeth = 6, description = "Vlakfrees Ø63 mm (6 snijkanten)", holder = ToolHolder("SK40", 63f, 25f, 63f, 40f)),
        TncTool(2, "FREES25", 25.0f, 100f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, description = "Schachtfrees Ø25 mm (R=12.5 mm)", holder = ToolHolder("SK40", 50f, 30f, 63f, 55f)),
        TncTool(3, "FREES16", 16.0f, 90f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, description = "VHM Schachtfrees Ø16 mm (R=8.0 mm)", holder = ToolHolder("SK40", 42f, 30f, 63f, 50f)),
        TncTool(4, "FREES10", 10.0f, 80f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, description = "VHM Schachtfrees Ø10 mm (R=5.0 mm)", holder = ToolHolder("SK40", 30f, 26f, 63f, 50f)),
        TncTool(5, "FREES8", 8.0f, 75f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 4, description = "VHM Schachtfrees Ø8 mm (R=4.0 mm)", holder = ToolHolder("SK40", 26f, 24f, 63f, 45f)),
        TncTool(6, "FREES6", 6.0f, 65f, ToolGeometryType.CYLINDRICAL_ENDMILL, teeth = 3, description = "VHM Schachtfrees Ø6 mm (R=3.0 mm)", holder = ToolHolder("SK40", 22f, 22f, 63f, 45f)),
        TncTool(7, "CENTERBOOR", 12.0f, 70f, ToolGeometryType.SPOT_DRILL_90, pointAngleDeg = 90f, teeth = 2, description = "NC Centerboor 90° Ø12 mm", holder = ToolHolder("SK40", 35f, 25f, 63f, 45f)),
        TncTool(8, "BOOR6", 6.0f, 85f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 118f, teeth = 2, description = "Spiraalboor Ø6.0 mm", holder = ToolHolder("SK40", 22f, 22f, 63f, 45f)),
        TncTool(9, "VHMBOOR6.8", 6.8f, 85f, ToolGeometryType.TWIST_DRILL, pointAngleDeg = 140f, teeth = 2, description = "VHM Kerngatboor Ø6.8 mm (voor M8)", holder = ToolHolder("SK40", 26f, 24f, 63f, 45f)),
        TncTool(10, "TAPM8", 8.0f, 90f, ToolGeometryType.TAP, pitchMm = 1.25f, teeth = 3, description = "Machinetap M8 × 1.25 mm", holder = ToolHolder("SK40", 26f, 24f, 63f, 50f)),
        TncTool(11, "KANTENFREES6", 6.0f, 60f, ToolGeometryType.CHAMFER_MILL_45, pointAngleDeg = 90f, teeth = 4, description = "Kantenfrees 45° (90° punt) Ø6 mm", holder = ToolHolder("SK40", 22f, 22f, 63f, 45f))
    )

    // Default list exposed to UI and interpreter
    var activePreset: MachinePreset = MachinePreset.DMU_50
    val defaultTools: List<TncTool> get() = when (activePreset) {
        MachinePreset.DMU_50 -> dmu50Tools
        MachinePreset.DVF_5000 -> dvf5000Tools
        MachinePreset.STANDARD_WORKSHOP -> standardTools
    }

    fun findByNumber(number: Int): TncTool? {
        return defaultTools.firstOrNull { it.number == number }
            ?: dmu50Tools.firstOrNull { it.number == number }
            ?: dvf5000Tools.firstOrNull { it.number == number }
            ?: standardTools.firstOrNull { it.number == number }
    }

    fun findByName(name: String): TncTool? {
        val clean = name.trim().removeSurrounding("\"").uppercase(Locale.ROOT)
        // Direct match in active preset
        defaultTools.firstOrNull { it.name.uppercase(Locale.ROOT) == clean }?.let { return it }

        // Search all catalogs
        val allCatalogs = dmu50Tools + dvf5000Tools + standardTools
        allCatalogs.firstOrNull { it.name.uppercase(Locale.ROOT) == clean }?.let { return it }

        // Flexible prefix / substring matching (e.g. "FREES16" matches "FREES16_VHM")
        return allCatalogs.firstOrNull {
            val tName = it.name.uppercase(Locale.ROOT)
            tName.startsWith(clean) || clean.startsWith(tName) || tName.contains(clean) || clean.contains(tName)
        }
    }

    fun resolve(identifier: String): TncTool {
        val clean = identifier.trim().removeSurrounding("\"")
        clean.toIntOrNull()?.let { num ->
            findByNumber(num)?.let { return it }
        }
        findByName(clean)?.let { return it }
        return defaultTools[3] // Default to FREES16_VHM / T4
    }
}

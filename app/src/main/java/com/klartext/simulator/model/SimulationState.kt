package com.klartext.simulator.model

data class MachineState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 100f,
    val feedRate: Float = 0f,
    val spindleSpeed: Float = 0f,
    val activeTool: Int = 1,
    val toolName: String = "FREES10",
    val toolRadius: Float = 5f,
    val effectiveRadius: Float = 5f,
    val toolLength: Float = 80f,
    val dl: Float = 0f,
    val dr: Float = 0f,
    val radiusComp: RadiusComp = RadiusComp.R0,
    val spindleOn: Boolean = false,
    val activeLineNumber: Int = 0,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val isRapid: Boolean = false,
    val datumShift: Point3D = Point3D(0f, 0f, 0f)
)

enum class ViewMode {
    TOP_XY,      // 2D Bovenaanzicht XY (met geprogrammeerde contour + equidistante freesbaan)
    FRONT_XZ,    // 2D Vooraanzicht XZ
    SIDE_YZ,     // 2D Zijaanzicht YZ
    MATERIAL_25D,// 2.5D Materiaalafname Bovenaanzicht (TNC 640 kleuren)
    ISOMETRIC    // 2.5D Isometrisch Hoogteveld + Freesbanen
}

data class ZMapHeightfield(
    val cols: Int,
    val rows: Int,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float,
    val heights: FloatArray
) {
    fun getZ(col: Int, row: Int): Float {
        if (col !in 0 until cols || row !in 0 until rows) return maxZ
        return heights[row * cols + col]
    }
}

data class DialogStep(
    val promptLabel: String,
    val tokenPrefix: String,
    val defaultValue: String,
    val omitOnNoEnt: Boolean = true,
    val valueOnEntIfBlank: String? = null
)

data class ActiveDialogSession(
    val commandTitle: String,
    val baseCommand: String,
    val steps: List<DialogStep>,
    val currentStepIndex: Int = 0,
    val accumulatedTokens: List<String> = emptyList(),
    val currentInput: String = ""
) {
    val currentStep: DialogStep? get() = steps.getOrNull(currentStepIndex)
}

data class SimulationUiState(
    val programName: String = "CONTOURFREZEN_V4.H",
    val rawCode: String = "",
    val blocks: List<KlartextBlock> = emptyList(),
    val segments: List<ToolpathSegment> = emptyList(),
    val diagnostics: List<TncDiagnostic> = emptyList(),
    val stock: StockBox = StockBox(),
    val machineState: MachineState = MachineState(),
    val heightfield: ZMapHeightfield? = null,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val viewMode: ViewMode = ViewMode.TOP_XY,
    val showRapidMoves: Boolean = true,
    val showProgrammedContour: Boolean = true,
    val showToolRadius: Boolean = true,
    val showGrid: Boolean = true,
    val selectedBlockIndex: Int = 0,
    val clipboardLine: String? = null,
    val activeDialog: ActiveDialogSession? = null,
    val showCalculator: Boolean = false,
    val showDiagnosticsSheet: Boolean = false,
    val showToolTableDialog: Boolean = false
)

package com.klartext.simulator.model

enum class RadiusComp {
    R0,  // Centerline / geen compensatie
    RL,  // Gereedschap links van de contourrichting
    RR   // Gereedschap rechts van de contourrichting
}

enum class UnitType {
    MM,
    INCH
}

enum class ApprDepType {
    LT,  // Line Tangential
    CT,  // Circle Tangential
    LCT, // Line-Circle Tangential
    LN   // Line Normal
}

sealed class KlartextBlock {
    abstract val lineNumber: Int
    abstract val rawText: String

    data class BeginPgm(
        override val lineNumber: Int,
        override val rawText: String,
        val programName: String,
        val unit: UnitType = UnitType.MM
    ) : KlartextBlock()

    data class EndPgm(
        override val lineNumber: Int,
        override val rawText: String,
        val programName: String,
        val unit: UnitType = UnitType.MM
    ) : KlartextBlock()

    data class BlkForm(
        override val lineNumber: Int,
        override val rawText: String,
        val subIndex: Int, // 1 for 0.1, 2 for 0.2
        val spindleAxis: String?, // "Z"
        val point: Point3D
    ) : KlartextBlock()

    data class ToolCall(
        override val lineNumber: Int,
        override val rawText: String,
        val toolIdentifier: String,
        val toolNumber: Int,
        val spindleAxis: String = "Z",
        val spindleSpeed: Float? = null,
        val feedRate: Float? = null,
        val dl: Float = 0f,
        val dr: Float = 0f
    ) : KlartextBlock()

    data class ToolDef(
        override val lineNumber: Int,
        override val rawText: String,
        val toolNumber: Int,
        val length: Float = 0f,
        val radius: Float = 0f
    ) : KlartextBlock()

    data class Linear(
        override val lineNumber: Int,
        override val rawText: String,
        val x: Float? = null,
        val y: Float? = null,
        val z: Float? = null,
        val rawX: String? = null,
        val rawY: String? = null,
        val rawZ: String? = null,
        val isIncrementalX: Boolean = false,
        val isIncrementalY: Boolean = false,
        val isIncrementalZ: Boolean = false,
        val comp: RadiusComp = RadiusComp.R0,
        val explicitComp: RadiusComp? = null, // null = NO ENT (modal stays active), R0 = explicit R0
        val isRapid: Boolean = false,
        val isFeedAuto: Boolean = false,
        val feedRate: Float? = null,
        val rawFeed: String? = null,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class LinearPolar(
        override val lineNumber: Int,
        override val rawText: String,
        val pr: Float,
        val pa: Float,
        val z: Float? = null,
        val rawPR: String? = null,
        val rawPA: String? = null,
        val rawZ: String? = null,
        val isIncrementalPR: Boolean = false,
        val isIncrementalPA: Boolean = false,
        val explicitComp: RadiusComp? = null,
        val isRapid: Boolean = false,
        val feedRate: Float? = null,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class CircleCenter(
        override val lineNumber: Int,
        override val rawText: String,
        val x: Float? = null,
        val y: Float? = null,
        val rawX: String? = null,
        val rawY: String? = null,
        val isIncrementalX: Boolean = false,
        val isIncrementalY: Boolean = false,
        val isIncremental: Boolean = false
    ) : KlartextBlock()

    data class Circle(
        override val lineNumber: Int,
        override val rawText: String,
        val x: Float? = null,
        val y: Float? = null,
        val z: Float? = null,
        val rawX: String? = null,
        val rawY: String? = null,
        val rawZ: String? = null,
        val isIncrementalX: Boolean = false,
        val isIncrementalY: Boolean = false,
        val isIncrementalZ: Boolean = false,
        val direction: Direction = Direction.CCW,
        val comp: RadiusComp = RadiusComp.R0,
        val explicitComp: RadiusComp? = null,
        val isRapid: Boolean = false,
        val feedRate: Float? = null,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class CirclePolar(
        override val lineNumber: Int,
        override val rawText: String,
        val pa: Float,
        val z: Float? = null,
        val rawPA: String? = null,
        val rawZ: String? = null,
        val isIncrementalPA: Boolean = false,
        val direction: Direction = Direction.CCW,
        val explicitComp: RadiusComp? = null,
        val feedRate: Float? = null,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class CircleRadius(
        override val lineNumber: Int,
        override val rawText: String,
        val x: Float? = null,
        val y: Float? = null,
        val rawX: String? = null,
        val rawY: String? = null,
        val isIncrementalX: Boolean = false,
        val isIncrementalY: Boolean = false,
        val radius: Float, // signed: + <= 180 deg, - > 180 deg
        val rawRadius: String? = null,
        val direction: Direction = Direction.CCW,
        val comp: RadiusComp = RadiusComp.R0,
        val explicitComp: RadiusComp? = null,
        val isRapid: Boolean = false,
        val feedRate: Float? = null,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class CircleTangential(
        override val lineNumber: Int,
        override val rawText: String,
        val x: Float? = null,
        val y: Float? = null,
        val z: Float? = null,
        val rawX: String? = null,
        val rawY: String? = null,
        val rawZ: String? = null,
        val isIncrementalX: Boolean = false,
        val isIncrementalY: Boolean = false,
        val explicitComp: RadiusComp? = null,
        val feedRate: Float? = null,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class CornerRounding(
        override val lineNumber: Int,
        override val rawText: String,
        val radius: Float,
        val feedRate: Float? = null
    ) : KlartextBlock()

    data class CornerChamfer(
        override val lineNumber: Int,
        override val rawText: String,
        val length: Float,
        val feedRate: Float? = null
    ) : KlartextBlock()

    data class Approach(
        override val lineNumber: Int,
        override val rawText: String,
        val apprType: ApprDepType,
        val x: Float,
        val y: Float,
        val z: Float? = null,
        val len: Float? = null,
        val cca: Float? = null,
        val radius: Float? = null,
        val comp: RadiusComp = RadiusComp.RL,
        val feedRate: Float? = null,
        val isFeedAuto: Boolean = false
    ) : KlartextBlock()

    data class Depart(
        override val lineNumber: Int,
        override val rawText: String,
        val depType: ApprDepType,
        val x: Float? = null,
        val y: Float? = null,
        val z: Float? = null,
        val len: Float? = null,
        val cca: Float? = null,
        val radius: Float? = null,
        val feedRate: Float? = null,
        val isFeedAuto: Boolean = false,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class LabelSet(
        override val lineNumber: Int,
        override val rawText: String,
        val labelNumber: Int,
        val labelName: String = labelNumber.toString()
    ) : KlartextBlock()

    data class LabelCall(
        override val lineNumber: Int,
        override val rawText: String,
        val labelNumber: Int,
        val labelName: String = labelNumber.toString(),
        val repeatCount: Int = 1
    ) : KlartextBlock()

    data class CycleDef(
        override val lineNumber: Int,
        override val rawText: String,
        val cycleNumber: Int,
        val subNumber: Int = 0,
        val cycleName: String,
        val qParams: Map<Int, Float> = emptyMap(),
        val rawQParams: Map<Int, String> = emptyMap(),
        val datumX: Float? = null,
        val datumY: Float? = null,
        val datumZ: Float? = null,
        val isIncDatum: Boolean = false,
        val labels: List<String> = emptyList(),
        val rotationAngle: Float? = null,
        val rawRotationAngle: String? = null,
        val mirrorAxes: Set<String> = emptySet(),
        val isClearMirror: Boolean = false
    ) : KlartextBlock()

    data class CycleCall(
        override val lineNumber: Int,
        override val rawText: String,
        val mCodes: List<Int> = emptyList()
    ) : KlartextBlock()

    data class QParamAssignment(
        override val lineNumber: Int,
        override val rawText: String,
        val targetQ: Int,
        val expression: String,
        val fnType: Int? = null
    ) : KlartextBlock()

    data class QParamConditionalJump(
        override val lineNumber: Int,
        override val rawText: String,
        val fnType: Int, // 9: EQU, 10: NE, 11: GT, 12: LT
        val operand1: String,
        val condition: String, // EQU, NE, GT, LT
        val operand2: String,
        val targetLabelNum: Int,
        val targetLabelName: String
    ) : KlartextBlock()

    data class QParamDef(
        override val lineNumber: Int,
        override val rawText: String,
        val fnType: Int = 0, // 0: =, 1: +, 2: -, 3: *, 4: /
        val targetQ: Int,
        val value1: Float,
        val isQ1: Boolean = false,
        val value2: Float = 0f,
        val isQ2: Boolean = false
    ) : KlartextBlock()

    data class Stop(
        override val lineNumber: Int,
        override val rawText: String,
        val mCode: Int? = null
    ) : KlartextBlock()

    data class Comment(
        override val lineNumber: Int,
        override val rawText: String,
        val text: String
    ) : KlartextBlock()

    data class Other(
        override val lineNumber: Int,
        override val rawText: String
    ) : KlartextBlock()
}

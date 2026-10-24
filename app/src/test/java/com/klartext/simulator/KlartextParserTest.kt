package com.klartext.simulator

import com.klartext.simulator.model.*
import com.klartext.simulator.parser.KlartextInterpreter
import com.klartext.simulator.parser.KlartextParser
import com.klartext.simulator.parser.ZMapSimulator
import com.klartext.simulator.sample.SamplePrograms
import org.junit.Assert.*
import org.junit.Test

class KlartextParserTest {

    @Test
    fun testContourfrezenV4() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.contourfrezenV4)
        assertTrue(blocks.isNotEmpty())

        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        assertTrue("Segments should not be empty", result.segments.isNotEmpty())
        assertEquals("Should have no errors in valid program", 0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })

        // Check stock boundaries
        assertEquals(0f, result.stock.min.x, 0.01f)
        assertEquals(120f, result.stock.max.x, 0.01f)
        assertEquals(-25f, result.stock.min.z, 0.01f)

        // Verify KANTENFREES6 chamfer pass is executed with DL-1.5 DR-1.5
        val chamferSegs = result.segments.filter { it.toolName == "KANTENFREES6" }
        assertTrue("Chamfer segments should exist", chamferSegs.isNotEmpty())
        assertEquals(-1.5f, chamferSegs.first().dl, 0.01f)
        assertEquals(-1.5f, chamferSegs.first().dr, 0.01f)
    }

    @Test
    fun testBoorFreescycli() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.boorFreescycli)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        assertTrue(result.segments.isNotEmpty())
        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })

        // Check that Cycle 251 (Rectangular pocket) and Cycle 252 (Circular pocket) generated moves
        val cutSegs = result.segments.filter { it.moveType == MoveType.FEED }
        assertTrue(cutSegs.size > 20)
    }

    @Test
    fun testKamersfrezenH5AndTapping() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.kamersfrezenH5)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        assertTrue(result.segments.isNotEmpty())
        // Cycle 206 M8 tap feedrate Q206=375 matches S=300 x 1.25 -> no tap mismatch warnings!
        val tapWarnings = result.diagnostics.filter { it.code == TncErrorCode.TAP_FEEDRATE_MISMATCH }
        assertEquals(0, tapWarnings.size)
    }

    @Test
    fun testUVormLabelAndDatumShift() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.uVormLabel)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        assertTrue(result.segments.isNotEmpty())
        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })

        // Check Cycle 208 bore milling arcs exist
        val boreArcs = result.segments.filterIsInstance<ToolpathSegment.Arc>()
        assertTrue("Cycle 208 should generate helical arcs", boreArcs.isNotEmpty())
    }

    @Test
    fun testR0ValkuilStudentHint() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.demoR0Pitfall)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        val hints = result.diagnostics.filter { it.code == TncErrorCode.R0_CANCELS_MODAL_COMPENSATION }
        assertEquals("Should trigger R0 vs RL student hint", 1, hints.size)
        assertTrue(hints.first().message.contains("schakelt RL uit"))
    }

    @Test
    fun testRadiusTooLargeError() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.demoRadiusTooLarge)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        val errors = result.diagnostics.filter { it.code == TncErrorCode.TOOL_RADIUS_TOO_LARGE }
        assertEquals("Should trigger Gereedschapsradius te groot error", 1, errors.size)
        assertEquals(DiagnosticSeverity.ERROR, errors.first().severity)
    }

    @Test
    fun testCycle208TooLargeError() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.demoCycle208TooLarge)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        val errors = result.diagnostics.filter { it.code == TncErrorCode.BORE_MILLING_DIAMETER_TOO_LARGE }
        assertEquals("Should trigger Te grote diameter boring error", 1, errors.size)
    }

    @Test
    fun testCircleEndpointMismatch() {
        val code = """
            0 BEGIN PGM TEST_CIR MM
            1 BLK FORM 0.1 Z X+0 Y+0 Z-10
            2 BLK FORM 0.2 X+100 Y+100 Z+0
            3 TOOL CALL 1 Z S3000 F500
            4 L X+0 Y+10 R0 FMAX
            5 CC X+0 Y+0
            6 C X+25 Y+0 DR+ ; R_start = 10, R_end = 25 -> FOUT!
            7 END PGM TEST_CIR MM
        """.trimIndent()

        val blocks = KlartextParser.parseProgram(code)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        val errors = result.diagnostics.filter { it.code == TncErrorCode.CIRCLE_ENDPOINT_MISMATCH }
        assertEquals(1, errors.size)
    }

    @Test
    fun testZMapSimulatorMaterialRemoval() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.contourfrezenV4)
        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        val hf = ZMapSimulator.computeHeightfield(
            stock = result.stock,
            segments = result.segments,
            maxStepIndex = result.segments.size,
            gridResolution = 60
        )

        assertNotNull(hf)
        assertEquals(60, hf.cols)
        assertEquals(60, hf.rows)

        // Find minimum cut Z in heightfield
        var minZFound = hf.maxZ
        for (z in hf.heights) {
            if (z < minZFound) minZFound = z
        }
        assertTrue("Material should be removed below top stock", minZFound < 0f)
    }

    @Test
    fun testDxfParsingAndConversion() {
        val sampleDxf = """
            0
            SECTION
            2
            ENTITIES
            0
            LINE
            10
            0.0
            20
            0.0
            11
            100.0
            20
            0.0
            0
            LINE
            10
            100.0
            20
            0.0
            11
            100.0
            20
            50.0
            0
            ENDSEC
            0
            EOF
        """.trimIndent()

        val entities = com.klartext.simulator.parser.DxfParser.parse(sampleDxf)
        assertEquals(2, entities.size)

        val config = com.klartext.simulator.parser.DxfToKlartextConverter.ConversionConfig(
            programName = "CAD_TEST",
            toolName = "FREES16",
            cuttingDepth = -15f
        )
        val klartext = com.klartext.simulator.parser.DxfToKlartextConverter.convert(entities, config)
        assertTrue(klartext.contains("BEGIN PGM CAD_TEST MM"))
        assertTrue(klartext.contains("TOOL CALL \"FREES16\""))
        assertTrue(klartext.contains("APPR LCT"))
        assertTrue(klartext.contains("DEP LCT"))

        // Verify the generated Klartekst parses and executes cleanly
        val blocks = KlartextParser.parseProgram(klartext)
        val result = KlartextInterpreter().interpret(blocks)
        assertTrue(result.segments.isNotEmpty())
        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
    }

    @Test
    fun testIsoGCodeToKlartextAndBack() {
        val isoInput = """
            %
            O1001 (TEST ISO)
            T2 M06
            S3000 M03
            G00 G90 X0.0 Y0.0 Z10.0
            G01 Z-5.0 F300
            G41 X50.0 Y0.0 F800
            G03 X50.0 Y50.0 R25.0
            G40 X0.0 Y0.0
            G00 Z100.0 M05
            M30
            %
        """.trimIndent()

        val klartext = com.klartext.simulator.parser.IsoGCodeConverter.convertIsoToKlartext(isoInput, "FROM_ISO")
        assertTrue(klartext.contains("BEGIN PGM FROM_ISO MM"))
        assertTrue(klartext.contains("TOOL CALL 2 Z"))
        assertTrue(klartext.contains("RL"))
        assertTrue(klartext.contains("CR X+50.000 Y+50.000 R+25.000 DR+"))

        // Verify generated Klartekst executes cleanly
        val blocks = KlartextParser.parseProgram(klartext)
        val result = KlartextInterpreter().interpret(blocks)
        assertTrue(result.segments.isNotEmpty())
        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })

        // Convert back to ISO
        val isoOutput = com.klartext.simulator.parser.IsoGCodeConverter.convertKlartextToIso(klartext)
        assertTrue(isoOutput.contains("G01"))
        assertTrue(isoOutput.contains("G03"))
        assertTrue(isoOutput.contains("M30"))
    }

    @Test
    fun testQExpressionEvaluator() {
        val qMap = mapOf(1 to 50f, 2 to 30f, 3 to 90f, 4 to 16f)

        // Basic arithmetic
        assertEquals(80f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("+Q1 + +Q2", qMap), 0.001f)
        assertEquals(20f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("+Q1 - +Q2", qMap), 0.001f)
        assertEquals(1500f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("Q1 * Q2", qMap), 0.001f)
        assertEquals(4f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("SQRT +Q4", qMap), 0.001f)

        // Trigonometry strictly in degrees
        assertEquals(1.0f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("SIN +Q3", qMap), 0.001f) // SIN(90 deg) = 1
        assertEquals(0.5f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("SIN 30", qMap), 0.001f)  // SIN(30 deg) = 0.5
        assertEquals(0.5f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("COS 60", qMap), 0.001f)  // COS(60 deg) = 0.5
        assertEquals(1.0f, com.klartext.simulator.parser.QExpressionEvaluator.evaluate("COS 0", qMap), 0.001f)   // COS(0 deg) = 1.0
    }

    @Test
    fun testParametricEllipseQParameters() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.demoParametricEllipse)
        assertTrue("Blocks should be parsed", blocks.isNotEmpty())

        val interpreter = KlartextInterpreter()
        val result = interpreter.interpret(blocks)

        assertEquals("No errors expected in parametric ellipse program", 0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        assertTrue("Should have generated many elliptical segments", result.segments.size > 70)

        // Verify final Q-parameters
        assertEquals(50f, result.qParameters[1] ?: 0f, 0.01f)
        assertEquals(30f, result.qParameters[2] ?: 0f, 0.01f)
        assertTrue("Loop counter Q3 should have reached >= 365", (result.qParameters[3] ?: 0f) >= 365f)
    }

    @Test
    fun testConditionalJumpsFN9to12() {
        val prog = """
            0 BEGIN PGM JUMP_TEST MM
            1 FN 0: Q1 = +10
            2 FN 0: Q2 = +0
            3 ; Unconditional jump via EQU:
            4 FN 9: IF +Q1 EQU +10 GOTO LBL "HIT_EQU"
            5 L X+999 Y+999 R0 FMAX ; Should be skipped
            6 LBL "HIT_EQU"
            7 FN 0: Q2 = +1
            8 ; Test GT jump:
            9 FN 11: IF +Q1 GT +5 GOTO LBL "HIT_GT"
            10 L X+888 Y+888 R0 FMAX ; Should be skipped
            11 LBL "HIT_GT"
            12 FN 0: Q2 = +2
            13 END PGM JUMP_TEST MM
        """.trimIndent()

        val blocks = KlartextParser.parseProgram(prog)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        assertEquals(2f, result.qParameters[2] ?: 0f, 0.01f)
        // Check that skipped blocks did not generate segments
        assertFalse(result.segments.any { it.end.x == 999f || it.end.x == 888f })
    }

    @Test
    fun testToolHolderCollisionDetection() {
        val collisionProg = """
            0 BEGIN PGM COLLISION_TEST MM
            1 BLK FORM 0.1 Z X+0 Y+0 Z-100
            2 BLK FORM 0.2 X+100 Y+100 Z+0
            3 TOOL CALL "FREES10" Z S3000 F500
            4 L X+50 Y+50 R0 FMAX
            5 ; Stick-out for FREES10 is ~44 mm (length 80 * 0.55).
            6 ; Plunging to Z-90 penetrates way past the tool stick-out into solid stock!
            7 L Z-90 R0 F300 M3
            8 L Z+50 R0 FMAX M30
            9 END PGM COLLISION_TEST MM
        """.trimIndent()

        val blocks = KlartextParser.parseProgram(collisionProg)
        val result = KlartextInterpreter().interpret(blocks)

        val collisions = result.diagnostics.filter { it.code == TncErrorCode.TOOL_HOLDER_COLLISION }
        assertTrue("Should detect tool holder collision when plunge exceeds stick-out", collisions.isNotEmpty())
        assertEquals(DiagnosticSeverity.ERROR, collisions.first().severity)
        assertTrue(collisions.first().message.contains("Houder botst") || collisions.first().message.contains("Gereedschapshouder"))
    }

    @Test
    fun testPitchCircle220() {
        val prog = """
            0 BEGIN PGM PITCH_CIRCLE MM
            1 BLK FORM 0.1 Z X-50 Y-50 Z-20
            2 BLK FORM 0.2 X+50 Y+50 Z+0
            3 TOOL CALL "BOOR6" Z S2000 F200
            4 CYCL DEF 200 BOHREN ~
              Q200=+2
              Q201=-15
              Q206=+150
              Q202=+5
              Q203=+0
              Q204=+50
            5 CYCL DEF 220 PUNKTEMUSTER KREIS ~
              Q216=+0
              Q217=+0
              Q244=+60
              Q245=+0
              Q246=+360
              Q247=+60
              Q241=+6
              Q200=+2
              Q203=+0
              Q204=+50
              Q301=+1
            6 CYCL CALL
            7 L Z+50 R0 FMAX M30
            8 END PGM PITCH_CIRCLE MM
        """.trimIndent()

        val blocks = KlartextParser.parseProgram(prog)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        val plunges = result.segments.filter { it.moveType == MoveType.PLUNGE && Math.abs(it.end.z - (-15f)) < 0.1f }
        assertEquals(6, plunges.size)
    }

    @Test
    fun testLinearPattern221() {
        val prog = """
            0 BEGIN PGM GRID_TEST MM
            1 BLK FORM 0.1 Z X-10 Y-10 Z-20
            2 BLK FORM 0.2 X+80 Y+60 Z+0
            3 TOOL CALL "CENTERBOOR" Z S3000 F250
            4 CYCL DEF 240 CENTREREN ~
              Q200=+2
              Q201=-3
              Q206=+200
              Q203=+0
              Q204=+30
            5 CYCL DEF 221 PUNKTEMUSTER LINIEN ~
              Q225=+10
              Q226=+10
              Q237=+20
              Q238=+15
              Q242=+3
              Q243=+2
              Q224=+0
              Q200=+2
              Q203=+0
              Q204=+30
              Q301=+1
            6 CYCL CALL
            7 L Z+50 R0 FMAX M30
            8 END PGM GRID_TEST MM
        """.trimIndent()

        val blocks = KlartextParser.parseProgram(prog)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        val centerHoles = result.segments.filter { it.moveType == MoveType.PLUNGE && Math.abs(it.end.z - (-3f)) < 0.1f }
        assertEquals(6, centerHoles.size)
    }

    @Test
    fun testFaceMilling232() {
        val prog = """
            0 BEGIN PGM FACE_MILL MM
            1 BLK FORM 0.1 Z X-50 Y-50 Z-20
            2 BLK FORM 0.2 X+50 Y+50 Z+0
            3 TOOL CALL "VLAKFREES63" Z S2000 F800
            4 CYCL DEF 232 PLANFRAESEN ~
              Q389=+0
              Q225=-50
              Q226=-50
              Q227=+0
              Q386=-2
              Q218=+100
              Q219=+100
              Q202=+2
              Q369=+0
              Q370=+1.2
              Q207=+750
              Q385=+600
              Q253=+99999
              Q200=+2
              Q204=+50
            5 CYCL CALL
            6 L Z+50 R0 FMAX M30
            7 END PGM FACE_MILL MM
        """.trimIndent()

        val blocks = KlartextParser.parseProgram(prog)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        val sweeps = result.segments.filter { it.moveType == MoveType.FEED && Math.abs(it.end.z - (-2f)) < 0.05f }
        assertTrue(sweeps.size >= 3)
    }

    @Test
    fun testRotationAndMirroring() {
        val prog = """
            0 BEGIN PGM TRANS_TEST MM
            1 BLK FORM 0.1 Z X-50 Y-50 Z-20
            2 BLK FORM 0.2 X+50 Y+50 Z+0
            3 TOOL CALL "FREES10" Z S3000 F500
            4 ; Test Rotation
            5 CYCL DEF 10.0 DREHUNG
            6 CYCL DEF 10.1 ROT+90
            7 L X+50 Y+0 R0 FMAX
            8 CYCL DEF 10.1 ROT+0
            9 ; Test Mirroring
            10 CYCL DEF 8.0 SPIEGELN
            11 CYCL DEF 8.1 X
            12 L X+40 Y+20 RL F500
            13 CYCL DEF 8.1
            14 END PGM TRANS_TEST MM
        """.trimIndent()

        val blocks = KlartextParser.parseProgram(prog)
        val result = KlartextInterpreter().interpret(blocks)

        val rotMove = result.segments.first { it.lineNumber == 7 }
        assertEquals(0f, rotMove.end.x, 0.1f)
        assertEquals(50f, rotMove.end.y, 0.1f)

        val mirrorMove = result.segments.last { it.lineNumber == 12 }
        assertEquals(-40f, mirrorMove.programmedEnd.x, 0.1f)
        assertEquals(RadiusComp.RR, mirrorMove.radiusComp)
    }

    @Test
    fun testSlCyclesWithIslands() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.slCycliEilanden)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        assertTrue("SL cycles should generate feed segments", result.segments.count { it.moveType == MoveType.FEED } > 20)

        // Verify island [-15, +15] is not gouged
        val islandGouges = result.segments.filter { seg ->
            seg.moveType == MoveType.FEED &&
            Math.abs((seg.start.x + seg.end.x) / 2f) < 9.0f &&
            Math.abs((seg.start.y + seg.end.y) / 2f) < 9.0f
        }
        assertEquals(0, islandGouges.size)
    }

    @Test
    fun testSteekcirkelVlakfreesDemo() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.steekcirkelVlakfrees)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        assertTrue(result.segments.isNotEmpty())
    }

    @Test
    fun testOefening1Pentagon() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.oefening1Pentagon)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        assertTrue("Pentagon toolpath must generate segments", result.segments.isNotEmpty())
        // Verify final position is safety clearance at (150, 150, 100)
        assertEquals(150f, result.finalState.x, 0.1f)
        assertEquals(150f, result.finalState.y, 0.1f)
        assertEquals(100f, result.finalState.z, 0.1f)
    }

    @Test
    fun testOefening2RuitApprDep() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.oefening2Ruit)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        assertTrue("Ruit toolpath must generate segments", result.segments.isNotEmpty())
        // APPR LT and DEP LT should produce feed moves with RL and return to R0
        val feedSegments = result.segments.filter { it.moveType == MoveType.FEED }
        assertTrue("Should have at least 6 feed segments", feedSegments.size >= 6)
    }

    @Test
    fun testOefening3BoogContour() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.oefening3Boog)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        val arcs = result.segments.filterIsInstance<ToolpathSegment.Arc>()
        assertEquals("Should have exactly 1 circular arc segment", 1, arcs.size)
        assertEquals(25f, arcs[0].center.x, 0.1f)
        assertEquals(25f, arcs[0].center.y, 0.1f)
        assertEquals(Direction.CCW, arcs[0].direction) // DR+ is counter-clockwise
    }

    @Test
    fun testOefening4BewerkingsCycli() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.oefening4Cycli)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        // Cycles 251, 252, 253 should all have executed plunging and pocketing moves
        val plungeMoves = result.segments.filter { it.moveType == MoveType.PLUNGE }
        assertTrue("Cycles should generate multiple plunge moves", plungeMoves.size >= 3)
    }

    @Test
    fun testOefening5BorenTappenSubprogramsAndRepeats() {
        val blocks = KlartextParser.parseProgram(SamplePrograms.oefening5BorenTappen)
        val result = KlartextInterpreter().interpret(blocks)

        assertEquals(0, result.diagnostics.count { it.severity == DiagnosticSeverity.ERROR })
        // 22 holes per tool: 8 (LBL 10 with REP 7) + 8 (LBL 11 with REP 7) + 6 (LBL 12 with REP 5) = 22 holes
        // 2 tools: T229 (Drill) + T264 (Tap) = 44 total cycle calls!
        val plungeMoves = result.segments.filter { it.moveType == MoveType.PLUNGE }
        assertEquals("Should execute exactly 44 hole plunges (22 drill + 22 tap)", 44, plungeMoves.size)
    }
}

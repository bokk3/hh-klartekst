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
}

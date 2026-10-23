package com.klartext.simulator.parser

import com.klartext.simulator.model.*
import java.util.Locale
import java.util.regex.Pattern

object IsoGCodeConverter {

    private val wordRegex = Pattern.compile("([A-Z])([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)

    /**
     * Converts standard ISO G-code (Fanuc/Haas/Siemens) into HEIDENHAIN Klartekst.
     */
    fun convertIsoToKlartext(gcode: String, programName: String = "ISO_CONVERTED"): String {
        val lines = gcode.lines()
        val sb = StringBuilder()
        var lineNum = 0

        fun emit(cmd: String) {
            sb.append("$lineNum $cmd\n")
            lineNum++
        }

        emit("BEGIN PGM ${programName.uppercase()} MM")
        emit("; --- GECONVERTEERD VANAF ISO G-CODE ---")

        var isRapid = false
        var currentFeed = 800f
        var currentRadiusComp = RadiusComp.R0
        var isAbsolute = true
        var spindleOn = false
        var activeTool = 1
        var activeSpeed = 3000f

        var curX = 0f; var curY = 0f; var curZ = 100f
        var minX = 0f; var minY = 0f; var maxX = 100f; var maxY = 100f; var minZ = -20f

        for (rawLine in lines) {
            val clean = rawLine.substringBefore(";").substringBefore("(").trim()
            if (clean.isEmpty() || clean.startsWith("%")) continue

            val words = mutableMapOf<Char, Float>()
            val matcher = wordRegex.matcher(clean)
            while (matcher.find()) {
                val letter = matcher.group(1)?.uppercase()?.firstOrNull() ?: continue
                val value = matcher.group(2)?.toFloatOrNull() ?: continue
                words[letter] = value
            }

            if (words.isEmpty()) continue

            // Modal G-codes
            val gCode = words['G']?.toInt()
            when (gCode) {
                0 -> isRapid = true
                1 -> isRapid = false
                2 -> isRapid = false
                3 -> isRapid = false
                40 -> currentRadiusComp = RadiusComp.R0
                41 -> currentRadiusComp = RadiusComp.RL
                42 -> currentRadiusComp = RadiusComp.RR
                90 -> isAbsolute = true
                91 -> isAbsolute = false
            }

            words['F']?.let { currentFeed = it }
            words['S']?.let { activeSpeed = it }
            words['T']?.let { activeTool = it.toInt() }

            val mCode = words['M']?.toInt()
            if (mCode == 3 || mCode == 4) spindleOn = true
            if (mCode == 5 || mCode == 30 || mCode == 2) spindleOn = false

            // Tool change T... M6
            if (words.containsKey('T') || mCode == 6) {
                emit("TOOL CALL $activeTool Z S${activeSpeed.toInt()} F${currentFeed.toInt()}")
                continue
            }

            // Coordinates
            val xVal = words['X']
            val yVal = words['Y']
            val zVal = words['Z']

            if (xVal != null || yVal != null || zVal != null || gCode != null) {
                val targetX = if (xVal != null) (if (isAbsolute) xVal else curX + xVal) else curX
                val targetY = if (yVal != null) (if (isAbsolute) yVal else curY + yVal) else curY
                val targetZ = if (zVal != null) (if (isAbsolute) zVal else curZ + zVal) else curZ

                minX = kotlin.math.min(minX, targetX)
                minY = kotlin.math.min(minY, targetY)
                maxX = kotlin.math.max(maxX, targetX)
                maxY = kotlin.math.max(maxY, targetY)
                minZ = kotlin.math.min(minZ, targetZ)

                when (gCode) {
                    2, 3 -> {
                        // Circular arc: G02 = CW (DR-), G03 = CCW (DR+)
                        val dirStr = if (gCode == 2) "DR-" else "DR+"
                        val rVal = words['R']
                        val iVal = words['I']
                        val jVal = words['J']

                        if (rVal != null) {
                            emit(String.format(Locale.US, "CR X%+1.3f Y%+1.3f R%+1.3f %s %s F%d",
                                targetX, targetY, rVal, dirStr, currentRadiusComp.name, currentFeed.toInt()))
                        } else if (iVal != null || jVal != null) {
                            val ccX = curX + (iVal ?: 0f)
                            val ccY = curY + (jVal ?: 0f)
                            emit(String.format(Locale.US, "CC X%+1.3f Y%+1.3f", ccX, ccY))
                            emit(String.format(Locale.US, "C X%+1.3f Y%+1.3f %s %s F%d",
                                targetX, targetY, dirStr, currentRadiusComp.name, currentFeed.toInt()))
                        }
                    }
                    else -> {
                        // Linear move G00 or G01
                        val feedStr = if (isRapid) "FMAX" else "F${currentFeed.toInt()}"
                        val mStr = if (mCode != null && mCode != 6) " M$mCode" else ""
                        val compStr = if (isRapid) "R0" else currentRadiusComp.name

                        val parts = mutableListOf<String>()
                        parts.add("L")
                        if (xVal != null) parts.add(String.format(Locale.US, "%s%+1.3f", if (isAbsolute) "X" else "IX", xVal))
                        if (yVal != null) parts.add(String.format(Locale.US, "%s%+1.3f", if (isAbsolute) "Y" else "IY", yVal))
                        if (zVal != null) parts.add(String.format(Locale.US, "%s%+1.3f", if (isAbsolute) "Z" else "IZ", zVal))
                        parts.add(compStr)
                        parts.add(feedStr)
                        if (mStr.isNotEmpty()) parts.add(mStr.trim())

                        emit(parts.joinToString(" "))
                    }
                }

                curX = targetX
                curY = targetY
                curZ = targetZ
            }
        }

        emit("L Z+100 R0 FMAX M30")
        emit("END PGM ${programName.uppercase()} MM")

        // Insert BLK FORM after line 0
        val resultLines = sb.toString().lines().toMutableList()
        val blk1 = "1 BLK FORM 0.1 Z X${minX.toInt() - 5} Y${minY.toInt() - 5} Z${minZ.toInt() - 5}"
        val blk2 = "2 BLK FORM 0.2 X${maxX.toInt() + 5} Y${maxY.toInt() + 5} Z+0"
        resultLines.add(2, blk1)
        resultLines.add(3, blk2)

        return resultLines.mapIndexed { idx, ln ->
            val stripped = ln.replaceFirst(Regex("^\\d+\\s+"), "")
            "$idx $stripped"
        }.joinToString("\n").trimEnd()
    }

    /**
     * Converts a HEIDENHAIN Klartekst program into universal ISO G-code.
     */
    fun convertKlartextToIso(klartext: String): String {
        val blocks = KlartextParser.parseProgram(klartext)
        val sb = StringBuilder()

        sb.append("%\n")
        sb.append("O1001 (CONVERTED FROM HEIDENHAIN KLARTEXT)\n")
        sb.append("G21 G90 G40 G17 (METRIC, ABSOLUTE, XY-PLANE)\n")

        for (b in blocks) {
            when (b) {
                is KlartextBlock.ToolCall -> {
                    sb.append("T${b.toolNumber} M06\n")
                    b.spindleSpeed?.let { sb.append("S${it.toInt()} M03\n") }
                }
                is KlartextBlock.Linear -> {
                    val g = if (b.isRapid) "G00" else "G01"
                    val comp = when (b.comp) {
                        RadiusComp.RL -> " G41"
                        RadiusComp.RR -> " G42"
                        RadiusComp.R0 -> " G40"
                    }
                    val parts = mutableListOf<String>()
                    parts.add(g)
                    b.x?.let { parts.add(String.format(Locale.US, "X%+1.3f", it)) }
                    b.y?.let { parts.add(String.format(Locale.US, "Y%+1.3f", it)) }
                    b.z?.let { parts.add(String.format(Locale.US, "Z%+1.3f", it)) }
                    if (!b.isRapid) parts.add(comp.trim())
                    b.feedRate?.let { if (!b.isRapid) parts.add(String.format(Locale.US, "F%d", it.toInt())) }
                    b.mCodes.forEach { parts.add("M$it") }
                    sb.append(parts.joinToString(" ") + "\n")
                }
                is KlartextBlock.CircleRadius -> {
                    val g = if (b.direction == Direction.CW) "G02" else "G03"
                    val parts = mutableListOf<String>()
                    parts.add(g)
                    b.x?.let { parts.add(String.format(Locale.US, "X%+1.3f", it)) }
                    b.y?.let { parts.add(String.format(Locale.US, "Y%+1.3f", it)) }
                    parts.add(String.format(Locale.US, "R%+1.3f", b.radius))
                    b.feedRate?.let { parts.add(String.format(Locale.US, "F%d", it.toInt())) }
                    sb.append(parts.joinToString(" ") + "\n")
                }
                is KlartextBlock.Circle -> {
                    val g = if (b.direction == Direction.CW) "G02" else "G03"
                    val parts = mutableListOf<String>()
                    parts.add(g)
                    b.x?.let { parts.add(String.format(Locale.US, "X%+1.3f", it)) }
                    b.y?.let { parts.add(String.format(Locale.US, "Y%+1.3f", it)) }
                    b.feedRate?.let { parts.add(String.format(Locale.US, "F%d", it.toInt())) }
                    sb.append(parts.joinToString(" ") + "\n")
                }
                is KlartextBlock.Stop -> {
                    sb.append("M30\n")
                }
                else -> Unit
            }
        }

        sb.append("M30\n")
        sb.append("%\n")
        return sb.toString().trimEnd()
    }
}

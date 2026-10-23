package com.klartext.simulator.parser

import com.klartext.simulator.model.*
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

object KlartextParser {

    private val lineNumRegex = Pattern.compile("^\\s*(\\d+)?\\s*(.*)$")
    private val coordXRegex = Pattern.compile("(?:^|\\s)(IX|X)\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val coordYRegex = Pattern.compile("(?:^|\\s)(IY|Y)\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val coordZRegex = Pattern.compile("(?:^|\\s)(IZ|Z)\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val polarPRRegex = Pattern.compile("(?:^|\\s)(IPR|PR)\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val polarPARegex = Pattern.compile("(?:^|\\s)(IPA|PA)\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val radiusRegex = Pattern.compile("(?:^|\\s)R\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val lenRegex = Pattern.compile("(?:^|\\s)LEN\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val ccaRegex = Pattern.compile("(?:^|\\s)CCA\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val dlRegex = Pattern.compile("(?:^|\\s)DL\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val drOffsetRegex = Pattern.compile("(?:^|\\s)DR\\s*([+-]\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val feedRegex = Pattern.compile("(?:^|\\s)F\\s*(\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val speedRegex = Pattern.compile("(?:^|\\s)S\\s*(\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val mCodeRegex = Pattern.compile("(?:^|\\s)M\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
    private val qParamPairRegex = Pattern.compile("Q(\\d+)\\s*=\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val quotedNameRegex = Pattern.compile("\"([^\"]+)\"")

    fun parseProgram(content: String): List<KlartextBlock> {
        val rawLines = content.lines()
        val blocks = mutableListOf<KlartextBlock>()

        var i = 0
        while (i < rawLines.size) {
            val rawLine = rawLines[i].trim()
            if (rawLine.isEmpty()) {
                i++
                continue
            }

            // Combine multi-line CYCL DEF blocks that use '~' continuation or consecutive Q-parameter lines
            val (lineNum, cmdStart) = extractLineNumberAndCommand(rawLine, i)
            val upperStart = stripInlineComment(cmdStart).uppercase(Locale.ROOT)

            if (upperStart.startsWith("CYCL DEF")) {
                val combinedLines = StringBuilder(rawLine)
                var j = i + 1
                while (j < rawLines.size) {
                    val nextRaw = rawLines[j].trim()
                    val (_, nextCmd) = extractLineNumberAndCommand(nextRaw, j)
                    val nextClean = stripInlineComment(nextCmd).trim().uppercase(Locale.ROOT)
                    if (nextClean.startsWith("Q") && nextClean.contains("=")) {
                        combinedLines.append(" ").append(nextClean)
                        j++
                    } else {
                        break
                    }
                }
                blocks.add(parseCycleDefBlock(lineNum, rawLine, combinedLines.toString()))
                i = j
                continue
            }

            blocks.add(parseLine(rawLine, i))
            i++
        }
        return blocks
    }

    fun parseLine(line: String, defaultIndex: Int): KlartextBlock {
        val (lineNum, commandWithComment) = extractLineNumberAndCommand(line, defaultIndex)

        if (commandWithComment.startsWith(";") || commandWithComment.startsWith("//") || commandWithComment.startsWith("*")) {
            return KlartextBlock.Comment(lineNum, line, commandWithComment.removePrefix(";").removePrefix("*").trim())
        }

        val commandText = stripInlineComment(commandWithComment).trim()
        val upper = commandText.uppercase(Locale.ROOT)

        return when {
            upper.startsWith("BEGIN PGM") -> {
                val parts = commandText.split("\\s+".toRegex())
                val name = parts.getOrNull(2) ?: "PROGRAM"
                val unit = if (upper.contains("INCH")) UnitType.INCH else UnitType.MM
                KlartextBlock.BeginPgm(lineNum, line, name, unit)
            }

            upper.startsWith("END PGM") -> {
                val parts = commandText.split("\\s+".toRegex())
                val name = parts.getOrNull(2) ?: "PROGRAM"
                val unit = if (upper.contains("INCH")) UnitType.INCH else UnitType.MM
                KlartextBlock.EndPgm(lineNum, line, name, unit)
            }

            upper.startsWith("BLK FORM 0.1") -> {
                val x = parseCoord(coordXRegex, upper)
                val y = parseCoord(coordYRegex, upper)
                val z = parseCoord(coordZRegex, upper)
                KlartextBlock.BlkForm(lineNum, line, 1, "Z", Point3D(x?.first ?: 0f, y?.first ?: 0f, z?.first ?: -20f))
            }

            upper.startsWith("BLK FORM 0.2") -> {
                val x = parseCoord(coordXRegex, upper)
                val y = parseCoord(coordYRegex, upper)
                val z = parseCoord(coordZRegex, upper)
                KlartextBlock.BlkForm(lineNum, line, 2, "Z", Point3D(x?.first ?: 100f, y?.first ?: 100f, z?.first ?: 0f))
            }

            upper.startsWith("TOOL CALL") -> {
                val quotedMatcher = quotedNameRegex.matcher(commandText)
                val toolId: String
                if (quotedMatcher.find()) {
                    toolId = quotedMatcher.group(1) ?: "1"
                } else {
                    val parts = upper.split("\\s+".toRegex())
                    toolId = parts.getOrNull(2) ?: "1"
                }
                val resolvedTool = ToolTable.resolve(toolId)
                val axis = when {
                    upper.contains(" X") -> "X"
                    upper.contains(" Y") -> "Y"
                    else -> "Z"
                }
                val speed = parseValue(speedRegex, upper)
                val feed = parseValue(feedRegex, upper)
                val dl = parseValue(dlRegex, upper) ?: 0f
                val dr = parseValue(drOffsetRegex, upper) ?: 0f
                KlartextBlock.ToolCall(
                    lineNumber = lineNum,
                    rawText = line,
                    toolIdentifier = toolId,
                    toolNumber = toolId.toIntOrNull() ?: resolvedTool.number,
                    spindleAxis = axis,
                    spindleSpeed = speed,
                    feedRate = feed,
                    dl = dl,
                    dr = dr
                )
            }

            upper.startsWith("TOOL DEF") -> {
                val parts = upper.split("\\s+".toRegex())
                val toolNum = parts.getOrNull(2)?.toIntOrNull() ?: 1
                val length = parseValue(Pattern.compile("(?:^|\\s)L\\s*([+-]?\\d*\\.?\\d+)"), upper) ?: 0f
                val radius = parseValue(radiusRegex, upper) ?: 0f
                KlartextBlock.ToolDef(lineNum, line, toolNum, length, radius)
            }

            upper.startsWith("APPR ") -> {
                val apprType = when {
                    upper.startsWith("APPR LCT") -> ApprDepType.LCT
                    upper.startsWith("APPR CT") -> ApprDepType.CT
                    upper.startsWith("APPR LN") -> ApprDepType.LN
                    else -> ApprDepType.LT
                }
                val xVal = parseCoord(coordXRegex, upper)?.first ?: 0f
                val yVal = parseCoord(coordYRegex, upper)?.first ?: 0f
                val zVal = parseCoord(coordZRegex, upper)?.first
                val lenVal = parseValue(lenRegex, upper)
                val ccaVal = parseValue(ccaRegex, upper)
                val rVal = parseValue(radiusRegex, upper)
                val comp = parseExplicitRadiusComp(upper) ?: RadiusComp.RL
                val feed = parseValue(feedRegex, upper)
                val isFeedAuto = upper.contains("F AUTO") || upper.contains("FAUTO")
                KlartextBlock.Approach(
                    lineNumber = lineNum,
                    rawText = line,
                    apprType = apprType,
                    x = xVal,
                    y = yVal,
                    z = zVal,
                    len = lenVal,
                    cca = ccaVal,
                    radius = rVal,
                    comp = comp,
                    feedRate = feed,
                    isFeedAuto = isFeedAuto
                )
            }

            upper.startsWith("DEP ") -> {
                val depType = when {
                    upper.startsWith("DEP LCT") -> ApprDepType.LCT
                    upper.startsWith("DEP CT") -> ApprDepType.CT
                    upper.startsWith("DEP LN") -> ApprDepType.LN
                    else -> ApprDepType.LT
                }
                val xVal = parseCoord(coordXRegex, upper)?.first
                val yVal = parseCoord(coordYRegex, upper)?.first
                val zVal = parseCoord(coordZRegex, upper)?.first
                val lenVal = parseValue(lenRegex, upper)
                val ccaVal = parseValue(ccaRegex, upper)
                val rVal = parseValue(radiusRegex, upper)
                val feed = parseValue(feedRegex, upper)
                val isFeedAuto = upper.contains("F AUTO") || upper.contains("FAUTO")
                val mCodes = parseMCodes(upper)
                KlartextBlock.Depart(
                    lineNumber = lineNum,
                    rawText = line,
                    depType = depType,
                    x = xVal,
                    y = yVal,
                    z = zVal,
                    len = lenVal,
                    cca = ccaVal,
                    radius = rVal,
                    feedRate = feed,
                    isFeedAuto = isFeedAuto,
                    mCodes = mCodes
                )
            }

            upper.startsWith("RND ") || upper == "RND" -> {
                val r = parseValue(radiusRegex, upper) ?: 5f
                val feed = parseValue(feedRegex, upper)
                KlartextBlock.CornerRounding(lineNum, line, abs(r), feed)
            }

            upper.startsWith("CHF ") || upper == "CHF" -> {
                val afterChf = upper.substringAfter("CHF").trim()
                val firstToken = afterChf.split("\\s+".toRegex()).firstOrNull()
                val chfLen = firstToken?.toFloatOrNull() ?: parseValue(lenRegex, upper) ?: 5f
                val feed = parseValue(feedRegex, upper)
                KlartextBlock.CornerChamfer(lineNum, line, abs(chfLen), feed)
            }

            upper.startsWith("CC ") || upper == "CC" -> {
                val xVal = parseCoord(coordXRegex, upper)
                val yVal = parseCoord(coordYRegex, upper)
                val incX = xVal?.second == true
                val incY = yVal?.second == true
                KlartextBlock.CircleCenter(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    isIncrementalX = incX,
                    isIncrementalY = incY,
                    isIncremental = incX || incY
                )
            }

            upper.startsWith("LP ") -> {
                val prVal = parseCoord(polarPRRegex, upper)
                val paVal = parseCoord(polarPARegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val explicitComp = parseExplicitRadiusComp(upper)
                val isRapid = upper.contains("FMAX")
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.LinearPolar(
                    lineNumber = lineNum,
                    rawText = line,
                    pr = prVal?.first ?: 0f,
                    pa = paVal?.first ?: 0f,
                    z = zVal?.first,
                    isIncrementalPR = prVal?.second ?: false,
                    isIncrementalPA = paVal?.second ?: false,
                    explicitComp = explicitComp,
                    isRapid = isRapid,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("CP ") -> {
                val paVal = parseCoord(polarPARegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val dir = if (upper.contains("DR-")) Direction.CW else Direction.CCW
                val explicitComp = parseExplicitRadiusComp(upper)
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.CirclePolar(
                    lineNumber = lineNum,
                    rawText = line,
                    pa = paVal?.first ?: 0f,
                    z = zVal?.first,
                    isIncrementalPA = paVal?.second ?: false,
                    direction = dir,
                    explicitComp = explicitComp,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("CR ") -> {
                val xVal = parseCoord(coordXRegex, upper)
                val yVal = parseCoord(coordYRegex, upper)
                val r = parseValue(radiusRegex, upper) ?: 10f
                val dir = if (upper.contains("DR-")) Direction.CW else Direction.CCW
                val explicitComp = parseExplicitRadiusComp(upper)
                val comp = explicitComp ?: RadiusComp.R0
                val isRapid = upper.contains("FMAX")
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.CircleRadius(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    isIncrementalX = xVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: false,
                    radius = r,
                    direction = dir,
                    comp = comp,
                    explicitComp = explicitComp,
                    isRapid = isRapid,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("CT ") -> {
                val xVal = parseCoord(coordXRegex, upper)
                val yVal = parseCoord(coordYRegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val explicitComp = parseExplicitRadiusComp(upper)
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.CircleTangential(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    z = zVal?.first,
                    isIncrementalX = xVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: false,
                    explicitComp = explicitComp,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("C ") -> {
                val xVal = parseCoord(coordXRegex, upper)
                val yVal = parseCoord(coordYRegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val dir = if (upper.contains("DR-")) Direction.CW else Direction.CCW
                val explicitComp = parseExplicitRadiusComp(upper)
                val comp = explicitComp ?: RadiusComp.R0
                val isRapid = upper.contains("FMAX")
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.Circle(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    z = zVal?.first,
                    isIncrementalX = xVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: false,
                    isIncrementalZ = zVal?.second ?: false,
                    direction = dir,
                    comp = comp,
                    explicitComp = explicitComp,
                    isRapid = isRapid,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("L ") || upper == "L" -> {
                val xVal = parseCoord(coordXRegex, upper)
                val yVal = parseCoord(coordYRegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val explicitComp = parseExplicitRadiusComp(upper)
                val comp = explicitComp ?: RadiusComp.R0
                val isRapid = upper.contains("FMAX")
                val isFeedAuto = upper.contains("F AUTO") || upper.contains("FAUTO")
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.Linear(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    z = zVal?.first,
                    isIncrementalX = xVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: false,
                    isIncrementalZ = zVal?.second ?: false,
                    comp = comp,
                    explicitComp = explicitComp,
                    isRapid = isRapid,
                    isFeedAuto = isFeedAuto,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("LBL ") -> {
                val rest = commandText.substringAfter("LBL").trim().removePrefix("SET").trim()
                val qMatcher = quotedNameRegex.matcher(rest)
                if (qMatcher.find()) {
                    val labelName = (qMatcher.group(1) ?: "").lowercase(Locale.ROOT)
                    KlartextBlock.LabelSet(lineNum, line, labelName.hashCode(), labelName)
                } else {
                    val numToken = rest.split("\\s+".toRegex()).firstOrNull() ?: "0"
                    val num = numToken.toIntOrNull() ?: numToken.lowercase(Locale.ROOT).hashCode()
                    KlartextBlock.LabelSet(lineNum, line, num, numToken.lowercase(Locale.ROOT))
                }
            }

            upper.startsWith("CALL LBL") -> {
                val rest = commandText.substringAfter("CALL LBL").trim()
                val qMatcher = quotedNameRegex.matcher(rest)
                val labelName: String
                val lblNum: Int
                if (qMatcher.find()) {
                    labelName = (qMatcher.group(1) ?: "").lowercase(Locale.ROOT)
                    lblNum = labelName.hashCode()
                } else {
                    val tok = rest.split("\\s+".toRegex()).firstOrNull() ?: "1"
                    labelName = tok.lowercase(Locale.ROOT)
                    lblNum = tok.toIntOrNull() ?: labelName.hashCode()
                }
                val rep = if (upper.contains("REP")) {
                    val repPart = upper.substringAfter("REP").trim().split("\\s+|/".toRegex()).firstOrNull()
                    repPart?.toIntOrNull() ?: 1
                } else 1
                KlartextBlock.LabelCall(lineNum, line, lblNum, labelName, rep)
            }

            upper.startsWith("CYCL DEF") -> {
                parseCycleDefBlock(lineNum, line, commandText)
            }

            upper.startsWith("CYCL CALL") -> {
                val mCodes = parseMCodes(upper)
                KlartextBlock.CycleCall(lineNum, line, mCodes)
            }

            upper.startsWith("FN ") || upper.matches(Regex("^Q\\d+\\s*=.*")) -> {
                parseQParamBlock(lineNum, line, upper)
            }

            upper.startsWith("STOP") || upper == "M30" || upper == "M2" -> {
                val mCodes = parseMCodes(upper)
                KlartextBlock.Stop(lineNum, line, mCodes.firstOrNull())
            }

            else -> {
                KlartextBlock.Other(lineNum, line)
            }
        }
    }

    private fun parseCycleDefBlock(lineNum: Int, rawLine: String, fullCycleText: String): KlartextBlock.CycleDef {
        val upper = fullCycleText.uppercase(Locale.ROOT)
        val afterDef = upper.substringAfter("CYCL DEF").trim()
        val firstToken = afterDef.split("\\s+".toRegex()).firstOrNull() ?: "0"

        val mainNum: Int
        val subNum: Int
        if (firstToken.contains(".")) {
            val sp = firstToken.split(".")
            mainNum = sp[0].toIntOrNull() ?: 0
            subNum = sp.getOrNull(1)?.toIntOrNull() ?: 0
        } else {
            mainNum = firstToken.toIntOrNull() ?: 0
            subNum = 0
        }

        val qMap = mutableMapOf<Int, Float>()
        val qMatcher = qParamPairRegex.matcher(upper)
        while (qMatcher.find()) {
            val qIdx = qMatcher.group(1)?.toIntOrNull()
            val qVal = qMatcher.group(2)?.toFloatOrNull()
            if (qIdx != null && qVal != null) {
                qMap[qIdx] = qVal
            }
        }

        // Datum shift CYCL DEF 7.1 X... / 7.2 Y... / 7.3 Z...
        var dx: Float? = null
        var dy: Float? = null
        var dz: Float? = null
        var isInc = false
        if (mainNum == 7 && subNum in 1..3) {
            parseCoord(coordXRegex, upper)?.let { dx = it.first; isInc = isInc || it.second }
            parseCoord(coordYRegex, upper)?.let { dy = it.first; isInc = isInc || it.second }
            parseCoord(coordZRegex, upper)?.let { dz = it.first; isInc = isInc || it.second }
        }

        return KlartextBlock.CycleDef(
            lineNumber = lineNum,
            rawText = rawLine,
            cycleNumber = mainNum,
            subNumber = subNum,
            cycleName = afterDef.lineSequence().firstOrNull() ?: afterDef,
            qParams = qMap,
            datumX = dx,
            datumY = dy,
            datumZ = dz,
            isIncDatum = isInc
        )
    }

    private fun parseQParamBlock(lineNum: Int, rawLine: String, upper: String): KlartextBlock {
        // e.g. FN 0: Q1 = +10  or  FN 1: Q1 = +Q2 + +Q3  or  Q1 = 25
        val fnType = when {
            upper.startsWith("FN 1") -> 1
            upper.startsWith("FN 2") -> 2
            upper.startsWith("FN 3") -> 3
            upper.startsWith("FN 4") -> 4
            else -> 0
        }
        val eqSide = upper.substringAfter(":", upper)
        val targetStr = eqSide.substringBefore("=").trim().removePrefix("Q")
        val targetQ = targetStr.toIntOrNull() ?: 1
        val rhs = eqSide.substringAfter("=", "").trim()

        val tokens = rhs.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val tok1 = tokens.getOrNull(0) ?: "0"
        val isQ1 = tok1.contains("Q")
        val val1 = tok1.replace("Q", "").toFloatOrNull() ?: 0f

        val tok2 = tokens.getOrNull(2) ?: "0"
        val isQ2 = tok2.contains("Q")
        val val2 = tok2.replace("Q", "").toFloatOrNull() ?: 0f

        return KlartextBlock.QParamDef(
            lineNumber = lineNum,
            rawText = rawLine,
            fnType = fnType,
            targetQ = targetQ,
            value1 = val1,
            isQ1 = isQ1,
            value2 = val2,
            isQ2 = isQ2
        )
    }

    private fun extractLineNumberAndCommand(line: String, defaultIndex: Int): Pair<Int, String> {
        val matcher = lineNumRegex.matcher(line)
        return if (matcher.matches()) {
            val numStr = matcher.group(1)
            val rest = matcher.group(2) ?: ""
            val num = numStr?.toIntOrNull() ?: defaultIndex
            Pair(num, rest.trim())
        } else {
            Pair(defaultIndex, line.trim())
        }
    }

    private fun stripInlineComment(text: String): String {
        val idx = text.indexOf(';')
        return if (idx >= 0) text.substring(0, idx) else text
    }

    private fun parseCoord(pattern: Pattern, text: String): Pair<Float, Boolean>? {
        val m = pattern.matcher(text)
        return if (m.find()) {
            val prefix = m.group(1) ?: ""
            val isInc = prefix.uppercase(Locale.ROOT).startsWith("I")
            val v = m.group(2)?.toFloatOrNull() ?: 0f
            Pair(v, isInc)
        } else null
    }

    private fun parseValue(pattern: Pattern, text: String): Float? {
        val m = pattern.matcher(text)
        return if (m.find()) {
            m.group(1)?.toFloatOrNull()
        } else null
    }

    /**
     * Returns explicit radius compensation if written on the line:
     * - RL -> RadiusComp.RL
     * - RR -> RadiusComp.RR
     * - R0 -> RadiusComp.R0 (explicit cancellation!)
     * - null -> omitted via NO ENT (modal state stays unchanged!)
     */
    private fun parseExplicitRadiusComp(text: String): RadiusComp? {
        val tokens = text.split("\\s+".toRegex())
        return when {
            tokens.contains("RL") -> RadiusComp.RL
            tokens.contains("RR") -> RadiusComp.RR
            tokens.contains("R0") -> RadiusComp.R0
            else -> null
        }
    }

    private fun parseMCodes(text: String): List<Int> {
        val list = mutableListOf<Int>()
        val m = mCodeRegex.matcher(text)
        while (m.find()) {
            m.group(1)?.toIntOrNull()?.let { list.add(it) }
        }
        return list
    }
}

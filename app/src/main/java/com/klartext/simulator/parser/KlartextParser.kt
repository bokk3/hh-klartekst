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
    private val rawXRegex = Pattern.compile("(?:^|\\s)(IX|X)\\s*([+-]?[A-Z0-9_().+*\\-]+)", Pattern.CASE_INSENSITIVE)
    private val rawYRegex = Pattern.compile("(?:^|\\s)(IY|Y)\\s*([+-]?[A-Z0-9_().+*\\-]+)", Pattern.CASE_INSENSITIVE)
    private val rawZRegex = Pattern.compile("(?:^|\\s)(IZ|Z)\\s*([+-]?[A-Z0-9_().+*\\-]+)", Pattern.CASE_INSENSITIVE)
    private val polarPRRegex = Pattern.compile("(?:^|\\s)(IPR|PR)\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val polarPARegex = Pattern.compile("(?:^|\\s)(IPA|PA)\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val rawPRRegex = Pattern.compile("(?:^|\\s)(IPR|PR)\\s*([+-]?[A-Z0-9_().+*\\-]+)", Pattern.CASE_INSENSITIVE)
    private val rawPARegex = Pattern.compile("(?:^|\\s)(IPA|PA)\\s*([+-]?[A-Z0-9_().+*\\-]+)", Pattern.CASE_INSENSITIVE)
    private val radiusRegex = Pattern.compile("(?:^|\\s)R\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val rawRRegex = Pattern.compile("(?:^|\\s)R\\s*([+-]?[A-Z0-9_().+*\\-]+)", Pattern.CASE_INSENSITIVE)
    private val lenRegex = Pattern.compile("(?:^|\\s)LEN\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val ccaRegex = Pattern.compile("(?:^|\\s)CCA\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val dlRegex = Pattern.compile("(?:^|\\s)DL\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val drOffsetRegex = Pattern.compile("(?:^|\\s)DR\\s*([+-]\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val feedRegex = Pattern.compile("(?:^|\\s)F\\s*(\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val rawFeedRegex = Pattern.compile("(?:^|\\s)F\\s*([+-]?[A-Z0-9_().+*\\-]+)", Pattern.CASE_INSENSITIVE)
    private val speedRegex = Pattern.compile("(?:^|\\s)S\\s*(\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val mCodeRegex = Pattern.compile("(?:^|\\s)M\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
    private val qParamPairRegex = Pattern.compile("Q(\\d+)\\s*=\\s*([+-]?\\d*\\.?\\d+)", Pattern.CASE_INSENSITIVE)
    private val quotedNameRegex = Pattern.compile("\"([^\"]+)\"")
    private val qConditionalRegex = Pattern.compile(
        """(?:FN\s*(\d+)\s*:\s*)?IF\s+([^\s]+)\s+(EQU|NE|GT|LT)\s+([^\s]+)\s+GOTO\s+LBL\s+(.+)""",
        Pattern.CASE_INSENSITIVE
    )

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
                    } else if (rawLines[j - 1].contains("~") || nextRaw.startsWith("~")) {
                        combinedLines.append(" ").append(nextClean.removePrefix("~").trim())
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
                val rawXVal = parseRawCoord(rawXRegex, upper)
                val rawYVal = parseRawCoord(rawYRegex, upper)
                val incX = xVal?.second ?: rawXVal?.second ?: false
                val incY = yVal?.second ?: rawYVal?.second ?: false
                KlartextBlock.CircleCenter(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    rawX = rawXVal?.first,
                    rawY = rawYVal?.first,
                    isIncrementalX = incX,
                    isIncrementalY = incY,
                    isIncremental = incX || incY
                )
            }

            upper.startsWith("LP ") -> {
                val prVal = parseCoord(polarPRRegex, upper)
                val paVal = parseCoord(polarPARegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val rawPRVal = parseRawCoord(rawPRRegex, upper)
                val rawPAVal = parseRawCoord(rawPARegex, upper)
                val rawZVal = parseRawCoord(rawZRegex, upper)
                val explicitComp = parseExplicitRadiusComp(upper)
                val isRapid = upper.contains("FMAX")
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.LinearPolar(
                    lineNumber = lineNum,
                    rawText = line,
                    pr = prVal?.first,
                    pa = paVal?.first,
                    z = zVal?.first,
                    rawPR = rawPRVal?.first,
                    rawPA = rawPAVal?.first,
                    rawZ = rawZVal?.first,
                    isIncrementalPR = prVal?.second ?: rawPRVal?.second ?: false,
                    isIncrementalPA = paVal?.second ?: rawPAVal?.second ?: false,
                    explicitComp = explicitComp,
                    isRapid = isRapid,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("CP ") -> {
                val paVal = parseCoord(polarPARegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val rawPAVal = parseRawCoord(rawPARegex, upper)
                val rawZVal = parseRawCoord(rawZRegex, upper)
                val dir = if (upper.contains("DR-")) Direction.CW else Direction.CCW
                val explicitComp = parseExplicitRadiusComp(upper)
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.CirclePolar(
                    lineNumber = lineNum,
                    rawText = line,
                    pa = paVal?.first ?: 0f,
                    z = zVal?.first,
                    rawPA = rawPAVal?.first,
                    rawZ = rawZVal?.first,
                    isIncrementalPA = paVal?.second ?: rawPAVal?.second ?: false,
                    direction = dir,
                    explicitComp = explicitComp,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("CR ") -> {
                val xVal = parseCoord(coordXRegex, upper)
                val yVal = parseCoord(coordYRegex, upper)
                val rawXVal = parseRawCoord(rawXRegex, upper)
                val rawYVal = parseRawCoord(rawYRegex, upper)
                val r = parseValue(radiusRegex, upper) ?: 10f
                val rawR = extractRawParam(rawRRegex, upper)
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
                    rawX = rawXVal?.first,
                    rawY = rawYVal?.first,
                    isIncrementalX = xVal?.second ?: rawXVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: rawYVal?.second ?: false,
                    radius = r,
                    rawRadius = rawR,
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
                val rawXVal = parseRawCoord(rawXRegex, upper)
                val rawYVal = parseRawCoord(rawYRegex, upper)
                val rawZVal = parseRawCoord(rawZRegex, upper)
                val explicitComp = parseExplicitRadiusComp(upper)
                val feed = parseValue(feedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.CircleTangential(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    z = zVal?.first,
                    rawX = rawXVal?.first,
                    rawY = rawYVal?.first,
                    rawZ = rawZVal?.first,
                    isIncrementalX = xVal?.second ?: rawXVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: rawYVal?.second ?: false,
                    explicitComp = explicitComp,
                    feedRate = feed,
                    mCodes = mCodes
                )
            }

            upper.startsWith("C ") -> {
                val xVal = parseCoord(coordXRegex, upper)
                val yVal = parseCoord(coordYRegex, upper)
                val zVal = parseCoord(coordZRegex, upper)
                val rawXVal = parseRawCoord(rawXRegex, upper)
                val rawYVal = parseRawCoord(rawYRegex, upper)
                val rawZVal = parseRawCoord(rawZRegex, upper)
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
                    rawX = rawXVal?.first,
                    rawY = rawYVal?.first,
                    rawZ = rawZVal?.first,
                    isIncrementalX = xVal?.second ?: rawXVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: rawYVal?.second ?: false,
                    isIncrementalZ = zVal?.second ?: rawZVal?.second ?: false,
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
                val rawXVal = parseRawCoord(rawXRegex, upper)
                val rawYVal = parseRawCoord(rawYRegex, upper)
                val rawZVal = parseRawCoord(rawZRegex, upper)
                val explicitComp = parseExplicitRadiusComp(upper)
                val comp = explicitComp ?: RadiusComp.R0
                val isRapid = upper.contains("FMAX")
                val isFeedAuto = upper.contains("F AUTO") || upper.contains("FAUTO")
                val feed = parseValue(feedRegex, upper)
                val rawFeed = extractRawParam(rawFeedRegex, upper)
                val mCodes = parseMCodes(upper)
                KlartextBlock.Linear(
                    lineNumber = lineNum,
                    rawText = line,
                    x = xVal?.first,
                    y = yVal?.first,
                    z = zVal?.first,
                    rawX = rawXVal?.first,
                    rawY = rawYVal?.first,
                    rawZ = rawZVal?.first,
                    isIncrementalX = xVal?.second ?: rawXVal?.second ?: false,
                    isIncrementalY = yVal?.second ?: rawYVal?.second ?: false,
                    isIncrementalZ = zVal?.second ?: rawZVal?.second ?: false,
                    comp = comp,
                    explicitComp = explicitComp,
                    isRapid = isRapid,
                    isFeedAuto = isFeedAuto,
                    feedRate = feed,
                    rawFeed = rawFeed,
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
                val hasRep = upper.contains("REP")
                val rep = if (hasRep) {
                    val repPart = upper.substringAfter("REP").trim().split("\\s+|/".toRegex()).firstOrNull()
                    repPart?.toIntOrNull() ?: 1
                } else 1
                KlartextBlock.LabelCall(lineNum, line, lblNum, labelName, rep, isRepeat = hasRep)
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
        val rawQMap = mutableMapOf<Int, String>()
        val qMatcher = Pattern.compile("Q(\\d+)\\s*=\\s*([+-]?[A-Z0-9_.]+)", Pattern.CASE_INSENSITIVE).matcher(upper)
        while (qMatcher.find()) {
            val qIdx = qMatcher.group(1)?.toIntOrNull()
            val rawVal = qMatcher.group(2)?.trim() ?: ""
            if (qIdx != null) {
                rawQMap[qIdx] = rawVal
                val numVal = rawVal.toFloatOrNull()
                if (numVal != null) {
                    qMap[qIdx] = numVal
                }
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

        // SL sub-contour labels CYCL DEF 14.1 KONTURLABEL 1 / 2 ...
        val slLabels = mutableListOf<String>()
        if (mainNum == 14 && subNum == 1) {
            val after141 = afterDef.substringAfter("14.1").trim()
            val labelText = after141
                .removePrefix("KONTURLABEL")
                .removePrefix("CONTOURSTATUS")
                .trim()
            val tokens = labelText.split("[/\\s]+".toRegex()).filter { it.isNotBlank() }
            for (tok in tokens) {
                val clean = tok.removeSurrounding("\"").trim()
                if (clean.isNotEmpty() && clean != "/" && clean != ";") {
                    slLabels.add(clean)
                }
            }
        }

        // Rotation angle CYCL DEF 10.1 ROT+...
        var rotAngle: Float? = null
        var rawRot: String? = null
        if (mainNum == 10 && subNum == 1) {
            val rotMatcher = Pattern.compile("ROT\\s*([+-]?[A-Z0-9_.]*)", Pattern.CASE_INSENSITIVE).matcher(upper)
            if (rotMatcher.find()) {
                val token = rotMatcher.group(1)?.trim() ?: ""
                rawRot = token
                rotAngle = token.toFloatOrNull() ?: 0f
            } else {
                rotAngle = 0f
            }
        }

        // Mirror axes CYCL DEF 8.1 X / Y
        val mirrorAxes = mutableSetOf<String>()
        var isClearMirror = false
        if (mainNum == 8 && subNum == 1) {
            val after81 = upper.substringAfter("8.1").trim()
            val tokens = after81.split("\\s+".toRegex()).filter { it.isNotBlank() }
            var foundAxis = false
            for (tok in tokens) {
                if (tok == "X" || tok.startsWith("X")) { mirrorAxes.add("X"); foundAxis = true }
                if (tok == "Y" || tok.startsWith("Y")) { mirrorAxes.add("Y"); foundAxis = true }
                if (tok == "Z" || tok.startsWith("Z")) { mirrorAxes.add("Z"); foundAxis = true }
            }
            if (!foundAxis || tokens.isEmpty()) {
                isClearMirror = true
            }
        }

        return KlartextBlock.CycleDef(
            lineNumber = lineNum,
            rawText = rawLine,
            cycleNumber = mainNum,
            subNumber = subNum,
            cycleName = afterDef.lineSequence().firstOrNull() ?: afterDef,
            qParams = qMap,
            rawQParams = rawQMap,
            datumX = dx,
            datumY = dy,
            datumZ = dz,
            isIncDatum = isInc,
            labels = slLabels,
            rotationAngle = rotAngle,
            rawRotationAngle = rawRot,
            mirrorAxes = mirrorAxes,
            isClearMirror = isClearMirror
        )
    }

    private fun parseQParamBlock(lineNum: Int, rawLine: String, upper: String): KlartextBlock {
        val condMatcher = qConditionalRegex.matcher(upper)
        if (condMatcher.find()) {
            val fnNum = condMatcher.group(1)?.toIntOrNull() ?: 9
            val op1 = condMatcher.group(2)?.trim() ?: "0"
            val condition = condMatcher.group(3)?.uppercase(Locale.ROOT) ?: "EQU"
            val op2 = condMatcher.group(4)?.trim() ?: "0"
            val rawTarget = condMatcher.group(5)?.trim() ?: "0"
            val targetName: String
            val targetNum: Int
            if (rawTarget.startsWith("\"") && rawTarget.endsWith("\"")) {
                targetName = rawTarget.removeSurrounding("\"").lowercase(Locale.ROOT)
                targetNum = targetName.hashCode()
            } else {
                targetName = rawTarget.lowercase(Locale.ROOT)
                targetNum = rawTarget.toIntOrNull() ?: targetName.hashCode()
            }
            return KlartextBlock.QParamConditionalJump(
                lineNumber = lineNum,
                rawText = rawLine,
                fnType = fnNum,
                operand1 = op1,
                condition = condition,
                operand2 = op2,
                targetLabelNum = targetNum,
                targetLabelName = targetName
            )
        }

        // Assignment: FN 0..8 or free formula Qx = ...
        val fnType = if (upper.startsWith("FN ")) {
            upper.removePrefix("FN ").substringBefore(":").trim().toIntOrNull()
        } else null

        val assignmentPart = if (upper.contains(":")) upper.substringAfter(":") else upper
        val targetStr = assignmentPart.substringBefore("=").trim().removePrefix("Q").trim()
        val targetQ = targetStr.toIntOrNull() ?: 1
        val expression = assignmentPart.substringAfter("=", "").trim()

        return KlartextBlock.QParamAssignment(
            lineNumber = lineNum,
            rawText = rawLine,
            targetQ = targetQ,
            expression = expression,
            fnType = fnType
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

    private fun parseRawCoord(pattern: Pattern, text: String): Pair<String, Boolean>? {
        val m = pattern.matcher(text)
        return if (m.find()) {
            val prefix = m.group(1) ?: ""
            val isInc = prefix.uppercase(Locale.ROOT).startsWith("I")
            val expr = if (m.groupCount() >= 2) m.group(2) else m.group(1)
            Pair((expr ?: "").trim(), isInc)
        } else null
    }

    private fun extractRawParam(pattern: Pattern, text: String): String? {
        val m = pattern.matcher(text)
        return if (m.find()) {
            val expr = if (m.groupCount() >= 2) m.group(2) else m.group(1)
            expr?.trim()
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

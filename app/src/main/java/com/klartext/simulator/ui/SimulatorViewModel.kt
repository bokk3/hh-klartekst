package com.klartext.simulator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klartext.simulator.model.*
import com.klartext.simulator.parser.KlartextInterpreter
import com.klartext.simulator.parser.KlartextParser
import com.klartext.simulator.parser.ZMapSimulator
import com.klartext.simulator.sample.SamplePrograms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SimulatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var heightfieldJob: Job? = null
    private val interpreter = KlartextInterpreter()

    init {
        loadProgram(
            name = "CONTOURFREZEN_V4.H",
            code = SamplePrograms.contourfrezenV4
        )
    }

    fun loadProgram(name: String, code: String) {
        pause()
        val blocks = KlartextParser.parseProgram(code)
        val result = interpreter.interpret(blocks)

        val firstSeg = result.segments.firstOrNull()
        val initialMachineState = MachineState(
            x = firstSeg?.start?.x ?: 0f,
            y = firstSeg?.start?.y ?: 0f,
            z = firstSeg?.start?.z ?: 100f,
            feedRate = firstSeg?.feedRate ?: 0f,
            spindleSpeed = firstSeg?.spindleSpeed ?: 0f,
            activeTool = firstSeg?.toolNumber ?: 1,
            toolName = firstSeg?.toolName ?: "FREES16",
            toolRadius = firstSeg?.toolRadius ?: 8f,
            effectiveRadius = firstSeg?.effectiveRadius ?: 8f,
            dl = firstSeg?.dl ?: 0f,
            dr = firstSeg?.dr ?: 0f,
            radiusComp = firstSeg?.radiusComp ?: RadiusComp.R0,
            activeLineNumber = firstSeg?.lineNumber ?: (blocks.firstOrNull()?.lineNumber ?: 0),
            currentStepIndex = 0,
            totalSteps = result.segments.size,
            isRapid = firstSeg?.moveType == MoveType.RAPID
        )

        _uiState.update {
            it.copy(
                programName = name,
                rawCode = code,
                blocks = blocks,
                segments = result.segments,
                diagnostics = result.diagnostics,
                stock = result.stock,
                qParameters = result.qParameters,
                machineState = initialMachineState,
                isPlaying = false,
                activeDialog = null
            )
        }

        updateHeightmap(stepIndex = 0)
    }

    private fun updateHeightmap(stepIndex: Int) {
        heightfieldJob?.cancel()
        heightfieldJob = viewModelScope.launch(Dispatchers.Default) {
            val stock = _uiState.value.stock
            val segments = _uiState.value.segments
            val hf = ZMapSimulator.computeHeightfield(
                stock = stock,
                segments = segments,
                maxStepIndex = stepIndex,
                gridResolution = 90
            )
            _uiState.update { it.copy(heightfield = hf) }
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    private fun play() {
        if (_uiState.value.machineState.currentStepIndex >= _uiState.value.segments.size) {
            seekToStep(0)
        }

        _uiState.update { it.copy(isPlaying = true) }
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_uiState.value.isPlaying && _uiState.value.machineState.currentStepIndex < _uiState.value.segments.size) {
                val feedOv = _uiState.value.feedOverride
                if (feedOv <= 0) {
                    // Feed Hold (0% override): pause motion
                    delay(100L)
                    continue
                }
                val effectiveSpeed = (_uiState.value.playbackSpeed * (feedOv / 100f)).coerceAtLeast(0.05f)
                val delayMs = (400L / effectiveSpeed).toLong().coerceIn(20L, 2500L)
                delay(delayMs)
                stepForward()
                if (_uiState.value.isSingleBlockActive) {
                    pause()
                    break
                }
            }
            if (_uiState.value.machineState.currentStepIndex >= _uiState.value.segments.size) {
                pause()
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun reset() {
        pause()
        seekToStep(0)
    }

    fun stepForward() {
        val next = _uiState.value.machineState.currentStepIndex + 1
        if (next <= _uiState.value.segments.size) {
            seekToStep(next)
        }
    }

    fun stepBackward() {
        val prev = _uiState.value.machineState.currentStepIndex - 1
        if (prev >= 0) {
            seekToStep(prev)
        }
    }

    fun seekToStep(step: Int) {
        val segments = _uiState.value.segments
        val clampedStep = step.coerceIn(0, segments.size)

        val updatedMachineState = if (clampedStep > 0 && clampedStep <= segments.size) {
            val currentSeg = segments[clampedStep - 1]
            val feedOv = _uiState.value.feedOverride / 100f
            val spinOv = _uiState.value.spindleOverride / 100f
            _uiState.value.machineState.copy(
                x = currentSeg.end.x,
                y = currentSeg.end.y,
                z = currentSeg.end.z,
                feedRate = currentSeg.feedRate * feedOv,
                spindleSpeed = currentSeg.spindleSpeed * spinOv,
                activeTool = currentSeg.toolNumber,
                toolName = currentSeg.toolName,
                toolRadius = currentSeg.toolRadius,
                effectiveRadius = currentSeg.effectiveRadius,
                dl = currentSeg.dl,
                dr = currentSeg.dr,
                radiusComp = currentSeg.radiusComp,
                activeLineNumber = currentSeg.lineNumber,
                currentStepIndex = clampedStep,
                isRapid = currentSeg.moveType == MoveType.RAPID
            )
        } else {
            val firstSeg = segments.firstOrNull()
            _uiState.value.machineState.copy(
                x = firstSeg?.start?.x ?: 0f,
                y = firstSeg?.start?.y ?: 0f,
                z = firstSeg?.start?.z ?: 100f,
                currentStepIndex = 0,
                activeLineNumber = firstSeg?.lineNumber ?: 0
            )
        }

        _uiState.update {
            it.copy(machineState = updatedMachineState)
        }

        updateHeightmap(clampedStep)
    }

    fun setFeedOverride(percent: Int) {
        val clamped = percent.coerceIn(0, 150)
        _uiState.update { it.copy(feedOverride = clamped) }
        val step = _uiState.value.machineState.currentStepIndex
        if (step > 0 && step <= _uiState.value.segments.size) {
            val seg = _uiState.value.segments[step - 1]
            _uiState.update {
                it.copy(machineState = it.machineState.copy(feedRate = seg.feedRate * (clamped / 100f)))
            }
        }
    }

    fun setSpindleOverride(percent: Int) {
        val clamped = percent.coerceIn(0, 150)
        _uiState.update { it.copy(spindleOverride = clamped) }
        val step = _uiState.value.machineState.currentStepIndex
        if (step > 0 && step <= _uiState.value.segments.size) {
            val seg = _uiState.value.segments[step - 1]
            _uiState.update {
                it.copy(machineState = it.machineState.copy(spindleSpeed = seg.spindleSpeed * (clamped / 100f)))
            }
        }
    }

    fun toggleSingleBlock() {
        _uiState.update { it.copy(isSingleBlockActive = !it.isSingleBlockActive) }
    }

    fun toggleConsoleTwinScreen() {
        _uiState.update {
            val current = it.isConsoleTwinScreen ?: false
            it.copy(isConsoleTwinScreen = !current)
        }
    }

    fun ncStart() {
        if (_uiState.value.isSingleBlockActive) {
            stepForward()
        } else {
            play()
        }
    }

    fun ncStop() {
        pause()
    }

    fun setMachinePreset(preset: MachinePreset) {
        ToolTable.activePreset = preset
        _uiState.update { it.copy(machinePreset = preset) }
    }

    fun selectActiveTool(tool: TncTool) {
        val curState = _uiState.value.machineState
        val effRadius = (tool.radius + curState.dr).coerceAtLeast(0.1f)
        _uiState.update {
            it.copy(
                machineState = curState.copy(
                    activeTool = tool.number,
                    toolName = tool.name,
                    toolRadius = tool.radius,
                    effectiveRadius = effRadius,
                    spindleSpeed = tool.recommendedRpm.toFloat() * (it.spindleOverride / 100f),
                    feedRate = tool.recommendedFeed.toFloat() * (it.feedOverride / 100f),
                    spindleOn = true
                )
            )
        }
    }

    fun insertToolCall(tool: TncTool) {
        val toolCallLine = "TOOL CALL \"${tool.name}\" Z S${tool.recommendedRpm} F${tool.recommendedFeed}"
        insertLineIntoProgram(toolCallLine)
        selectActiveTool(tool)
    }

    fun navigateCodeLine(delta: Int) {
        val currentLine = _uiState.value.machineState.activeLineNumber
        val blockLines = _uiState.value.blocks.map { it.lineNumber }
        if (blockLines.isEmpty()) return
        val curIdx = blockLines.indexOf(currentLine)
        val targetIdx = if (curIdx >= 0) {
            (curIdx + delta).coerceIn(0, blockLines.lastIndex)
        } else {
            0
        }
        seekToLine(blockLines[targetIdx])
    }

    fun seekToLine(lineNumber: Int) {
        val segIndex = _uiState.value.segments.indexOfFirst { it.lineNumber >= lineNumber }
        if (segIndex >= 0) {
            seekToStep(segIndex + 1)
        } else {
            _uiState.update {
                it.copy(machineState = it.machineState.copy(activeLineNumber = lineNumber))
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun toggleProgrammedContour() {
        _uiState.update { it.copy(showProgrammedContour = !it.showProgrammedContour) }
    }

    fun openCalculator() { _uiState.update { it.copy(showCalculator = true) } }
    fun closeCalculator() { _uiState.update { it.copy(showCalculator = false) } }

    fun openDiagnostics() { _uiState.update { it.copy(showDiagnosticsSheet = true) } }
    fun closeDiagnostics() { _uiState.update { it.copy(showDiagnosticsSheet = false) } }

    fun openToolTable() { _uiState.update { it.copy(showToolTableDialog = true) } }
    fun closeToolTable() { _uiState.update { it.copy(showToolTableDialog = false) } }

    fun openQParamDialog() { _uiState.update { it.copy(showQParamDialog = true) } }
    fun closeQParamDialog() { _uiState.update { it.copy(showQParamDialog = false) } }

    fun openImportExport() { _uiState.update { it.copy(showImportExportDialog = true) } }
    fun closeImportExport() { _uiState.update { it.copy(showImportExportDialog = false) } }

    fun toggleSectionPlane() {
        val next = if (_uiState.value.sectionPlane == SectionPlane.X_AXIS) SectionPlane.Y_AXIS else SectionPlane.X_AXIS
        _uiState.update { it.copy(sectionPlane = next) }
    }

    fun toggleToolHolder() {
        _uiState.update { it.copy(showToolHolder = !it.showToolHolder) }
    }

    // --- TNC 640 Interactive Keypad Dialog Session ---

    fun startCommand(cmd: String) {
        val steps = when (cmd) {
            "L" -> listOf(
                DialogStep("COÖRDINAAT X?", "X", "+0", valueOnEntIfBlank = "X+0"),
                DialogStep("COÖRDINAAT Y?", "Y", "+0", valueOnEntIfBlank = "Y+0"),
                DialogStep("COÖRDINAAT Z?", "Z", "+0", omitOnNoEnt = true, valueOnEntIfBlank = null),
                DialogStep("RADIUSCOMPENSATIE? [R0 / RL / RR]", "", "R0", omitOnNoEnt = true, valueOnEntIfBlank = "R0"),
                DialogStep("VOEDING? [FMAX / F... / F AUTO]", "F", "FMAX", valueOnEntIfBlank = "FMAX"),
                DialogStep("M-FUNCTIE?", "M", "", omitOnNoEnt = true, valueOnEntIfBlank = null)
            )
            "CC" -> listOf(
                DialogStep("POOL / CIRKELMIDDELPUNT X?", "X", "+0", valueOnEntIfBlank = "X+0"),
                DialogStep("POOL / CIRKELMIDDELPUNT Y?", "Y", "+0", valueOnEntIfBlank = "Y+0")
            )
            "C" -> listOf(
                DialogStep("EINDPUNT CIRKELBOOG X?", "X", "+0", valueOnEntIfBlank = "X+0"),
                DialogStep("EINDPUNT CIRKELBOOG Y?", "Y", "+0", valueOnEntIfBlank = "Y+0"),
                DialogStep("DRAAIRICHTING? [DR+ / DR-]", "DR", "DR+", valueOnEntIfBlank = "DR+"),
                DialogStep("RADIUSCOMPENSATIE?", "", "R0", omitOnNoEnt = true, valueOnEntIfBlank = "R0"),
                DialogStep("VOEDING?", "F", "F800", valueOnEntIfBlank = "F800")
            )
            "CR" -> listOf(
                DialogStep("EINDPUNT CIRKEL X?", "X", "+0", valueOnEntIfBlank = "X+0"),
                DialogStep("EINDPUNT CIRKEL Y?", "Y", "+0", valueOnEntIfBlank = "Y+0"),
                DialogStep("CIRKELSTRAAL R? (+<=180° / ->180°)", "R+", "10", valueOnEntIfBlank = "R+10"),
                DialogStep("DRAAIRICHTING? [DR+ / DR-]", "DR", "DR+", valueOnEntIfBlank = "DR+"),
                DialogStep("RADIUSCOMPENSATIE?", "", "R0", omitOnNoEnt = true, valueOnEntIfBlank = "R0"),
                DialogStep("VOEDING?", "F", "F800", valueOnEntIfBlank = "F800")
            )
            "CT" -> listOf(
                DialogStep("TANGENTIEEL EINDPUNT X?", "X", "+0", valueOnEntIfBlank = "X+0"),
                DialogStep("TANGENTIEEL EINDPUNT Y?", "Y", "+0", valueOnEntIfBlank = "Y+0"),
                DialogStep("RADIUSCOMPENSATIE?", "", "R0", omitOnNoEnt = true, valueOnEntIfBlank = "R0"),
                DialogStep("VOEDING?", "F", "F800", valueOnEntIfBlank = "F800")
            )
            "RND" -> listOf(
                DialogStep("AFRONDINGSRADIUS R?", "R", "5", valueOnEntIfBlank = "R5")
            )
            "CHF" -> listOf(
                DialogStep("AFSCHUININGSLENGTE CHF?", "", "5", valueOnEntIfBlank = "5")
            )
            "APPR" -> listOf(
                DialogStep("TYPE [LCT / LT / CT]?", "", "LCT", valueOnEntIfBlank = "LCT"),
                DialogStep("AANLOOPPUNT X?", "X", "+0", valueOnEntIfBlank = "X+0"),
                DialogStep("AANLOOPPUNT Y?", "Y", "+0", valueOnEntIfBlank = "Y+0"),
                DialogStep("STRAAL R? / LENGTE LEN?", "R", "10", valueOnEntIfBlank = "R10"),
                DialogStep("RADIUSCOMPENSATIE? [RL / RR]", "", "RL", valueOnEntIfBlank = "RL"),
                DialogStep("VOEDING?", "F", "F800", valueOnEntIfBlank = "F800")
            )
            "DEP" -> listOf(
                DialogStep("TYPE [LCT / LT / CT]?", "", "LCT", valueOnEntIfBlank = "LCT"),
                DialogStep("AFROEPPUNT X?", "X", "-20", valueOnEntIfBlank = "X-20"),
                DialogStep("AFROEPPUNT Y?", "Y", "-20", valueOnEntIfBlank = "Y-20"),
                DialogStep("STRAAL R?", "R", "10", valueOnEntIfBlank = "R10"),
                DialogStep("VOEDING?", "F", "F1000", valueOnEntIfBlank = "F1000")
            )
            "TOOL CALL" -> listOf(
                DialogStep("GEREEDSCHAP NR OF NAAM?", "\"", "FREES16\"", valueOnEntIfBlank = "\"FREES16\""),
                DialogStep("SPIL-AS? [Z / X / Y]", "", "Z", valueOnEntIfBlank = "Z"),
                DialogStep("TOERENTAL S?", "S", "3500", valueOnEntIfBlank = "S3500"),
                DialogStep("VOEDING F?", "F", "1000", valueOnEntIfBlank = "F1000"),
                DialogStep("LENGTECORRECTIE DL?", "DL", "0", omitOnNoEnt = true, valueOnEntIfBlank = null),
                DialogStep("RADIUSCORRECTIE DR?", "DR", "0", omitOnNoEnt = true, valueOnEntIfBlank = null)
            )
            "LBL SET" -> listOf(
                DialogStep("LABEL NUMMER OF NAAM?", "", "1", valueOnEntIfBlank = "1")
            )
            "CALL LBL" -> listOf(
                DialogStep("CALL LABEL?", "", "1", valueOnEntIfBlank = "1"),
                DialogStep("HERHALINGEN REP?", "REP", "1", omitOnNoEnt = true, valueOnEntIfBlank = null)
            )
            else -> listOf(
                DialogStep("PARAMETERS INVOEREN:", "", "", valueOnEntIfBlank = null)
            )
        }

        _uiState.update {
            it.copy(
                activeDialog = ActiveDialogSession(
                    commandTitle = cmd,
                    baseCommand = cmd,
                    steps = steps,
                    currentStepIndex = 0,
                    accumulatedTokens = listOf(cmd),
                    currentInput = ""
                )
            )
        }
    }

    fun onDialogChar(str: String) {
        _uiState.update {
            val d = it.activeDialog ?: return@update it
            it.copy(activeDialog = d.copy(currentInput = d.currentInput + str))
        }
    }

    fun onDialogDel() {
        _uiState.update {
            val d = it.activeDialog ?: return@update it
            if (d.currentInput.isNotEmpty()) {
                it.copy(activeDialog = d.copy(currentInput = d.currentInput.dropLast(1)))
            } else it
        }
    }

    fun onDialogEnt() {
        val d = _uiState.value.activeDialog ?: return
        val step = d.currentStep ?: return
        val rawInput = d.currentInput.trim()

        val tokenToAdd: String? = when {
            rawInput.isNotEmpty() -> {
                if (step.tokenPrefix.isNotEmpty() && !rawInput.startsWith(step.tokenPrefix)) {
                    step.tokenPrefix + rawInput
                } else rawInput
            }
            step.valueOnEntIfBlank != null -> step.valueOnEntIfBlank
            else -> null
        }

        val newTokens = if (tokenToAdd != null) d.accumulatedTokens + tokenToAdd else d.accumulatedTokens
        advanceDialog(d, newTokens)
    }

    fun onDialogNoEnt() {
        // Essential TNC feature: skips optional token (e.g. radius compensation prompt) WITHOUT writing R0!
        val d = _uiState.value.activeDialog ?: return
        advanceDialog(d, d.accumulatedTokens)
    }

    fun onDialogEnd() {
        val d = _uiState.value.activeDialog ?: return
        finishDialog(d.accumulatedTokens)
    }

    private fun advanceDialog(d: ActiveDialogSession, updatedTokens: List<String>) {
        val nextIdx = d.currentStepIndex + 1
        if (nextIdx < d.steps.size) {
            _uiState.update {
                it.copy(
                    activeDialog = d.copy(
                        currentStepIndex = nextIdx,
                        accumulatedTokens = updatedTokens,
                        currentInput = ""
                    )
                )
            }
        } else {
            finishDialog(updatedTokens)
        }
    }

    private fun finishDialog(tokens: List<String>) {
        val fullLine = tokens.joinToString(" ")
        _uiState.update { it.copy(activeDialog = null) }
        insertLineIntoProgram(fullLine)
    }

    private fun insertLineIntoProgram(lineText: String) {
        val currentLines = _uiState.value.rawCode.lines().toMutableList()
        val curActiveLine = _uiState.value.machineState.activeLineNumber
        val insertIdx = (currentLines.indexOfFirst {
            val n = it.trim().split("\\s+".toRegex()).firstOrNull()?.toIntOrNull()
            n != null && n >= curActiveLine
        } + 1).coerceIn(1, currentLines.size)

        currentLines.add(insertIdx, lineText)

        // Renumber lines sequentially 0, 1, 2, ...
        val renumbered = currentLines.mapIndexed { idx, ln ->
            val stripped = ln.trim().replaceFirst(Regex("^\\d+\\s+"), "")
            "$idx $stripped"
        }.joinToString("\n")

        loadProgram(_uiState.value.programName, renumbered)
        seekToLine(insertIdx)
    }

    fun copyCurrentLine() {
        val activeLine = _uiState.value.machineState.activeLineNumber
        val line = _uiState.value.blocks.firstOrNull { it.lineNumber == activeLine }?.rawText
        if (line != null) {
            _uiState.update { it.copy(clipboardLine = line) }
        }
    }

    fun pasteLine() {
        val clip = _uiState.value.clipboardLine ?: return
        insertLineIntoProgram(clip)
    }

    fun deleteCurrentLine() {
        val activeLine = _uiState.value.machineState.activeLineNumber
        val currentLines = _uiState.value.rawCode.lines().filterNot {
            val n = it.trim().split("\\s+".toRegex()).firstOrNull()?.toIntOrNull()
            n == activeLine
        }
        val renumbered = currentLines.mapIndexed { idx, ln ->
            val stripped = ln.trim().replaceFirst(Regex("^\\d+\\s+"), "")
            "$idx $stripped"
        }.joinToString("\n")

        loadProgram(_uiState.value.programName, renumbered)
    }
}

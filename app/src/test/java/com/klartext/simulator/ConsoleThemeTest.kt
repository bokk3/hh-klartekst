package com.klartext.simulator

import androidx.compose.ui.graphics.Color
import com.klartext.simulator.model.ConsoleTheme
import com.klartext.simulator.model.MobileWorkspaceTab
import com.klartext.simulator.model.SimulationUiState
import com.klartext.simulator.ui.theme.ClassicTnc640ConsoleColors
import com.klartext.simulator.ui.theme.HypermodernConsoleColors
import com.klartext.simulator.ui.theme.getConsoleColors
import org.junit.Assert.*
import org.junit.Test

class ConsoleThemeTest {

    @Test
    fun testDefaultThemeIsClassicTnc640() {
        val state = SimulationUiState()
        assertEquals(
            "Default console theme should be CLASSIC_TNC640",
            ConsoleTheme.CLASSIC_TNC640,
            state.consoleTheme
        )
    }

    @Test
    fun testClassicTnc640ColorsAreAuthentic() {
        val colors = getConsoleColors(ConsoleTheme.CLASSIC_TNC640)
        assertTrue(colors.isClassic)

        // Casing is authentic light metallic grey (RAL 7035 equivalent)
        assertEquals(Color(0xFFC7CED6), colors.casingBackground)

        // Numpad keycap is ivory / off-white with dark ink text
        assertEquals(Color(0xFFF8FAFC), colors.numpadKeyBackground)
        assertEquals(Color(0xFF0F172A), colors.numpadKeyText)

        // Contour keys have dark graphite background with Heidenhain orange legends
        assertEquals(Color(0xFF1E2832), colors.contourKeyBackground)
        assertEquals(Color(0xFFFF9100), colors.contourKeyText)

        // Machine green ENT and warning red NO ENT
        assertEquals(Color(0xFF15803D), colors.entKeyBackground)
        assertEquals(Color(0xFFB91C1C), colors.noEntKeyBackground)

        // Rotary dial faceplate is brushed aluminum
        assertEquals(Color(0xFFD3DBE3), colors.dialCardBackground)
    }

    @Test
    fun testHypermodernColorsAreCyberDark() {
        val colors = getConsoleColors(ConsoleTheme.HYPERMODERN)
        assertFalse(colors.isClassic)

        // Dark cyber carbon casing
        assertEquals(Color(0xFF141A20), colors.casingBackground)

        // Dark slate numpad keys with light text
        assertEquals(Color(0xFF2C3942), colors.numpadKeyBackground)
        assertEquals(Color(0xFFECEFF1), colors.numpadKeyText)

        // Dial faceplate is dark slate
        assertEquals(Color(0xFF1B2228), colors.dialCardBackground)
    }

    @Test
    fun testDefaultMobileWorkspaceIsSimulationAndKeypadReady() {
        val state = SimulationUiState()
        assertEquals(MobileWorkspaceTab.SIMULATION, state.activeMobileTab)
        assertTrue(state.isPortraitKeypadExpanded)
    }
}

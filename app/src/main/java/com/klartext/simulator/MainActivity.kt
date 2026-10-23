package com.klartext.simulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.klartext.simulator.ui.SimulatorScreen
import com.klartext.simulator.ui.SimulatorViewModel
import com.klartext.simulator.ui.theme.KlartextSimulatorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SimulatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KlartextSimulatorTheme {
                SimulatorScreen(viewModel = viewModel)
            }
        }
    }
}

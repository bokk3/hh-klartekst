package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.klartext.simulator.ui.theme.*
import java.util.Locale
import kotlin.math.PI

@Composable
fun CuttingCalculatorDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = CncPanelBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TNC 640 VERSPANINGSCALCULATOR",
                        color = CncAccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CncSurfaceDark),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Sluiten", fontSize = 11.sp, color = CncTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("Toerental / F", "Tap M3-M16", "Centerboor 90°")
                    tabs.forEachIndexed { idx, label ->
                        val isSel = selectedTab == idx
                        Button(
                            onClick = { selectedTab = idx },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) CncAccentOrange else CncSurfaceDark
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSel) CncDarkBackground else CncTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> MillingSpeedFeedCalculator()
                    1 -> TappingCalculator()
                    2 -> SpotDrillCalculator()
                }
            }
        }
    }
}

@Composable
private fun MillingSpeedFeedCalculator() {
    var vcStr by remember { mutableStateOf("120") }
    var diameterStr by remember { mutableStateOf("16") }
    var teethStr by remember { mutableStateOf("4") }
    var fzStr by remember { mutableStateOf("0.08") }

    val vc = vcStr.toFloatOrNull() ?: 120f
    val d = (diameterStr.toFloatOrNull() ?: 16f).coerceAtLeast(0.1f)
    val z = (teethStr.toIntOrNull() ?: 4).coerceAtLeast(1)
    val fz = fzStr.toFloatOrNull() ?: 0.08f

    // N = (Vc * 1000) / (pi * D)
    val rpm = ((vc * 1000f) / (PI.toFloat() * d)).roundToInt()
    // F = z * fz * N
    val feed = (z * fz * rpm).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcInputField(label = "Snijsnelheid Vc (m/min):", value = vcStr, onValueChange = { vcStr = it })
        CalcInputField(label = "Freesdiameter D (mm):", value = diameterStr, onValueChange = { diameterStr = it })
        CalcInputField(label = "Aantal tanden z:", value = teethStr, onValueChange = { teethStr = it })
        CalcInputField(label = "Aanzet per tand fz (mm/tand):", value = fzStr, onValueChange = { fzStr = it })

        Divider(color = CncBorder, modifier = Modifier.padding(vertical = 4.dp))

        ResultBox(
            title = "BEREKENDE S & F",
            items = listOf(
                "Spiltoerental S:" to "$rpm RPM",
                "Voeding F:" to "$feed mm/min",
                "Formule:" to "N=(Vc×1000)/(π×D) | F=z×fz×N"
            )
        )
    }
}

@Composable
private fun TappingCalculator() {
    val taps = listOf(
        Triple("M3", 0.5f, 2.5f),
        Triple("M4", 0.7f, 3.3f),
        Triple("M5", 0.8f, 4.2f),
        Triple("M6", 1.0f, 5.0f),
        Triple("M8", 1.25f, 6.8f),
        Triple("M10", 1.5f, 8.5f),
        Triple("M12", 1.75f, 10.2f),
        Triple("M16", 2.0f, 14.0f)
    )
    var selectedTapIdx by remember { mutableIntStateOf(4) } // Default M8
    var rpmStr by remember { mutableStateOf("300") }

    val tap = taps[selectedTapIdx]
    val rpm = rpmStr.toFloatOrNull() ?: 300f
    val feed = (rpm * tap.second).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kies schroefdraadmaat:", fontSize = 12.sp, color = CncTextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            taps.take(4).forEachIndexed { i, t ->
                TapPill(label = t.first, isSelected = selectedTapIdx == i) { selectedTapIdx = i }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            taps.drop(4).forEachIndexed { i, t ->
                val realIdx = i + 4
                TapPill(label = t.first, isSelected = selectedTapIdx == realIdx) { selectedTapIdx = realIdx }
            }
        }

        CalcInputField(label = "Taptoerental S (RPM):", value = rpmStr, onValueChange = { rpmStr = it })

        Divider(color = CncBorder, modifier = Modifier.padding(vertical = 4.dp))

        ResultBox(
            title = "TAP PARAMETERS VOOR ${tap.first}",
            items = listOf(
                "Spoed (P):" to "${tap.second} mm",
                "Voorboor D (kerngat):" to "Ø${tap.third} mm (VHMBOOR${tap.third})",
                "Geprogrammeerde voeding Q206:" to "$feed mm/min",
                "Verificatie Cyclus 206:" to "Q206 MOET gelijk zijn aan S(${rpm.toInt()}) × ${tap.second}!"
            )
        )
    }
}

@Composable
private fun RowScope.TapPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) CncAccentOrange else CncSurfaceDark
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) CncDarkBackground else CncTextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SpotDrillCalculator() {
    var holeDiameterStr by remember { mutableStateOf("6.8") }
    var chamferStr by remember { mutableStateOf("0.5") }

    val holeD = holeDiameterStr.toFloatOrNull() ?: 6.8f
    val chamf = chamferStr.toFloatOrNull() ?: 0.5f

    // Z = -(D_gat + 2 * chamfer) / 2
    val depthZ = -((holeD + 2f * chamf) / 2f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcInputField(label = "Diameter gat D_gat (mm):", value = holeDiameterStr, onValueChange = { holeDiameterStr = it })
        CalcInputField(label = "Gewenste afschuining (mm x 45°):", value = chamferStr, onValueChange = { chamferStr = it })

        Divider(color = CncBorder, modifier = Modifier.padding(vertical = 4.dp))

        ResultBox(
            title = "NC CENTERBOOR 90° DIEPTE (CYCL DEF 240)",
            items = listOf(
                "Centerdiepte Z (Q201):" to String.format(Locale.US, "%.3f mm", depthZ),
                "Bovenkant afschuining Ø:" to String.format(Locale.US, "Ø%.2f mm", holeD + 2f * chamf),
                "Formule:" to "Z = -(D_gat + 2×afschuining) / 2"
            )
        )
    }
}

@Composable
private fun CalcInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = CncTextSecondary, modifier = Modifier.weight(1.2f))
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CncSurfaceDark,
                unfocusedContainerColor = CncSurfaceDark,
                focusedTextColor = CncDroGreen,
                unfocusedTextColor = CncDroGreen,
                focusedIndicatorColor = CncAccentOrange
            ),
            modifier = Modifier
                .width(100.dp)
                .height(44.dp)
        )
    }
}

@Composable
private fun ResultBox(
    title: String,
    items: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CncDarkBackground, RoundedCornerShape(6.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, fontSize = 11.sp, color = CncAccentOrange, fontWeight = FontWeight.Bold)
        items.forEach { (lbl, valStr) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(lbl, fontSize = 11.sp, color = CncTextSecondary)
                Text(
                    valStr,
                    fontSize = 12.sp,
                    color = CncDroGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun Float.roundToInt(): Int = Math.round(this)

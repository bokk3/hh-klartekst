package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.klartext.simulator.model.RadiusComp
import com.klartext.simulator.model.ToolTable
import com.klartext.simulator.parser.DxfParser
import com.klartext.simulator.parser.DxfToKlartextConverter
import com.klartext.simulator.parser.IsoGCodeConverter
import com.klartext.simulator.ui.theme.*

@Composable
fun ImportExportDialog(
    currentKlartext: String,
    currentProgramName: String,
    onLoadConvertedProgram: (name: String, code: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = CncPanelBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BESTANDSBEHEER & CONVERSIE",
                        color = CncAccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Sluiten", tint = CncTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("DXF Import", "ISO -> Klartekst", "Klartekst -> ISO").forEachIndexed { idx, label ->
                        val isSel = selectedTab == idx
                        Button(
                            onClick = { selectedTab = idx },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) CncAccentOrange else CncSurfaceDark
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = if (isSel) CncDarkBackground else CncTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> DxfImportTab(onLoadConvertedProgram, onDismiss)
                    1 -> IsoToKlartextTab(onLoadConvertedProgram, onDismiss)
                    2 -> KlartextToIsoTab(currentKlartext, clipboardManager)
                }
            }
        }
    }
}

@Composable
private fun DxfImportTab(
    onLoadConvertedProgram: (name: String, code: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sampleDxf = """
        0
        SECTION
        2
        ENTITIES
        0
        LINE
        10
        10.0
        20
        10.0
        11
        90.0
        20
        10.0
        0
        ARC
        10
        90.0
        20
        25.0
        40
        15.0
        50
        270.0
        51
        90.0
        0
        LINE
        10
        90.0
        20
        40.0
        11
        10.0
        20
        40.0
        0
        LINE
        10
        10.0
        20
        40.0
        11
        10.0
        20
        10.0
        0
        ENDSEC
        0
        EOF
    """.trimIndent()

    var dxfText by remember { mutableStateOf(sampleDxf) }
    var programName by remember { mutableStateOf("DXF_CONTOUR") }
    var selectedTool by remember { mutableStateOf("FREES16") }
    var depthStr by remember { mutableStateOf("-12") }
    var leadRadiusStr by remember { mutableStateOf("10") }
    var isRL by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextField(
                value = programName,
                onValueChange = { programName = it },
                label = { Text("Programma", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextField(
                value = depthStr,
                onValueChange = { depthStr = it },
                label = { Text("Diepte Z", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )
            TextField(
                value = leadRadiusStr,
                onValueChange = { leadRadiusStr = it },
                label = { Text("Aanloop R", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Radiuscompensatie: ", fontSize = 11.sp, color = CncTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { isRL = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRL) CncAccentOrange else CncSurfaceDark),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("RL (Links)", fontSize = 10.sp, color = if (isRL) CncDarkBackground else CncTextPrimary)
                }
                Button(
                    onClick = { isRL = false },
                    colors = ButtonDefaults.buttonColors(containerColor = if (!isRL) CncAccentOrange else CncSurfaceDark),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("RR (Rechts)", fontSize = 10.sp, color = if (!isRL) CncDarkBackground else CncTextPrimary)
                }
            }
        }

        Text("Plak DXF ASCII code (of gebruik voorbeeldcontour):", fontSize = 10.sp, color = CncTextSecondary)
        TextField(
            value = dxfText,
            onValueChange = { dxfText = it },
            maxLines = 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        )

        Button(
            onClick = {
                val entities = DxfParser.parse(dxfText)
                val config = DxfToKlartextConverter.ConversionConfig(
                    programName = programName,
                    toolName = selectedTool,
                    cuttingDepth = depthStr.toFloatOrNull() ?: -12f,
                    leadRadius = leadRadiusStr.toFloatOrNull() ?: 10f,
                    comp = if (isRL) RadiusComp.RL else RadiusComp.RR
                )
                val klartextCode = DxfToKlartextConverter.convert(entities, config)
                onLoadConvertedProgram(programName + ".H", klartextCode)
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CncAccentOrange),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Genereer Klartekst & Open in Simulator", color = CncDarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun IsoToKlartextTab(
    onLoadConvertedProgram: (name: String, code: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sampleIso = """
        %
        O2001 (ISO MILLING DEMO)
        T3 M06
        S3500 M03
        G00 G90 X-20.0 Y-20.0 Z50.0
        Z2.0
        G01 Z-8.0 F400
        G41 X15.0 Y15.0 F1000
        X105.0 Y15.0
        G03 X105.0 Y65.0 R25.0
        X15.0 Y65.0
        X15.0 Y15.0
        G40 X-20.0 Y-20.0
        G00 Z100.0 M05
        M30
        %
    """.trimIndent()

    var isoCode by remember { mutableStateOf(sampleIso) }
    var programName by remember { mutableStateOf("ISO_DEMO") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextField(
            value = programName,
            onValueChange = { programName = it },
            label = { Text("Programmanaam", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Plak Fanuc / Haas / Siemens G-code:", fontSize = 10.sp, color = CncTextSecondary)
        TextField(
            value = isoCode,
            onValueChange = { isoCode = it },
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        )

        Button(
            onClick = {
                val converted = IsoGCodeConverter.convertIsoToKlartext(isoCode, programName)
                onLoadConvertedProgram(programName + ".H", converted)
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CncAccentOrange),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Converteer ISO naar Klartekst & Simuleer", color = CncDarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun KlartextToIsoTab(
    currentKlartext: String,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val isoConverted = remember(currentKlartext) {
        IsoGCodeConverter.convertKlartextToIso(currentKlartext)
    }
    var copied by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Geëxporteerde universele ISO G-code:", fontSize = 11.sp, color = CncTextSecondary)
        TextField(
            value = isoConverted,
            onValueChange = {},
            readOnly = true,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Button(
            onClick = {
                clipboardManager.setText(AnnotatedString(isoConverted))
                copied = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = CncSurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (copied) "Gekopieerd naar klembord!" else "Kopieer ISO G-code", color = CncDroGreen, fontSize = 12.sp)
        }
    }
}

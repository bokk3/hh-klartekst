package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.klartext.simulator.model.*
import com.klartext.simulator.ui.theme.*
import java.util.Locale

/**
 * Functional 5-Axis Tool Management Dialog (TOOL.T)
 * Supports authentic standard tooling for:
 * - DMG MORI DMU 50 (HSK-A63, 30 Pockets)
 * - DN Solutions DVF 5000 (BT40, 40 Pockets)
 * - Standard Workshop (SK40, 11 Tools)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolTableDialog(
    activePreset: MachinePreset = MachinePreset.DMU_50,
    onSelectPreset: (MachinePreset) -> Unit,
    onSelectTool: (TncTool) -> Unit,
    onInsertToolCall: (TncTool) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALLE") }

    val currentTools = when (activePreset) {
        MachinePreset.DMU_50 -> ToolTable.dmu50Tools
        MachinePreset.DVF_5000 -> ToolTable.dvf5000Tools
        MachinePreset.STANDARD_WORKSHOP -> ToolTable.standardTools
    }

    val filteredTools = currentTools.filter { tool ->
        val matchesCategory = when (selectedCategory) {
            "ALLE" -> true
            "FREZEN" -> tool.geometryType == ToolGeometryType.CYLINDRICAL_ENDMILL || tool.geometryType == ToolGeometryType.FACE_MILL
            "3D VORM" -> tool.geometryType == ToolGeometryType.BALL_NOSE || tool.geometryType == ToolGeometryType.TORUS_MILL
            "BOREN" -> tool.geometryType == ToolGeometryType.TWIST_DRILL || tool.geometryType == ToolGeometryType.SPOT_DRILL_90
            "TAPPEN" -> tool.geometryType == ToolGeometryType.TAP || tool.geometryType == ToolGeometryType.THREAD_MILL
            "KANTEN" -> tool.geometryType == ToolGeometryType.CHAMFER_MILL_45
            "TASTER" -> tool.geometryType == ToolGeometryType.TOUCH_PROBE
            else -> true
        }

        val q = searchQuery.trim().uppercase(Locale.ROOT)
        val matchesSearch = q.isEmpty() ||
                tool.name.uppercase(Locale.ROOT).contains(q) ||
                "T${tool.number}".contains(q) ||
                tool.description.uppercase(Locale.ROOT).contains(q) ||
                "${tool.diameter}".contains(q)

        matchesCategory && matchesSearch
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = CncPanelBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TNC 640 GEREEDSCHAPSTABEL (TOOL.T)",
                            color = CncAccentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${activePreset.displayName} (${currentTools.size} Tools - ${activePreset.holderStandard})",
                            color = CncTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Sluiten", tint = CncTextPrimary)
                    }
                }

                // 1. Machine Preset Switcher Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CncSurfaceDark, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        MachinePreset.DMU_50 to "DMU 50 (HSK-A63)",
                        MachinePreset.DVF_5000 to "DVF 5000 (BT40)",
                        MachinePreset.STANDARD_WORKSHOP to "STANDAARD (SK40)"
                    ).forEach { (preset, label) ->
                        val isSelected = activePreset == preset
                        Button(
                            onClick = { onSelectPreset(preset) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CncAccentOrange else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CncDarkBackground else CncTextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. Search Field & Category Filter Chips
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Zoek tool op naam, nummer (T...) of diameter...", fontSize = 11.sp, color = CncTextSecondary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CncTextPrimary,
                        unfocusedTextColor = CncTextPrimary,
                        focusedBorderColor = CncAccentOrange,
                        unfocusedBorderColor = CncBorder
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Zoeken", tint = CncTextSecondary, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Wissen", tint = CncTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                // Category Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ALLE", "FREZEN", "3D VORM", "BOREN", "TAPPEN", "KANTEN", "TASTER").forEach { cat ->
                        val isSel = selectedCategory == cat
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CncDroGreen,
                                selectedLabelColor = CncDarkBackground,
                                containerColor = CncSurfaceDark,
                                labelColor = CncTextSecondary
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                // 3. Tool List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredTools) { tool ->
                        ToolCardItem(
                            tool = tool,
                            onSelectTool = {
                                onSelectTool(tool)
                                onDismiss()
                            },
                            onInsertToolCall = {
                                onInsertToolCall(tool)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCardItem(
    tool: TncTool,
    onSelectTool: () -> Unit,
    onInsertToolCall: () -> Unit
) {
    val typeBadgeColor = when (tool.geometryType) {
        ToolGeometryType.FACE_MILL -> Color(0xFFD35400)
        ToolGeometryType.CYLINDRICAL_ENDMILL -> Color(0xFF00ACC1)
        ToolGeometryType.BALL_NOSE -> Color(0xFF43A047)
        ToolGeometryType.TORUS_MILL -> Color(0xFF7CB342)
        ToolGeometryType.SPOT_DRILL_90, ToolGeometryType.TWIST_DRILL -> Color(0xFF1E88E5)
        ToolGeometryType.TAP, ToolGeometryType.THREAD_MILL -> Color(0xFF8E24AA)
        ToolGeometryType.CHAMFER_MILL_45 -> Color(0xFFFB8C00)
        ToolGeometryType.TOUCH_PROBE -> Color(0xFFE91E63)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CncSurfaceDark, RoundedCornerShape(6.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Line 1: T-Number, Name, Type Badge, Diameter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = CncBorder,
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = "T${tool.number.toString().padStart(2, '0')}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = CncAccentOrange,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "\"${tool.name}\"",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CncTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = typeBadgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(3.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, typeBadgeColor)
                ) {
                    Text(
                        text = tool.geometryType.name.replace("_", " "),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeBadgeColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = "Ø${tool.diameter} mm" + (if (tool.cornerRadius > 0) " R${tool.cornerRadius}" else ""),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CncDroGreen
            )
        }

        // Line 2: Description & Geometry Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tool.description,
                fontSize = 10.sp,
                color = CncTextSecondary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "L=${tool.length.toInt()}mm | Z=${tool.teeth} | ${tool.holder.type}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFB0BEC5)
            )
        }

        // Line 3: Cutting technology & Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141A20), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "S: ${tool.recommendedRpm} RPM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CncDroGreen,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "F: ${tool.recommendedFeed} mm/min",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CncFeedCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onSelectTool,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.height(22.dp)
                ) {
                    Text("ACTIVEREN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CncAccentOrange)
                }

                Button(
                    onClick = onInsertToolCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.height(22.dp)
                ) {
                    Text("TOOL CALL (.H)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

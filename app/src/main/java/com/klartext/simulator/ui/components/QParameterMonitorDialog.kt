package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.klartext.simulator.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@Composable
fun QParameterMonitorDialog(
    qParameters: Map<Int, Float>,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showNonZeroOnly by remember { mutableStateOf(false) }

    val sortedEntries = remember(qParameters, searchQuery, showNonZeroOnly) {
        qParameters.entries
            .filter { (q, value) ->
                val matchesQuery = searchQuery.isBlank() ||
                        "Q$q".contains(searchQuery, ignoreCase = true) ||
                        q.toString().contains(searchQuery) ||
                        String.format(Locale.US, "%.3f", value).contains(searchQuery)
                val matchesNonZero = !showNonZeroOnly || abs(value) > 1e-5f
                matchesQuery && matchesNonZero
            }
            .sortedBy { it.key }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
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
                    Column {
                        Text(
                            text = "TNC 640 Q-PARAMETER MONITOR",
                            color = CncAccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${qParameters.size} parameters geladen • ${sortedEntries.size} zichtbaar",
                            color = CncTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Sluiten", tint = CncTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search & Filter controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Zoek Q..", fontSize = 11.sp, color = CncTextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CncTextSecondary, modifier = Modifier.size(16.dp))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CncTextPrimary,
                            unfocusedTextColor = CncTextPrimary,
                            focusedBorderColor = CncAccentCyan,
                            unfocusedBorderColor = CncBorder
                        )
                    )

                    FilterChip(
                        selected = showNonZeroOnly,
                        onClick = { showNonZeroOnly = !showNonZeroOnly },
                        label = { Text("≠ 0", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CncAccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CncAccentCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Informational note
                Surface(
                    color = CncCardBackground,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TNC INFO: Q0..Q99 programma-variabelen. FN 6/7 hoeken in graden (°). Evaluatie in runtime.",
                        color = CncTextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (sortedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (qParameters.isEmpty())
                                "Geen Q-parameters actief.\nGebruik FN 0..12 of Qx = ... in Klartekst."
                            else "Geen Q-parameters gevonden met filter.",
                            color = CncTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sortedEntries) { entry ->
                            QParamRow(qIndex = entry.key, value = entry.value)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CncAccentCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sluiten", color = CncBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun QParamRow(qIndex: Int, value: Float) {
    Surface(
        color = CncCardBackground,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, CncBorder, RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = CncAccentCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Q$qIndex",
                        color = CncAccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val roleHint = when (qIndex) {
                    1 -> "A / Radius / Lengte"
                    2 -> "B / Diepte / Breedte"
                    3 -> "Hoek phi / Teller"
                    4 -> "Stap d_phi"
                    10 -> "Berekende X"
                    11 -> "Berekende Y"
                    12 -> "Berekende Z"
                    20 -> "Sinus component"
                    21 -> "Cosinus component"
                    else -> null
                }
                if (roleHint != null) {
                    Text(
                        text = "($roleHint)",
                        color = CncTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val formattedVal = String.format(Locale.US, "%+10.4f", value)
                Text(
                    text = formattedVal,
                    color = if (abs(value) > 1e-4f) CncFeedGreen else CncTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (abs(value - value.toInt()) < 1e-4f) {
                    Text(
                        text = "= ${value.toInt()}",
                        color = CncTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

package com.klartext.simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.model.DiagnosticSeverity
import com.klartext.simulator.model.KlartextBlock
import com.klartext.simulator.model.TncDiagnostic
import com.klartext.simulator.ui.theme.*

@Composable
fun CodeViewer(
    blocks: List<KlartextBlock>,
    activeLineNumber: Int,
    diagnostics: List<TncDiagnostic>,
    onLineClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeLineNumber) {
        val targetIndex = blocks.indexOfFirst { it.lineNumber == activeLineNumber }
        if (targetIndex >= 0) {
            listState.animateScrollToItem((targetIndex - 2).coerceAtLeast(0))
        }
    }

    val diagMap = diagnostics.groupBy { it.lineNumber }

    Box(
        modifier = modifier
            .background(CncPanelBackground, RoundedCornerShape(8.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(blocks) { index, block ->
                val isActive = block.lineNumber == activeLineNumber
                val lineDiags = diagMap[block.lineNumber]
                val hasError = lineDiags?.any { it.severity == DiagnosticSeverity.ERROR } == true
                val hasHint = lineDiags?.any { it.severity == DiagnosticSeverity.STUDENT_HINT } == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isActive -> Color(0xFF1565C0) // TNC 640 Blue Selection Bar
                                hasError -> CncRapidOrange.copy(alpha = 0.2f)
                                hasHint -> CncAccentOrange.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { onLineClick(block.lineNumber) }
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Line number
                    Text(
                        text = String.format("%3d", block.lineNumber),
                        color = if (isActive) Color.White else CncTextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(28.dp)
                    )

                    // Error/Warning/Hint icon badge
                    if (hasError) {
                        Text("🔴", fontSize = 9.sp, modifier = Modifier.padding(end = 2.dp))
                    } else if (hasHint) {
                        Text("⚠️", fontSize = 9.sp, modifier = Modifier.padding(end = 2.dp))
                    }

                    // Syntax-highlighted Klartext Code
                    Text(
                        text = formatKlartextCode(block.rawText, isActive),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun formatKlartextCode(rawText: String, isActive: Boolean) = buildAnnotatedString {
    val parts = rawText.split(" ")
    for ((i, part) in parts.withIndex()) {
        if (i > 0) append(" ")
        when {
            part.startsWith(";") || part.startsWith("//") -> {
                withStyle(SpanStyle(color = if (isActive) Color(0xFFE0E0E0) else CncTextSecondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(parts.drop(i).joinToString(" "))
                }
                return@buildAnnotatedString
            }
            part in listOf("BEGIN", "PGM", "END", "BLK", "FORM", "TOOL", "CALL", "DEF", "L", "CC", "C", "CR", "CT", "CP", "LP", "CYCL", "LBL", "APPR", "DEP", "RND", "CHF") -> {
                withStyle(SpanStyle(color = if (isActive) Color(0xFFFFD54F) else CncAccentOrange, fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            }
            part.startsWith("X") || part.startsWith("IX") ||
            part.startsWith("Y") || part.startsWith("IY") ||
            part.startsWith("Z") || part.startsWith("IZ") ||
            part.startsWith("PR") || part.startsWith("PA") -> {
                withStyle(SpanStyle(color = if (isActive) Color.White else CncDroGreen)) {
                    append(part)
                }
            }
            part.startsWith("F") || part.startsWith("S") -> {
                withStyle(SpanStyle(color = if (isActive) Color(0xFF80D8FF) else CncFeedCyan)) {
                    append(part)
                }
            }
            part.startsWith("R") || part in listOf("RL", "RR", "R0", "DR+", "DR-", "RO") || part.startsWith("DR") || part.startsWith("DL") -> {
                withStyle(SpanStyle(color = if (isActive) Color(0xFFFF80AB) else CncPlungePurple, fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            }
            part.startsWith("M") -> {
                withStyle(SpanStyle(color = if (isActive) Color(0xFFFF8A80) else CncRapidOrange)) {
                    append(part)
                }
            }
            part.startsWith("Q") -> {
                withStyle(SpanStyle(color = if (isActive) Color(0xFFCCFF90) else Color(0xFF76FF03))) {
                    append(part)
                }
            }
            else -> {
                withStyle(SpanStyle(color = if (isActive) Color.White else CncTextPrimary)) {
                    append(part)
                }
            }
        }
    }
}

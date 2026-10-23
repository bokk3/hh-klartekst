package com.klartext.simulator.parser

import java.util.Locale
import kotlin.math.*

object QExpressionEvaluator {

    /**
     * Evaluates a mathematical expression containing numbers, Q-parameters (Q1, Q2..),
     * basic arithmetic (+, -, *, /), and functions (SIN, COS, TAN, SQRT, ABS).
     * Note: Angles in SIN, COS, TAN are in DEGREES, matching HEIDENHAIN convention!
     */
    fun evaluate(expression: String, qMap: Map<Int, Float>): Float {
        val clean = expression.replace(" ", "").uppercase(Locale.ROOT)
        if (clean.isEmpty()) return 0f

        val parser = ExpressionParser(clean, qMap)
        return parser.parse()
    }

    private class ExpressionParser(
        private val text: String,
        private val qMap: Map<Int, Float>
    ) {
        private var pos = 0

        fun parse(): Float {
            val result = parseExpression()
            return result
        }

        private fun parseExpression(): Float {
            var value = parseTerm()
            while (pos < text.length) {
                when (text[pos]) {
                    '+' -> {
                        pos++
                        value += parseTerm()
                    }
                    '-' -> {
                        pos++
                        value -= parseTerm()
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parseTerm(): Float {
            var value = parseFactor()
            while (pos < text.length) {
                when (text[pos]) {
                    '*' -> {
                        pos++
                        value *= parseFactor()
                    }
                    '/' -> {
                        pos++
                        val denom = parseFactor()
                        value = if (abs(denom) > 1e-7f) value / denom else 0f
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parseFactor(): Float {
            if (pos >= text.length) return 0f

            // Unary + or -
            if (text[pos] == '+') {
                pos++
                return parseFactor()
            }
            if (text[pos] == '-') {
                pos++
                return -parseFactor()
            }

            // Parentheses
            if (text[pos] == '(') {
                pos++
                val value = parseExpression()
                if (pos < text.length && text[pos] == ')') pos++
                return value
            }

            // Function call: SIN, COS, TAN, SQRT, ABS
            val func = checkFunction()
            if (func != null) {
                val arg = parseFactor()
                return when (func) {
                    "SIN" -> sin(Math.toRadians(arg.toDouble())).toFloat()
                    "COS" -> cos(Math.toRadians(arg.toDouble())).toFloat()
                    "TAN" -> tan(Math.toRadians(arg.toDouble())).toFloat()
                    "SQRT" -> sqrt(max(0f, arg))
                    "ABS" -> abs(arg)
                    else -> arg
                }
            }

            // Q-Parameter: e.g. Q1, Q10, Q105
            if (text[pos] == 'Q') {
                pos++
                val start = pos
                while (pos < text.length && text[pos].isDigit()) {
                    pos++
                }
                val qIndex = text.substring(start, pos).toIntOrNull() ?: 0
                return qMap[qIndex] ?: 0f
            }

            // Number (literal float)
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) {
                pos++
            }
            return text.substring(start, pos).toFloatOrNull() ?: 0f
        }

        private fun checkFunction(): String? {
            for (fn in listOf("SQRT", "SIN", "COS", "TAN", "ABS")) {
                if (text.startsWith(fn, pos)) {
                    pos += fn.length
                    return fn
                }
            }
            return null
        }
    }
}

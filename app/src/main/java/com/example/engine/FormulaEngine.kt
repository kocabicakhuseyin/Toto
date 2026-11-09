package com.example.engine

import com.example.model.FormulaType
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.model.TotoMatch
import kotlin.random.Random

object FormulaEngine {

    /**
     * Generates columns based on match selections and chosen guarantee formula.
     */
    fun generateColumns(
        matches: List<TotoMatch>,
        formulaType: FormulaType,
        maxColumnsCap: Int = 256
    ): List<TotoColumn> {
        val optionsPerMatch = matches.map { match ->
            if (match.selections.isEmpty()) {
                val h = match.probability.home.firstOrNull() ?: 50
                val d = match.probability.draw.firstOrNull() ?: 25
                val a = match.probability.away.firstOrNull() ?: 25
                val best = when {
                    h >= d && h >= a -> PredictionOutcome.HOME
                    d >= h && d >= a -> PredictionOutcome.DRAW
                    else -> PredictionOutcome.AWAY
                }
                listOf(best)
            } else {
                match.selections.toList()
            }
        }

        val allCombinations = cartesianProduct(optionsPerMatch, limit = 2048)

        val reducedColumns = when (formulaType) {
            FormulaType.GUARANTEE_15 -> {
                // N-0: All combinations capped
                allCombinations.take(maxColumnsCap)
            }
            FormulaType.GUARANTEE_14 -> {
                // N-1: Hamming distance <= 1 error (14 Garanti)
                reduceByHammingDistance(allCombinations, maxDiff = 1, maxColumns = 48)
            }
            FormulaType.GUARANTEE_13 -> {
                // N-2: Hamming distance <= 2 errors (13 Garanti)
                reduceByHammingDistance(allCombinations, maxDiff = 2, maxColumns = 24)
            }
            FormulaType.GUARANTEE_12 -> {
                // N-3: Hamming distance <= 3 errors (12 Garanti)
                reduceByHammingDistance(allCombinations, maxDiff = 3, maxColumns = 12)
            }
            FormulaType.GUARANTEED_9_COL -> {
                generate9ColumnSystem(optionsPerMatch)
            }
            FormulaType.PERCENTAGE_FORMULA -> {
                filterByProbability(allCombinations, matches, takeRatio = 0.35f, minCols = 8)
            }
            FormulaType.SUPER_SEVEN -> {
                generateSuperSeven(optionsPerMatch)
            }
            FormulaType.EXTRA1X2_TXT, FormulaType.EXCEL_UPLOAD -> {
                allCombinations.take(32)
            }
        }

        return reducedColumns.mapIndexed { idx, preds ->
            TotoColumn(index = idx + 1, predictions = preds)
        }
    }

    /**
     * N-k Reduction:
     * A column C covers target T if they differ in at most `maxDiff` matches out of 15.
     * maxDiff = 1 -> 14 Garanti (N-1)
     * maxDiff = 2 -> 13 Garanti (N-2)
     * maxDiff = 3 -> 12 Garanti (N-3)
     */
    private fun reduceByHammingDistance(
        allCombinations: List<List<PredictionOutcome>>,
        maxDiff: Int,
        maxColumns: Int
    ): List<List<PredictionOutcome>> {
        if (allCombinations.size <= maxColumns) return allCombinations

        val selected = mutableListOf<List<PredictionOutcome>>()
        val uncovered = allCombinations.toMutableList()

        while (uncovered.isNotEmpty() && selected.size < maxColumns) {
            var bestCandidate: List<PredictionOutcome>? = null
            var bestCoveredCount = -1

            val candidates = if (uncovered.size > 80) uncovered.shuffled().take(80) else uncovered
            for (cand in candidates) {
                var count = 0
                for (target in uncovered) {
                    if (hammingDistance(cand, target) <= maxDiff) {
                        count++
                    }
                }
                if (count > bestCoveredCount) {
                    bestCoveredCount = count
                    bestCandidate = cand
                }
            }

            val chosen = bestCandidate ?: uncovered.first()
            selected.add(chosen)
            uncovered.removeAll { hammingDistance(chosen, it) <= maxDiff }
        }

        return if (selected.isEmpty()) allCombinations.take(maxColumns) else selected
    }

    private fun generate9ColumnSystem(options: List<List<PredictionOutcome>>): List<List<PredictionOutcome>> {
        val result = mutableListOf<List<PredictionOutcome>>()
        for (colIdx in 0 until 9) {
            val col = mutableListOf<PredictionOutcome>()
            for (matchIdx in 0 until 15) {
                val matchOpts = options.getOrElse(matchIdx) { listOf(PredictionOutcome.HOME) }
                val pickIndex = (colIdx + matchIdx * 2) % matchOpts.size
                col.add(matchOpts[pickIndex])
            }
            result.add(col)
        }
        return result
    }

    private fun hammingDistance(a: List<PredictionOutcome>, b: List<PredictionOutcome>): Int {
        var diff = 0
        for (i in 0 until 15.coerceAtMost(a.size.coerceAtMost(b.size))) {
            if (a[i] != b[i]) diff++
        }
        return diff
    }

    private fun filterByProbability(
        combinations: List<List<PredictionOutcome>>,
        matches: List<TotoMatch>,
        takeRatio: Float,
        minCols: Int
    ): List<List<PredictionOutcome>> {
        val scored = combinations.map { col ->
            var score = 0
            for (i in col.indices) {
                val match = matches.getOrNull(i)
                val outcome = col[i]
                val prob = when (outcome) {
                    PredictionOutcome.HOME -> match?.probability?.home?.firstOrNull() ?: 33
                    PredictionOutcome.DRAW -> match?.probability?.draw?.firstOrNull() ?: 33
                    PredictionOutcome.AWAY -> match?.probability?.away?.firstOrNull() ?: 33
                }
                score += prob
            }
            Pair(col, score)
        }

        val sorted = scored.sortedByDescending { it.second }.map { it.first }
        val count = (sorted.size * takeRatio).toInt().coerceAtLeast(minCols).coerceAtMost(sorted.size)
        return sorted.take(count)
    }

    private fun generateSuperSeven(options: List<List<PredictionOutcome>>): List<List<PredictionOutcome>> {
        val result = mutableListOf<List<PredictionOutcome>>()
        for (i in 0 until 14) {
            val col = mutableListOf<PredictionOutcome>()
            for (m in 0 until 15) {
                val opts = options.getOrElse(m) { listOf(PredictionOutcome.HOME) }
                if (m < 7) {
                    col.add(opts.first())
                } else {
                    col.add(opts[(i + m) % opts.size])
                }
            }
            result.add(col)
        }
        return result
    }

    private fun cartesianProduct(
        lists: List<List<PredictionOutcome>>,
        limit: Int
    ): List<List<PredictionOutcome>> {
        var result = listOf(emptyList<PredictionOutcome>())
        for (list in lists) {
            val newResult = mutableListOf<List<PredictionOutcome>>()
            for (acc in result) {
                for (item in list) {
                    newResult.add(acc + item)
                    if (newResult.size >= limit) {
                        return newResult
                    }
                }
            }
            result = newResult
        }
        return result
    }

    fun parseExtra1X2Txt(content: String): List<TotoColumn> {
        val lines = content.lines()
        val parsed = mutableListOf<TotoColumn>()
        var idx = 1
        for (rawLine in lines) {
            val line = rawLine.trim().uppercase()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

            val clean = line.replace("0", "X").filter { it in listOf('1', 'X', '2') }
            if (clean.length == 15) {
                val outcomes = clean.map { char ->
                    when (char) {
                        '1' -> PredictionOutcome.HOME
                        '2' -> PredictionOutcome.AWAY
                        else -> PredictionOutcome.DRAW
                    }
                }
                parsed.add(TotoColumn(index = idx++, predictions = outcomes))
            }
        }
        return parsed
    }

    fun exportToExtra1X2Txt(columns: List<TotoColumn>): String {
        val sb = StringBuilder()
        sb.append("# Spor Toto Hedef 15 - Extra1X2 Kolon Listesi\n")
        sb.append("# Toplam Kolon: ${columns.size}\n")
        columns.forEach { col ->
            sb.append(col.asString).append("\n")
        }
        return sb.toString()
    }
}

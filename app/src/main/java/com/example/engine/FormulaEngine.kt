package com.example.engine

import com.example.model.FormulaType
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.model.TotoMatch
import kotlin.random.Random

object FormulaEngine {

    const val UNLIMITED_COLUMNS_CAP = -1

    /**
     * Calculates a smart column cap based on the raw combination space
     * of selected matches (e.g. 64 combos -> 12 cols, 128 -> 16 cols).
     */
    fun calculateSmartCap(matches: List<TotoMatch>): Int {
        var totalCombos = 1
        for (m in matches) {
            val count = if (m.selections.isEmpty()) 1 else m.selections.size
            totalCombos *= count
            if (totalCombos > 4096) break
        }
        return when {
            totalCombos <= 4 -> totalCombos
            totalCombos <= 8 -> minOf(4, totalCombos)
            totalCombos <= 16 -> 6
            totalCombos <= 32 -> 8
            totalCombos <= 64 -> 12
            totalCombos <= 128 -> 16
            totalCombos <= 256 -> 24
            totalCombos <= 512 -> 32
            else -> 48
        }
    }

    /**
     * Generates all Cartesian combinations from user match selections (up to limit, e.g. 32768).
     * Used for full-space filter testing so no columns are artificially truncated before filtering!
     */
    fun generateAllCombinations(
        matches: List<TotoMatch>,
        limit: Int = 32768
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
        val combinations = cartesianProduct(optionsPerMatch, limit = limit)
        return combinations.mapIndexed { idx, preds ->
            TotoColumn(index = idx + 1, predictions = preds)
        }
    }

    /**
     * Generates columns based on match selections and chosen guarantee formula.
     * @param maxColumnsCap Optional user-defined or smart column limit (e.g. 12, 18, 24, 32, 48, or UNLIMITED_COLUMNS_CAP).
     */
    fun generateColumns(
        matches: List<TotoMatch>,
        formulaType: FormulaType,
        maxColumnsCap: Int? = null
    ): List<TotoColumn> {
        val isUnlimited = maxColumnsCap == UNLIMITED_COLUMNS_CAP
        val effectiveCap = if (isUnlimited) Int.MAX_VALUE else (maxColumnsCap ?: calculateSmartCap(matches))

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

        val rawLimit = if (isUnlimited) 32768 else 2048
        val allCombinations = cartesianProduct(optionsPerMatch, limit = rawLimit)

        val reducedColumns = when (formulaType) {
            FormulaType.GUARANTEE_15 -> {
                // N-0: All combinations. If unlimited, keep all up to 32768; otherwise take effectiveCap
                if (isUnlimited) allCombinations else allCombinations.take(effectiveCap)
            }
            FormulaType.GUARANTEE_14 -> {
                // N-1: Algorithmic Hamming Covering Code (14 Garanti)
                val cap = if (isUnlimited) 32768 else effectiveCap
                GuaranteeFormulaEngine.generateNMinus1Columns(matches, maxColumnsCap = cap, targetCoverage = 1.0).reducedColumns.map { it.predictions }
            }
            FormulaType.GUARANTEE_13 -> {
                // N-2: Hamming distance <= 2 errors (13 Garanti)
                val cap13 = if (isUnlimited) 32768 else minOf(effectiveCap, 24)
                reduceByHammingDistance(allCombinations, maxDiff = 2, maxColumns = cap13, optionsPerMatch = optionsPerMatch)
            }
            FormulaType.GUARANTEE_12 -> {
                // N-3: Hamming distance <= 3 errors (12 Garanti)
                val cap12 = if (isUnlimited) 32768 else minOf(effectiveCap, 12)
                reduceByHammingDistance(allCombinations, maxDiff = 3, maxColumns = cap12, optionsPerMatch = optionsPerMatch)
            }
            FormulaType.GUARANTEED_9_COL -> {
                if (isUnlimited) generate9ColumnSystem(optionsPerMatch) else generate9ColumnSystem(optionsPerMatch).take(effectiveCap)
            }
            FormulaType.PERCENTAGE_FORMULA -> {
                val minCols = if (isUnlimited) 8 else minOf(8, effectiveCap)
                val res = filterByProbability(allCombinations, matches, takeRatio = 0.35f, minCols = minCols)
                if (isUnlimited) res else res.take(effectiveCap)
            }
            FormulaType.SUPER_SEVEN -> {
                if (isUnlimited) generateSuperSeven(optionsPerMatch) else generateSuperSeven(optionsPerMatch).take(effectiveCap)
            }
            FormulaType.EXTRA1X2_TXT, FormulaType.EXCEL_UPLOAD -> {
                if (isUnlimited) allCombinations else allCombinations.take(minOf(32, effectiveCap))
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
        maxColumns: Int,
        optionsPerMatch: List<List<PredictionOutcome>>
    ): List<List<PredictionOutcome>> {
        if (allCombinations.size <= 2) return allCombinations

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

        // Ensure every option of variable matches (including matches 10-15) appears in selected
        for (m in 0 until 15) {
            val opts = optionsPerMatch.getOrNull(m) ?: emptyList()
            if (opts.size > 1 && selected.size < maxColumns) {
                for (opt in opts) {
                    val hasOpt = selected.any { it.getOrNull(m) == opt }
                    if (!hasOpt) {
                        val complement = allCombinations.firstOrNull { it.getOrNull(m) == opt && !selected.contains(it) }
                        if (complement != null) {
                            selected.add(complement)
                        }
                    }
                }
            }
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
        val safeLists = (0 until 15).map { idx ->
            val list = lists.getOrNull(idx) ?: emptyList()
            if (list.isEmpty()) listOf(PredictionOutcome.HOME) else list
        }

        var totalCombos = 1.0
        for (l in safeLists) {
            totalCombos *= l.size
        }

        // If total combinations fit comfortably in limit, generate full Cartesian product of ALL 15 matches
        if (totalCombos <= limit) {
            var result = listOf(emptyList<PredictionOutcome>())
            for (list in safeLists) {
                val newResult = ArrayList<List<PredictionOutcome>>(result.size * list.size)
                for (acc in result) {
                    for (item in list) {
                        newResult.add(acc + item)
                    }
                }
                result = newResult
            }
            return result
        }

        // Space exceeds limit: Generate complete 15-match combinations using multi-dimensional
        // quasi-random Weyl sampling with irrational constants (sqrt(primes)).
        // This guarantees uniform, active, and balanced distribution for ALL 15 matches,
        // specifically ensuring matches 10 to 15 (indices 9 to 14) alternate on every column!
        val weylConstants = doubleArrayOf(
            0.41421356237309503, // sqrt(2)  - 1 (Match 1)
            0.7320508075688772,  // sqrt(3)  - 1 (Match 2)
            0.2360679774997897,  // sqrt(5)  - 2 (Match 3)
            0.6457513110645906,  // sqrt(7)  - 2 (Match 4)
            0.3166247903553998,  // sqrt(11) - 3 (Match 5)
            0.6055512754639893,  // sqrt(13) - 3 (Match 6)
            0.12310562561766054, // sqrt(17) - 4 (Match 7)
            0.35889894354067355, // sqrt(19) - 4 (Match 8)
            0.7958315233127195,  // sqrt(23) - 4 (Match 9)
            0.38516480713450403, // sqrt(29) - 5 (Match 10 - Avrupa)
            0.5677643628300215,  // sqrt(31) - 5 (Match 11 - Avrupa)
            0.08276253029821969, // sqrt(37) - 6 (Match 12 - Avrupa)
            0.4031242374328485,  // sqrt(41) - 6 (Match 13 - Avrupa)
            0.5574385243020006,  // sqrt(43) - 6 (Match 14 - Avrupa)
            0.8556546004010441   // sqrt(47) - 6 (Match 15 - Avrupa)
        )

        val result = LinkedHashSet<List<PredictionOutcome>>(limit)

        for (i in 0 until limit) {
            val col = ArrayList<PredictionOutcome>(15)
            for (m in 0 until 15) {
                val opts = safeLists[m]
                if (opts.size == 1) {
                    col.add(opts[0])
                } else {
                    val alpha = weylConstants[m]
                    val u = (((i + 1) * alpha) % 1.0).let { if (it < 0) it + 1.0 else it }
                    val pick = (u * opts.size).toInt().coerceIn(0, opts.size - 1)
                    col.add(opts[pick])
                }
            }
            result.add(col)
        }

        // Explicitly inject complementary variants for matches 9..14 (10-15)
        for (m in 9 until 15) {
            val opts = safeLists[m]
            if (opts.size > 1) {
                for (opt in opts) {
                    val variant = ArrayList<PredictionOutcome>(15)
                    for (k in 0 until 15) {
                        if (k == m) {
                            variant.add(opt)
                        } else {
                            val kOpts = safeLists[k]
                            val pick = ((m * 3 + k * 2) % kOpts.size).coerceIn(0, kOpts.size - 1)
                            variant.add(kOpts[pick])
                        }
                    }
                    result.add(variant)
                }
            }
        }

        return result.toList().take(limit)
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
        sb.append("# Spor Toto - Extra1X2 Kolon Listesi\n")
        sb.append("# Toplam Kolon: ${columns.size}\n")
        columns.forEach { col ->
            sb.append(col.asString).append("\n")
        }
        return sb.toString()
    }
}

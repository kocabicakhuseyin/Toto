package com.example.engine

import com.example.model.FilterApplicationMode
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.model.TotoFilterCriteria
import com.example.model.TotoMatch

data class ColumnAnalysis(
    val totalSurprises: Int,
    val totalDraws: Int,
    val totalHardSurprises: Int,
    val countOf1s: Int,
    val countOf2s: Int,
    val surprises1To9: Int,
    val draws1To9: Int,
    val hardSurprises1To9: Int,
    val surprises10To15: Int,
    val draws10To15: Int,
    val hardSurprises10To15: Int,
    val maxConsecutive1s: Int,
    val maxConsecutiveDraws: Int,
    val maxConsecutive2s: Int,
    val customSurprisesCount: Int
)

data class FilterPreviewResult(
    val initialColumnCount: Int,
    val filteredColumnCount: Int,
    val eliminatedCount: Int,
    val savingsPercentage: Int,
    val initialCostTL: Double,
    val filteredCostTL: Double
)


/** Full-matrix evaluation result, including the exact pre-cap match count. */
data class MatrixFilterResult(
    val initialColumnCount: Int,
    val matchingColumnCount: Int,
    val columns: List<TotoColumn>,
    val isTooLarge: Boolean
) {
    val savedColumnCount: Int get() = columns.size
    val isLimitedByCap: Boolean get() = !isTooLarge && matchingColumnCount > columns.size

    fun toPreview(): FilterPreviewResult {
        val eliminated = (initialColumnCount - matchingColumnCount).coerceAtLeast(0)
        return FilterPreviewResult(
            initialColumnCount = initialColumnCount,
            filteredColumnCount = savedColumnCount,
            eliminatedCount = eliminated,
            savingsPercentage = if (initialColumnCount > 0) eliminated * 100 / initialColumnCount else 0,
            initialCostTL = initialColumnCount * 0.20,
            filteredCostTL = savedColumnCount * 0.20
        )
    }
}

object TotoFilterEngine {

    /**
     * Determines whether an outcome for a specific match is considered:
     * - favorite
     * - surprise
     * - hard surprise (ters sürpriz)
     */
    fun analyzeMatchOutcome(match: TotoMatch, outcome: PredictionOutcome): OutcomeType {
        val h = match.probability.home.firstOrNull() ?: 33
        val d = match.probability.draw.firstOrNull() ?: 33
        val a = match.probability.away.firstOrNull() ?: 33

        val favorite = when {
            h >= d && h >= a -> PredictionOutcome.HOME
            d >= h && d >= a -> PredictionOutcome.DRAW
            else -> PredictionOutcome.AWAY
        }

        if (outcome == favorite) {
            return OutcomeType.FAVORITE
        }

        // It is a surprise
        val isHardSurprise = when (favorite) {
            PredictionOutcome.HOME -> outcome == PredictionOutcome.AWAY
            PredictionOutcome.AWAY -> outcome == PredictionOutcome.HOME
            PredictionOutcome.DRAW -> (outcome == PredictionOutcome.AWAY && a < h) || (outcome == PredictionOutcome.HOME && h < a)
        }

        return if (isHardSurprise) OutcomeType.HARD_SURPRISE else OutcomeType.SURPRISE
    }

    enum class OutcomeType {
        FAVORITE,
        SURPRISE,
        HARD_SURPRISE
    }

    /**
     * Analyzes all characteristics of a single 15-match column.
     */
    fun analyzeColumn(column: TotoColumn, matches: List<TotoMatch>, customMatches: Set<Int> = emptySet()): ColumnAnalysis {
        var totalSurprises = 0
        var totalDraws = 0
        var totalHardSurprises = 0
        var countOf1s = 0
        var countOf2s = 0

        var surprises1To9 = 0
        var draws1To9 = 0
        var hardSurprises1To9 = 0

        var surprises10To15 = 0
        var draws10To15 = 0
        var hardSurprises10To15 = 0

        var customSurprises = 0

        val preds = column.predictions
        val size = preds.size.coerceAtMost(matches.size)

        for (i in 0 until size) {
            val outcome = preds[i]
            val match = matches[i]
            val matchNumber = match.matchNumber

            if (outcome == PredictionOutcome.HOME) countOf1s++
            if (outcome == PredictionOutcome.DRAW) totalDraws++
            if (outcome == PredictionOutcome.AWAY) countOf2s++

            val type = analyzeMatchOutcome(match, outcome)
            val isSurprise = (type != OutcomeType.FAVORITE)
            val isHard = (type == OutcomeType.HARD_SURPRISE)

            if (isSurprise) totalSurprises++
            if (isHard) totalHardSurprises++

            // 1-9 Matches (Süper Lig)
            if (i < 9) {
                if (isSurprise) surprises1To9++
                if (outcome == PredictionOutcome.DRAW) draws1To9++
                if (isHard) hardSurprises1To9++
            } else {
                // 10-15 Matches (Avrupa)
                if (isSurprise) surprises10To15++
                if (outcome == PredictionOutcome.DRAW) draws10To15++
                if (isHard) hardSurprises10To15++
            }

            if (customMatches.contains(matchNumber) && isSurprise) {
                customSurprises++
            }
        }

        // Streak analysis
        val max1s = calculateMaxStreak(preds, PredictionOutcome.HOME)
        val maxDraws = calculateMaxStreak(preds, PredictionOutcome.DRAW)
        val max2s = calculateMaxStreak(preds, PredictionOutcome.AWAY)

        return ColumnAnalysis(
            totalSurprises = totalSurprises,
            totalDraws = totalDraws,
            totalHardSurprises = totalHardSurprises,
            countOf1s = countOf1s,
            countOf2s = countOf2s,
            surprises1To9 = surprises1To9,
            draws1To9 = draws1To9,
            hardSurprises1To9 = hardSurprises1To9,
            surprises10To15 = surprises10To15,
            draws10To15 = draws10To15,
            hardSurprises10To15 = hardSurprises10To15,
            maxConsecutive1s = max1s,
            maxConsecutiveDraws = maxDraws,
            maxConsecutive2s = max2s,
            customSurprisesCount = customSurprises
        )
    }

    private fun calculateMaxStreak(preds: List<PredictionOutcome>, target: PredictionOutcome): Int {
        var maxStreak = 0
        var currentStreak = 0
        for (p in preds) {
            if (p == target) {
                currentStreak++
                if (currentStreak > maxStreak) maxStreak = currentStreak
            } else {
                currentStreak = 0
            }
        }
        return maxStreak
    }

    /**
     * Checks if a column passes the criteria.
     * Returns true if accepted, false if eliminated.
     */
    fun evaluateColumn(
        column: TotoColumn,
        criteria: TotoFilterCriteria,
        matches: List<TotoMatch>
    ): Boolean {
        if (!criteria.isActive) return true

        val analysis = analyzeColumn(column, matches, criteria.customSurpriseMatches)

        var violations = 0

        // 1. Toplam Sürpriz Sayısı (0..6)
        if (criteria.allowedTotalSurprises.isNotEmpty()) {
            if (!criteria.allowedTotalSurprises.contains(analysis.totalSurprises)) violations++
        }

        // 2. Beklenen Beraberlik Sayısı (0..6)
        if (criteria.allowedTotalDraws.isNotEmpty()) {
            if (!criteria.allowedTotalDraws.contains(analysis.totalDraws)) violations++
        }

        // 3. Beklenen Ters Sürpriz Sayısı (0..3)
        if (criteria.allowedTotalHardSurprises.isNotEmpty()) {
            if (!criteria.allowedTotalHardSurprises.contains(analysis.totalHardSurprises)) violations++
        }

        // 4. 1'lerin Sayısı (7..14)
        if (criteria.allowedCountOf1s.isNotEmpty()) {
            if (!criteria.allowedCountOf1s.contains(analysis.countOf1s)) violations++
        }

        // 5. 2'lerin Sayısı (1..5)
        if (criteria.allowedCountOf2s.isNotEmpty()) {
            if (!criteria.allowedCountOf2s.contains(analysis.countOf2s)) violations++
        }

        // 6. 1-9 Sürpriz
        if (criteria.allowedSurprises1To9.isNotEmpty()) {
            if (!criteria.allowedSurprises1To9.contains(analysis.surprises1To9)) violations++
        }

        // 7. 1-9 Beraberlik
        if (criteria.allowedDraws1To9.isNotEmpty()) {
            if (!criteria.allowedDraws1To9.contains(analysis.draws1To9)) violations++
        }

        // 8. 1-9 Ters Sürpriz
        if (criteria.allowedHardSurprises1To9.isNotEmpty()) {
            if (!criteria.allowedHardSurprises1To9.contains(analysis.hardSurprises1To9)) violations++
        }

        // 9. 10-15 Sürpriz
        if (criteria.allowedSurprises10To15.isNotEmpty()) {
            if (!criteria.allowedSurprises10To15.contains(analysis.surprises10To15)) violations++
        }

        // 10. 10-15 Beraberlik
        if (criteria.allowedDraws10To15.isNotEmpty()) {
            if (!criteria.allowedDraws10To15.contains(analysis.draws10To15)) violations++
        }

        // 11. 10-15 Ters Sürpriz
        if (criteria.allowedHardSurprises10To15.isNotEmpty()) {
            if (!criteria.allowedHardSurprises10To15.contains(analysis.hardSurprises10To15)) violations++
        }

        // 12. Ard Arda 1'ler
        if (criteria.maxConsecutive1s != null) {
            if (analysis.maxConsecutive1s > criteria.maxConsecutive1s) violations++
        }

        // 13. Ard Arda Beraberlikler
        if (criteria.maxConsecutiveDraws != null) {
            if (analysis.maxConsecutiveDraws > criteria.maxConsecutiveDraws) violations++
        }

        // 14. Ard Arda 2'ler
        if (criteria.maxConsecutive2s != null) {
            if (analysis.maxConsecutive2s > criteria.maxConsecutive2s) violations++
        }

        // 15. Özel Sürpriz Filtreleme
        if (criteria.customSurpriseMatches.isNotEmpty() && criteria.allowedCustomSurprisesCount.isNotEmpty()) {
            if (!criteria.allowedCustomSurprisesCount.contains(analysis.customSurprisesCount)) violations++
        }

        return when (criteria.applicationMode) {
            FilterApplicationMode.PROBABILITY_STRICT -> violations == 0
            FilterApplicationMode.GUARANTEE_FLEXIBLE -> violations <= 1
        }
    }

    /**
     * Takes candidate columns, applies filters, and re-indexes remaining columns.
     */
    fun filterColumns(
        columns: List<TotoColumn>,
        criteria: TotoFilterCriteria,
        matches: List<TotoMatch>
    ): List<TotoColumn> {
        if (!criteria.isActive) return columns

        val filtered = columns.filter { evaluateColumn(it, criteria, matches) }
        return filtered.mapIndexed { index, col ->
            col.copy(index = index + 1)
        }
    }

    /**
     * Previews filtering impact without changing current coupon.
     */
    fun previewFilter(
        columns: List<TotoColumn>,
        criteria: TotoFilterCriteria,
        matches: List<TotoMatch>
    ): FilterPreviewResult {
        val initialCount = columns.size
        val filtered = if (criteria.isActive) columns.filter { evaluateColumn(it, criteria, matches) } else columns
        val filteredCount = filtered.size
        val eliminated = (initialCount - filteredCount).coerceAtLeast(0)
        val savingsPct = if (initialCount > 0) ((eliminated.toDouble() / initialCount) * 100).toInt() else 0

        return FilterPreviewResult(
            initialColumnCount = initialCount,
            filteredColumnCount = filteredCount,
            eliminatedCount = eliminated,
            savingsPercentage = savingsPct,
            initialCostTL = initialCount * 0.20,
            filteredCostTL = filteredCount * 0.20
        )
    }

    /**
     * Filters the full selected 15-match matrix BEFORE applying a coupon column cap.
     * Never substitutes formula-reduced coupons or previously saved coupons as input.
     * If the matrix exceeds the safe exact-evaluation limit, returns an explicit
     * unsupported result instead of silently sampling or inventing columns.
     */
    fun filterSelectedMatrix(
        matches: List<TotoMatch>,
        criteria: TotoFilterCriteria,
        maxColumnsCap: Int? = null,
        shouldCancel: () -> Boolean = { false }
    ): MatrixFilterResult {
        if (matches.isEmpty()) {
            return MatrixFilterResult(0, 0, emptyList(), isTooLarge = false)
        }

        val options = matches.map { match ->
            if (match.selections.isNotEmpty()) {
                match.selections.sortedBy { it.ordinal }
            } else {
                val home = match.probability.home.firstOrNull() ?: 33
                val draw = match.probability.draw.firstOrNull() ?: 33
                val away = match.probability.away.firstOrNull() ?: 33
                val favorite = when {
                    home >= draw && home >= away -> PredictionOutcome.HOME
                    draw >= home && draw >= away -> PredictionOutcome.DRAW
                    else -> PredictionOutcome.AWAY
                }
                listOf(favorite)
            }
        }

        val total = options.fold(1L) { count, choices -> count * choices.size }
        if (total > MAX_EXACT_MATRIX_COMBINATIONS) {
            return MatrixFilterResult(total.toInt(), 0, emptyList(), isTooLarge = true)
        }

        val requestedCap = when (maxColumnsCap) {
            com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP -> MAX_STORED_MATRIX_COLUMNS
            null -> com.example.engine.FormulaEngine.calculateSmartCap(matches)
            else -> maxColumnsCap.coerceAtLeast(0)
        }
        val outputLimit = requestedCap.coerceAtMost(MAX_STORED_MATRIX_COLUMNS)
        val selected = ArrayList<TotoColumn>(minOf(outputLimit, total.toInt()))
        val predictionBuffer = ArrayList<PredictionOutcome>(matches.size)

        // An inactive filter accepts the whole matrix: enumerate only the saved
        // prefix, while the exact matching count is known mathematically.
        if (!criteria.isActive) {
            fun addPrefix(depth: Int) {
                if (selected.size >= outputLimit) return
                if (depth == options.size) {
                    selected.add(TotoColumn(selected.size + 1, predictionBuffer.toList()))
                    return
                }
                for (outcome in options[depth]) {
                    predictionBuffer.add(outcome)
                    addPrefix(depth + 1)
                    predictionBuffer.removeAt(predictionBuffer.lastIndex)
                    if (selected.size >= outputLimit) break
                }
            }
            if (outputLimit > 0) addPrefix(0)
            return MatrixFilterResult(total.toInt(), total.toInt(), selected, isTooLarge = false)
        }

        var matchingCount = 0
        var visited = 0

        fun evaluate(depth: Int) {
            if (depth == options.size) {
                visited++
                if (visited % 1024 == 0 && shouldCancel()) {
                    throw java.util.concurrent.CancellationException("Matrix filter preview superseded")
                }
                val column = TotoColumn(0, predictionBuffer.toList())
                if (evaluateColumn(column, criteria, matches)) {
                    matchingCount++
                    if (selected.size < outputLimit) {
                        selected.add(column.copy(index = selected.size + 1))
                    }
                }
                return
            }
            for (outcome in options[depth]) {
                predictionBuffer.add(outcome)
                evaluate(depth + 1)
                predictionBuffer.removeAt(predictionBuffer.lastIndex)
            }
        }

        evaluate(0)
        return MatrixFilterResult(total.toInt(), matchingCount, selected, isTooLarge = false)
    }

    const val MAX_EXACT_MATRIX_COMBINATIONS = 1_000_000
    const val MAX_STORED_MATRIX_COLUMNS = 100_000

}

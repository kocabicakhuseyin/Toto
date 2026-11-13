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


/**
 * The choices a selected 15-match matrix can really produce. Count chips are
 * derived from the user's singles/doubles/triples instead of fixed assumptions.
 * A streak is only relevant inside a run of adjacent double/triple matches.
 */
data class MatchFilterAvailability(
    val totalSurpriseCounts: List<Int>,
    val totalDrawCounts: List<Int>,
    val totalHardSurpriseCounts: List<Int>,
    val homeCounts: List<Int>,
    val awayCounts: List<Int>,
    val surprises1To9: List<Int>,
    val draws1To9: List<Int>,
    val hardSurprises1To9: List<Int>,
    val surprises10To15: List<Int>,
    val draws10To15: List<Int>,
    val hardSurprises10To15: List<Int>,
    val maximumHomeStreak: Int,
    val maximumDrawStreak: Int,
    val maximumAwayStreak: Int,
    val variableMatchNumbers: Set<Int>
) {
    val hasVariableSelections: Boolean get() = variableMatchNumbers.isNotEmpty()
    val hasApplicableStreakFilter: Boolean get() =
        maximumHomeStreak >= 2 || maximumDrawStreak >= 2 || maximumAwayStreak >= 2

    /** Clears conditions that cannot apply after the match selections change. */
    fun restrict(criteria: TotoFilterCriteria): TotoFilterCriteria {
        fun keep(values: Set<Int>, options: List<Int>): Set<Int> =
            if (options.size > 1) values.intersect(options.toSet()) else emptySet()

        fun keepStreak(value: Int?, maximum: Int): Int? =
            value?.takeIf { maximum >= 2 && it in 1..maximum }

        val validCustomMatches = criteria.customSurpriseMatches.intersect(variableMatchNumbers)
        return criteria.copy(
            allowedTotalSurprises = keep(criteria.allowedTotalSurprises, totalSurpriseCounts),
            allowedTotalDraws = keep(criteria.allowedTotalDraws, totalDrawCounts),
            allowedTotalHardSurprises = keep(criteria.allowedTotalHardSurprises, totalHardSurpriseCounts),
            allowedCountOf1s = keep(criteria.allowedCountOf1s, homeCounts),
            allowedCountOf2s = keep(criteria.allowedCountOf2s, awayCounts),
            allowedSurprises1To9 = keep(criteria.allowedSurprises1To9, surprises1To9),
            allowedDraws1To9 = keep(criteria.allowedDraws1To9, draws1To9),
            allowedHardSurprises1To9 = keep(criteria.allowedHardSurprises1To9, hardSurprises1To9),
            allowedSurprises10To15 = keep(criteria.allowedSurprises10To15, surprises10To15),
            allowedDraws10To15 = keep(criteria.allowedDraws10To15, draws10To15),
            allowedHardSurprises10To15 = keep(criteria.allowedHardSurprises10To15, hardSurprises10To15),
            maxConsecutive1s = keepStreak(criteria.maxConsecutive1s, maximumHomeStreak),
            maxConsecutiveDraws = keepStreak(criteria.maxConsecutiveDraws, maximumDrawStreak),
            maxConsecutive2s = keepStreak(criteria.maxConsecutive2s, maximumAwayStreak),
            customSurpriseMatches = validCustomMatches,
            allowedCustomSurprisesCount =
                if (validCustomMatches.isNotEmpty())
                    criteria.allowedCustomSurprisesCount.filterTo(mutableSetOf()) {
                        it in 0..validCustomMatches.size
                    }
                else emptySet()
        )
    }
}

object TotoFilterEngine {


    /** Identical empty-selection fallback to full-matrix generation. */
    private fun optionsForMatch(match: TotoMatch): List<PredictionOutcome> {
        if (match.selections.isNotEmpty()) return match.selections.sortedBy { it.ordinal }
        return listOf(match.favoriteOutcome)
    }

    /** Computes attainable number buttons from current 1/X/2 selections. */
    fun filterAvailability(matches: List<TotoMatch>): MatchFilterAvailability {
        val options = matches.map(::optionsForMatch)

        fun range(
            indices: IntRange,
            qualifies: (TotoMatch, PredictionOutcome) -> Boolean
        ): List<Int> {
            var least = 0
            var most = 0
            for (index in indices) {
                if (index !in matches.indices) continue
                val choices = options[index]
                if (choices.all { qualifies(matches[index], it) }) least++
                if (choices.any { qualifies(matches[index], it) }) most++
            }
            return (least..most).toList()
        }

        fun streakMaximum(outcome: PredictionOutcome): Int {
            var current = 0
            var maximum = 0
            for (choices in options) {
                // Singles interrupt the variable streak even if they match.
                current = if (choices.size >= 2 && outcome in choices) current + 1 else 0
                maximum = maxOf(maximum, current)
            }
            return if (maximum >= 2) maximum else 0
        }

        val all = matches.indices
        val firstNine = 0..8
        val lastSix = 9..14
        val surprise: (TotoMatch, PredictionOutcome) -> Boolean = { match, outcome ->
            analyzeMatchOutcome(match, outcome) != OutcomeType.FAVORITE
        }
        val hard: (TotoMatch, PredictionOutcome) -> Boolean = { match, outcome ->
            analyzeMatchOutcome(match, outcome) == OutcomeType.HARD_SURPRISE
        }

        return MatchFilterAvailability(
            totalSurpriseCounts = range(all, surprise),
            totalDrawCounts = range(all) { _, outcome -> outcome == PredictionOutcome.DRAW },
            totalHardSurpriseCounts = range(all, hard),
            homeCounts = range(all) { _, outcome -> outcome == PredictionOutcome.HOME },
            awayCounts = range(all) { _, outcome -> outcome == PredictionOutcome.AWAY },
            surprises1To9 = range(firstNine, surprise),
            draws1To9 = range(firstNine) { _, outcome -> outcome == PredictionOutcome.DRAW },
            hardSurprises1To9 = range(firstNine, hard),
            surprises10To15 = range(lastSix, surprise),
            draws10To15 = range(lastSix) { _, outcome -> outcome == PredictionOutcome.DRAW },
            hardSurprises10To15 = range(lastSix, hard),
            maximumHomeStreak = streakMaximum(PredictionOutcome.HOME),
            maximumDrawStreak = streakMaximum(PredictionOutcome.DRAW),
            maximumAwayStreak = streakMaximum(PredictionOutcome.AWAY),
            variableMatchNumbers = matches.withIndex().filter { options[it.index].size >= 2 }
                .mapTo(mutableSetOf()) { it.value.matchNumber }
        )
    }

    /**
     * Consecutive results only count when BOTH adjacent match positions were
     * selected as doubles or triples; bankos/singles break the streak.
     */
    private fun selectedVariableStreak(
        predictions: List<PredictionOutcome>,
        matches: List<TotoMatch>,
        outcome: PredictionOutcome
    ): Int {
        var current = 0
        var maximum = 0
        for (index in 0 until minOf(predictions.size, matches.size)) {
            current = if (matches[index].selections.size >= 2 && predictions[index] == outcome) current + 1 else 0
            maximum = maxOf(maximum, current)
        }
        return if (maximum >= 2) maximum else 0
    }

    /**
     * A chosen set is an OR group: {2, 3} accepts 2 OR 3. Each different
     * numeric filter is AND-ed with the others by evaluateColumn.
     * Flexible mode expands EACH selected value independently, preserving OR.
     */
    private fun countAllowed(
        actual: Int,
        selected: Set<Int>,
        mode: FilterApplicationMode
    ): Boolean {
        if (selected.isEmpty()) return true
        val tolerance = if (mode == FilterApplicationMode.GUARANTEE_FLEXIBLE) 1 else 0
        return selected.any { kotlin.math.abs(actual - it) <= tolerance }
    }

    private fun maximumAllowed(
        actual: Int,
        selected: Int?,
        mode: FilterApplicationMode
    ): Boolean {
        if (selected == null) return true
        val tolerance = if (mode == FilterApplicationMode.GUARANTEE_FLEXIBLE) 1 else 0
        return actual <= selected + tolerance
    }

    /**
     * Determines whether an outcome for a specific match is considered:
     * - favorite
     * - surprise
     * - hard surprise (ters sürpriz)
     */
    fun analyzeMatchOutcome(match: TotoMatch, outcome: PredictionOutcome): OutcomeType {
        val favorite = match.favoriteOutcome
        if (outcome == favorite) return OutcomeType.FAVORITE

        // When 0 is the favorite, 1 or 2 is a normal surprise, not a
        // reverse 1-vs-2 upset. With a team favorite, the opposite team is
        // a hard surprise; a non-favorite draw is a normal surprise.
        return if (outcome == PredictionOutcome.DRAW ||
            favorite == PredictionOutcome.DRAW
        ) OutcomeType.SURPRISE else OutcomeType.HARD_SURPRISE
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

        // Every active condition intersects with all the others. The flexible
        // mode is a ±1 margin PER numeric condition, never permission to skip
        // a failing condition (which previously made adding a filter expand results).
        val mode = criteria.applicationMode
        if (!countAllowed(analysis.totalSurprises, criteria.allowedTotalSurprises, mode)) return false
        if (!countAllowed(analysis.totalDraws, criteria.allowedTotalDraws, mode)) return false
        if (!countAllowed(analysis.totalHardSurprises, criteria.allowedTotalHardSurprises, mode)) return false
        if (!countAllowed(analysis.countOf1s, criteria.allowedCountOf1s, mode)) return false
        if (!countAllowed(analysis.countOf2s, criteria.allowedCountOf2s, mode)) return false
        if (!countAllowed(analysis.surprises1To9, criteria.allowedSurprises1To9, mode)) return false
        if (!countAllowed(analysis.draws1To9, criteria.allowedDraws1To9, mode)) return false
        if (!countAllowed(analysis.hardSurprises1To9, criteria.allowedHardSurprises1To9, mode)) return false
        if (!countAllowed(analysis.surprises10To15, criteria.allowedSurprises10To15, mode)) return false
        if (!countAllowed(analysis.draws10To15, criteria.allowedDraws10To15, mode)) return false
        if (!countAllowed(analysis.hardSurprises10To15, criteria.allowedHardSurprises10To15, mode)) return false

        if (criteria.maxConsecutive1s != null &&
            !maximumAllowed(
                selectedVariableStreak(column.predictions, matches, PredictionOutcome.HOME),
                criteria.maxConsecutive1s, mode
            )
        ) return false
        if (criteria.maxConsecutiveDraws != null &&
            !maximumAllowed(
                selectedVariableStreak(column.predictions, matches, PredictionOutcome.DRAW),
                criteria.maxConsecutiveDraws, mode
            )
        ) return false
        if (criteria.maxConsecutive2s != null &&
            !maximumAllowed(
                selectedVariableStreak(column.predictions, matches, PredictionOutcome.AWAY),
                criteria.maxConsecutive2s, mode
            )
        ) return false

        if (criteria.customSurpriseMatches.isNotEmpty() &&
            !countAllowed(analysis.customSurprisesCount, criteria.allowedCustomSurprisesCount, mode)
        ) return false

        return true
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
     * Filters an existing coupon's columns when provided. Otherwise filters the
     * complete selected match matrix. Every result is a subset of its input;
     * the coupon cap is applied only after evaluating the filter predicates.
     */
    fun filterSelectedMatrix(
        matches: List<TotoMatch>,
        criteria: TotoFilterCriteria,
        maxColumnsCap: Int? = null,
        sourceColumns: List<TotoColumn>? = null,
        shouldCancel: () -> Boolean = { false }
    ): MatrixFilterResult {
        if (sourceColumns == null && matches.isEmpty()) {
            return MatrixFilterResult(0, 0, emptyList(), isTooLarge = false)
        }

        // Re-filter the active coupon itself; never regenerate extra columns
        // when the user opens filtering a second time or relaxes a condition.
        if (sourceColumns != null) {
            val initial = sourceColumns.size
            if (initial > MAX_EXACT_MATRIX_COMBINATIONS) {
                return MatrixFilterResult(initial, 0, emptyList(), isTooLarge = true)
            }

            val requested = when (maxColumnsCap) {
                FormulaEngine.UNLIMITED_COLUMNS_CAP -> MAX_STORED_MATRIX_COLUMNS
                null -> FormulaEngine.calculateSmartCap(matches)
                else -> maxColumnsCap.coerceAtLeast(0)
            }
            val outputLimit = requested.coerceAtMost(MAX_STORED_MATRIX_COLUMNS)
            val accepted = ArrayList<TotoColumn>(minOf(initial, outputLimit))
            var matching = 0
            for ((position, col) in sourceColumns.withIndex()) {
                if (position % 1024 == 0 && shouldCancel()) {
                    throw java.util.concurrent.CancellationException("Matrix filter preview superseded")
                }
                if (!criteria.isActive || evaluateColumn(col, criteria, matches)) {
                    matching++
                    if (accepted.size < outputLimit) {
                        accepted.add(col.copy(index = accepted.size + 1))
                    }
                }
            }
            return MatrixFilterResult(initial, matching, accepted, isTooLarge = false)
        }

        val options = matches.map(::optionsForMatch)

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

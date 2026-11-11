package com.example

import com.example.engine.TotoFilterEngine
import com.example.model.FilterApplicationMode
import com.example.model.MatchProbability
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.model.TotoFilterCriteria
import com.example.model.TotoMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TotoFilterEngineTest {

    private lateinit var testMatches: List<TotoMatch>

    @Before
    fun setup() {
        // Create 15 matches where match 1-10 home is favorite, match 11-13 away favorite, match 14-15 draw favorite
        testMatches = (1..15).map { idx ->
            val prob = when {
                idx <= 10 -> MatchProbability(listOf(70), listOf(20), listOf(10)) // Home favorite
                idx <= 13 -> MatchProbability(listOf(15), listOf(25), listOf(60)) // Away favorite
                else -> MatchProbability(listOf(20), listOf(60), listOf(20))      // Draw favorite
            }
            TotoMatch(
                id = idx,
                matchNumber = idx,
                homeTeam = "Home$idx",
                awayTeam = "Away$idx",
                matchDate = "2026-11-15",
                probability = prob
            )
        }
    }

    @Test
    fun testOutcomeTypeAnalysis() {
        val homeFavMatch = testMatches[0]
        assertEquals(
            TotoFilterEngine.OutcomeType.FAVORITE,
            TotoFilterEngine.analyzeMatchOutcome(homeFavMatch, PredictionOutcome.HOME)
        )
        assertEquals(
            TotoFilterEngine.OutcomeType.SURPRISE,
            TotoFilterEngine.analyzeMatchOutcome(homeFavMatch, PredictionOutcome.DRAW)
        )
        assertEquals(
            TotoFilterEngine.OutcomeType.HARD_SURPRISE,
            TotoFilterEngine.analyzeMatchOutcome(homeFavMatch, PredictionOutcome.AWAY)
        )
    }

    @Test
    fun testColumnAnalysisStats() {
        // Create a column with all HOME predictions
        val allHomePreds = List(15) { PredictionOutcome.HOME }
        val col = TotoColumn(index = 1, predictions = allHomePreds)

        val analysis = TotoFilterEngine.analyzeColumn(col, testMatches)

        assertEquals(15, analysis.countOf1s)
        assertEquals(0, analysis.totalDraws)
        assertEquals(0, analysis.countOf2s)
        assertEquals(15, analysis.maxConsecutive1s)

        // For matches 1..10, HOME is favorite (0 surprises). For matches 11..13, AWAY is favorite, so HOME is surprise (3 surprises). For 14..15 DRAW is favorite, so HOME is surprise (2 surprises).
        // Total surprises = 5
        assertEquals(5, analysis.totalSurprises)
        assertEquals(0, analysis.surprises1To9)
        assertEquals(5, analysis.surprises10To15)
    }

    @Test
    fun testStrictFilterElimination() {
        val allHome = TotoColumn(1, List(15) { PredictionOutcome.HOME })
        val mixedCol = TotoColumn(2, listOf(
            PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.HOME, PredictionOutcome.HOME,
            PredictionOutcome.AWAY, PredictionOutcome.HOME, PredictionOutcome.HOME, PredictionOutcome.HOME,
            PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.HOME, PredictionOutcome.AWAY,
            PredictionOutcome.AWAY, PredictionOutcome.DRAW, PredictionOutcome.DRAW
        ))

        // Criteria: exactly 2 draws allowed
        val criteria = TotoFilterCriteria(
            applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
            allowedTotalDraws = setOf(2)
        )

        // allHome has 0 draws -> must be eliminated
        assertFalse(TotoFilterEngine.evaluateColumn(allHome, criteria, testMatches))

        // mixedCol has 4 draws (indices 1, 9, 13, 14) -> must be eliminated under strict setOf(2)
        assertFalse(TotoFilterEngine.evaluateColumn(mixedCol, criteria, testMatches))

        // Criteria allowing 4 draws
        val criteria4Draws = criteria.copy(allowedTotalDraws = setOf(4))
        assertTrue(TotoFilterEngine.evaluateColumn(mixedCol, criteria4Draws, testMatches))
    }

    @Test
    fun testGuaranteeFlexibleModeAllowsOneViolation() {
        val mixedCol = TotoColumn(1, listOf(
            PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.HOME, PredictionOutcome.HOME,
            PredictionOutcome.AWAY, PredictionOutcome.HOME, PredictionOutcome.HOME, PredictionOutcome.HOME,
            PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.HOME, PredictionOutcome.AWAY,
            PredictionOutcome.AWAY, PredictionOutcome.DRAW, PredictionOutcome.DRAW
        ))

        // Two filters active:
        // Filter 1: draws must be in {4} (PASSES for mixedCol)
        // Filter 2: countOf1s must be in {12} (mixedCol has 8 1s -> FAILS)
        val strictCriteria = TotoFilterCriteria(
            applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
            allowedTotalDraws = setOf(4),
            allowedCountOf1s = setOf(12)
        )
        assertFalse(TotoFilterEngine.evaluateColumn(mixedCol, strictCriteria, testMatches))

        // Under GUARANTEE_FLEXIBLE, 1 violation is tolerated to safeguard guarantee tier
        val flexibleCriteria = strictCriteria.copy(applicationMode = FilterApplicationMode.GUARANTEE_FLEXIBLE)
        assertTrue(TotoFilterEngine.evaluateColumn(mixedCol, flexibleCriteria, testMatches))
    }

    @Test
    fun testFilterColumnsAndSavings() {
        val col1 = TotoColumn(1, List(15) { PredictionOutcome.HOME })
        val col2 = TotoColumn(2, List(15) { PredictionOutcome.DRAW })
        val col3 = TotoColumn(3, List(15) { PredictionOutcome.AWAY })

        val criteria = TotoFilterCriteria(
            applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
            allowedTotalDraws = setOf(15)
        )

        val filtered = TotoFilterEngine.filterColumns(listOf(col1, col2, col3), criteria, testMatches)
        assertEquals(1, filtered.size)
        assertEquals(1, filtered[0].index) // Re-indexed to 1

        val preview = TotoFilterEngine.previewFilter(listOf(col1, col2, col3), criteria, testMatches)
        assertEquals(3, preview.initialColumnCount)
        assertEquals(1, preview.filteredColumnCount)
        assertEquals(2, preview.eliminatedCount)
        assertEquals(66, preview.savingsPercentage) // 2/3 = 66%
        assertEquals(0.60, preview.initialCostTL, 0.001)
        assertEquals(0.20, preview.filteredCostTL, 0.001)
    }

    @Test
    fun testStreakFilter() {
        // Column with 4 consecutive 1s
        val preds = listOf(
            PredictionOutcome.HOME, PredictionOutcome.HOME, PredictionOutcome.HOME, PredictionOutcome.HOME,
            PredictionOutcome.DRAW, PredictionOutcome.HOME, PredictionOutcome.HOME, PredictionOutcome.AWAY,
            PredictionOutcome.HOME, PredictionOutcome.HOME, PredictionOutcome.HOME, PredictionOutcome.AWAY,
            PredictionOutcome.AWAY, PredictionOutcome.DRAW, PredictionOutcome.DRAW
        )
        val col = TotoColumn(1, preds)

        // If maxConsecutive1s is 3, col should be eliminated
        val criteriaMax3 = TotoFilterCriteria(
            applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
            maxConsecutive1s = 3
        )
        assertFalse(TotoFilterEngine.evaluateColumn(col, criteriaMax3, testMatches))

        // If maxConsecutive1s is 4 or 5, col should pass
        val criteriaMax4 = criteriaMax3.copy(maxConsecutive1s = 4)
        assertTrue(TotoFilterEngine.evaluateColumn(col, criteriaMax4, testMatches))
    }

    @Test
    fun testFormulaEngineUnlimitedColumnsCapGeneratesBeyond48() {
        // Create 7 double selections: 2^7 = 128 combinations
        val matchesWithDoubles = testMatches.mapIndexed { idx, match ->
            if (idx < 7) {
                match.copy(selections = setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW))
            } else {
                match.copy(selections = setOf(PredictionOutcome.HOME))
            }
        }

        // Unlimited generation
        val allCols = com.example.engine.FormulaEngine.generateAllCombinations(matchesWithDoubles, limit = 1024)
        assertEquals(128, allCols.size)

        // When UNLIMITED_COLUMNS_CAP is passed to generateColumns with GUARANTEE_15, all 128 are kept, not 48!
        val genUnlimited = com.example.engine.FormulaEngine.generateColumns(
            matchesWithDoubles,
            com.example.model.FormulaType.GUARANTEE_15,
            maxColumnsCap = com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP
        )
        assertEquals(128, genUnlimited.size)
    }

    @Test
    fun testViewModelApplyFiltersWithUnlimitedCapDoesNotTruncateTo48() {
        val viewModel = com.example.viewmodel.TotoViewModel()

        // Set Kolon Limiti Yok (-1)
        viewModel.setTargetColumnCap(com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP)

        // Configure 8 doubles on the first 8 matches: 2^8 = 256 combinations
        (1..8).forEach { matchId ->
            viewModel.togglePrediction(matchId, PredictionOutcome.HOME)
            viewModel.togglePrediction(matchId, PredictionOutcome.DRAW)
        }

        // Apply a filter criteria: allow total draws in 2..5
        val filterCriteria = TotoFilterCriteria(
            applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
            allowedTotalDraws = setOf(2, 3, 4, 5)
        )

        val filteredCoupon = viewModel.applyFiltersAndEliminate(filterCriteria)

        // The filtered coupon should narrow down based on the filter rules,
        // and because there is no column cap, it should have the exact number of matching columns,
        // NOT truncated or capped to 48!
        assertTrue("Filtered columns size (${filteredCoupon.totalColumns}) should not be forced to 48!", filteredCoupon.totalColumns > 48)
    }

    @Test
    fun testMatrixFiltersBeforeColumnCap() {
        val matchesWithDoubles = testMatches.mapIndexed { idx, match ->
            if (idx < 4) match.copy(
                selections = setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
            ) else match.copy(selections = setOf(PredictionOutcome.HOME))
        }
        val criteria = TotoFilterCriteria(
            applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
            allowedTotalDraws = setOf(2)
        )

        // 2^4 = 16 source columns, C(4,2) = 6 passing columns.
        // A coupon cap of 2 must be applied ONLY after all 16 are checked.
        val result = TotoFilterEngine.filterSelectedMatrix(
            matchesWithDoubles, criteria, maxColumnsCap = 2
        )
        assertEquals(16, result.initialColumnCount)
        assertEquals(6, result.matchingColumnCount)
        assertEquals(2, result.savedColumnCount)
        assertTrue(result.isLimitedByCap)
        assertTrue(result.columns.all { col -> col.predictions.count { it == PredictionOutcome.DRAW } == 2 })
    }

    @Test
    fun testFull41472ColumnMatrixMatchesReferenceFilter() {
        val selections = testMatches.mapIndexed { index, match ->
            val options = when {
                index < 4 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.AWAY)
                index < 13 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                else -> setOf(PredictionOutcome.HOME)
            }
            match.copy(selections = options)
        }
        // The previous implementation filtered a formula-reduced sample instead of
        // all 4 triple + 9 double positions (3^4 * 2^9 = 41472 columns).
        val criteria = TotoFilterCriteria(
            allowedTotalDraws = setOf(3, 4),
            allowedDraws1To9 = setOf(2, 3),
            allowedDraws10To15 = setOf(1, 2),
            maxConsecutive1s = 3,
            maxConsecutive2s = 2
        )
        val reference = com.example.engine.FormulaEngine.generateAllCombinations(selections, 50000)
        val expected = TotoFilterEngine.filterColumns(reference, criteria, selections)
        val actual = TotoFilterEngine.filterSelectedMatrix(
            selections, criteria,
            maxColumnsCap = com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP
        )
        assertEquals(41472, reference.size)
        assertEquals(41472, actual.initialColumnCount)
        assertEquals(expected.size, actual.matchingColumnCount)
        assertEquals(expected.map { it.asString }.toSet(), actual.columns.map { it.asString }.toSet())
        assertFalse(actual.isTooLarge)
    }

    @Test
    fun testImpossibleFiltersDoNotRestoreRejectedColumns() {
        val fixed = testMatches.map { it.copy(selections = setOf(PredictionOutcome.HOME)) }
        val impossible = TotoFilterCriteria(allowedTotalDraws = setOf(16))
        val result = TotoFilterEngine.filterSelectedMatrix(fixed, impossible)
        assertEquals(1, result.initialColumnCount)
        assertEquals(0, result.matchingColumnCount)
        assertTrue(result.columns.isEmpty())

        val vm = com.example.viewmodel.TotoViewModel()
        val coupon = vm.applyFiltersAndEliminate(impossible)
        assertEquals(0, coupon.totalColumns)
    }

    @Test
    fun testOversizedMatrixIsNotSilentlySampled() {
        val manyTriples = testMatches.mapIndexed { index, match ->
            if (index < 13) match.copy(
                selections = setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.AWAY)
            ) else match.copy(selections = setOf(PredictionOutcome.HOME))
        }
        val result = TotoFilterEngine.filterSelectedMatrix(
            manyTriples, TotoFilterCriteria(allowedTotalDraws = setOf(3))
        )
        assertEquals(1_594_323, result.initialColumnCount)
        assertTrue(result.isTooLarge)
        assertTrue(result.columns.isEmpty())
    }


    @Test
    fun testRepeatedFilteringNeverExpandsCurrentCoupon() {
        val viewModel = com.example.viewmodel.TotoViewModel()
        viewModel.setTargetColumnCap(com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP)

        // Four user-selected doubles produce 16 full 15-match columns.
        for (id in 1..4) {
            viewModel.togglePrediction(id, PredictionOutcome.HOME)
            viewModel.togglePrediction(id, PredictionOutcome.DRAW)
        }
        val generated = viewModel.generateCoupon(
            com.example.model.FormulaType.GUARANTEE_15
        )
        assertEquals(16, generated.totalColumns)

        val narrowed = viewModel.applyFiltersAndEliminate(
            TotoFilterCriteria(allowedTotalDraws = setOf(2))
        )
        assertEquals(6, narrowed.totalColumns)

        // Resetting filter criteria does not quietly regenerate the 16
        // original combinations or add back any of the 10 rejected columns.
        val repeated = viewModel.applyFiltersAndEliminate(TotoFilterCriteria())
        assertEquals(6, repeated.totalColumns)
        assertEquals(narrowed.columns.map { it.asString }.toSet(),
            repeated.columns.map { it.asString }.toSet())
    }

    @Test
    fun testChangingMatchSelectionsStartsAFreshMatrix() {
        val viewModel = com.example.viewmodel.TotoViewModel()
        viewModel.setTargetColumnCap(com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP)
        for (id in 1..3) {
            viewModel.togglePrediction(id, PredictionOutcome.HOME)
            viewModel.togglePrediction(id, PredictionOutcome.DRAW)
        }
        assertEquals(8, viewModel.generateCoupon(
            com.example.model.FormulaType.GUARANTEE_15
        ).totalColumns)

        // A changed selection invalidates the previously generated coupon.
        viewModel.togglePrediction(1, PredictionOutcome.AWAY)
        assertEquals(null, viewModel.currentFilterSourceColumns())
        val refreshed = viewModel.applyFiltersAndEliminate(TotoFilterCriteria())
        assertEquals(12, refreshed.totalColumns)
    }

    @Test
    fun testFlexibleModeWithOneActiveFilterCannotAcceptItsFailures() {
        val allHome = TotoColumn(1, List(15) { PredictionOutcome.HOME })
        val criteria = TotoFilterCriteria(
            applicationMode = FilterApplicationMode.GUARANTEE_FLEXIBLE,
            allowedTotalDraws = setOf(3)
        )
        assertFalse(TotoFilterEngine.evaluateColumn(allHome, criteria, testMatches))
        assertTrue(TotoFilterEngine.evaluateColumn(
            allHome, criteria.copy(allowedTotalDraws = setOf(0)), testMatches
        ))
    }

    @Test
    fun testExistingMatrixSubsetRemainsSubsetAfterEveryFilter() {
        val fixed = testMatches.mapIndexed { index, m ->
            m.copy(selections = if (index < 5)
                setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
            else setOf(PredictionOutcome.HOME))
        }
        val original = com.example.engine.FormulaEngine.generateAllCombinations(fixed, 1000)
        val first = TotoFilterEngine.filterSelectedMatrix(
            fixed,
            TotoFilterCriteria(allowedTotalDraws = setOf(1, 2)),
            maxColumnsCap = com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP,
            sourceColumns = original
        )
        val second = TotoFilterEngine.filterSelectedMatrix(
            fixed,
            TotoFilterCriteria(allowedTotalDraws = setOf(0, 1, 2, 3, 4, 5)),
            maxColumnsCap = com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP,
            sourceColumns = first.columns
        )
        assertEquals(32, first.initialColumnCount)
        assertEquals(first.savedColumnCount, second.initialColumnCount)
        assertEquals(first.savedColumnCount, second.savedColumnCount)
        assertTrue(second.columns.all { it.asString in first.columns.map { c -> c.asString }.toSet() })
    }

}

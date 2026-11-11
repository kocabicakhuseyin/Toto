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
}

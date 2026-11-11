package com.example

import com.example.engine.GuaranteeFormulaEngine
import com.example.model.MatchProbability
import com.example.model.PredictionOutcome
import com.example.model.TotoMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GuaranteeFormulaEngineTest {

    private lateinit var sampleMatches: List<TotoMatch>

    @Before
    fun setup() {
        // Matches 1..12 are singles (HOME), Matches 13, 14, 15 are doubles (HOME and DRAW)
        // Total full combinations = 2^3 = 8 combinations
        sampleMatches = (1..15).map { idx ->
            val selections = if (idx >= 13) {
                setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
            } else {
                setOf(PredictionOutcome.HOME)
            }
            TotoMatch(
                id = idx,
                matchNumber = idx,
                homeTeam = "Team$idx",
                awayTeam = "Opponent$idx",
                matchDate = "2026-11-15",
                probability = MatchProbability(listOf(60), listOf(25), listOf(15)),
                selections = selections
            )
        }
    }

    @Test
    fun testNMinus1CoveringGuaranteesMax1Error() {
        val result = GuaranteeFormulaEngine.generateNMinus1Columns(
            matches = sampleMatches,
            maxColumnsCap = 16,
            targetCoverage = 1.0
        )

        assertEquals(8, result.fullCombinationsCount)
        assertTrue(result.reducedColumns.isNotEmpty())
        assertTrue(result.reducedColumns.size < result.fullCombinationsCount)
        assertEquals(100.0, result.guaranteed14CoveragePercentage, 0.01)

        // All 8 potential target combinations must have Hamming distance <= 1 to at least one column in result
        val all8Combinations = mutableListOf<List<PredictionOutcome>>()
        for (m13 in listOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)) {
            for (m14 in listOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)) {
                for (m15 in listOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)) {
                    val fullList = List(12) { PredictionOutcome.HOME } + listOf(m13, m14, m15)
                    all8Combinations.add(fullList)
                }
            }
        }

        for (target in all8Combinations) {
            val satisfies = GuaranteeFormulaEngine.satisfiesNMinus1Guarantee(result.reducedColumns, target)
            assertTrue("Target $target must be covered within Hamming distance <= 1", satisfies)
        }
    }

    @Test
    fun testHammingDistance() {
        val a = List(15) { PredictionOutcome.HOME }
        val b = List(14) { PredictionOutcome.HOME } + listOf(PredictionOutcome.DRAW)
        val c = List(13) { PredictionOutcome.HOME } + listOf(PredictionOutcome.DRAW, PredictionOutcome.AWAY)

        assertEquals(0, GuaranteeFormulaEngine.hammingDistance(a, a))
        assertEquals(1, GuaranteeFormulaEngine.hammingDistance(a, b))
        assertEquals(2, GuaranteeFormulaEngine.hammingDistance(a, c))
    }

    @Test
    fun testCostAndSavingsCalculation() {
        val result = GuaranteeFormulaEngine.generateNMinus1Columns(
            matches = sampleMatches,
            maxColumnsCap = 10
        )

        val expectedFullCost = 8 * 0.20 // 1.60 TL
        val expectedReducedCost = result.reducedColumns.size * 0.20

        assertEquals(expectedFullCost, result.fullCostTL, 0.001)
        assertEquals(expectedReducedCost, result.reducedCostTL, 0.001)
        assertTrue(result.savingsPercentage > 0)
    }

    @Test
    fun testMatches10To15AreEvenlyDistributed() {
        // Matches 1..9 are HOME bankos, Matches 10..15 are doubles (indices 9..14)
        val matchesWith10To15Variables = (1..15).map { idx ->
            val selections = when (idx) {
                10 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                11 -> setOf(PredictionOutcome.DRAW, PredictionOutcome.AWAY)
                12 -> setOf(PredictionOutcome.HOME, PredictionOutcome.AWAY)
                13 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                14 -> setOf(PredictionOutcome.DRAW, PredictionOutcome.AWAY)
                15 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                else -> setOf(PredictionOutcome.HOME)
            }
            TotoMatch(
                id = idx,
                matchNumber = idx,
                homeTeam = "Team$idx",
                awayTeam = "Opponent$idx",
                matchDate = "2026-11-15",
                probability = MatchProbability(listOf(50), listOf(25), listOf(25)),
                selections = selections
            )
        }

        // Test N-1 Guarantee
        val n1Result = GuaranteeFormulaEngine.generateNMinus1Columns(
            matches = matchesWith10To15Variables,
            maxColumnsCap = 48
        )
        assertTrue("N-1 must generate multiple columns", n1Result.reducedColumns.size >= 8)

        // Verify that EVERY match from 10 to 15 (indices 9 to 14) has its choices distributed
        for (mIdx in 9 until 15) {
            val match = matchesWith10To15Variables[mIdx]
            val outcomesInColumns = n1Result.reducedColumns.map { it.predictions[mIdx] }.toSet()
            for (opt in match.selections) {
                assertTrue(
                    "Match ${mIdx + 1} option $opt must appear in generated columns, but was: $outcomesInColumns",
                    outcomesInColumns.contains(opt)
                )
            }
        }

        // Test FormulaEngine for 14, 13, and 15 guarantees
        val f14Cols = com.example.engine.FormulaEngine.generateColumns(
            matches = matchesWith10To15Variables,
            formulaType = com.example.model.FormulaType.GUARANTEE_14
        )
        for (mIdx in 9 until 15) {
            val match = matchesWith10To15Variables[mIdx]
            val outcomes = f14Cols.map { it.predictions[mIdx] }.toSet()
            for (opt in match.selections) {
                assertTrue("F14: Match ${mIdx + 1} option $opt must appear in columns", outcomes.contains(opt))
            }
        }

        val f13Cols = com.example.engine.FormulaEngine.generateColumns(
            matches = matchesWith10To15Variables,
            formulaType = com.example.model.FormulaType.GUARANTEE_13
        )
        for (mIdx in 9 until 15) {
            val match = matchesWith10To15Variables[mIdx]
            val outcomes = f13Cols.map { it.predictions[mIdx] }.toSet()
            for (opt in match.selections) {
                assertTrue("F13: Match ${mIdx + 1} option $opt must appear in columns", outcomes.contains(opt))
            }
        }
    }

    @Test
    fun testDynamicSmartCapAndUserCap() {
        // 8 combinations: smart cap should be <= 4, definitely NOT 48
        val smartCap8 = com.example.engine.FormulaEngine.calculateSmartCap(sampleMatches)
        assertTrue("Smart cap for 8 combos should be <= 8, but was $smartCap8", smartCap8 <= 8)
        assertTrue("Smart cap for 8 combos must not be 48", smartCap8 < 48)

        val cols = com.example.engine.FormulaEngine.generateColumns(
            matches = sampleMatches,
            formulaType = com.example.model.FormulaType.GUARANTEE_14
        )
        assertTrue("Columns for 8 combos should be <= 8, but was: ${cols.size}", cols.size <= 8)
        assertTrue("Columns must not be hardcoded 48, was: ${cols.size}", cols.size != 48)

        // Custom cap (e.g. 12) must be respected
        val colsCap12 = com.example.engine.FormulaEngine.generateColumns(
            matches = sampleMatches,
            formulaType = com.example.model.FormulaType.GUARANTEE_14,
            maxColumnsCap = 12
        )
        assertTrue(colsCap12.size <= 12)
    }

    @Test
    fun testFilterProfilePresets() {
        val fav = com.example.model.FilterProfileType.FAVORITE_WEIGHTED.toCriteria()
        assertEquals(com.example.model.FilterApplicationMode.PROBABILITY_STRICT, fav.applicationMode)
        assertEquals(setOf(0, 1, 2), fav.allowedTotalSurprises)
        assertEquals(4, fav.maxConsecutive1s)

        val bal = com.example.model.FilterProfileType.BALANCED.toCriteria()
        assertEquals(com.example.model.FilterApplicationMode.GUARANTEE_FLEXIBLE, bal.applicationMode)
        assertEquals(setOf(2, 3, 4), bal.allowedTotalSurprises)

        val sur = com.example.model.FilterProfileType.SURPRISE_WEIGHTED.toCriteria()
        assertEquals(setOf(4, 5, 6), sur.allowedTotalSurprises)
    }
}

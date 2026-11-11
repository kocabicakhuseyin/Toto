package com.example

import com.example.engine.GuaranteeFormulaEngine
import com.example.model.MatchProbability
import com.example.model.PredictionOutcome
import com.example.model.TotoFilterCriteria
import com.example.model.TotoMatch
import com.example.viewmodel.GuaranteeFormulaViewModel
import com.example.viewmodel.GuaranteeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuaranteeFormulaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: GuaranteeFormulaViewModel
    private lateinit var sampleMatches: List<TotoMatch>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GuaranteeFormulaViewModel(defaultDispatcher = testDispatcher)

        sampleMatches = (1..15).map { idx ->
            val selections = when {
                idx <= 10 -> setOf(PredictionOutcome.HOME) // 10 Bankos
                idx <= 13 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW) // 3 Doubles
                else -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.AWAY) // 2 Triples
            }
            TotoMatch(
                id = idx,
                matchNumber = idx,
                homeTeam = "Home$idx",
                awayTeam = "Away$idx",
                matchDate = "2026-11-15",
                probability = MatchProbability(listOf(50), listOf(30), listOf(20)),
                selections = selections
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMatrixSummaryCalculation() {
        // 10 bankos, 3 doubles (2^3 = 8), 2 triples (3^2 = 9) => 8 * 9 = 72 full combinations
        val summary = viewModel.calculateMatrixSummary(sampleMatches)

        assertEquals(10, summary.bankoCount)
        assertEquals(3, summary.doubleCount)
        assertEquals(2, summary.tripleCount)
        assertEquals(72, summary.totalEstimatedCombinations)
        assertEquals(72 * 0.20, summary.fullCostTL, 0.001)
    }

    @Test
    fun testGenerateNMinus1Execution() = runTest {
        assertEquals(GuaranteeUiState.Idle, viewModel.uiState.value)

        viewModel.generateNMinus1(sampleMatches)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Success but was $state", state is GuaranteeUiState.Success)

        val successState = state as GuaranteeUiState.Success
        val result = successState.result

        assertEquals(72, result.fullCombinationsCount)
        assertTrue(result.reducedColumns.isNotEmpty())
        assertTrue(result.reducedColumns.size < result.fullCombinationsCount)
        assertTrue("Savings percentage should be positive", result.savingsPercentage > 0)

        // Convert to coupon
        val coupon = viewModel.createCouponFromResult()
        assertNotNull(coupon)
        assertEquals(result.reducedColumns.size, coupon?.totalColumns)
    }

    @Test
    fun testApplySecondaryFilters() = runTest {
        viewModel.generateNMinus1(sampleMatches)
        advanceUntilIdle()

        // Filter: only allow 0 or 1 draw
        val criteria = TotoFilterCriteria(allowedTotalDraws = setOf(0, 1))
        viewModel.applySecondaryFilters(criteria, sampleMatches)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is GuaranteeUiState.Success)
    }
}

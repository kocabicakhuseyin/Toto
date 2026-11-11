package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.GuaranteeFormulaEngine
import com.example.engine.GuaranteeGenerationResult
import com.example.engine.TotoFilterEngine
import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.TotoColumn
import com.example.model.TotoFilterCriteria
import com.example.model.TotoMatch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GuaranteeUiState {
    object Idle : GuaranteeUiState
    data class Generating(val progressMessage: String) : GuaranteeUiState
    data class Success(val result: GuaranteeGenerationResult) : GuaranteeUiState
    data class Error(val message: String) : GuaranteeUiState
}

data class FormulaMatrixSummary(
    val bankoCount: Int,
    val doubleCount: Int,
    val tripleCount: Int,
    val totalEstimatedCombinations: Int,
    val fullCostTL: Double
)

class GuaranteeFormulaViewModel(
    private val engine: GuaranteeFormulaEngine = GuaranteeFormulaEngine,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow<GuaranteeUiState>(GuaranteeUiState.Idle)
    val uiState: StateFlow<GuaranteeUiState> = _uiState.asStateFlow()

    private val _maxColumnsCap = MutableStateFlow(64)
    val maxColumnsCap: StateFlow<Int> = _maxColumnsCap.asStateFlow()

    private val _targetCoverage = MutableStateFlow(1.0) // 100% 14 Garanti
    val targetCoverage: StateFlow<Double> = _targetCoverage.asStateFlow()

    private val _lastGeneratedResult = MutableStateFlow<GuaranteeGenerationResult?>(null)
    val lastGeneratedResult: StateFlow<GuaranteeGenerationResult?> = _lastGeneratedResult.asStateFlow()

    fun setMaxColumnsCap(cap: Int) {
        _maxColumnsCap.value = cap.coerceIn(8, 256)
    }

    fun setTargetCoverage(coverage: Double) {
        _targetCoverage.value = coverage.coerceIn(0.5, 1.0)
    }

    /**
     * Calculates the matrix complexity summary from the user's 15 matches.
     */
    fun calculateMatrixSummary(matches: List<TotoMatch>): FormulaMatrixSummary {
        var bankos = 0
        var doubles = 0
        var triples = 0
        var combinations = 1

        matches.take(15).forEach { match ->
            when (match.selections.size) {
                0, 1 -> {
                    bankos++
                    combinations *= 1
                }
                2 -> {
                    doubles++
                    combinations = (combinations * 2).coerceAtMost(100_000)
                }
                else -> {
                    triples++
                    combinations = (combinations * 3).coerceAtMost(100_000)
                }
            }
        }

        return FormulaMatrixSummary(
            bankoCount = bankos,
            doubleCount = doubles,
            tripleCount = triples,
            totalEstimatedCombinations = combinations,
            fullCostTL = combinations * GuaranteeFormulaEngine.COLUMN_FEE_TL
        )
    }

    /**
     * Executes the N-1 (14 Garanti) Covering Code algorithm asynchronously.
     */
    fun generateNMinus1(matches: List<TotoMatch>) {
        if (matches.isEmpty()) {
            _uiState.value = GuaranteeUiState.Error("Bülten maçları yüklenemedi!")
            return
        }

        viewModelScope.launch {
            _uiState.value = GuaranteeUiState.Generating("15 Maçlık N-1 (14 Garanti) kapsama matrisi hesaplanıyor...")

            try {
                val cap = _maxColumnsCap.value
                val coverage = _targetCoverage.value

                val result = withContext(defaultDispatcher) {
                    engine.generateNMinus1Columns(
                        matches = matches,
                        maxColumnsCap = cap,
                        targetCoverage = coverage
                    )
                }

                _lastGeneratedResult.value = result
                _uiState.value = GuaranteeUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = GuaranteeUiState.Error("Formül hesaplanırken hata oluştu: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Optional post-generation filtering: eliminates columns from the N-1 result
     * based on user's surprise/draw criteria.
     */
    fun applySecondaryFilters(
        criteria: TotoFilterCriteria,
        matches: List<TotoMatch>
    ) {
        val current = _lastGeneratedResult.value ?: return

        viewModelScope.launch {
            _uiState.value = GuaranteeUiState.Generating("N-1 kolonlarına ek filtreler uygulanıyor...")

            val filtered = withContext(defaultDispatcher) {
                TotoFilterEngine.filterColumns(current.reducedColumns, criteria, matches)
            }

            val before = current.fullCombinationsCount
            val after = filtered.size
            val savings = if (before > 0) (((before - after).toDouble() / before) * 100).toInt() else 0

            val updatedResult = current.copy(
                reducedColumns = filtered,
                reducedCostTL = filtered.size * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = savings,
                chanceOf15Percentage = if (before > 0) (after.toDouble() / before) * 100.0 else 0.0
            )

            _lastGeneratedResult.value = updatedResult
            _uiState.value = GuaranteeUiState.Success(updatedResult)
        }
    }

    /**
     * Converts the current generation result into a playable Coupon.
     */
    fun createCouponFromResult(customTitle: String? = null): Coupon? {
        val result = _lastGeneratedResult.value ?: return null
        val title = customTitle ?: "14 Garanti (N-1) Özel Formül Kuponu (${result.reducedColumns.size} Kolon)"

        return Coupon(
            id = "n1_${System.currentTimeMillis()}",
            title = title,
            formulaType = FormulaType.GUARANTEE_14,
            columns = result.reducedColumns
        )
    }

    fun reset() {
        _uiState.value = GuaranteeUiState.Idle
        _lastGeneratedResult.value = null
    }
}

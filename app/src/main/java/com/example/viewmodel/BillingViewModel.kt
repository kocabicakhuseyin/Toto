package com.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.GooglePlayBillingClientWrapper
import com.example.billing.GooglePlayBillingManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PurchaseHistoryEntity
import com.example.data.repository.WalletRepository
import com.example.model.COLUMN_FEE_TL
import com.example.model.GooglePlayPackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

import androidx.lifecycle.ViewModel

sealed class ChargeResult {
    data class Success(val columnCount: Int, val chargedAmountTL: Double, val remainingBalanceTL: Double) : ChargeResult()
    data class InsufficientBalance(val columnCount: Int, val requiredAmountTL: Double, val currentBalanceTL: Double) : ChargeResult()
    data class Error(val message: String) : ChargeResult()
}

/**
 * ViewModel to handle Google Play Billing Library integration, package purchases,
 * and processing 0.20 TL charges per coupon column backed by Room database persistence.
 */
class BillingViewModel(
    application: Application,
    private val walletRepository: WalletRepository = WalletRepository(
        AppDatabase.getInstance(application).walletDao()
    ),
    private val billingWrapper: GooglePlayBillingClientWrapper? = GooglePlayBillingClientWrapper(application)
) : AndroidViewModel(application) {

    // Secondary constructor for testing with mock/fake WalletRepository
    constructor(walletRepository: WalletRepository) : this(
        application = Application(),
        walletRepository = walletRepository,
        billingWrapper = null
    )

    val balanceTL: StateFlow<Double> = walletRepository.balanceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 50.0
    )

    val purchaseHistory: StateFlow<List<PurchaseHistoryEntity>> = walletRepository.purchaseHistoryFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availablePackages: List<GooglePlayPackage> = GooglePlayBillingManager.packages

    val isBillingConnected: StateFlow<Boolean> = billingWrapper?.isConnected ?: MutableStateFlow(true)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastChargeResult = MutableStateFlow<ChargeResult?>(null)
    val lastChargeResult: StateFlow<ChargeResult?> = _lastChargeResult.asStateFlow()

    init {
        // Observe Google Play purchase events
        billingWrapper?.let { wrapper ->
            viewModelScope.launch {
                wrapper.purchasedEvents.collect { event ->
                    when (event) {
                        is GooglePlayBillingClientWrapper.PurchaseEvent.Success -> {
                            val pkg = availablePackages.find { it.id == event.productId }
                                ?: GooglePlayPackage(event.productId, "Google Play Paketi", 25.0, (25.0 / COLUMN_FEE_TL).toInt())
                            creditTopUp(pkg, event.orderId, event.purchase.purchaseToken)
                        }
                        is GooglePlayBillingClientWrapper.PurchaseEvent.SimulatedSuccess -> {
                            creditTopUp(event.pkg, event.orderId, null)
                        }
                        is GooglePlayBillingClientWrapper.PurchaseEvent.Failure -> {
                            _isProcessing.value = false
                            _statusMessage.value = "Ödeme tamamlanamadı: ${event.debugMessage}"
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates total cost in TL for a given column count (0.20 TL / column).
     */
    fun calculateChargeTL(columnCount: Int): Double {
        return columnCount * COLUMN_FEE_TL
    }

    /**
     * Checks if current Room balance is sufficient to play the specified column count.
     */
    fun canAffordColumns(columnCount: Int): Boolean {
        val required = calculateChargeTL(columnCount)
        return balanceTL.value >= required
    }

    /**
     * Processes 0.20 TL charge per column in Room database.
     * Records transaction in PurchaseHistoryEntity with type COLUMN_CHARGE.
     */
    fun processColumnCharge(
        columnCount: Int,
        couponId: String? = null,
        platform: String? = null,
        onResult: (ChargeResult) -> Unit = {}
    ) {
        if (columnCount <= 0) {
            val err = ChargeResult.Error("Kolon sayısı 0'dan büyük olmalıdır.")
            _lastChargeResult.value = err
            onResult(err)
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val required = calculateChargeTL(columnCount)
            val current = walletRepository.getCurrentBalance()

            if (current < required) {
                val insufficient = ChargeResult.InsufficientBalance(
                    columnCount = columnCount,
                    requiredAmountTL = required,
                    currentBalanceTL = current
                )
                _lastChargeResult.value = insufficient
                _statusMessage.value = "Yetersiz bakiye! $columnCount kolon için %.2f ₺ gerekli. Mevcut bakiye: %.2f ₺"
                    .format(Locale.US, required, current)
                _isProcessing.value = false
                onResult(insufficient)
                return@launch
            }

            val result = walletRepository.chargeForCouponColumns(columnCount, couponId, platform)
            result.onSuccess { chargedFee ->
                val newBalance = walletRepository.getCurrentBalance()
                val success = ChargeResult.Success(
                    columnCount = columnCount,
                    chargedAmountTL = chargedFee,
                    remainingBalanceTL = newBalance
                )
                _lastChargeResult.value = success
                _statusMessage.value = "$columnCount kolon için %.2f ₺ tahsil edildi. Kalan bakiye: %.2f ₺"
                    .format(Locale.US, chargedFee, newBalance)
                _isProcessing.value = false
                onResult(success)
            }.onFailure { ex ->
                val error = ChargeResult.Error(ex.message ?: "Kolon ücreti tahsil edilemedi.")
                _lastChargeResult.value = error
                _statusMessage.value = error.message
                _isProcessing.value = false
                onResult(error)
            }
        }
    }

    /**
     * Launches Google Play Billing purchase flow for the selected package.
     */
    fun launchPurchase(activity: Activity?, pkg: GooglePlayPackage) {
        _isProcessing.value = true
        _statusMessage.value = "Google Play ödeme işlemi başlatılıyor..."
        val handled = billingWrapper?.launchPurchaseFlow(activity, pkg) ?: false
        if (!handled) {
            creditTopUp(pkg, "GPA.${(1000..9999).random()}-${(1000..9999).random()}", null)
        }
    }

    /**
     * Credits the purchased package amount into the Room database.
     */
    fun creditTopUp(pkg: GooglePlayPackage, orderId: String, purchaseToken: String?) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = walletRepository.topUp(pkg, orderId, purchaseToken)
            result.onSuccess {
                _statusMessage.value = "${pkg.title} başarıyla yüklendi! (+%.2f ₺, %d Kolon eklendi)"
                    .format(Locale.US, pkg.amountTL, pkg.columnCount)
            }.onFailure { ex ->
                _statusMessage.value = "Bakiye kaydedilirken hata oluştu: ${ex.message}"
            }
            _isProcessing.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        billingWrapper?.endConnection()
    }
}

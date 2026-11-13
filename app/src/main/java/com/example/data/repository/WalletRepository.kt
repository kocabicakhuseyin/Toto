package com.example.data.repository

import com.example.data.local.dao.WalletDao
import com.example.data.local.entity.PurchaseHistoryEntity
import com.example.data.local.entity.UserBalanceEntity
import com.example.model.COLUMN_FEE_TL
import com.example.model.GooglePlayPackage
import com.example.model.INSUFFICIENT_BALANCE_ERROR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalletRepository(
    private val walletDao: WalletDao,
    private val userId: String = UserBalanceEntity.DEFAULT_USER_ID
) {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))

    /**
     * Reactive user balance flow in TL. Emits whenever balance changes in Room.
     */
    val balanceFlow: Flow<Double> = walletDao.observeBalance(userId).map { entity ->
        entity?.balanceTL ?: UserBalanceEntity.DEFAULT_INITIAL_BALANCE_TL
    }

    /**
     * Reactive purchase and charge history flow.
     */
    val purchaseHistoryFlow: Flow<List<PurchaseHistoryEntity>> =
        walletDao.observeAllPurchases(userId)

    suspend fun getCurrentBalance(): Double {
        val entity = walletDao.getBalance(userId)
        return entity?.balanceTL ?: run {
            // Seed initial balance if record doesn't exist
            val initial = UserBalanceEntity(userId = userId, balanceTL = UserBalanceEntity.DEFAULT_INITIAL_BALANCE_TL)
            walletDao.insertOrUpdateBalance(initial)
            initial.balanceTL
        }
    }

    suspend fun canAfford(columnCount: Int): Boolean {
        val required = columnCount * COLUMN_FEE_TL
        val current = getCurrentBalance()
        return current > 0.0 && current >= required
    }

    /**
     * Records a successful Google Play top-up in Room and increments balance.
     */
    suspend fun topUp(
        pkg: GooglePlayPackage,
        orderId: String,
        purchaseToken: String? = null
    ): Result<UserBalanceEntity> {
        return try {
            val updated = walletDao.recordTopUp(
                userId = userId,
                amountTL = pkg.amountTL,
                orderId = orderId,
                productId = pkg.id,
                title = "Google Play: ${pkg.title} (${pkg.columnCount} Kolon)",
                columnCount = pkg.columnCount,
                purchaseToken = purchaseToken,
                formattedDate = dateFormat.format(Date())
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Charges 0.20 TL per column from Room database balance.
     * Returns Result.success(totalFeeCharged) or Result.failure(Exception("Yetersiz Bakiye")).
     */
    suspend fun chargeForCouponColumns(
        columnCount: Int,
        couponId: String? = null,
        platform: String? = null
    ): Result<Double> {
        val totalFeeTL = columnCount * COLUMN_FEE_TL
        return try {
            val success = walletDao.chargeColumnFee(
                userId = userId,
                columnCount = columnCount,
                unitFeeTL = COLUMN_FEE_TL,
                couponId = couponId,
                platform = platform,
                formattedDate = dateFormat.format(Date())
            )
            if (success) {
                Result.success(totalFeeTL)
            } else {
                val current = getCurrentBalance()
                Result.failure(
                    IllegalStateException(
                        "$INSUFFICIENT_BALANCE_ERROR! $columnCount kolon için %.2f ₺ gerekli. Mevcut bakiye: %.2f ₺".format(
                            Locale.US,
                            totalFeeTL,
                            current
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

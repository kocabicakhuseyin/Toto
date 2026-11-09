package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.PurchaseHistoryEntity
import com.example.data.local.entity.UserBalanceEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface WalletDao {

    @Query("SELECT * FROM user_balance WHERE userId = :userId LIMIT 1")
    fun observeBalance(userId: String = UserBalanceEntity.DEFAULT_USER_ID): Flow<UserBalanceEntity?>

    @Query("SELECT * FROM user_balance WHERE userId = :userId LIMIT 1")
    suspend fun getBalance(userId: String = UserBalanceEntity.DEFAULT_USER_ID): UserBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBalance(balance: UserBalanceEntity)

    @Query("SELECT * FROM purchase_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeAllPurchases(userId: String = UserBalanceEntity.DEFAULT_USER_ID): Flow<List<PurchaseHistoryEntity>>

    @Query("SELECT * FROM purchase_history WHERE userId = :userId AND transactionType = :type ORDER BY timestamp DESC")
    fun observePurchasesByType(
        userId: String = UserBalanceEntity.DEFAULT_USER_ID,
        type: String
    ): Flow<List<PurchaseHistoryEntity>>

    @Query("SELECT * FROM purchase_history WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllPurchases(userId: String = UserBalanceEntity.DEFAULT_USER_ID): List<PurchaseHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseHistoryEntity)

    /**
     * Atomically top up the user's balance and record the Google Play purchase history.
     */
    @Transaction
    suspend fun recordTopUp(
        userId: String = UserBalanceEntity.DEFAULT_USER_ID,
        amountTL: Double,
        orderId: String?,
        productId: String?,
        title: String,
        columnCount: Int,
        purchaseToken: String?,
        formattedDate: String
    ): UserBalanceEntity {
        val existing = getBalance(userId) ?: UserBalanceEntity(
            userId = userId,
            balanceTL = UserBalanceEntity.DEFAULT_INITIAL_BALANCE_TL,
            lastUpdated = System.currentTimeMillis()
        )
        val updated = existing.copy(
            balanceTL = existing.balanceTL + amountTL,
            lastUpdated = System.currentTimeMillis()
        )
        insertOrUpdateBalance(updated)

        val purchaseRecord = PurchaseHistoryEntity(
            transactionId = UUID.randomUUID().toString(),
            userId = userId,
            orderId = orderId,
            productId = productId,
            title = title,
            transactionType = PurchaseHistoryEntity.TYPE_TOP_UP,
            amountTL = amountTL,
            columnCount = columnCount,
            unitFeeTL = PurchaseHistoryEntity.DEFAULT_UNIT_FEE_TL,
            timestamp = System.currentTimeMillis(),
            formattedDate = formattedDate,
            purchaseToken = purchaseToken,
            purchaseState = 1,
            isAcknowledged = true
        )
        insertPurchase(purchaseRecord)
        return updated
    }

    /**
     * Atomically process a 0.20 TL charge per column.
     * Returns true if balance was sufficient and charged, or false if insufficient balance.
     */
    @Transaction
    suspend fun chargeColumnFee(
        userId: String = UserBalanceEntity.DEFAULT_USER_ID,
        columnCount: Int,
        unitFeeTL: Double = PurchaseHistoryEntity.DEFAULT_UNIT_FEE_TL,
        couponId: String? = null,
        platform: String? = null,
        formattedDate: String
    ): Boolean {
        val totalFeeTL = columnCount * unitFeeTL
        val existing = getBalance(userId) ?: UserBalanceEntity(
            userId = userId,
            balanceTL = UserBalanceEntity.DEFAULT_INITIAL_BALANCE_TL,
            lastUpdated = System.currentTimeMillis()
        )

        if (existing.balanceTL < totalFeeTL) {
            return false
        }

        val updated = existing.copy(
            balanceTL = existing.balanceTL - totalFeeTL,
            lastUpdated = System.currentTimeMillis()
        )
        insertOrUpdateBalance(updated)

        val chargeRecord = PurchaseHistoryEntity(
            transactionId = UUID.randomUUID().toString(),
            userId = userId,
            orderId = null,
            productId = "column_charge_x$columnCount",
            title = "$columnCount Kolon Oynatma Bedeli (0.20 ₺/Kolon)",
            transactionType = PurchaseHistoryEntity.TYPE_COLUMN_CHARGE,
            amountTL = totalFeeTL,
            columnCount = columnCount,
            unitFeeTL = unitFeeTL,
            timestamp = System.currentTimeMillis(),
            formattedDate = formattedDate,
            couponId = couponId,
            bettingPlatform = platform,
            purchaseState = 1,
            isAcknowledged = true
        )
        insertPurchase(chargeRecord)
        return true
    }
}

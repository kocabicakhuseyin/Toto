package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entity to track Google Play in-app purchases and 0.20 TL / column charges in Room.
 */
@Entity(tableName = "purchase_history")
data class PurchaseHistoryEntity(
    @PrimaryKey
    val transactionId: String = UUID.randomUUID().toString(),
    val userId: String = UserBalanceEntity.DEFAULT_USER_ID,
    val orderId: String? = null,
    val productId: String? = null,
    val title: String,
    val transactionType: String, // "TOP_UP" or "COLUMN_CHARGE"
    val amountTL: Double,
    val columnCount: Int = 0,
    val unitFeeTL: Double = 0.20,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String,
    val couponId: String? = null,
    val bettingPlatform: String? = null,
    val purchaseToken: String? = null,
    val purchaseState: Int = 1, // 1 = PURCHASED
    val isAcknowledged: Boolean = true
) {
    companion object {
        const val TYPE_TOP_UP = "TOP_UP"
        const val TYPE_COLUMN_CHARGE = "COLUMN_CHARGE"
        const val DEFAULT_UNIT_FEE_TL = 0.20
    }
}

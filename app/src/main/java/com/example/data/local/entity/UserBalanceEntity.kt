package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store and track persistent user wallet balance in Room Database.
 */
@Entity(tableName = "user_balance")
data class UserBalanceEntity(
    @PrimaryKey
    val userId: String = DEFAULT_USER_ID,
    val balanceTL: Double = DEFAULT_INITIAL_BALANCE_TL,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_USER_ID = "default_user"
        const val DEFAULT_INITIAL_BALANCE_TL = 50.0 // 250 kolon başlangıç bakiyesi
    }
}

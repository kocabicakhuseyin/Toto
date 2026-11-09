package com.example

import com.example.data.local.dao.WalletDao
import com.example.data.local.entity.PurchaseHistoryEntity
import com.example.data.local.entity.UserBalanceEntity
import com.example.data.repository.WalletRepository
import com.example.model.GooglePlayPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeWalletDao : WalletDao {
    private val balances = mutableMapOf<String, UserBalanceEntity>()
    private val purchases = mutableListOf<PurchaseHistoryEntity>()
    private val balanceFlow = MutableStateFlow<Map<String, UserBalanceEntity>>(emptyMap())
    private val purchasesFlow = MutableStateFlow<List<PurchaseHistoryEntity>>(emptyList())

    override fun observeBalance(userId: String): Flow<UserBalanceEntity?> {
        return balanceFlow.map { it[userId] }
    }

    override suspend fun getBalance(userId: String): UserBalanceEntity? {
        return balances[userId]
    }

    override suspend fun insertOrUpdateBalance(balance: UserBalanceEntity) {
        balances[balance.userId] = balance
        balanceFlow.value = HashMap(balances)
    }

    override fun observeAllPurchases(userId: String): Flow<List<PurchaseHistoryEntity>> {
        return purchasesFlow
    }

    override fun observePurchasesByType(userId: String, type: String): Flow<List<PurchaseHistoryEntity>> {
        return purchasesFlow.map { list -> list.filter { it.userId == userId && it.transactionType == type } }
    }

    override suspend fun getAllPurchases(userId: String): List<PurchaseHistoryEntity> {
        return purchases.filter { it.userId == userId }.reversed()
    }

    override suspend fun insertPurchase(purchase: PurchaseHistoryEntity) {
        purchases.add(0, purchase)
        purchasesFlow.value = ArrayList(purchases)
    }
}

class RoomWalletAndBillingTest {

    private lateinit var walletDao: FakeWalletDao
    private lateinit var repository: WalletRepository

    @Before
    fun setUp() {
        walletDao = FakeWalletDao()
        repository = WalletRepository(walletDao, "test_user")
    }

    @Test
    fun testInitialBalanceAndSeeding() = runBlocking {
        val initialBalance = repository.getCurrentBalance()
        assertEquals(50.0, initialBalance, 0.001)

        val balanceFlowVal = repository.balanceFlow.first()
        assertEquals(50.0, balanceFlowVal, 0.001)
    }

    @Test
    fun testGooglePlayTopUpInRoom() = runBlocking {
        val pkg = GooglePlayPackage(
            id = "toto_balance_100",
            title = "Pro Kuponcu Paketi",
            amountTL = 100.0,
            columnCount = 525,
            bonusBadge = "+25 BONUS",
            isPopular = false
        )

        val result = repository.topUp(pkg, "GPA.1234-5678-9012-34567")
        assertTrue(result.isSuccess)

        // Initial 50.0 + 100.0 = 150.0 TL
        val newBalance = repository.getCurrentBalance()
        assertEquals(150.0, newBalance, 0.001)

        val purchases = repository.purchaseHistoryFlow.first()
        assertEquals(1, purchases.size)
        val purchase = purchases[0]
        assertEquals("GPA.1234-5678-9012-34567", purchase.orderId)
        assertEquals(PurchaseHistoryEntity.TYPE_TOP_UP, purchase.transactionType)
        assertEquals(100.0, purchase.amountTL, 0.001)
        assertEquals(525, purchase.columnCount)
        assertTrue(purchase.title.contains("Pro Kuponcu Paketi"))
    }

    @Test
    fun testColumnFeeDeductionInRoom() = runBlocking {
        // Charge for 64 columns * 0.20 TL = 12.80 TL
        val columnCount = 64
        val result = repository.chargeForCouponColumns(columnCount, couponId = "coupon_test_01", platform = "NESINE")
        assertTrue(result.isSuccess)
        assertEquals(12.80, result.getOrNull()!!, 0.001)

        // 50.0 - 12.80 = 37.20 TL
        val remaining = repository.getCurrentBalance()
        assertEquals(37.20, remaining, 0.001)

        val purchases = repository.purchaseHistoryFlow.first()
        assertEquals(1, purchases.size)
        val charge = purchases[0]
        assertEquals(PurchaseHistoryEntity.TYPE_COLUMN_CHARGE, charge.transactionType)
        assertEquals(12.80, charge.amountTL, 0.001)
        assertEquals(64, charge.columnCount)
        assertEquals(0.20, charge.unitFeeTL, 0.001)
        assertEquals("coupon_test_01", charge.couponId)
        assertEquals("NESINE", charge.bettingPlatform)
    }

    @Test
    fun testInsufficientBalanceHandlingInRoom() = runBlocking {
        // Attempt to charge for 300 columns * 0.20 TL = 60.00 TL (Current balance is 50.00 TL)
        val columnCount = 300
        val canAfford = repository.canAfford(columnCount)
        assertFalse(canAfford)

        val result = repository.chargeForCouponColumns(columnCount)
        assertTrue(result.isFailure)

        // Balance should remain unchanged at 50.0 TL
        val balance = repository.getCurrentBalance()
        assertEquals(50.0, balance, 0.001)

        // No charge transaction should have been inserted
        val purchases = repository.purchaseHistoryFlow.first()
        assertTrue(purchases.isEmpty())
    }

    @Test
    fun testMultipleTransactionsAndHistoryOrder() = runBlocking {
        // 1. Top up 25 TL -> Balance = 75 TL
        val pkg25 = GooglePlayPackage("p25", "Başlangıç", 25.0, 125)
        repository.topUp(pkg25, "GPA.001")

        // 2. Play 100 columns (20 TL) -> Balance = 55 TL
        val chargeResult1 = repository.chargeForCouponColumns(100, "c1", "IDDAA")
        assertTrue(chargeResult1.isSuccess)
        assertEquals(20.0, chargeResult1.getOrNull()!!, 0.001)

        // 3. Play 50 columns (10 TL) -> Balance = 45 TL
        val chargeResult2 = repository.chargeForCouponColumns(50, "c2", "BILYONER")
        assertTrue(chargeResult2.isSuccess)
        assertEquals(10.0, chargeResult2.getOrNull()!!, 0.001)

        val finalBalance = repository.getCurrentBalance()
        assertEquals(45.0, finalBalance, 0.001)

        val history = repository.purchaseHistoryFlow.first()
        assertEquals(3, history.size)
        // Most recent first:
        assertEquals(50, history[0].columnCount)
        assertEquals(100, history[1].columnCount)
        assertEquals(PurchaseHistoryEntity.TYPE_TOP_UP, history[2].transactionType)
    }
}

package com.example

import com.example.data.repository.WalletRepository
import com.example.model.GooglePlayPackage
import com.example.viewmodel.BillingViewModel
import com.example.viewmodel.ChargeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var dao: FakeWalletDao
    private lateinit var repository: WalletRepository
    private lateinit var viewModel: BillingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeWalletDao()
        repository = WalletRepository(dao, "billing_vm_test_user")
        viewModel = BillingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testColumnFeeCalculation() {
        // 0.20 TL per column
        assertEquals(0.20, viewModel.calculateChargeTL(1), 0.001)
        assertEquals(12.80, viewModel.calculateChargeTL(64), 0.001)
        assertEquals(20.00, viewModel.calculateChargeTL(100), 0.001)
        assertEquals(25.60, viewModel.calculateChargeTL(128), 0.001)
    }

    @Test
    fun testCanAffordColumnsLogic() = runTest {
        testScheduler.advanceUntilIdle()
        // Default initial balance is 50.0 TL
        // 50.0 / 0.20 = 250 columns can be afforded
        assertTrue(viewModel.canAffordColumns(100))
        assertTrue(viewModel.canAffordColumns(250))
        assertFalse(viewModel.canAffordColumns(251))
    }

    @Test
    fun testProcessColumnChargeSuccess() = runTest {
        testScheduler.advanceUntilIdle()

        var receivedResult: ChargeResult? = null
        viewModel.processColumnCharge(64, couponId = "coup_99", platform = "MISLI") { result ->
            receivedResult = result
        }
        testScheduler.advanceUntilIdle()

        assertNotNull(receivedResult)
        assertTrue(receivedResult is ChargeResult.Success)
        val success = receivedResult as ChargeResult.Success
        assertEquals(64, success.columnCount)
        assertEquals(12.80, success.chargedAmountTL, 0.001)
        assertEquals(37.20, success.remainingBalanceTL, 0.001)

        // Verify balance in Room repository
        assertEquals(37.20, repository.getCurrentBalance(), 0.001)
    }

    @Test
    fun testProcessColumnChargeInsufficientBalance() = runTest {
        testScheduler.advanceUntilIdle()

        var receivedResult: ChargeResult? = null
        // 500 columns * 0.20 TL = 100.0 TL > 50.0 TL balance
        viewModel.processColumnCharge(500) { result ->
            receivedResult = result
        }
        testScheduler.advanceUntilIdle()

        assertNotNull(receivedResult)
        assertTrue(receivedResult is ChargeResult.InsufficientBalance)
        val insufficient = receivedResult as ChargeResult.InsufficientBalance
        assertEquals(500, insufficient.columnCount)
        assertEquals(100.0, insufficient.requiredAmountTL, 0.001)
        assertEquals(50.0, insufficient.currentBalanceTL, 0.001)

        // Verify balance unchanged
        assertEquals(50.0, repository.getCurrentBalance(), 0.001)
    }

    @Test
    fun testCreditTopUp() = runTest {
        testScheduler.advanceUntilIdle()

        val pkg = GooglePlayPackage("p50", "Standart", 50.0, 250)
        viewModel.creditTopUp(pkg, "GPA.9999", null)
        testScheduler.advanceUntilIdle()

        // 50.0 initial + 50.0 top up = 100.0 TL
        val currentBalance = repository.getCurrentBalance()
        assertEquals(100.0, currentBalance, 0.001)
    }
}

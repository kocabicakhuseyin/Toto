package com.example

import com.example.engine.FormulaEngine
import com.example.engine.NesineIntegrationManager
import com.example.engine.PlatformAutomationManager
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.MatchProbability
import com.example.model.PlatformCredentials
import com.example.model.PredictionOutcome
import com.example.model.TotoMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private fun createTestMatches(): List<TotoMatch> {
        return (1..15).map { idx ->
            TotoMatch(
                id = idx,
                matchNumber = idx,
                homeTeam = "Ev $idx",
                awayTeam = "Dep $idx",
                matchDate = "15.11.2026",
                probability = MatchProbability(listOf(50), listOf(25), listOf(25)),
                selections = if (idx <= 4) setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW) else setOf(PredictionOutcome.HOME)
            )
        }
    }

    @Test
    fun testGenerateGuaranteeFormulas() {
        val matches = createTestMatches()

        // 15 Garanti (N-0)
        val col15 = FormulaEngine.generateColumns(matches, FormulaType.GUARANTEE_15)
        assertTrue(col15.isNotEmpty())
        assertEquals(15, col15[0].predictions.size)

        // 14 Garanti (N-1)
        val col14 = FormulaEngine.generateColumns(matches, FormulaType.GUARANTEE_14)
        assertTrue(col14.isNotEmpty())
        assertTrue(col14.size <= col15.size)

        // 13 Garanti (N-2)
        val col13 = FormulaEngine.generateColumns(matches, FormulaType.GUARANTEE_13)
        assertTrue(col13.isNotEmpty())
        assertTrue(col13.size <= col14.size)

        // 12 Garanti (N-3)
        val col12 = FormulaEngine.generateColumns(matches, FormulaType.GUARANTEE_12)
        assertTrue(col12.isNotEmpty())
        assertTrue(col12.size <= col13.size)
    }

    @Test
    fun testGenerate9ColumnSystem() {
        val matches = (1..15).map { idx ->
            TotoMatch(
                id = idx,
                matchNumber = idx,
                homeTeam = "Ev $idx",
                awayTeam = "Dep $idx",
                matchDate = "15.11.2026",
                probability = MatchProbability(listOf(50), listOf(25), listOf(25)),
                selections = setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
            )
        }

        val columns = FormulaEngine.generateColumns(matches, FormulaType.GUARANTEED_9_COL)
        assertEquals(9, columns.size)
        columns.forEach { col ->
            assertEquals(15, col.predictions.size)
        }
    }

    @Test
    fun testExtra1X2TxtParsingAndExport() {
        val sampleTxt = """
            1X211X212111X21
            111111111111111
            222222222222222
        """.trimIndent()

        val parsed = FormulaEngine.parseExtra1X2Txt(sampleTxt)
        assertEquals(3, parsed.size)
        assertEquals("1X211X212111X21", parsed[0].asString)
        assertEquals("111111111111111", parsed[1].asString)
        assertEquals("222222222222222", parsed[2].asString)

        val exported = FormulaEngine.exportToExtra1X2Txt(parsed)
        assertTrue(exported.contains("1X211X212111X21"))
    }

    @Test
    fun testAllPlatformsFormatting() {
        val parsed = FormulaEngine.parseExtra1X2Txt("1X211X212111X21\n111111111111111")
        val coupon = Coupon(
            id = "test_1",
            title = "Test Kuponu",
            formulaType = FormulaType.GUARANTEE_14,
            columns = parsed
        )

        BettingPlatform.values().forEach { platform ->
            val creds = PlatformCredentials(platform, "testuser_${platform.id}", "secret123")
            val formatted = PlatformAutomationManager.formatCouponForPlatform(coupon, platform, creds)
            assertTrue(formatted.contains(platform.displayName.uppercase()))
            assertTrue(formatted.contains(creds.username))
            assertTrue(formatted.contains("Kolon 01: 1X211X212111X21"))
        }
    }

    @Test
    fun testWeeklyBulletinDataModelAndRepository() = kotlinx.coroutines.runBlocking {
        val repo = com.example.repository.SporTotoBulletinRepositoryImpl()
        val result = repo.getWeeklyBulletin(forceRefresh = true)

        assertTrue(result.isSuccess)
        val bulletin = result.getOrNull()
        assertNotNull(bulletin)
        assertEquals(38, bulletin!!.weekNumber)
        assertEquals(15, bulletin.matches.size)

        // Verify match teams, dates, and live odds
        val match1 = bulletin.matches[0]
        assertEquals(1, match1.matchNumber)
        assertEquals("FRANSA", match1.homeTeam)
        assertEquals("TÜRKİYE", match1.awayTeam)
        assertEquals("15.11.2026 22:45", match1.matchDate)

        // Live odds checks
        assertTrue(match1.liveOdds.homeWinOdd > 1.0)
        assertTrue(match1.liveOdds.drawOdd > 1.0)
        assertTrue(match1.liveOdds.awayWinOdd > 1.0)
        assertNotNull(match1.liveOdds.doubleChance1X)

        // Verify domain mapping
        val domainMatches = repo.getDomainMatches().getOrNull()
        assertNotNull(domainMatches)
        assertEquals(15, domainMatches!!.size)
        assertEquals("FRANSA", domainMatches[0].homeTeam)
        assertEquals("TÜRKİYE", domainMatches[0].awayTeam)
    }

    @Test
    fun testWeeklyBulletinMoshiJsonSerialization() {
        val original = com.example.network.NetworkModule.createFallbackBulletin(39)
        val moshi = com.example.network.NetworkModule.moshi
        val adapter = moshi.adapter(com.example.network.model.WeeklyBulletinResponse::class.java)

        val json = adapter.toJson(original)
        assertNotNull(json)
        assertTrue(json.contains("\"week_number\":39"))
        assertTrue(json.contains("live_odds"))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(39, deserialized!!.weekNumber)
        assertEquals(15, deserialized.matches.size)
        assertEquals("FRANSA", deserialized.matches[0].homeTeam)
        assertEquals(1.32, deserialized.matches[0].liveOdds.homeWinOdd, 0.001)
    }

    @Test
    fun testColumnFeeAndWalletDeduction() {
        val parsed = FormulaEngine.parseExtra1X2Txt((1..64).joinToString("\n") { "111111111111111" })
        val coupon = Coupon(
            id = "c_fee_test",
            title = "Fee Test Kuponu",
            formulaType = FormulaType.GUARANTEE_14,
            columns = parsed
        )

        // Exactly 64 columns * 0.20 TL = 12.80 TL
        assertEquals(64, coupon.totalColumns)
        assertEquals(com.example.model.COLUMN_FEE_TL, 0.20, 0.001)
        assertEquals(12.80, coupon.columnFeeTotalTL, 0.001)

        val vm = com.example.viewmodel.TotoViewModel()
        // Default wallet has 50.0 TL
        assertTrue(vm.canAffordCoupon(coupon))

        val success = vm.deductColumnFee(coupon)
        assertTrue(success)
        // 50.0 - 12.80 = 37.20 TL
        assertEquals(37.20, vm.userProfile.value.walletBalanceTL, 0.001)
    }

    @Test
    fun testGooglePlayBillingPackages() {
        val packages = com.example.billing.GooglePlayBillingManager.packages
        assertEquals(4, packages.size)

        val pkg50 = packages.first { it.amountTL == 50.0 }
        assertEquals(250, pkg50.columnCount) // 50 / 0.20 = 250 columns

        val tx = com.example.billing.GooglePlayBillingManager.createPurchaseTransaction(pkg50)
        assertNotNull(tx.orderId)
        assertTrue(tx.orderId!!.startsWith("GPA."))
        assertEquals(50.0, tx.amountTL, 0.001)

        val vm = com.example.viewmodel.TotoViewModel()
        val initialBalance = vm.userProfile.value.walletBalanceTL
        vm.topUpWallet(pkg50)
        assertEquals(initialBalance + 50.0, vm.userProfile.value.walletBalanceTL, 0.001)
    }
}

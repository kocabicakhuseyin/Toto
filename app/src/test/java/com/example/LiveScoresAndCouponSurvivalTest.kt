package com.example

import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.repository.SporTotoBulletinRepositoryImpl
import com.example.viewmodel.TotoViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScoresAndCouponSurvivalTest {

    @Test
    fun testMatchLiveScoreWinningOutcomeCalculation() {
        val homeWin = MatchLiveScore(1, "Team A", "Team B", homeScore = 2, awayScore = 1, status = MatchStatus.FINISHED)
        assertEquals(PredictionOutcome.HOME, homeWin.winningOutcome)
        assertEquals("2 - 1", homeWin.scoreDisplay)
        assertFalse(homeWin.isLive)

        val drawMatch = MatchLiveScore(2, "Team C", "Team D", homeScore = 1, awayScore = 1, status = MatchStatus.LIVE, minuteText = "65'")
        assertEquals(PredictionOutcome.DRAW, drawMatch.winningOutcome)
        assertEquals("1 - 1", drawMatch.scoreDisplay)
        assertTrue(drawMatch.isLive)

        val awayWin = MatchLiveScore(3, "Team E", "Team F", homeScore = 0, awayScore = 2, status = MatchStatus.FINISHED)
        assertEquals(PredictionOutcome.AWAY, awayWin.winningOutcome)

        val upcomingMatch = MatchLiveScore(4, "Team G", "Team H", homeScore = null, awayScore = null, status = MatchStatus.UPCOMING)
        assertNull(upcomingMatch.winningOutcome)
        assertEquals("- : -", upcomingMatch.scoreDisplay)
        assertFalse(upcomingMatch.isLive)
    }

    @Test
    fun testLiveScoresPollingRepositoryProgression() = runBlocking {
        val repo = SporTotoBulletinRepositoryImpl()
        val initialScores = repo.liveScores.value
        assertEquals(15, initialScores.size)

        val pollResult = repo.pollLiveScores()
        assertTrue(pollResult.isSuccess)
        val updatedScores = pollResult.getOrThrow()
        assertEquals(15, updatedScores.size)

        // Verify status counts
        val liveCount = updatedScores.count { it.isLive }
        val finishedCount = updatedScores.count { it.status == MatchStatus.FINISHED }
        val upcomingCount = updatedScores.count { it.status == MatchStatus.UPCOMING }

        assertTrue(liveCount > 0)
        assertTrue(finishedCount > 0)
        assertTrue(upcomingCount > 0)
        assertEquals(15, liveCount + finishedCount + upcomingCount)
    }

    @Test
    fun testCouponSurvivalReportCalculation() {
        val viewModel = TotoViewModel()

        // Create 2 test columns of 15 predictions each:
        // Col 1: All HOME ("1")
        // Col 2: All AWAY ("2")
        val col1 = TotoColumn(1, List(15) { PredictionOutcome.HOME })
        val col2 = TotoColumn(2, List(15) { PredictionOutcome.AWAY })
        val coupon = Coupon(
            id = "test_coupon",
            title = "Test Kuponu",
            formulaType = FormulaType.GUARANTEE_14,
            columns = listOf(col1, col2)
        )

        // Mock 15 match live scores:
        // Match 1: Finished Home Win (Col 1 matches, Col 2 fails)
        // Match 2: Live Home Win (Col 1 matches, Col 2 fails)
        // Match 3 to 15: Upcoming (not evaluated)
        val scores = (1..15).map { idx ->
            when (idx) {
                1 -> MatchLiveScore(idx, "Team A", "Team B", homeScore = 1, awayScore = 0, status = MatchStatus.FINISHED)
                2 -> MatchLiveScore(idx, "Team C", "Team D", homeScore = 2, awayScore = 1, status = MatchStatus.LIVE, minuteText = "50'")
                else -> MatchLiveScore(idx, "Team E", "Team F", homeScore = null, awayScore = null, status = MatchStatus.UPCOMING)
            }
        }

        val report = viewModel.computeCouponSurvival(coupon, scores)

        // Col 1: 0 errors -> alive for 15, 14, 13, 12
        // Col 2: 2 errors -> not alive for 15 (requires 0), not alive for 14 (requires <= 1), alive for 13 (requires <= 2), alive for 12 (requires <= 3)
        assertEquals(2, report.totalColumns)
        assertEquals(1, report.aliveFor15) // Only Col 1
        assertEquals(1, report.aliveFor14) // Only Col 1
        assertEquals(2, report.aliveFor13) // Both Col 1 & Col 2
        assertEquals(2, report.aliveFor12) // Both Col 1 & Col 2
        assertEquals(1, report.finishedMatchesCount)
        assertEquals(1, report.liveMatchesCount)
        assertEquals(13, report.remainingMatchesCount)
    }
}

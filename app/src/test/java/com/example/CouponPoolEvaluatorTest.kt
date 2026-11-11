package com.example

import com.example.model.CouponPoolEvaluator
import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.PoolCouponDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CouponPoolEvaluatorTest {
    private val matchLabels = listOf("TÜRKİYE|BELÇİKA", "FRANSA|ALMANYA") +
        (3..15).map { "Ev$it|Dep$it" }

    @Test
    fun onlyPartiallyMaskedNameIsDisplayed() {
        assertEquals("H***n", CouponPoolEvaluator.maskedName("Hüseyin"))
        assertEquals("M***", CouponPoolEvaluator.maskedName("Me"))
    }

    @Test
    fun liveScoresAreProvisionalAndFinishedScoresAreFinal() {
        val coupon = PoolCouponDto(
            id = "my-coupon", weekKey = "2026-2027-38", title = "Kupon",
            maskedName = "H***n", matchLabels = matchLabels,
            columns = listOf("11" + "X".repeat(13), "22" + "X".repeat(13))
        )
        val scores = listOf(
            MatchLiveScore(
                matchNumber = 12, homeTeam = "Türkiye", awayTeam = "Belgium",
                homeScore = 2, awayScore = 0, status = MatchStatus.FINISHED
            ),
            MatchLiveScore(
                matchNumber = 13, homeTeam = "France", awayTeam = "Germany",
                homeScore = 1, awayScore = 0, status = MatchStatus.LIVE
            )
        )

        val ranks = CouponPoolEvaluator.rank(listOf(coupon), scores)
        assertEquals(2, ranks.size)
        assertEquals(1, ranks.first().finishedCorrect)
        assertEquals(1, ranks.first().liveCorrect)
        assertEquals(0, ranks.first().finishedWrong)
        assertEquals(15, ranks.first().maximumPossible)
        assertEquals(1, ranks.last().finishedWrong)
        assertEquals(14, ranks.last().maximumPossible)
    }

    @Test
    fun unknownScoresNeverBecomeCorrectPredictions() {
        val coupon = PoolCouponDto("unknown", "2026-2027-38", "Kupon", "A***z", matchLabels,
            listOf("1".repeat(15)))
        val ranks = CouponPoolEvaluator.rank(listOf(coupon), emptyList())
        assertEquals(0, ranks.single().availableResults)
        assertEquals(15, ranks.single().maximumPossible)
        assertTrue(CouponPoolEvaluator.rank(listOf(coupon.copy(columns = listOf("short"))), emptyList()).isEmpty())
    }

    @Test
    fun teamsFromAnotherWeekAreNotMistakenForCurrentFixture() {
        val coupon = PoolCouponDto("dated", "2026-2027-38", "Kupon", "A***z",
            listOf("TÜRKİYE|BELÇİKA|12.11.2026 20:00") + matchLabels.drop(1),
            listOf("1".repeat(15)))
        val oldResult = MatchLiveScore(
            123, "Turkey", "Belgium", 3, 0, MatchStatus.FINISHED,
            matchDate = "2026-10-01 17:00:00"
        )
        assertEquals(0, CouponPoolEvaluator.rank(listOf(coupon), listOf(oldResult)).single().availableResults)
    }
}

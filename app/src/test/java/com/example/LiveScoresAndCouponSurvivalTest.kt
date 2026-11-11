package com.example

import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.PredictionOutcome
import com.example.network.ApiFootballLiveResponse
import com.example.network.NetworkModule
import com.example.repository.SporTotoBulletinRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScoresAndCouponSurvivalTest {
    @Test
    fun aRealScoreIsDisplayedWithoutEstimatedMinutes() {
        val match = MatchLiveScore(
            matchNumber = 556123,
            homeTeam = "Ev Sahibi",
            awayTeam = "Deplasman",
            homeScore = 2,
            awayScore = 1,
            status = MatchStatus.LIVE,
            minuteText = "67'"
        )
        assertEquals("2 - 1", match.scoreDisplay)
        assertEquals("67'", match.minuteText)
        assertEquals(PredictionOutcome.HOME, match.winningOutcome)
        assertTrue(match.isLive)
    }

    @Test
    fun missingScoreRemainsUnknownRatherThanZero() {
        val match = MatchLiveScore(556124, "A", "B")
        assertEquals("- : -", match.scoreDisplay)
        assertNull(match.winningOutcome)
        assertFalse(match.isLive)
    }

    @Test
    fun missingApiKeyNeverCreatesMockMatches() = runBlocking {
        val repository = SporTotoBulletinRepositoryImpl(liveScoreApiKey = "")
        assertTrue(repository.liveScores.value.isEmpty())
        val result = repository.pollLiveScores()
        assertTrue(result.isFailure)
        assertTrue(repository.liveScores.value.isEmpty())
    }

    @Test
    fun providerJsonKeepsActualScoreAndElapsedMinute() {
        val json = """
            {
              "errors": [],
              "response": [{
                "fixture": {"id": 44821, "status": {"short": "2H", "elapsed": 73}},
                "teams": {"home": {"name": "Home"}, "away": {"name": "Away"}},
                "goals": {"home": 1, "away": 2},
                "league": {"name": "Test League"}
              }]
            }
        """.trimIndent()
        val parsed = NetworkModule.moshi.adapter(ApiFootballLiveResponse::class.java)
            .fromJson(json)!!
        assertEquals(1, parsed.response.size)
        assertEquals(73, parsed.response.single().fixture.status.elapsed)
        assertEquals(1, parsed.response.single().goals.home)
        assertEquals(2, parsed.response.single().goals.away)
    }
}

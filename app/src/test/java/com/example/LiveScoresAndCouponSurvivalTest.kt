package com.example

import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.PredictionOutcome
import com.example.network.ApiFootballLiveResponse
import com.example.network.NetworkModule
import com.example.network.ScoreCardsLiveService
import com.example.network.ScoreCardsWidgetResponse
import com.example.repository.SporTotoBulletinRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

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
    fun missingApiKeyUsesThePublicFeedForRealScores() = runBlocking {
        val scoreCardsJson = """
          {
            "success":true,
            "league":{"name":"UEFA Nations League"},
            "cache_age_seconds":43,
            "live":[{"fixture_id":123456,"home_team":"Türkiye","away_team":"Belgium",
                     "home_score":2,"away_score":1,"state":"live","status":"2H","elapsed":67}],
            "recently_finished":[]
          }
        """.trimIndent()
        val response = NetworkModule.moshi.adapter(ScoreCardsWidgetResponse::class.java)
            .fromJson(scoreCardsJson)!!
        val publicService = object : ScoreCardsLiveService {
            override suspend fun getLeague(leagueId: Int): Response<ScoreCardsWidgetResponse> =
                Response.success(response)
        }
        val repository = SporTotoBulletinRepositoryImpl(
            liveScoreApiKey = "", scoreCardsService = publicService, trackedLeagueIds = listOf(5)
        )
        val result = repository.pollLiveScores()
        assertTrue(result.isSuccess)
        assertEquals("ScoreCards", repository.liveScoreProvider.value)
        assertEquals("2 - 1", result.getOrThrow().single().scoreDisplay)
        assertEquals("67'", result.getOrThrow().single().minuteText)
    }

    @Test
    fun serviceFailureNeverCreatesMockMatches() = runBlocking {
        val unavailable = object : ScoreCardsLiveService {
            override suspend fun getLeague(leagueId: Int): Response<ScoreCardsWidgetResponse> =
                Response.success(ScoreCardsWidgetResponse(success = false))
        }
        val repository = SporTotoBulletinRepositoryImpl(
            liveScoreApiKey = "", scoreCardsService = unavailable, trackedLeagueIds = listOf(5)
        )
        assertTrue(repository.liveScores.value.isEmpty())
        val result = repository.pollLiveScores()
        assertTrue(result.isFailure)
        assertTrue(repository.liveScores.value.isEmpty())
    }

    @Test
    fun providerOnlyShowsStartedGamesAndDoesNotInventMinutes() {
        val json = """
          {"success":true,"league":{"name":"UEFA Nations League"},
          "live":[{"fixture_id":1,"home_team":"A","away_team":"B","home_score":0,
                   "away_score":0,"state":"live","status":"1H","elapsed":null}],
          "recently_finished":[{"fixture_id":2,"home_team":"C","away_team":"D",
                   "home_score":1,"away_score":0,"state":"finished","status":"FT"}]}
        """.trimIndent()
        val response = NetworkModule.moshi.adapter(ScoreCardsWidgetResponse::class.java).fromJson(json)!!
        assertEquals("—", response.live.single().asLiveScore("League")!!.minuteText)
        assertEquals(MatchStatus.FINISHED, response.recentlyFinished.single().asLiveScore("League")!!.status)
        val upcoming = response.live.single().copy(state = "upcoming", status = "NS")
        assertNull(upcoming.asLiveScore("League"))
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

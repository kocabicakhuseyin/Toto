package com.example

import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.PredictionOutcome
import com.example.network.ApiFootballLiveResponse
import com.example.network.ApiFootballLiveService
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
    fun missingApiKeyNeverCallsOtherLiveScoreProviders() = runBlocking {
        var fallbackCalls = 0
        val publicService = object : ScoreCardsLiveService {
            override suspend fun getLeague(leagueId: Int): Response<ScoreCardsWidgetResponse> {
                fallbackCalls++
                return Response.success(ScoreCardsWidgetResponse(success = false))
            }
        }
        val repository = SporTotoBulletinRepositoryImpl(
            liveScoreApiKey = "",
            scoreCardsService = publicService,
            trackedLeagueIds = listOf(5)
        )
        val result = repository.pollLiveScores()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("anahtar"))
        assertEquals(0, fallbackCalls)
        assertTrue(repository.liveScores.value.isEmpty())
    }

    @Test
    fun apiFootballIsPrimaryAndRespectsFreePlanPollingCache() = runBlocking {
        val fixtureJson = """
            {
              "errors": [],
              "response": [{
                "fixture": {"id": 44821, "status": {"short": "2H", "elapsed": 73}},
                "teams": {"home": {"name": "Türkiye"}, "away": {"name": "Belçika"}},
                "goals": {"home": 2, "away": 1},
                "league": {"name": "UEFA"}
              }]
            }
        """.trimIndent()
        val parsed = NetworkModule.moshi.adapter(ApiFootballLiveResponse::class.java)
            .fromJson(fixtureJson)!!
        var apiCalls = 0
        val liveApi = object : ApiFootballLiveService {
            override suspend fun getLiveFixtures(
                apiKey: String, live: String
            ): Response<ApiFootballLiveResponse> {
                apiCalls++
                assertEquals("free-test-key", apiKey)
                assertEquals("all", live)
                return Response.success(parsed)
            }
        }
        val repository = SporTotoBulletinRepositoryImpl(
            liveScoreService = liveApi, liveScoreApiKey = "free-test-key"
        )

        val first = repository.pollLiveScores().getOrThrow()
        assertEquals("API-FOOTBALL (Free)", repository.liveScoreProvider.value)
        assertEquals("2 - 1", first.single().scoreDisplay)
        assertEquals("73'", first.single().minuteText)
        assertEquals(1, apiCalls)
        assertEquals(79, repository.freeScoreRequestsRemaining())

        // Consecutive manual refreshes reuse the verified data and cost 0 requests.
        val second = repository.pollLiveScores().getOrThrow()
        assertEquals(first, second)
        assertEquals(1, apiCalls)
        assertEquals(79, repository.freeScoreRequestsRemaining())
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

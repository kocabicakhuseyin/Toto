package com.example.network

import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * ScoreCards' documented, keyless widget endpoint. Using the published JSON
 * endpoint is more stable than scraping its HTML and is explicitly permitted
 * for mobile apps with visible attribution on the results screen.
 * https://www.scorecards.in/developers
 */
interface ScoreCardsLiveService {
    @GET("api/widget/{leagueId}")
    suspend fun getLeague(@Path("leagueId") leagueId: Int): Response<ScoreCardsWidgetResponse>
}

@JsonClass(generateAdapter = true)
data class ScoreCardsWidgetResponse(
    val success: Boolean = false,
    val league: ScoreCardsLeague? = null,
    val live: List<ScoreCardsMatch> = emptyList(),
    @Json(name = "recently_finished") val recentlyFinished: List<ScoreCardsMatch> = emptyList(),
    @Json(name = "cache_age_seconds") val cacheAgeSeconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class ScoreCardsLeague(val name: String = "")

@JsonClass(generateAdapter = true)
data class ScoreCardsMatch(
    @Json(name = "fixture_id") val fixtureId: Int,
    @Json(name = "home_team") val homeTeam: String,
    @Json(name = "away_team") val awayTeam: String,
    @Json(name = "home_score") val homeScore: Int? = null,
    @Json(name = "away_score") val awayScore: Int? = null,
    val state: String = "",
    val status: String = "",
    val elapsed: Int? = null,
    @Json(name = "scheduled_at") val scheduledAt: String? = null
) {
    fun asLiveScore(leagueName: String): MatchLiveScore? {
        val matchStatus = when (status.uppercase()) {
            "HT" -> MatchStatus.HALF_TIME
            "FT", "AET", "PEN" -> MatchStatus.FINISHED
            "1H", "2H", "ET", "BT", "P", "LIVE", "INT", "SUSP" -> MatchStatus.LIVE
            else -> when (state.lowercase()) {
                "live" -> MatchStatus.LIVE
                "finished" -> MatchStatus.FINISHED
                else -> return null // Never display unstarted matches as live.
            }
        }
        return MatchLiveScore(
            matchNumber = fixtureId, // Provider fixture ID, not a Toto coupon number.
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            status = matchStatus,
            minuteText = when {
                matchStatus == MatchStatus.HALF_TIME -> "İY"
                matchStatus == MatchStatus.FINISHED -> "MS"
                elapsed != null -> "${elapsed}'"
                else -> "—" // Never estimate the minute from device time.
            },
            matchDate = scheduledAt.orEmpty(),
            league = leagueName
        )
    }
}

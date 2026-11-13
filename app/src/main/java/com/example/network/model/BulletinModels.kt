package com.example.network.model

import com.example.model.MatchProbability
import com.example.model.TotoMatch
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data model representing the complete weekly Spor Toto bulletin.
 */
@JsonClass(generateAdapter = true)
data class WeeklyBulletinResponse(
    @Json(name = "week_number")
    val weekNumber: Int,
    @Json(name = "season")
    val season: String,
    @Json(name = "bulletin_title")
    val bulletinTitle: String,
    @Json(name = "start_date")
    val startDate: String,
    @Json(name = "end_date")
    val endDate: String,
    @Json(name = "jackpot_estimate_tl")
    val jackpotEstimateTL: Double,
    @Json(name = "is_active")
    val isActive: Boolean = true,
    @Json(name = "matches")
    val matches: List<BulletinMatchDto>,
    /** Provenance and the time this program was fetched from the official host. */
    val sourceName: String = "Spor Toto Teşkilat Başkanlığı",
    val fetchedAt: String = ""
)

/**
 * Data model for an individual match in the Spor Toto bulletin with teams, dates, and live odds.
 */
@JsonClass(generateAdapter = true)
data class BulletinMatchDto(
    @Json(name = "match_number")
    val matchNumber: Int, // 1 through 15
    @Json(name = "home_team")
    val homeTeam: String,
    @Json(name = "away_team")
    val awayTeam: String,
    @Json(name = "match_date")
    val matchDate: String,
    @Json(name = "league")
    val league: String? = null,
    @Json(name = "venue")
    val venue: String? = null,
    @Json(name = "status")
    val status: String? = "UPCOMING", // UPCOMING, LIVE, FINISHED
    @Json(name = "live_odds")
    val liveOdds: LiveBettingOddsDto? = null,
    @Json(name = "probabilities")
    val probabilities: MatchProbabilitiesDto? = null
)

/**
 * Real-time live betting odds (1 - X - 2 and double chances).
 */
@JsonClass(generateAdapter = true)
data class LiveBettingOddsDto(
    @Json(name = "home_win_odd")
    val homeWinOdd: Double, // 1
    @Json(name = "draw_odd")
    val drawOdd: Double,    // X
    @Json(name = "away_win_odd")
    val awayWinOdd: Double, // 2
    @Json(name = "double_chance_1x")
    val doubleChance1X: Double? = null,
    @Json(name = "double_chance_12")
    val doubleChance12: Double? = null,
    @Json(name = "double_chance_x2")
    val doubleChanceX2: Double? = null,
    @Json(name = "last_updated")
    val lastUpdated: String? = null
)

/**
 * Outcome distribution probabilities in percentage (1 - X - 2).
 */
@JsonClass(generateAdapter = true)
data class MatchProbabilitiesDto(
    @Json(name = "home_probability")
    val homeProbability: Int,
    @Json(name = "draw_probability")
    val drawProbability: Int,
    @Json(name = "away_probability")
    val awayProbability: Int
)

/**
 * Converter extension to transform DTO into internal domain TotoMatch.
 */
fun BulletinMatchDto.toDomainTotoMatch(): TotoMatch {
    return TotoMatch(
        id = matchNumber,
        matchNumber = matchNumber,
        homeTeam = homeTeam.uppercase().trim(),
        awayTeam = awayTeam.uppercase().trim(),
        matchDate = matchDate,
        // The official program supplies fixtures, not 1/0/2 playing percentages.
        // Empty lists mean "unavailable", never a fabricated 50/25/25 estimate.
        probability = MatchProbability(
            home = probabilities?.let { List(4) { _ -> it.homeProbability } } ?: emptyList(),
            draw = probabilities?.let { List(4) { _ -> it.drawProbability } } ?: emptyList(),
            away = probabilities?.let { List(4) { _ -> it.awayProbability } } ?: emptyList()
        )
    )
}

/** The official webapi.sportoto.gov.tr response, not a third-party odds feed. */
@JsonClass(generateAdapter = true)
data class OfficialGameRoundsResponse(
    @Json(name = "isSucceed") val isSucceed: Boolean? = null,
    @Json(name = "object") val rounds: List<OfficialGameRound>? = null
)

@JsonClass(generateAdapter = true)
data class OfficialGameRound(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "roundCloseDate") val roundCloseDate: String? = null
)

@JsonClass(generateAdapter = true)
data class OfficialGameMatchResponse(
    @Json(name = "isSucceed") val isSucceed: Boolean? = null,
    @Json(name = "object") val items: List<OfficialRoundMatch>? = null
)

@JsonClass(generateAdapter = true)
data class OfficialRoundMatch(
    @Json(name = "gameRoundName") val gameRoundName: String? = null,
    @Json(name = "match") val match: OfficialFixture? = null
)

@JsonClass(generateAdapter = true)
data class OfficialFixture(
    @Json(name = "date") val date: String? = null,
    @Json(name = "homeTeam") val homeTeam: OfficialNamedTeam? = null,
    @Json(name = "awayTeam") val awayTeam: OfficialNamedTeam? = null,
    @Json(name = "stage") val stage: OfficialNamedTeam? = null,
    @Json(name = "round") val round: OfficialNamedTeam? = null,
    @Json(name = "fullTimeWin") val fullTimeWin: Int? = null,
    @Json(name = "noterWin") val noterWin: Int? = null
)

@JsonClass(generateAdapter = true)
data class OfficialNamedTeam(
    @Json(name = "name") val name: String? = null
)

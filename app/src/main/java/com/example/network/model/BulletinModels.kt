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
    val matches: List<BulletinMatchDto>
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
    val liveOdds: LiveBettingOddsDto,
    @Json(name = "probabilities")
    val probabilities: MatchProbabilitiesDto
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
        probability = MatchProbability(
            home = listOf(probabilities.homeProbability, probabilities.homeProbability, probabilities.homeProbability, probabilities.homeProbability),
            draw = listOf(probabilities.drawProbability, probabilities.drawProbability, probabilities.drawProbability, probabilities.drawProbability),
            away = listOf(probabilities.awayProbability, probabilities.awayProbability, probabilities.awayProbability, probabilities.awayProbability)
        )
    )
}

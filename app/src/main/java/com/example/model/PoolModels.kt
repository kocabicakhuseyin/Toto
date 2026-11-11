package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Only masked nicknames, never emails or platform credentials, are public. */
@JsonClass(generateAdapter = true)
data class PoolCouponDto(
    val id: String,
    @Json(name = "week_key") val weekKey: String,
    @Json(name = "coupon_title") val title: String,
    @Json(name = "masked_name") val maskedName: String,
    @Json(name = "match_labels") val matchLabels: List<String>,
    val columns: List<String>,
    @Json(name = "shared_at") val sharedAt: String = "",
    @Json(name = "owner_id") val ownerId: String = ""
)

@JsonClass(generateAdapter = true)
data class PoolCouponUpload(
    @Json(name = "week_key") val weekKey: String,
    @Json(name = "source_coupon_id") val sourceCouponId: String,
    @Json(name = "coupon_title") val title: String,
    @Json(name = "masked_name") val maskedName: String,
    @Json(name = "match_labels") val matchLabels: List<String>,
    val columns: List<String>,
    @Json(name = "confirmed_by_member") val confirmedByMember: Boolean = true
)

data class PoolColumnRank(
    val couponId: String,
    val displayName: String,
    val columnNumber: Int,
    val predictions: String,
    val finishedCorrect: Int,
    val finishedWrong: Int,
    val liveCorrect: Int,
    val liveWrong: Int,
    val maximumPossible: Int,
    val availableResults: Int
)

object CouponPoolEvaluator {
    private val aliases = mapOf(
        "turkey" to "turkiye",
        "fransa" to "france", "almanya" to "germany", "italya" to "italy",
        "ispanya" to "spain", "belcika" to "belgium", "ingiltere" to "england",
        "hollanda" to "netherlands", "hirvatistan" to "croatia",
        "portekiz" to "portugal", "danimarka" to "denmark",
        "isvicre" to "switzerland", "isvec" to "sweden", "polonya" to "poland",
        "sirbistan" to "serbia", "macaristan" to "hungary",
        "estonya" to "estonia", "gurcistan" to "georgia",
        "izlanda" to "iceland", "norvec" to "norway", "galler" to "wales",
        "azerbaycan" to "azerbaijan", "lihtenstayn" to "liechtenstein",
        "luksemburg" to "luxembourg", "faroe adalari" to "faroe islands",
        "slovakya" to "slovakia", "avusturya" to "austria"
    )

    fun maskedName(nickname: String): String {
        val visible = nickname.trim().replace(Regex("[\\p{C}\\r\\n]"), "")
            .take(24)
        if (visible.isEmpty()) return "Ü***e"
        if (visible.length <= 2) return visible.first() + "***"
        return visible.first() + "***" + visible.last()
    }

    fun matchLabels(matches: List<TotoMatch>): List<String> = matches
        .sortedBy { it.matchNumber }
        .take(15)
        .map { "${it.homeTeam}|${it.awayTeam}|${it.matchDate}" }

    /** Each character is 1/X/2 if verified, or '-' when not yet known. */
    fun resultCodes(matchLabels: List<String>, liveScores: List<MatchLiveScore>): Pair<String, String> {
        if (matchLabels.size != 15) return "-".repeat(15) to "-".repeat(15)
        val matched = resolveScores(matchLabels, liveScores)
        val final = matched.joinToString("") { score ->
            if (score?.status == MatchStatus.FINISHED) score.winningOutcome?.code ?: "-" else "-"
        }
        val live = matched.joinToString("") { score ->
            if (score?.isLive == true) score.winningOutcome?.code ?: "-" else "-"
        }
        return final to live
    }

    fun rank(coupons: List<PoolCouponDto>, liveScores: List<MatchLiveScore>): List<PoolColumnRank> {
        return coupons.asSequence().flatMap { coupon ->
            if (coupon.matchLabels.size != 15) return@flatMap emptySequence()
            val results = resolveScores(coupon.matchLabels, liveScores)
            coupon.columns.asSequence().mapIndexedNotNull { idx, code ->
                if (!code.matches(Regex("[1X2]{15}"))) return@mapIndexedNotNull null
                var finishedCorrect = 0
                var finishedWrong = 0
                var liveCorrect = 0
                var liveWrong = 0
                results.forEachIndexed { index, score ->
                    val outcome = score?.winningOutcome?.code ?: return@forEachIndexed
                    val correct = code[index].toString() == outcome
                    when (score.status) {
                        MatchStatus.FINISHED -> if (correct) finishedCorrect++ else finishedWrong++
                        MatchStatus.LIVE, MatchStatus.HALF_TIME -> if (correct) liveCorrect++ else liveWrong++
                        MatchStatus.UPCOMING -> Unit
                    }
                }
                PoolColumnRank(
                    couponId = coupon.id,
                    displayName = coupon.maskedName,
                    columnNumber = idx + 1,
                    predictions = code,
                    finishedCorrect = finishedCorrect,
                    finishedWrong = finishedWrong,
                    liveCorrect = liveCorrect,
                    liveWrong = liveWrong,
                    maximumPossible = 15 - finishedWrong,
                    availableResults = finishedCorrect + finishedWrong + liveCorrect + liveWrong
                )
            }
        }.sortedWith(
            compareByDescending<PoolColumnRank> { it.finishedCorrect }
                .thenByDescending { it.maximumPossible }
                .thenByDescending { it.liveCorrect }
                .thenBy { it.finishedWrong }
        ).take(40).toList()
    }

    private fun resolveScores(labels: List<String>, liveScores: List<MatchLiveScore>): List<MatchLiveScore?> {
        val scoresByTeams = liveScores
            .filter { it.homeScore != null && it.awayScore != null && it.status != MatchStatus.UPCOMING }
            .groupBy { pairKey(it.homeTeam, it.awayTeam) }
        return labels.map { label ->
            val parts = label.split('|', limit = 3)
            if (parts.size < 2) return@map null
            val expectedDate = parts.getOrNull(2)?.let { matchTimeMs(it, isBulletin = true) }
            scoresByTeams[pairKey(parts[0], parts[1])].orEmpty().filter { score ->
                val actualDate = matchTimeMs(score.matchDate, isBulletin = false)
                expectedDate == null || actualDate == null || abs(expectedDate - actualDate) < 12 * 60 * 60 * 1000L
            }.singleOrNull() // Ambiguous fixtures are never guessed.
        }
    }

    private fun pairKey(home: String, away: String): String =
        "${canonical(home)}|${canonical(away)}"

    private fun canonical(team: String): String {
        val deaccented = Normalizer.normalize(team.lowercase(Locale.ROOT).replace('ı', 'i'), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .trim().replace(Regex("\\s+"), " ")
        return aliases[deaccented] ?: deaccented
    }

    private fun matchTimeMs(value: String, isBulletin: Boolean): Long? {
        if (value.isBlank()) return null
        val patterns = if (isBulletin) listOf("dd.MM.yyyy HH:mm", "yyyy-MM-dd HH:mm:ss")
            else listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd HH:mm:ss")
        return patterns.firstNotNullOfOrNull { pattern ->
            try {
                SimpleDateFormat(pattern, Locale.ROOT).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone(if (isBulletin) "Europe/Istanbul" else "UTC")
                }.parse(value)?.time
            } catch (_: Exception) { null }
        }
    }
}

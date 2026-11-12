package com.example.network

import com.example.network.model.BulletinMatchDto
import com.example.network.model.MatchProbabilitiesDto
import java.text.SimpleDateFormat
import java.util.Locale

/** One visible row from https://www.nesine.com/sportoto (no login required). */
data class NesinePlayedRow(
    val number: Int,
    val date: String,
    val fixture: String,
    val playedOne: Int,
    val playedDraw: Int,
    val playedTwo: Int
)

/**
 * Strict parser/validator for the *played percentages*, not decimal betting
 * odds or predicted chances. Never silently substitute invented numbers.
 */
object NesineSportotoParser {
    private val dateRegex = Regex("^\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}$")

    fun toBulletinMatches(rows: List<NesinePlayedRow>): List<BulletinMatchDto> {
        require(rows.size == 15) { "Nesine bülteni tam 15 maç içermiyor (${rows.size})." }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).apply {
            isLenient = false
        }
        val fixtures = mutableSetOf<String>()

        return rows.mapIndexed { index, row ->
            require(row.number == index + 1) {
                "Nesine maç sırası değişti: ${index + 1}. satır (${row.number})."
            }
            require(dateRegex.matches(row.date) && runCatching {
                dateFormat.parse(row.date) != null
            }.getOrDefault(false)) { "Nesine maç tarihi geçersiz: ${row.date}" }
            // A hyphen within a club name is not the home/away separator.
            val separator = if (Regex("\\s+[-–—]\\s+").containsMatchIn(row.fixture)) {
                Regex("\\s+[-–—]\\s+")
            } else {
                Regex("\\s*[–—]\\s*|\\s*-\\s*")
            }
            val pieces = row.fixture.split(separator, limit = 2)
                .map { it.trim() }
            require(pieces.size == 2 && pieces.all { it.isNotBlank() } && pieces[0] != pieces[1]) {
                "Nesine takım adları ayrıştırılamadı: ${row.fixture}"
            }
            require(listOf(row.playedOne, row.playedDraw, row.playedTwo).all { it in 0..100 } &&
                row.playedOne + row.playedDraw + row.playedTwo in 95..105
            ) { "${row.number}. maçın Nesine 1/0/2 oynanma yüzdeleri geçersiz." }
            require(fixtures.add("${row.date}|${pieces[0]}|${pieces[1]}")) {
                "Nesine bülteninde tekrar eden maç var: ${row.fixture}"
            }

            BulletinMatchDto(
                matchNumber = row.number,
                homeTeam = pieces[0],
                awayTeam = pieces[1],
                matchDate = row.date,
                status = "UPCOMING",
                probabilities = MatchProbabilitiesDto(
                    homeProbability = row.playedOne,
                    drawProbability = row.playedDraw,
                    awayProbability = row.playedTwo
                )
            )
        }
    }
}

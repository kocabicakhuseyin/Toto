package com.example

import com.example.model.PredictionOutcome
import com.example.network.NesinePlayedRow
import com.example.network.NesineSportotoParser
import com.example.network.model.toDomainTotoMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NesineSportotoParserTest {
    private fun validRows() = (1..15).map { number ->
        NesinePlayedRow(
            number = number,
            date = "15.11.2026 22:45",
            fixture = "Ev $number - Dep $number",
            playedOne = 70,
            playedDraw = 20,
            playedTwo = 10
        )
    }

    @Test
    fun `imports exactly fifteen matches with genuine 1 0 2 percentages`() {
        val rows = validRows().toMutableList()
        rows[0] = rows[0].copy(fixture = "Fransa - Türkiye", playedOne = 78, playedDraw = 12, playedTwo = 10)
        // A draw can have the highest played percentage and be the favorite.
        rows[1] = rows[1].copy(playedOne = 20, playedDraw = 60, playedTwo = 20)
        val bulletin = NesineSportotoParser.toBulletinMatches(rows)

        assertEquals(15, bulletin.size)
        assertEquals("Fransa", bulletin[0].homeTeam)
        assertEquals("Türkiye", bulletin[0].awayTeam)
        assertEquals(78, bulletin[0].probabilities?.homeProbability)
        assertEquals(12, bulletin[0].probabilities?.drawProbability)
        assertEquals(10, bulletin[0].probabilities?.awayProbability)
        assertEquals(listOf(78), bulletin[0].toDomainTotoMatch().probability.home)
        assertEquals(PredictionOutcome.DRAW, bulletin[1].toDomainTotoMatch().favoriteOutcome)
    }

    @Test
    fun `rejects missing or misplaced matches`() {
        assertTrue(runCatching { NesineSportotoParser.toBulletinMatches(validRows().dropLast(1)) }.isFailure)
        val reordered = validRows().toMutableList()
        reordered[4] = reordered[4].copy(number = 6)
        assertTrue(runCatching { NesineSportotoParser.toBulletinMatches(reordered) }.isFailure)
    }

    @Test
    fun `rejects percentages that cannot be trusted`() {
        val rows = validRows().toMutableList()
        rows[0] = rows[0].copy(playedOne = 0, playedDraw = 0, playedTwo = 0)
        assertTrue(runCatching { NesineSportotoParser.toBulletinMatches(rows) }.isFailure)
        rows[0] = rows[0].copy(playedOne = 130, playedDraw = 20, playedTwo = 10)
        assertTrue(runCatching { NesineSportotoParser.toBulletinMatches(rows) }.isFailure)
    }

    @Test
    fun `rejects duplicate fixtures and bad dates`() {
        val rows = validRows().toMutableList()
        rows[1] = rows[0].copy(number = 2)
        assertTrue(runCatching { NesineSportotoParser.toBulletinMatches(rows) }.isFailure)
        rows[1] = rows[1].copy(date = "33.11.2026 22:45", fixture = "Ev 2 - Dep 2")
        assertTrue(runCatching { NesineSportotoParser.toBulletinMatches(rows) }.isFailure)
    }
}

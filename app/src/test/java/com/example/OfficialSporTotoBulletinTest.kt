package com.example

import com.example.network.SporTotoApiService
import com.example.network.model.OfficialFixture
import com.example.network.model.OfficialGameMatchResponse
import com.example.network.model.OfficialGameRound
import com.example.network.model.OfficialGameRoundsResponse
import com.example.network.model.OfficialNamedTeam
import com.example.network.model.OfficialRoundMatch
import com.example.network.model.toDomainTotoMatch
import com.example.repository.SporTotoBulletinRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OfficialSporTotoBulletinTest {
    private fun officialDate(daysAhead: Int): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Istanbul")
        }.format(Date(System.currentTimeMillis() + daysAhead * 24L * 60 * 60 * 1000))

    private fun service(matchCount: Int): SporTotoApiService {
        val date = officialDate(1)
        return object : SporTotoApiService {
            override suspend fun getPublishedRounds(
                season: String,
                published: Boolean
            ): Response<OfficialGameRoundsResponse> =
                Response.success(
                    OfficialGameRoundsResponse(
                        isSucceed = true,
                        rounds = listOf(OfficialGameRound(1542, "15. Oyun Programı", date))
                    )
                )

            override suspend fun getRoundMatches(
                roundId: Int
            ): Response<OfficialGameMatchResponse> =
                Response.success(
                    OfficialGameMatchResponse(
                        isSucceed = true,
                        items = (1..matchCount).map { i ->
                            OfficialRoundMatch(
                                gameRoundName = "2026/2027 15. Oyun Programı",
                                match = OfficialFixture(
                                    date = date,
                                    homeTeam = OfficialNamedTeam("Ev ${i}"),
                                    awayTeam = OfficialNamedTeam("Dep ${i}")
                                )
                            )
                        }
                    )
                )
        }
    }

    @Test fun parsesOfficialJsonShape() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val json = """{"isSucceed":true,"object":[{"id":1542,"name":"15. Oyun Programı","roundCloseDate":"2026-11-13T22:40:00"}]}"""
        val adapter = moshi.adapter(OfficialGameRoundsResponse::class.java)
        val parsed = adapter.fromJson(json)
        assertTrue(parsed?.isSucceed == true)
        assertEquals(1542, parsed?.rounds?.single()?.id)
    }

    @Test fun readsExactlyFifteenOfficialFixturesWithoutMadeUpOdds() = runBlocking {
        val repository = SporTotoBulletinRepositoryImpl(apiService = service(15))
        val result = repository.getWeeklyBulletin(forceRefresh = true).getOrThrow()
        assertEquals(15, result.matches.size)
        assertEquals((1..15).toList(), result.matches.map { it.matchNumber })
        assertEquals(15, result.weekNumber)
        assertEquals("Spor Toto Teşkilat Başkanlığı", result.sourceName)
        assertEquals("Ev 1", result.matches.first().homeTeam)
        assertNull(result.matches.first().liveOdds)
        assertNull(result.matches.first().probabilities)
        assertTrue(result.matches.first().toDomainTotoMatch().probability.home.isEmpty())
    }

    @Test fun rejectsIncompleteOfficialFixtureInsteadOfReturningSample() = runBlocking {
        val repository = SporTotoBulletinRepositoryImpl(apiService = service(14))
        val result = repository.getWeeklyBulletin(forceRefresh = true)
        assertTrue(result.isFailure)
        assertNull(repository.cachedBulletin.value)
    }
}

package com.example

import com.example.engine.NesineAutoPlayJsInjector
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.PlatformCredentials
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.network.CloudflareBrowserRunner
import com.example.network.CloudflareRunStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareBrowserRunnerTest {

    private fun sampleCoupon(): Coupon {
        val picks = List(15) { idx ->
            when (idx % 3) {
                0 -> PredictionOutcome.HOME
                1 -> PredictionOutcome.DRAW
                else -> PredictionOutcome.AWAY
            }
        }
        return Coupon(
            id = "c_test",
            title = "Test Kupon",
            formulaType = FormulaType.GUARANTEE_14,
            columns = listOf(
                TotoColumn(index = 1, predictions = picks),
                TotoColumn(index = 2, predictions = picks.reversed())
            )
        )
    }

    @Test
    fun testPlaceholderSecretsAreDetectedAsNotConfigured() {
        assertFalse(
            CloudflareBrowserRunner.isRestApiConfigured(
                accountId = "YOUR_CLOUDFLARE_ACCOUNT_ID",
                apiToken = "YOUR_CLOUDFLARE_API_TOKEN"
            )
        )
        assertFalse(
            CloudflareBrowserRunner.isWorkerConfigured(
                workerUrl = "https://nesine-browser-runner.example.workers.dev"
            )
        )
        assertTrue(
            CloudflareBrowserRunner.isRestApiConfigured(
                accountId = "a1b2c3d4e5f6",
                apiToken = "cf_live_token_123"
            )
        )
        assertTrue(
            CloudflareBrowserRunner.isWorkerConfigured(
                workerUrl = "https://nesine-browser-runner.myaccount.workers.dev"
            )
        )
    }

    @Test
    fun testBuildAutoPlayRequestPayloadSerializes15MatchesPerColumn() {
        val coupon = sampleCoupon()
        val creds = PlatformCredentials(
            platform = BettingPlatform.NESINE,
            username = "testuser",
            password = "secret"
        )
        val payload = CloudflareBrowserRunner.buildAutoPlayRequestPayload(coupon, creds, dryRun = true)

        assertEquals("testuser", payload.username)
        assertTrue(payload.dryRun)
        assertEquals(2, payload.columns.size)
        assertEquals(15, payload.columns[0].predictions.size)
        assertEquals(listOf("1", "X", "2"), payload.columns[0].predictions.take(3))
    }

    @Test
    fun testUnconfiguredWorkerReturnsNotConfiguredStatus() = runBlocking {
        val result = CloudflareBrowserRunner.runNesineAutoPlayWorker(
            coupon = sampleCoupon(),
            credentials = PlatformCredentials(BettingPlatform.NESINE, "u", "p"),
            dryRun = true,
            workerUrl = "https://nesine-browser-runner.example.workers.dev"
        )
        assertEquals(CloudflareRunStatus.NOT_CONFIGURED, result.status)
        assertEquals(0, result.playedColumnsCount)
    }

    @Test
    fun testJsInjectorEscapesCredentialsAndBuilds15MatchPicks() {
        val loginJs = NesineAutoPlayJsInjector.buildLoginFillScript("user'test", "pass\"123")
        assertTrue(loginJs.contains("user'test"))
        assertTrue(loginJs.contains("pass\\\"123"))

        val colJs = NesineAutoPlayJsInjector.buildColumnFillScript(sampleCoupon().columns.first())
        assertTrue(colJs.contains("[\"1\",\"X\",\"2\""))
        assertTrue(colJs.contains("Kolon #1"))
    }

    @Test
    fun testWorkerHttpErrorBodiesNeverProduceNullOrUnknownError() {
        runBlocking {
            val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
            var statusToReturn = 401
            var bodyToReturn = """{"error":"Unauthorized"}"""
            server.createContext("/") { exchange ->
                val bytes = bodyToReturn.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(statusToReturn, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
            try {
                val url = "http://127.0.0.1:${server.address.port}/"
                val coupon = sampleCoupon()
                val creds = PlatformCredentials(BettingPlatform.NESINE, "u", "p")

                // Case 1: 401 Unauthorized
                val res401 = CloudflareBrowserRunner.runNesineAutoPlayWorker(
                    coupon = coupon, credentials = creds, workerUrl = url, apiToken = "bad"
                )
                assertEquals(CloudflareRunStatus.ERROR, res401.status)
                assertTrue(res401.errorMessage.orEmpty().contains("401"))
                assertFalse(res401.errorMessage.orEmpty().contains("Bilinmeyen hata"))

                // Case 2: 404 from cloudflare_nesine bridge URL
                statusToReturn = 404
                bodyToReturn = """{"detail":"İşlem bulunamadı."}"""
                val res404 = CloudflareBrowserRunner.runNesineAutoPlayWorker(
                    coupon = coupon, credentials = creds, workerUrl = url, apiToken = "tok"
                )
                assertEquals(CloudflareRunStatus.ERROR, res404.status)
                assertTrue(res404.errorMessage.orEmpty().contains("404"))
                assertTrue(res404.errorMessage.orEmpty().contains("Bulut Chrome"))

                // Case 3: NO_BULLETIN
                statusToReturn = 200
                bodyToReturn = """{"success":false,"status":"NO_BULLETIN","errorMessage":"Yeni bülten yüklenmedi"}"""
                val resNoBulletin = CloudflareBrowserRunner.runNesineAutoPlayWorker(
                    coupon = coupon, credentials = creds, workerUrl = url, apiToken = "tok"
                )
                assertEquals(CloudflareRunStatus.NO_BULLETIN, resNoBulletin.status)
                assertEquals("Yeni bülten yüklenmedi", resNoBulletin.errorMessage)

                // Case 4: INSUFFICIENT_BALANCE
                statusToReturn = 200
                bodyToReturn = """{"success":false,"status":"INSUFFICIENT_BALANCE","errorMessage":"Bakiye yetersiz"}"""
                val resNoBalance = CloudflareBrowserRunner.runNesineAutoPlayWorker(
                    coupon = coupon, credentials = creds, workerUrl = url, apiToken = "tok"
                )
                assertEquals(CloudflareRunStatus.INSUFFICIENT_BALANCE, resNoBalance.status)
                assertEquals("Bakiye yetersiz", resNoBalance.errorMessage)
            } finally {
                server.stop(0)
            }
        }
    }
}

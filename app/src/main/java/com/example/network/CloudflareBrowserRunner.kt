package com.example.network

import com.example.BuildConfig
import com.example.model.Coupon
import com.example.model.PlatformCredentials
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CloudflareColumnPayload(
    val index: Int,
    val predictions: List<String>
)

@JsonClass(generateAdapter = true)
data class CloudflareAutoPlayRequest(
    val username: String,
    val password: String,
    val couponTitle: String,
    val totalCostTL: Double,
    val dryRun: Boolean = true,
    val columns: List<CloudflareColumnPayload>
)

@JsonClass(generateAdapter = true)
data class CloudflareAutoPlayResponse(
    val success: Boolean = false,
    val status: String = "ERROR",
    val sessionId: String = "",
    val playedColumnsCount: Int = 0,
    val steps: List<String> = emptyList(),
    val screenshotBase64: String? = null,
    val errorMessage: String? = null
)

data class CloudflareRestPreviewResult(
    val success: Boolean,
    val screenshotBytes: ByteArray? = null,
    val statusMessage: String,
    val httpCode: Int = 0
)

enum class CloudflareRunStatus {
    COMPLETED,
    NO_BULLETIN,
    INSUFFICIENT_BALANCE,
    REQUIRES_2FA,
    WAF_BLOCKED,
    LOGIN_FAILED,
    NOT_CONFIGURED,
    ERROR
}

data class CloudflareWorkerRunResult(
    val status: CloudflareRunStatus,
    val sessionId: String = "",
    val playedColumnsCount: Int = 0,
    val steps: List<String> = emptyList(),
    val screenshotBase64: String? = null,
    val errorMessage: String? = null
)

object CloudflareBrowserRunner {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isRestApiConfigured(
        accountId: String = BuildConfig.CLOUDFLARE_ACCOUNT_ID,
        apiToken: String = BuildConfig.CLOUDFLARE_API_TOKEN
    ): Boolean {
        return accountId.isNotBlank() &&
            !accountId.startsWith("YOUR_") &&
            apiToken.isNotBlank() &&
            !apiToken.startsWith("YOUR_")
    }

    fun isWorkerConfigured(
        workerUrl: String = BuildConfig.CLOUDFLARE_BROWSER_WORKER_URL
    ): Boolean {
        return workerUrl.isNotBlank() &&
            workerUrl.startsWith("https://") &&
            !workerUrl.contains("example.workers.dev")
    }

    /**
     * Calls Cloudflare Browser Rendering REST API (/browser-rendering/screenshot)
     * to test live Cloudflare Browser Run rendering against nesine.com/spor-toto.
     */
    suspend fun captureNesineBulletinScreenshot(
        accountId: String = BuildConfig.CLOUDFLARE_ACCOUNT_ID,
        apiToken: String = BuildConfig.CLOUDFLARE_API_TOKEN
    ): CloudflareRestPreviewResult = withContext(Dispatchers.IO) {
        if (!isRestApiConfigured(accountId, apiToken)) {
            return@withContext CloudflareRestPreviewResult(
                success = false,
                statusMessage = "Cloudflare Secrets eksik: AI Studio Secrets panelinden CLOUDFLARE_ACCOUNT_ID ve CLOUDFLARE_API_TOKEN tanımlayın."
            )
        }

        val endpoint = "https://api.cloudflare.com/client/v4/accounts/$accountId/browser-rendering/screenshot"
        val bodyJson = """
            {
              "url": "https://www.nesine.com/sportoto",
              "viewport": { "width": 1280, "height": 800 },
              "gotoOptions": { "waitUntil": "networkidle2", "timeout": 25000 },
              "screenshotOptions": { "fullPage": false, "type": "png" }
            }
        """.trimIndent()

        try {
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiToken")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val rawBytes = response.body?.bytes()
                if (response.isSuccessful && rawBytes != null && rawBytes.isNotEmpty()) {
                    CloudflareRestPreviewResult(
                        success = true,
                        screenshotBytes = rawBytes,
                        statusMessage = "Cloudflare Browser Run (/screenshot) başarılı! Nesine Spor Toto görüntüsü alındı.",
                        httpCode = response.code
                    )
                } else {
                    val errorText = rawBytes?.decodeToString()?.take(220) ?: response.message
                    CloudflareRestPreviewResult(
                        success = false,
                        statusMessage = "Cloudflare Browser Run hatası (HTTP ${response.code}): $errorText",
                        httpCode = response.code
                    )
                }
            }
        } catch (e: Exception) {
            CloudflareRestPreviewResult(
                success = false,
                statusMessage = "Cloudflare bağlantı hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            )
        }
    }

    /**
     * Sends the coupon columns and credentials to the Cloudflare Browser Rendering Worker
     * (@cloudflare/puppeteer) to run interactive browser automation on nesine.com/spor-toto.
     */
    suspend fun runNesineAutoPlayWorker(
        coupon: Coupon,
        credentials: PlatformCredentials,
        dryRun: Boolean = true,
        workerUrl: String = BuildConfig.CLOUDFLARE_BROWSER_WORKER_URL,
        apiToken: String = BuildConfig.CLOUDFLARE_API_TOKEN
    ): CloudflareWorkerRunResult = withContext(Dispatchers.IO) {
        if (!isWorkerConfigured(workerUrl)) {
            return@withContext CloudflareWorkerRunResult(
                status = CloudflareRunStatus.NOT_CONFIGURED,
                steps = listOf(
                    "Cloudflare Browser Worker URL yapılandırılmamış.",
                    "Proje kökündeki cloudflare-worker/ klasörünü `npx wrangler deploy` ile Cloudflare'a dağıtın.",
                    "AI Studio Secrets paneline CLOUDFLARE_BROWSER_WORKER_URL ve CLOUDFLARE_API_TOKEN ekleyin."
                ),
                errorMessage = "Cloudflare Browser Worker henüz yapılandırılmadı. Uygulama İçi Tarayıcı Robotu (WebView) ile hemen oynayabilirsiniz."
            )
        }

        val payload = buildAutoPlayRequestPayload(coupon, credentials, dryRun)
        val adapter = NetworkModule.moshi.adapter(CloudflareAutoPlayRequest::class.java)
        val jsonBody = adapter.toJson(payload)

        try {
            val reqBuilder = Request.Builder()
                .url(workerUrl)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))

            if (apiToken.isNotBlank() && !apiToken.startsWith("YOUR_")) {
                reqBuilder.addHeader("Authorization", "Bearer $apiToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseStr = response.body?.string().orEmpty()
                val respAdapter = NetworkModule.moshi.adapter(CloudflareAutoPlayResponse::class.java)
                val parsed = runCatching { respAdapter.fromJson(responseStr) }.getOrNull()

                if (parsed != null) {
                    val mappedStatus = when (parsed.status.uppercase()) {
                        "COMPLETED" -> if (parsed.success) CloudflareRunStatus.COMPLETED else CloudflareRunStatus.ERROR
                        "NO_BULLETIN" -> CloudflareRunStatus.NO_BULLETIN
                        "INSUFFICIENT_BALANCE" -> CloudflareRunStatus.INSUFFICIENT_BALANCE
                        "REQUIRES_2FA" -> CloudflareRunStatus.REQUIRES_2FA
                        "WAF_BLOCKED" -> CloudflareRunStatus.WAF_BLOCKED
                        "LOGIN_FAILED" -> CloudflareRunStatus.LOGIN_FAILED
                        else -> CloudflareRunStatus.ERROR
                    }
                    CloudflareWorkerRunResult(
                        status = mappedStatus,
                        sessionId = parsed.sessionId,
                        playedColumnsCount = parsed.playedColumnsCount,
                        steps = parsed.steps,
                        screenshotBase64 = parsed.screenshotBase64,
                        errorMessage = when (mappedStatus) {
                            CloudflareRunStatus.NO_BULLETIN ->
                                parsed.errorMessage?.takeIf { it.isNotBlank() } ?: "Yeni bülten yüklenmedi"
                            CloudflareRunStatus.INSUFFICIENT_BALANCE ->
                                parsed.errorMessage?.takeIf { it.isNotBlank() } ?: "Bakiye yetersiz"
                            else -> parsed.errorMessage
                        }
                    )
                } else {
                    CloudflareWorkerRunResult(
                        status = CloudflareRunStatus.ERROR,
                        steps = listOf("Cloudflare Worker HTTP ${response.code} yanıtı verdi."),
                        errorMessage = responseStr.take(240).ifBlank { response.message }
                    )
                }
            }
        } catch (e: Exception) {
            CloudflareWorkerRunResult(
                status = CloudflareRunStatus.ERROR,
                steps = listOf("Worker çağrısı sırasında ağ hatası oluştu."),
                errorMessage = e.localizedMessage ?: "Bağlantı hatası"
            )
        }
    }

    fun buildAutoPlayRequestPayload(
        coupon: Coupon,
        credentials: PlatformCredentials,
        dryRun: Boolean
    ): CloudflareAutoPlayRequest {
        return CloudflareAutoPlayRequest(
            username = credentials.username,
            password = credentials.password,
            couponTitle = coupon.title,
            totalCostTL = coupon.totalCostTL,
            dryRun = dryRun,
            columns = coupon.columns.map { col ->
                CloudflareColumnPayload(
                    index = col.index,
                    predictions = col.predictions.map { it.code }
                )
            }
        )
    }
}

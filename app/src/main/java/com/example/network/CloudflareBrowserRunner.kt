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
    val errorMessage: String? = null,
    val error: String? = null,
    val detail: String? = null,
    val message: String? = null,
    val code: String? = null
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
        val trimmed = workerUrl.trim()
        return trimmed.isNotBlank() &&
            (trimmed.startsWith("https://") || trimmed.startsWith("http://127.0.0.1") || trimmed.startsWith("http://localhost")) &&
            !trimmed.contains("example.workers.dev")
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
                statusMessage = "Cloudflare bağlantı hatası: ${e.localizedMessage?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName}"
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
        val normalizedUrl = workerUrl.trim()
        if (!isWorkerConfigured(normalizedUrl)) {
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
                .url(normalizedUrl)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))

            if (apiToken.isNotBlank() && !apiToken.startsWith("YOUR_")) {
                reqBuilder.addHeader("Authorization", "Bearer ${apiToken.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseStr = response.body?.string().orEmpty()
                val respAdapter = NetworkModule.moshi.adapter(CloudflareAutoPlayResponse::class.java)
                val parsed = runCatching { respAdapter.fromJson(responseStr) }.getOrNull()

                if (parsed != null) {
                    val rawServerError = listOfNotNull(
                        parsed.errorMessage?.takeIf { it.isNotBlank() },
                        parsed.detail?.takeIf { it.isNotBlank() },
                        parsed.error?.takeIf { it.isNotBlank() },
                        parsed.message?.takeIf { it.isNotBlank() }
                    ).firstOrNull()

                    val mappedStatus = when {
                        parsed.code.equals("NO_BULLETIN", ignoreCase = true) ||
                            parsed.status.equals("NO_BULLETIN", ignoreCase = true) ->
                            CloudflareRunStatus.NO_BULLETIN
                        parsed.code.equals("INSUFFICIENT_BALANCE", ignoreCase = true) ||
                            parsed.status.equals("INSUFFICIENT_BALANCE", ignoreCase = true) ->
                            CloudflareRunStatus.INSUFFICIENT_BALANCE
                        parsed.status.equals("COMPLETED", ignoreCase = true) && parsed.success ->
                            CloudflareRunStatus.COMPLETED
                        parsed.status.equals("REQUIRES_2FA", ignoreCase = true) ->
                            CloudflareRunStatus.REQUIRES_2FA
                        parsed.status.equals("WAF_BLOCKED", ignoreCase = true) ->
                            CloudflareRunStatus.WAF_BLOCKED
                        parsed.status.equals("LOGIN_FAILED", ignoreCase = true) ->
                            CloudflareRunStatus.LOGIN_FAILED
                        else -> CloudflareRunStatus.ERROR
                    }

                    val resolvedErrorMessage = when (mappedStatus) {
                        CloudflareRunStatus.COMPLETED -> null
                        CloudflareRunStatus.NO_BULLETIN -> rawServerError ?: "Yeni bülten yüklenmedi"
                        CloudflareRunStatus.INSUFFICIENT_BALANCE -> rawServerError ?: "Bakiye yetersiz"
                        else -> formatWorkerHttpOrPayloadError(
                            httpCode = response.code,
                            httpMessage = response.message,
                            rawError = rawServerError,
                            steps = parsed.steps,
                            rawBody = responseStr
                        )
                    }

                    val resolvedSteps = if (parsed.steps.isNotEmpty()) {
                        parsed.steps
                    } else if (!response.isSuccessful || mappedStatus == CloudflareRunStatus.ERROR) {
                        listOf("Cloudflare Worker HTTP ${response.code} yanıtı verdi.")
                    } else {
                        emptyList()
                    }

                    CloudflareWorkerRunResult(
                        status = mappedStatus,
                        sessionId = parsed.sessionId,
                        playedColumnsCount = parsed.playedColumnsCount,
                        steps = resolvedSteps,
                        screenshotBase64 = parsed.screenshotBase64,
                        errorMessage = resolvedErrorMessage
                    )
                } else {
                    CloudflareWorkerRunResult(
                        status = CloudflareRunStatus.ERROR,
                        steps = listOf("Cloudflare Worker HTTP ${response.code} yanıtı verdi."),
                        errorMessage = formatWorkerHttpOrPayloadError(
                            httpCode = response.code,
                            httpMessage = response.message,
                            rawError = null,
                            steps = emptyList(),
                            rawBody = responseStr
                        )
                    )
                }
            }
        } catch (e: Exception) {
            CloudflareWorkerRunResult(
                status = CloudflareRunStatus.ERROR,
                steps = listOf("Worker çağrısı sırasında ağ hatası oluştu."),
                errorMessage = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "${e.javaClass.simpleName}: Worker bağlantısı kurulamadı."
            )
        }
    }

    private fun formatWorkerHttpOrPayloadError(
        httpCode: Int,
        httpMessage: String,
        rawError: String?,
        steps: List<String>,
        rawBody: String
    ): String {
        val baseDetail = rawError
            ?: steps.lastOrNull()?.takeIf { it.isNotBlank() }
            ?: rawBody.trim().take(200).takeIf { it.isNotBlank() }
            ?: httpMessage.takeIf { it.isNotBlank() }

        return when (httpCode) {
            401, 403 -> "Yetkisiz erişim (HTTP $httpCode): Worker erişim anahtarı ile CLOUDFLARE_API_TOKEN eşleşmiyor." +
                (baseDetail?.let { " ($it)" } ?: "")
            404 -> if (baseDetail?.contains("İşlem bulunamadı", ignoreCase = true) == true) {
                "Worker uç noktası bulunamadı (HTTP 404). 'Bulut Chrome' (/v1/nesine/session) Worker'ı kullanıyorsanız '☁ Nesine • Bulut Chrome' kartından açın; otomatik oynama için cloudflare-worker/ projesini dağıtın."
            } else {
                "Worker adresi bulunamadı (HTTP 404): ${baseDetail ?: "URL'yi kontrol edin."}"
            }
            405 -> "Worker bu adreste POST isteği kabul etmiyor (HTTP 405): ${baseDetail ?: "Worker URL'sini kontrol edin."}"
            else -> baseDetail ?: "Cloudflare Worker geçersiz yanıt döndürdü (HTTP $httpCode)."
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

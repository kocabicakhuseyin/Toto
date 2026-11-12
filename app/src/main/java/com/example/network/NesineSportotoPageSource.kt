package com.example.network

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.network.model.BulletinMatchDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONTokener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Renders Nesine's public React bulletin in the Android system WebView.
 * Plain HTML requests cannot see the 15 asynchronously rendered match rows.
 * No credentials or wagering actions are used; only visible DOM text is read.
 */
class NesineSportotoPageSource(private val context: Context) {
    companion object {
        const val URL = "https://www.nesine.com/sportoto"

        // Verified against the publicly rendered Nov 12, 2026 bulletin.
        // data-test-id is more stable than the hashed CSS class names.
        private val EXTRACT_ROWS_JS = """
            (function () {
              const rows = Array.from(document.querySelectorAll('table tbody tr'))
                .filter(row => row.querySelector('td[data-test-id="name"]'));
              if (rows.length !== 15) return null;
              const result = rows.map(row => {
                const field = key => row.querySelector('td[data-test-id="' + key + '"]')?.textContent?.trim();
                const number = row.cells[0]?.textContent?.trim();
                const date = row.cells[1]?.textContent?.trim();
                const fixture = row.querySelector('td[data-test-id="name"] button')?.textContent?.trim();
                return {
                  number: Number(number), date, fixture,
                  one: field('percentage1'), draw: field('percentage0'), two: field('percentage2')
                };
              });
              return JSON.stringify(result);
            })()
        """.trimIndent()

        private fun officialPage(value: String): Boolean = runCatching {
            val uri = Uri.parse(value)
            uri.scheme == "https" && uri.host == "www.nesine.com" &&
                uri.path?.trimEnd('/') == "/sportoto"
        }.getOrDefault(false)

        private fun parsePercentage(raw: String?): Int {
            val value = raw?.trim()?.removePrefix("%")?.removeSuffix("%")?.trim()?.toIntOrNull()
            require(value != null && value in 0..100) {
                "Nesine oynanma yüzdesi okunamadı."
            }
            return value
        }

        fun decodeRows(encodedJavaScriptResult: String): List<BulletinMatchDto>? {
            if (encodedJavaScriptResult == "null" || encodedJavaScriptResult.isBlank()) return null
            val unwrapped = JSONTokener(encodedJavaScriptResult).nextValue() as? String
                ?: return null
            val array = JSONArray(unwrapped)
            if (array.length() != 15) return null
            val rows = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                NesinePlayedRow(
                    number = item.getInt("number"),
                    date = item.getString("date"),
                    fixture = item.getString("fixture"),
                    playedOne = parsePercentage(item.getString("one")),
                    playedDraw = parsePercentage(item.getString("draw")),
                    playedTwo = parsePercentage(item.getString("two"))
                )
            }
            return NesineSportotoParser.toBulletinMatches(rows)
        }
    }

    suspend fun fetch(): List<BulletinMatchDto> = try {
        withTimeout(45_000L) {
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    val mainHandler = Handler(Looper.getMainLooper())
                    val webView = WebView(context.applicationContext)
                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        javaScriptCanOpenWindowsAutomatically = false
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    var ended = false
                    var inspect: Runnable? = null

                    fun dispose() {
                        if (ended) return
                        ended = true
                        inspect?.let(mainHandler::removeCallbacks)
                        webView.stopLoading()
                        webView.webViewClient = WebViewClient()
                        webView.destroy()
                    }

                    fun fail(error: Exception) {
                        if (ended) return
                        dispose()
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }

                    inspect = object : Runnable {
                        override fun run() {
                            if (ended || !continuation.isActive) return
                            webView.evaluateJavascript(EXTRACT_ROWS_JS) { raw ->
                                if (ended || !continuation.isActive) return@evaluateJavascript
                                try {
                                    val matches = decodeRows(raw)
                                    if (matches == null) {
                                        mainHandler.postDelayed(this, 750L)
                                    } else {
                                        dispose()
                                        if (continuation.isActive) continuation.resume(matches)
                                    }
                                } catch (_: Exception) {
                                    // All rows can appear before the asynchronous
                                    // percentage cells are populated. Try again
                                    // rather than accepting an incomplete week.
                                    mainHandler.postDelayed(this, 750L)
                                }
                            }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                            request?.isForMainFrame == true && !officialPage(request.url.toString())

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!ended && url != null && officialPage(url)) {
                                inspect?.let {
                                    mainHandler.removeCallbacks(it)
                                    mainHandler.postDelayed(it, 500L)
                                }
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                fail(IllegalStateException("Nesine sayfası yüklenemedi."))
                            }
                        }
                    }
                    continuation.invokeOnCancellation {
                        mainHandler.post { dispose() }
                    }
                    webView.loadUrl(URL)
                }
            }
        }
    } catch (_: TimeoutCancellationException) {
        throw IllegalStateException("Nesine bülteni 45 saniyede yüklenemedi; 15 maç ve yüzdeler alınamadı.")
    } catch (cancelled: CancellationException) {
        throw cancelled
    }
}

package com.example.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.NesineReceipt
import com.example.model.NesineReceiptParser
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Reads only the rendered public Spor Toto receipt. No Nesine login form,
 * sportsbook password, clipboard access or native JavaScript bridge is used.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NesineReceiptWebView(link: String, onResult: (Result<NesineReceipt>) -> Unit) {
    val context = LocalContext.current
    val latestCallback = rememberUpdatedState(onResult)
    val handler = remember(link) { Handler(Looper.getMainLooper()) }
    val webView = remember(link) {
        WebView(context).apply {
            settings.javaScriptEnabled = true // The receipt table is rendered by Nesine JavaScript.
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            var completed = false
            var attempts = 0

            fun finish(result: Result<NesineReceipt>) {
                if (completed) return
                completed = true
                latestCallback.value(result)
            }

            fun checkReceipt() {
                if (completed) return
                attempts++
                evaluateJavascript(
                    """
                    (function() {
                      const tag = document.querySelector('[data-coupon-id]');
                      const rows = [...document.querySelectorAll('tbody tr[data-testid^="event-row-"]')]
                        .map(r => [...r.querySelectorAll('td')].map(c => c.innerText.trim()));
                      return JSON.stringify({
                        couponId: tag ? tag.getAttribute('data-coupon-id') : '',
                        playedAt: tag ? (tag.innerText || '').trim() : '',
                        status: (document.querySelector('[data-testid="coupon-status"]')?.innerText || '').trim(),
                        amount: (document.querySelector('[data-testid="coupon-amount"]')?.innerText || '').trim(),
                        rows: rows
                      });
                    })();
                    """.trimIndent()
                ) { raw ->
                    if (completed) return@evaluateJavascript
                    try {
                        val json = JSONTokener(raw).nextValue() as? String
                            ?: error("Kupon bilgileri okunamadı.")
                        val obj = JSONObject(json)
                        val rows = obj.getJSONArray("rows")
                        val parsedRows = (0 until rows.length()).map { i ->
                            val row = rows.getJSONArray(i)
                            (0 until row.length()).map { j -> row.getString(j) }
                        }
                        val status = obj.optString("status")
                        if (parsedRows.size < 15 && attempts < 18) {
                            handler.postDelayed({ checkReceipt() }, 700L)
                        } else {
                            finish(NesineReceiptParser.parse(
                                couponId = obj.optString("couponId"),
                                status = status,
                                playedAt = obj.optString("playedAt"),
                                amount = obj.optString("amount"),
                                rows = parsedRows
                            ))
                        }
                    } catch (_: Exception) {
                        if (attempts < 18) handler.postDelayed({ checkReceipt() }, 700L)
                        else finish(Result.failure(IllegalStateException("Nesine kupon sayfası doğrulanamadı.")))
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return request.isForMainFrame && !NesineReceiptParser.isShareUrl(request.url.toString())
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (completed) return
                    val uri = android.net.Uri.parse(url)
                    if (uri.host?.lowercase() !in listOf("www.nesine.com", "nesine.com") ||
                        !uri.path.orEmpty().equals("/SporToto/ViewCoupon", ignoreCase = true)) {
                        if (!NesineReceiptParser.isShareUrl(url)) {
                            finish(Result.failure(IllegalStateException("Nesine dışındaki sayfalar doğrulanamaz.")))
                        }
                        return
                    }
                    handler.postDelayed({ checkReceipt() }, 600L)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) {
                        finish(Result.failure(IllegalStateException("Nesine bağlantısı açılamadı.")))
                    }
                }
            }
            loadUrl(link)
        }
    }
    DisposableEffect(webView) {
        onDispose {
            handler.removeCallbacksAndMessages(null)
            webView.stopLoading()
            webView.destroy()
        }
    }
    AndroidView(factory = { webView }, modifier = Modifier.fillMaxWidth().height(2.dp))
}

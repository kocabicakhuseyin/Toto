package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.NesineAutoPlayJsInjector
import com.example.engine.PlatformAutomationManager
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.PlatformCredentials
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NesineWebBrowserAutomationDialog(
    coupon: Coupon,
    credentials: PlatformCredentials,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("nesine.com/spor-toto") }
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var activeColumnIdx by remember { mutableIntStateOf(0) }
    var isBatchRunning by remember { mutableStateOf(false) }
    var automationFeedback by remember {
        mutableStateOf("Giriş yaptıktan (SMS 2FA dahil) sonra 'Kolonu İşaretle' ile seçimleri otomatik aktarabilirsiniz.")
    }

    val columns = coupon.columns
    val currentColumn = columns.getOrNull(activeColumnIdx)

    // Batch execution coroutine when user triggers "Tümünü Sırala"
    LaunchedEffect(isBatchRunning) {
        if (isBatchRunning && columns.isNotEmpty()) {
            for (idx in activeColumnIdx until columns.size) {
                if (!isBatchRunning) break
                activeColumnIdx = idx
                val col = columns[idx]
                val script = NesineAutoPlayJsInjector.buildColumnFillScript(col)
                var failedMsg: String? = null
                webViewRef?.evaluateJavascript(script) { rawResult ->
                    val cleaned = rawResult?.trim('"')?.replace("\\\"", "\"")
                    val json = runCatching { JSONObject(cleaned ?: "{}") }.getOrNull()
                    val ok = json?.optBoolean("success", true) ?: true
                    val msg = json?.optString("message").orEmpty()
                    if (!ok) {
                        failedMsg = msg.ifBlank { "Yeni bülten yüklenmedi" }
                        automationFeedback = "❌ $failedMsg"
                        isBatchRunning = false
                    } else {
                        automationFeedback = msg.ifBlank { "Kolon #${col.index} JS enjeksiyonu çalıştırıldı." }
                    }
                }
                delay(1100L)
                if (!isBatchRunning || failedMsg != null) break
            }
            if (isBatchRunning) {
                isBatchRunning = false
                automationFeedback = "✅ ${columns.size} kolon işleme alındı. Nesine sepetinden son onayı verebilirsiniz."
            }
        }
    }

    Dialog(
        onDismissRequest = {
            isBatchRunning = false
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Browser Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                isBatchRunning = false
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_close_nesine_browser")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextPrimary)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nesine Canlı Tarayıcı Robotu",
                                color = CasinoGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pageTitle,
                                color = TextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_reload_nesine_browser")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = ElectricCyan)
                        }

                        IconButton(
                            onClick = {
                                val text = PlatformAutomationManager.formatCouponForPlatform(
                                    coupon,
                                    BettingPlatform.NESINE,
                                    credentials
                                )
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Nesine Spor Toto", text))
                                Toast.makeText(context, "Kupon panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_copy_nesine_coupon")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kuponu Kopyala", tint = CasinoGold)
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = CasinoGold,
                        trackColor = DarkSurface
                    )
                }

                // Embedded Real WebView loading nesine.com/spor-toto
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                CookieManager.getInstance().setAcceptCookie(true)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadProgress = newProgress
                                        isLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) pageTitle = title
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                    }
                                }
                                loadUrl(BettingPlatform.NESINE.webUrl)
                                webViewRef = this
                            }
                        }
                    )
                }

                // Bottom Automation Control HUD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .border(1.dp, DarkCardBorder)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Feedback Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = automationFeedback,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }

                    // Active Column Strip + Selector
                    if (currentColumn != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { if (activeColumnIdx > 0) activeColumnIdx-- },
                                enabled = activeColumnIdx > 0 && !isBatchRunning,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Önceki Kolon",
                                    tint = if (activeColumnIdx > 0) CasinoGold else TextMuted
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Kolon ${activeColumnIdx + 1} / ${columns.size}",
                                    color = CasinoGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = currentColumn.asString,
                                    color = ElectricGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { if (activeColumnIdx < columns.lastIndex) activeColumnIdx++ },
                                enabled = activeColumnIdx < columns.lastIndex && !isBatchRunning,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Sonraki Kolon",
                                    tint = if (activeColumnIdx < columns.lastIndex) CasinoGold else TextMuted
                                )
                            }
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val script = NesineAutoPlayJsInjector.buildLoginFillScript(
                                    credentials.username,
                                    credentials.password
                                )
                                webViewRef?.evaluateJavascript(script) { raw ->
                                    val cleaned = raw?.trim('"')?.replace("\\\"", "\"")
                                    val reason = runCatching {
                                        JSONObject(cleaned ?: "{}").optString("reason")
                                    }.getOrNull()
                                    automationFeedback = reason?.ifBlank { null }
                                        ?: "Giriş formu doldurma komutu gönderildi."
                                }
                            },
                            enabled = !isBatchRunning,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_webview_fill_login")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Giriş Doldur", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val col = currentColumn ?: return@Button
                                val script = NesineAutoPlayJsInjector.buildColumnFillScript(col)
                                webViewRef?.evaluateJavascript(script) { raw ->
                                    val cleaned = raw?.trim('"')?.replace("\\\"", "\"")
                                    val msg = runCatching {
                                        JSONObject(cleaned ?: "{}").optString("message")
                                    }.getOrNull()
                                    automationFeedback = msg?.ifBlank { null }
                                        ?: "Kolon #${col.index} işaretlendi."
                                }
                                if (activeColumnIdx < columns.lastIndex) {
                                    activeColumnIdx++
                                }
                            },
                            enabled = !isBatchRunning && currentColumn != null,
                            colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_webview_fill_column")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kolonu İşaretle", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = {
                                isBatchRunning = !isBatchRunning
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBatchRunning) Color(0xFFDC2626) else ElectricGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_webview_batch_fill")
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isBatchRunning) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBatchRunning) "Durdur" else "Sırala",
                                color = if (isBatchRunning) Color.White else Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

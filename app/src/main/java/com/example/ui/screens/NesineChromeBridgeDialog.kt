package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Coupon
import com.example.ui.theme.DarkCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Talks only to the user's own HTTPS bridge. No Nesine username/password is
 * collected; the browser is controlled on the user's own computer/server.
 */
@Composable
fun NesineChromeBridgeDialog(coupon: Coupon, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nesine_chrome_bridge", Context.MODE_PRIVATE) }
    var baseUrl by remember { mutableStateOf(prefs.getString("bridge_https_url", "") ?: "") }
    // Only in memory; never save the API token or a bookmaker password.
    var accessToken by remember { mutableStateOf("") }
    var batchIndex by remember { mutableIntStateOf(0) }
    var sameWeek by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val totalBatches = (coupon.totalColumns + 3) / 4
    val start = batchIndex * 4
    val currentColumns = coupon.columns.drop(start).take(4)
    val titleRange = (if (currentColumns.isEmpty()) 0 else start + 1).toString() +
        "–" + (start + currentColumns.size) + " / " + coupon.totalColumns + " kolon"

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        containerColor = DarkCard,
        title = {
            Text("Nesine • Chrome Sunucusu", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Chrome bilgisayarınızda/sunucunuzda arka planda çalışır. " +
                        "Nesine şifresini yalnızca o bilgisayardaki resmî Nesine sayfasına girin.",
                    color = TextPrimary, fontSize = 12.sp
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; message = "" },
                    label = { Text("Sunucunuzun HTTPS adresi") },
                    placeholder = { Text("https://sunucum.example") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true, enabled = !working
                )
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it; message = "" },
                    label = { Text("Sunucu erişim anahtarı") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true, enabled = !working
                )
                Text(titleRange, color = ElectricCyan, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        enabled = !working && batchIndex > 0,
                        onClick = {
                            batchIndex--
                            sameWeek = false
                            message = ""
                            preview = null
                        }
                    ) { Text("◀ Önceki") }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        enabled = !working && batchIndex + 1 < totalBatches,
                        onClick = {
                            batchIndex++
                            sameWeek = false
                            message = ""
                            preview = null
                        }
                    ) { Text("Sonraki ▶") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = sameWeek,
                        enabled = !working,
                        onCheckedChange = { sameWeek = it }
                    )
                    Text(
                        "İki sitedeki 15 maçın haftasını ve sırasını karşılaştırdım.",
                        modifier = Modifier.clickable(enabled = !working) { sameWeek = !sameWeek },
                        color = TextPrimary, fontSize = 12.sp
                    )
                }
                Text(
                    "Bir grupta en fazla 4 ayrı kolon doldurulur. " +
                        "Filtreli matris genişletilmez; bahis oynanmaz.",
                    color = TextMuted, fontSize = 11.sp
                )
                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = if (message.startsWith("✓")) ElectricGreen else TextPrimary,
                        fontSize = 12.sp
                    )
                }
                if (preview != null) {
                    Text("Chrome ekranı • büyütmek için dokunun", color = ElectricCyan, fontSize = 12.sp)
                    Image(
                        bitmap = preview!!,
                        contentDescription = "Sunucudaki Nesine sayfasının önizlemesi",
                        modifier = Modifier.fillMaxWidth().height(140.dp).clickable {
                            previewExpanded = true
                        },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !working && sameWeek && currentColumns.isNotEmpty(),
                onClick = {
                    val host = verifiedHttpsUrl(baseUrl)
                    if (host == null) {
                        message = "Geçerli bir HTTPS sunucu adresi girin."
                        return@Button
                    }
                    if (accessToken.length < 32) {
                        message = "Sunucu erişim anahtarı en az 32 karakter olmalı."
                        return@Button
                    }
                    val rows = currentColumns.map { it.asString }
                    if (rows.any { it.length != 15 || !it.all { ch -> ch in "1X2" } }) {
                        message = "Seçilen grupta geçersiz 15 maçlık kolon var."
                        return@Button
                    }
                    working = true
                    message = "Sunucudaki Chrome formuna aktarılıyor..."
                    preview = null
                    scope.launch {
                        val outcome = withContext(Dispatchers.IO) {
                            NesineBridgeHttp.fill(host, accessToken, rows)
                        }
                        working = false
                        message = outcome.getOrElse { "Aktarım başarısız: " + (it.message ?: "Bağlantı hatası") }
                        if (outcome.isSuccess) prefs.edit().putString("bridge_https_url", host).apply()
                    }
                }
            ) {
                if (working) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Chrome'da Doldur")
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.Start) {
                TextButton(
                    enabled = !working,
                    onClick = {
                        val host = verifiedHttpsUrl(baseUrl)
                        if (host == null || accessToken.length < 32) {
                            message = "Önizleme için HTTPS adresi ve erişim anahtarı girin."
                        } else {
                            working = true
                            scope.launch {
                                val outcome = withContext(Dispatchers.IO) {
                                    NesineBridgeHttp.preview(host, accessToken)
                                }
                                working = false
                                outcome.onSuccess {
                                    preview = it
                                    previewExpanded = true
                                }.onFailure {
                                    message = "Chrome ekranı alınamadı: " + (it.message ?: "Hata")
                                }
                            }
                        }
                    }
                ) { Text("Chrome Ekranını Gör", color = ElectricCyan) }
                TextButton(onClick = onDismiss, enabled = !working) {
                    Text("Kapat", color = TextMuted)
                }
            }
        }
    )

    if (previewExpanded && preview != null) {
        Dialog(
            onDismissRequest = { previewExpanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = { previewExpanded = false }) { Text("Önizlemeyi Kapat") }
                Image(
                    bitmap = preview!!,
                    contentDescription = "Nesine Chrome ekranı",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun verifiedHttpsUrl(input: String): String? = try {
    val trimmed = input.trim().trimEnd('/')
    val parsed = URI(trimmed)
    if (parsed.scheme != "https" || parsed.host.isNullOrBlank() ||
        parsed.userInfo != null || parsed.query != null || parsed.fragment != null
    ) null else trimmed
} catch (_: Exception) { null }

private object NesineBridgeHttp {
    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        // The bearer token must never travel to a redirect destination.
        .followRedirects(false).followSslRedirects(false).build()

    fun fill(base: String, token: String, columns: List<String>): Result<String> = runCatching {
        val body = JSONObject()
            .put("columns", JSONArray(columns))
            .put("same_bulletin_confirmed", true)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(base + "/v1/nesine/fill")
            .header("Authorization", "Bearer " + token)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (!response.isSuccessful) {
                throw IllegalStateException(json?.optString("detail")?.take(150) ?: "HTTP " + response.code)
            }
            val count = json?.optInt("filled_columns") ?: 0
            val checks = json?.optInt("checked_outcomes") ?: 0
            val verified = json?.optBoolean("verified") ?: false
            val wagered = json?.optBoolean("wager_submitted") ?: true
            if (!verified || wagered || count != columns.size || checks != columns.size * 15) {
                throw IllegalStateException("Sunucu aktarımı tam doğrulamadı; kupon oynanmış sayılmaz.")
            }
            "✓ " + count + " kolon (" + checks + " seçim) Chrome'da doğrulandı. Henüz oynanmadı."
        }
    }

    fun preview(base: String, token: String): Result<ImageBitmap> = runCatching {
        val request = Request.Builder()
            .url(base + "/v1/nesine/preview.png")
            .header("Authorization", "Bearer " + token)
            .get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP " + response.code)
            val content = response.body?.bytes() ?: throw IllegalStateException("Boş görüntü")
            if (content.size > 8 * 1024 * 1024) throw IllegalStateException("Görüntü çok büyük")
            BitmapFactory.decodeByteArray(content, 0, content.size)?.asImageBitmap()
                ?: throw IllegalStateException("Geçersiz görüntü")
        }
    }
}

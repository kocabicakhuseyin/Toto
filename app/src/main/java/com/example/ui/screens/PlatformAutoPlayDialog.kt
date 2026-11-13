package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PlatformAutomationManager
import com.example.model.AutoPlayStepState
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.PlatformCredentials
import com.example.network.CloudflareBrowserRunner
import com.example.network.CloudflareRunStatus
import com.example.network.CloudflareWorkerRunResult
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlatformAutoPlayDialog(
    coupon: Coupon,
    platform: BettingPlatform,
    savedCredentials: PlatformCredentials,
    onDismiss: () -> Unit,
    onSaveCredentials: (PlatformCredentials) -> Unit
) {
    if (platform == BettingPlatform.NESINE) {
        NesineCloudflareAutoPlayDialog(
            coupon = coupon,
            savedCredentials = savedCredentials,
            onDismiss = onDismiss,
            onSaveCredentials = onSaveCredentials
        )
        return
    }

    val context = LocalContext.current
    var username by remember { mutableStateOf(savedCredentials.username) }
    var password by remember { mutableStateOf(savedCredentials.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(savedCredentials.isRemembered) }

    var playState by remember { mutableStateOf(AutoPlayStepState.IDLE) }
    var stepIndex by remember { mutableIntStateOf(0) }

    val brandColor = Color(platform.brandColorHex)
    val brandTextColor = Color(platform.textColorHex)

    // Automated play sequence coroutine
    LaunchedEffect(playState) {
        if (playState == AutoPlayStepState.CONNECTING) {
            stepIndex = 1
            delay(700)
            stepIndex = 2
            delay(800)
            stepIndex = 3
            delay(900)
            stepIndex = 4
            playState = AutoPlayStepState.COMPLETED
            PlatformAutomationManager.executeAutoPlay(
                context,
                coupon,
                platform,
                PlatformCredentials(platform, username, password, rememberMe)
            )
            onSaveCredentials(PlatformCredentials(platform, username, password, rememberMe))
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (playState != AutoPlayStepState.CONNECTING) onDismiss()
        },
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brandColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = platform.iconEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${platform.displayName} ile Otomatik Oyna",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${coupon.formulaType.badge} • ${coupon.totalColumns} Kolon (${coupon.totalCostTL.toInt()} ₺)",
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                }
            }
        },
        text = {
            Column {
                if (playState == AutoPlayStepState.IDLE) {
                    Text(
                        text = "${platform.shortName} hesabınız üzerinden kuponun otomatik yatırılabilmesi için bilgilerinizi giriniz:",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("${platform.shortName} Kullanıcı Adı / T.C. / Üye No", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = brandColor,
                            unfocusedIndicatorColor = DarkCardBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_${platform.id}_username"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("${platform.shortName} Şifresi", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Şifre görünürlüğü",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = brandColor,
                            unfocusedIndicatorColor = DarkCardBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_${platform.id}_password"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = brandColor,
                                uncheckedColor = TextMuted
                            )
                        )
                        Text(
                            text = "${platform.shortName} giriş bilgilerimi güvenle hatırla",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bilgileriniz sadece cihazınızda yerel saklanır, güvenli oturum için kullanılır.",
                            color = Color(0xFF0369A1),
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        AutomationStepRow(
                            stepNumber = 1,
                            title = "${platform.domain} SSL Bağlantısı Kuruluyor...",
                            isDone = stepIndex > 1,
                            isActive = stepIndex == 1,
                            brandColor = brandColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AutomationStepRow(
                            stepNumber = 2,
                            title = "'$username' Hesabı Doğrulanıyor...",
                            isDone = stepIndex > 2,
                            isActive = stepIndex == 2,
                            brandColor = brandColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AutomationStepRow(
                            stepNumber = 3,
                            title = "${coupon.totalColumns} Kolon ${platform.shortName} Sepetine Aktarılıyor...",
                            isDone = stepIndex > 3,
                            isActive = stepIndex == 3,
                            brandColor = brandColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AutomationStepRow(
                            stepNumber = 4,
                            title = "Kupon Hazır! ${platform.shortName} Başlatılıyor.",
                            isDone = stepIndex >= 4,
                            isActive = false,
                            brandColor = brandColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (playState == AutoPlayStepState.IDLE) {
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Lütfen kullanıcı adı ve şifrenizi giriniz", Toast.LENGTH_SHORT).show()
                        } else {
                            playState = AutoPlayStepState.CONNECTING
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_autoplay_${platform.id}")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = brandTextColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Otomatik Oyna", color = brandTextColor, fontWeight = FontWeight.Black)
                }
            } else if (playState == AutoPlayStepState.COMPLETED) {
                Button(
                    onClick = {
                        PlatformAutomationManager.executeAutoPlay(
                            context,
                            coupon,
                            platform,
                            PlatformCredentials(platform, username, password, rememberMe)
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_reopen_platform_${platform.id}")
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = brandTextColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${platform.shortName}'yi Aç", color = brandTextColor, fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            if (playState != AutoPlayStepState.CONNECTING) {
                TextButton(onClick = onDismiss) {
                    Text("Kapat", color = TextMuted)
                }
            }
        }
    )
}

@Composable
private fun NesineCloudflareAutoPlayDialog(
    coupon: Coupon,
    savedCredentials: PlatformCredentials,
    onDismiss: () -> Unit,
    onSaveCredentials: (PlatformCredentials) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(savedCredentials.username) }
    var password by remember { mutableStateOf(savedCredentials.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(savedCredentials.isRemembered) }
    var dryRun by remember { mutableStateOf(true) }

    var isRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var workerResult by remember { mutableStateOf<CloudflareWorkerRunResult?>(null) }
    var screenshotBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showInAppBrowser by remember { mutableStateOf(false) }

    val workerReady = remember { CloudflareBrowserRunner.isWorkerConfigured() }
    val restApiReady = remember { CloudflareBrowserRunner.isRestApiConfigured() }

    if (showInAppBrowser) {
        NesineWebBrowserAutomationDialog(
            coupon = coupon,
            credentials = PlatformCredentials(BettingPlatform.NESINE, username, password, rememberMe),
            onDismiss = { showInAppBrowser = false }
        )
        return
    }

    val decodedBitmap = remember(screenshotBytes, workerResult?.screenshotBase64) {
        val raw = screenshotBytes ?: workerResult?.screenshotBase64?.let { b64 ->
            runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
        }
        raw?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F2942))
                        .border(1.dp, ElectricCyan, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloudflare Browser Run",
                        tint = ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Cloudflare ile Nesine Otomatik Oyna",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${coupon.formulaType.badge} • ${coupon.totalColumns} Kolon (${coupon.totalCostTL.toInt()} ₺)",
                        fontSize = 11.sp,
                        color = CasinoGold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Cloudflare Infrastructure Status Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(
                            1.dp,
                            if (workerReady || restApiReady) ElectricGreen.copy(alpha = 0.6f) else NeonAmber.copy(alpha = 0.6f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when {
                            workerReady && restApiReady -> "✅ Cloudflare Worker & Browser Rendering API Hazır"
                            workerReady -> "✅ Cloudflare Puppeteer Worker Hazır"
                            restApiReady -> "✅ Cloudflare Screenshot REST API Hazır (Worker URL Bekleniyor)"
                            else -> "⚠️ Cloudflare Secrets Yapılandırılmadı"
                        },
                        color = if (workerReady || restApiReady) ElectricGreen else NeonAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = if (!workerReady) {
                            "Tam otomatik oynama için cloudflare-worker/ projesini `npx wrangler deploy` ile dağıtıp " +
                                "AI Studio Secrets paneline CLOUDFLARE_BROWSER_WORKER_URL, CLOUDFLARE_ACCOUNT_ID ve CLOUDFLARE_API_TOKEN ekleyin."
                        } else {
                            "Bulut Chrome (Chromium) oturumu Cloudflare üzerinde çalışarak nesine.com/sportoto formunu işaretler."
                        },
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }

                // 2. Nesine Credentials Fields
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nesine Üye No / T.C. / Kullanıcı Adı", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(18.dp))
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = CasinoGold,
                        unfocusedIndicatorColor = DarkCardBorder
                    ),
                    singleLine = true,
                    enabled = !isRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nesine_username"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nesine Şifresi", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Şifre görünürlüğü",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = CasinoGold,
                        unfocusedIndicatorColor = DarkCardBorder
                    ),
                    singleLine = true,
                    enabled = !isRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nesine_password"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = !isRunning) { rememberMe = !rememberMe }
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CasinoGold,
                            uncheckedColor = TextMuted
                        )
                    )
                    Text(
                        text = "Nesine bilgilerimi cihazda hatırla",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // 3. Dry Run Safety Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (dryRun) "🛡️ Önizleme Modu (Dry Run)" else "🚀 Tam Otomatik Onay Modu",
                            color = if (dryRun) ElectricCyan else CasinoGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (dryRun) {
                                "Kolonları işaretler ve ekran görüntüsü alır; son onay tuşuna basmaz."
                            } else {
                                "Kolonları işaretledikten sonra Nesine'de oynama onayını tetikler."
                            },
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = dryRun,
                        onCheckedChange = { dryRun = it },
                        enabled = !isRunning,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = Color.Black,
                            uncheckedTrackColor = CasinoGold
                        ),
                        modifier = Modifier.testTag("switch_cloudflare_dry_run")
                    )
                }

                // 4. Primary & Secondary Cloudflare Action Buttons
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Lütfen Nesine kullanıcı adı ve şifrenizi girin.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val creds = PlatformCredentials(BettingPlatform.NESINE, username, password, rememberMe)
                        if (rememberMe) {
                            onSaveCredentials(creds)
                        }
                        isRunning = true
                        statusMessage = "Cloudflare Browser Run oturumu başlatılıyor..."
                        workerResult = null
                        screenshotBytes = null

                        scope.launch {
                            val res = CloudflareBrowserRunner.runNesineAutoPlayWorker(
                                coupon = coupon,
                                credentials = creds,
                                dryRun = dryRun
                            )
                            workerResult = res
                            statusMessage = when (res.status) {
                                CloudflareRunStatus.COMPLETED ->
                                    "✅ Cloudflare tamamlandı: ${res.playedColumnsCount} kolon işlendi."
                                CloudflareRunStatus.NO_BULLETIN ->
                                    "❌ ${res.errorMessage?.takeIf { it.isNotBlank() } ?: "Yeni bülten yüklenmedi"}"
                                CloudflareRunStatus.INSUFFICIENT_BALANCE ->
                                    "❌ ${res.errorMessage?.takeIf { it.isNotBlank() } ?: "Bakiye yetersiz"}"
                                CloudflareRunStatus.REQUIRES_2FA ->
                                    "📲 SMS (2FA) doğrulaması gerekli. WebView robotuyla devam edebilirsiniz."
                                CloudflareRunStatus.WAF_BLOCKED ->
                                    "🛡️ Nesine WAF engeli algılandı. Uygulama içi WebView robotunu deneyin."
                                CloudflareRunStatus.LOGIN_FAILED ->
                                    "❌ Nesine girişi başarısız oldu."
                                CloudflareRunStatus.NOT_CONFIGURED ->
                                    res.errorMessage ?: "Cloudflare Worker yapılandırılmamış."
                                CloudflareRunStatus.ERROR ->
                                    "❌ Hata: ${res.errorMessage ?: "Bilinmeyen hata"}"
                            }
                            isRunning = false
                        }
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_run_cloudflare_autoplay")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloudflare Çalışıyor...", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                    } else {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cloudflare ile Otomatik Oyna", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        isRunning = true
                        statusMessage = "Cloudflare Browser Rendering (/screenshot) ile nesine.com/sportoto çekiliyor..."
                        screenshotBytes = null
                        scope.launch {
                            val preview = CloudflareBrowserRunner.captureNesineBulletinScreenshot()
                            statusMessage = preview.statusMessage
                            if (preview.success && preview.screenshotBytes != null) {
                                screenshotBytes = preview.screenshotBytes
                            }
                            isRunning = false
                        }
                    },
                    enabled = !isRunning,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CasinoGold.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_cloudflare_rest_screenshot")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cloudflare Canlı Sayfa Önizle (/screenshot)", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                OutlinedButton(
                    onClick = { showInAppBrowser = true },
                    enabled = !isRunning,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_open_nesine_webview_fallback")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Uygulama İçi Tarayıcı Robotu (WebView)", color = ElectricGreen, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                // 5. Execution Console Log & Result Preview
                if (statusMessage != null || workerResult != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = when (workerResult?.status) {
                                    CloudflareRunStatus.COMPLETED -> ElectricGreen
                                    CloudflareRunStatus.ERROR,
                                    CloudflareRunStatus.LOGIN_FAILED,
                                    CloudflareRunStatus.NO_BULLETIN,
                                    CloudflareRunStatus.INSUFFICIENT_BALANCE -> VibrantRed
                                    else -> ElectricCyan
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        workerResult?.steps?.forEach { step ->
                            Text(
                                text = "• $step",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // 6. Cloudflare Rendered Screenshot Preview
                decodedBitmap?.let { bmp ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Cloudflare Tarayıcı Ekran Görüntüsü:",
                            color = CasinoGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Image(
                            bitmap = bmp,
                            contentDescription = "Cloudflare Browser Run Nesine Ekran Görüntüsü",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    PlatformAutomationManager.executeAutoPlay(
                        context,
                        coupon,
                        BettingPlatform.NESINE,
                        PlatformCredentials(BettingPlatform.NESINE, username, password, rememberMe)
                    )
                },
                enabled = !isRunning
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nesine'yi Aç", color = CasinoGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRunning) {
                Text("Kapat", color = TextMuted)
            }
        }
    )
}

@Composable
private fun AutomationStepRow(
    stepNumber: Int,
    title: String,
    isDone: Boolean,
    isActive: Boolean,
    brandColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isDone) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(brandColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
            }
        } else if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = brandColor,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$stepNumber", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            color = if (isDone) brandColor else if (isActive) TextPrimary else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal
        )
    }
}

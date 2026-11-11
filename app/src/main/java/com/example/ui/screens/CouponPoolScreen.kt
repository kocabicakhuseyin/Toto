package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Coupon
import com.example.model.CouponPoolEvaluator
import com.example.model.MatchLiveScore
import com.example.model.NesineReceipt
import com.example.model.NesineReceiptParser
import com.example.model.PoolCouponDto
import com.example.model.PoolCouponUpload
import com.example.model.PoolColumnRank
import com.example.model.TotoMatch
import com.example.repository.PoolRepository
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared coupon leaderboard. Only member-confirmed coupons are sent to Supabase.
 * No betting-site credentials, payment details, or full member names are sent.
 */
@Composable
fun CouponPoolScreen(
    weekKey: String,
    matches: List<TotoMatch>,
    coupons: List<Coupon>,
    liveScores: List<MatchLiveScore>,
    repository: PoolRepository,
    incomingNesineUrl: String? = null,
    displayUsername: String = "Üye",
    onNesineUrlHandled: () -> Unit = {},
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(repository.hasSession) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var sharingConsent by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(coupons.firstOrNull()?.id.orEmpty()) }
    var entries by remember { mutableStateOf<List<PoolCouponDto>>(emptyList()) }
    var ranked by remember { mutableStateOf<List<PoolColumnRank>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val matchLabels = remember(matches) { CouponPoolEvaluator.matchLabels(matches) }
    var nesineLinkInput by remember { mutableStateOf("") }
    var nesineLinkToVerify by remember { mutableStateOf<String?>(null) }
    var verifiedNesineReceipt by remember { mutableStateOf<NesineReceipt?>(null) }
    var uploadedNesineId by remember { mutableStateOf("") }
    var importRetry by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val activeWeekKey = verifiedNesineReceipt?.weekKey ?: weekKey
    val activeMatchLabels = verifiedNesineReceipt?.matchLabels ?: matchLabels

    LaunchedEffect(incomingNesineUrl) {
        if (incomingNesineUrl != null) {
            nesineLinkInput = incomingNesineUrl
            nesineLinkToVerify = incomingNesineUrl
            verifiedNesineReceipt = null
            uploadedNesineId = ""
            onNesineUrlHandled()
        }
    }

    // A Nesine share action is the user's one-time authorization. No extra
    // publish prompt is shown after the actual public receipt is verified.
    LaunchedEffect(verifiedNesineReceipt, signedIn, importRetry) {
        val receipt = verifiedNesineReceipt ?: return@LaunchedEffect
        if (!signedIn || uploadedNesineId == receipt.couponId) return@LaunchedEffect
        loading = true
        val upload = PoolCouponUpload(
            weekKey = receipt.weekKey,
            sourceCouponId = NesineReceiptParser.sourceId(receipt.couponId),
            title = "Nesine • ${receipt.playedAt}",
            maskedName = CouponPoolEvaluator.maskedName(displayUsername),
            matchLabels = receipt.matchLabels,
            columns = receipt.columns,
            confirmedByMember = true
        )
        repository.share(upload)
            .onSuccess {
                uploadedNesineId = receipt.couponId
                message = "Nesine kuponunuz doğrulandı ve ${receipt.columns.size} kolon havuza eklendi."
            }
            .onFailure { err ->
                if (err.message.orEmpty().contains("daha önce")) {
                    uploadedNesineId = receipt.couponId
                    message = "Bu Nesine kuponu zaten havuzda bulunuyor."
                } else message = err.message.orEmpty()
            }
        loading = false
    }

    fun refresh() {
        if (!signedIn || activeWeekKey.isBlank()) return
        scope.launch {
            loading = true
            repository.getWeek(activeWeekKey)
                .onSuccess { entries = it; message = "" }
                .onFailure { message = it.message.orEmpty() }
            repository.getRanking(activeWeekKey, activeMatchLabels, liveScores)
                .onSuccess { ranked = it }
                .onFailure { message = it.message.orEmpty() }
            loading = false
        }
    }

    LaunchedEffect(activeWeekKey, signedIn, activeMatchLabels, uploadedNesineId) {
        if (signedIn && activeWeekKey.isNotBlank()) {
            while (true) {
                repository.getWeek(activeWeekKey)
                    .onSuccess { entries = it; message = "" }
                    .onFailure { message = it.message.orEmpty() }
                repository.getRanking(activeWeekKey, activeMatchLabels, liveScores)
                    .onSuccess { ranked = it }
                    .onFailure { message = it.message.orEmpty() }
                delay(60_000L)
            }
        } else {
            entries = emptyList()
            ranked = emptyList()
        }
    }

    LaunchedEffect(activeWeekKey, signedIn, activeMatchLabels, liveScores) {
        if (signedIn && activeWeekKey.isNotBlank() && activeMatchLabels.size == 15) {
            repository.getRanking(activeWeekKey, activeMatchLabels, liveScores)
                .onSuccess { ranked = it }
        }
    }

    val selectedCoupon = coupons.firstOrNull { it.id == selectedId } ?: coupons.firstOrNull()
    val ownEntries = entries.filter { it.ownerId.isNotBlank() && it.ownerId == repository.currentUserId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kuponlarıma dön", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Canlı Kupon Havuzu", fontSize = 19.sp, color = TextPrimary, fontWeight = FontWeight.Black)
                    Text("${activeWeekKey.ifBlank { "Bülten yükleniyor" }} • Üye onaylı kuponlar", fontSize = 11.sp, color = TextMuted)
                }
                if (signedIn) {
                    IconButton(onClick = { refresh() }, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Havuzu yenile", tint = CasinoGold)
                    }
                }
            }
        }

        item {
            PoolCard {
                Text("Nesine kuponunu doğrula", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Nesine'deki oynanmış Spor Toto kuponunu 'Paylaş' ile Toto'ya gönderebilir ya da bağlantısını buraya yapıştırabilirsiniz. Doğrulanırsa tekrar onay sorulmadan havuza eklenir.",
                    color = TextSecondary, fontSize = 11.sp
                )
                OutlinedTextField(
                    value = nesineLinkInput,
                    onValueChange = { nesineLinkInput = it.take(800) },
                    label = { Text("nsn.li kupon bağlantısı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = nesineLinkToVerify == null && !loading,
                    onClick = {
                        val url = nesineLinkInput.trim()
                        if (!NesineReceiptParser.isShareUrl(url)) {
                            message = "Yalnızca Nesine'nin güvenli Spor Toto paylaşım bağlantısı kabul edilir."
                        } else {
                            verifiedNesineReceipt = null
                            uploadedNesineId = ""
                            nesineLinkToVerify = url
                            message = "Nesine'nin kupon sayfası kontrol ediliyor..."
                        }
                    }
                ) { Text("Bağlantıyı doğrula") }
                verifiedNesineReceipt?.let { receipt ->
                    Text(
                        "Nesine doğrulandı: ${receipt.columns.size} kolon • ${receipt.status} • ${receipt.amount}",
                        color = ElectricGreen, fontSize = 12.sp
                    )
                    if (!signedIn) {
                        Text("Havuza otomatik eklemek için aşağıdan üye girişi yapın.", color = CasinoGold, fontSize = 11.sp)
                    } else if (uploadedNesineId != receipt.couponId) {
                        Button(
                            onClick = { importRetry++ },
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF283346))
                        ) { Text("Havuz kaydını yeniden dene") }
                    }
                }
            }
        }
        if (nesineLinkToVerify != null) {
            item {
                NesineReceiptWebView(link = nesineLinkToVerify.orEmpty()) { result ->
                    nesineLinkToVerify = null
                    result.onSuccess { receipt ->
                        verifiedNesineReceipt = receipt
                        message = "Nesine kuponu doğrulandı: ${receipt.columns.size} kolon."
                    }.onFailure { err -> message = err.message.orEmpty() }
                }
            }
        }

        if (!signedIn) {
            item {
                PoolCard {
                    Text("Üye girişi", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Gerçek üyelerin paylaştığı kolonları görüntülemek için e-posta hesabınızla giriş yapın.", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.take(160) },
                        label = { Text("E-posta") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Şifre (en az 6 karakter)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !loading,
                            onClick = {
                                scope.launch {
                                    loading = true
                                    repository.signIn(email, password)
                                        .onSuccess { signedIn = true; password = ""; message = "Giriş başarılı." }
                                        .onFailure { message = it.message.orEmpty() }
                                    loading = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Giriş") }
                        Button(
                            enabled = !loading,
                            onClick = {
                                scope.launch {
                                    loading = true
                                    repository.register(email, password)
                                        .onSuccess { active ->
                                            signedIn = active
                                            password = ""
                                            message = if (active) "Kayıt tamamlandı." else "E-postanıza gelen doğrulama bağlantısını açıp giriş yapın."
                                        }
                                        .onFailure { message = it.message.orEmpty() }
                                    loading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CasinoGold, contentColor = Color.Black)
                        ) { Text("Kayıt ol") }
                    }
                }
            }
        } else {
            item {
                PoolCard {
                    Text("Kuponunu havuza ekle", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Yalnızca dış sitede oynadığınızı onayladığınız kuponu paylaşın. Adınızın tamamı gönderilmez.", color = TextSecondary, fontSize = 12.sp)
                    if (coupons.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            coupons.forEach { coupon ->
                                Button(
                                    onClick = { selectedId = coupon.id },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (coupon.id == selectedCoupon?.id) CasinoGold else Color(0xFF283346),
                                        contentColor = if (coupon.id == selectedCoupon?.id) Color.Black else TextPrimary
                                    )
                                ) { Text(coupon.title.take(18), maxLines = 1, fontSize = 11.sp) }
                            }
                        }
                    } else {
                        Text("Henüz kayıtlı kuponunuz yok.", color = TextMuted, fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it.take(24) },
                        label = { Text("Üye takma adınız") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nickname.isNotBlank()) {
                        Text("Diğer üyelerin göreceği ad: ${CouponPoolEvaluator.maskedName(nickname)}", fontSize = 12.sp, color = CasinoGold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sharingConsent, onCheckedChange = { sharingConsent = it })
                        Text("Kuponu dış sitede oynadım; kolonlarımın anonim havuzda gösterilmesini kabul ediyorum.", color = TextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && weekKey.isNotBlank() && selectedCoupon != null && nickname.isNotBlank() && sharingConsent && matches.size == 15,
                        onClick = {
                            val coupon = selectedCoupon ?: return@Button
                            val upload = PoolCouponUpload(
                                weekKey = weekKey,
                                sourceCouponId = coupon.id.take(100),
                                title = coupon.title.take(100),
                                maskedName = CouponPoolEvaluator.maskedName(nickname),
                                matchLabels = CouponPoolEvaluator.matchLabels(matches),
                                columns = coupon.columns.map { it.asString },
                                confirmedByMember = true
                            )
                            scope.launch {
                                loading = true
                                repository.share(upload)
                                    .onSuccess { message = "Kupon havuza eklendi."; sharingConsent = false; refresh() }
                                    .onFailure { message = it.message.orEmpty() }
                                loading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CasinoGold, contentColor = Color.Black)
                    ) { Text("Onaylı kuponu havuza ekle") }
                    Button(onClick = { repository.signOut(); signedIn = false; message = "Çıkış yapıldı." },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF283346))) {
                        Text("Üye çıkışı")
                    }
                }
            }
            item {
                PoolCard {
                    Text("Haftanın en iyi kolonları", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Son ${entries.size} paylaşım • Tüm uygun kolonlardan en iyi 40", color = CasinoGold, fontSize = 12.sp)
                    Text("Biten maçlar kesin, devam eden maçlar geçicidir. Eşleşmeyen maçlar puanlandırılmaz.", color = TextMuted, fontSize = 11.sp)
                }
            }
            if (ownEntries.isNotEmpty()) {
                item {
                    PoolCard {
                        Text("Paylaştığım kuponlar", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        ownEntries.forEach { own ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(own.title, color = TextSecondary, fontSize = 12.sp,
                                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Button(
                                    onClick = {
                                        scope.launch {
                                            loading = true
                                            repository.withdraw(own.id)
                                                .onSuccess { message = "Paylaşım kaldırıldı."; refresh() }
                                                .onFailure { message = it.message.orEmpty() }
                                            loading = false
                                        }
                                    },
                                    enabled = !loading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B2631))
                                ) { Text("Kaldır", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
            if (ranked.isEmpty() && !loading) {
                item {
                    PoolCard { Text("Bu hafta henüz paylaşılmış kupon yok.", color = TextSecondary, fontSize = 13.sp) }
                }
            }
            itemsIndexed(ranked, key = { _, item -> "${item.couponId}:${item.columnNumber}" }) { index, rank ->
                RankedPoolColumnCard(index + 1, rank)
            }
        }

        if (loading) {
            item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CasinoGold)
            } }
        }
        if (message.isNotBlank()) {
            item { Text(message, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@Composable
private fun PoolCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun RankedPoolColumnCard(position: Int, rank: PoolColumnRank) {
    PoolCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#$position", color = CasinoGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Text(rank.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Kolon ${rank.columnNumber}", color = TextMuted, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kesin: ${rank.finishedCorrect}", color = ElectricGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Canlı: ${rank.liveCorrect}", color = CasinoGold, fontSize = 12.sp)
            Text("Olası: ${rank.maximumPossible}/15", color = TextSecondary, fontSize = 12.sp)
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            rank.predictions.forEach { prediction ->
                Text(
                    text = prediction.toString(),
                    modifier = Modifier.background(Color(0xFF26344D), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                    color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }
    }
}

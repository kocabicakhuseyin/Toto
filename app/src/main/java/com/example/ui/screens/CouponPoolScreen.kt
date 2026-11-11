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
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun refresh() {
        if (!signedIn || weekKey.isBlank()) return
        scope.launch {
            loading = true
            repository.getWeek(weekKey)
                .onSuccess { entries = it; message = "" }
                .onFailure { message = it.message.orEmpty() }
            loading = false
        }
    }

    LaunchedEffect(weekKey, signedIn) {
        if (signedIn && weekKey.isNotBlank()) {
            while (true) {
                repository.getWeek(weekKey)
                    .onSuccess { entries = it; message = "" }
                    .onFailure { message = it.message.orEmpty() }
                delay(60_000L)
            }
        } else {
            entries = emptyList()
        }
    }

    val ranked = remember(entries, liveScores) { CouponPoolEvaluator.rank(entries, liveScores) }
    val selectedCoupon = coupons.firstOrNull { it.id == selectedId } ?: coupons.firstOrNull()

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
                    Text("${weekKey.ifBlank { "Bülten yükleniyor" }} • Üye onaylı kuponlar", fontSize = 11.sp, color = TextMuted)
                }
                if (signedIn) {
                    IconButton(onClick = { refresh() }, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Havuzu yenile", tint = CasinoGold)
                    }
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
                    Text("${entries.size} paylaşılmış kupon • ${entries.sumOf { it.columns.size }} kolon", color = CasinoGold, fontSize = 12.sp)
                    Text("Biten maçlar kesin, devam eden maçlar geçicidir. Eşleşmeyen maçlar puanlandırılmaz.", color = TextMuted, fontSize = 11.sp)
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

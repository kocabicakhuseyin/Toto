package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsScore
import com.example.data.local.SavedJsonCouponMeta
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FormulaEngine
import com.example.engine.PlatformAutomationManager
import com.example.model.BettingPlatform
import com.example.model.COLUMN_FEE_TL
import com.example.model.Coupon
import com.example.model.PlatformCredentials
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
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

import com.example.model.TotoMatch
import com.example.ui.components.CouponMatrixTableView

@Composable
fun CouponsScreen(
    coupons: List<Coupon>,
    currentCoupon: Coupon?,
    matches: List<TotoMatch> = emptyList(),
    savedCredentials: PlatformCredentials,
    walletBalanceTL: Double,
    onSelectCoupon: (Coupon) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onSaveCredentials: (PlatformCredentials) -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onOpenWallet: () -> Unit,
    onDeductFeeAndPlay: (Coupon) -> Boolean,
    onShareToForum: (Coupon) -> Unit = {},
    onOpenWizard: () -> Unit = {},
    onOpenFilterDialog: () -> Unit = {},
    onSaveCouponToJson: (Coupon) -> Unit = {},
    onLoadSavedJsonCoupons: () -> List<SavedJsonCouponMeta> = { emptyList() },
    onLoadCouponFromFile: (File) -> Unit = {},
    onDeleteSavedJsonFile: (File) -> Unit = {},
    onNavigateToLiveResults: () -> Unit = {},
    onOpenPool: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPlatformForPlay by remember { mutableStateOf<BettingPlatform?>(null) }
    var showSiteSelectionDialog by remember { mutableStateOf(false) }
    var showSavedJsonDialog by remember { mutableStateOf(false) }
    var showCloudflareBridge by remember { mutableStateOf(false) }

    if (coupons.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(CasinoGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Aktif Kupon Bulunmuyor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "12, 13, 14 veya 15 garantili formüllerden birini seçerek bülteninizi oluşturabilir, 0.20 ₺ kolon ücretiyle dilediğiniz sitede oynayabilirsiniz.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToAnalysis,
                        colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_empty_to_analysis")
                    ) {
                        Text("Bültene Git & Kupon Yap", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenWizard,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_empty_create_coupon")
                    ) {
                        Text("Yeni Kupon Oluştur", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onOpenPool, modifier = Modifier.testTag("btn_open_coupon_pool")) {
                        Text("Canlı Kupon Havuzunu Gör")
                    }
                }
            }
        }
        return
    }

    val activeCoupon = currentCoupon ?: coupons.first()
    val canAfford = walletBalanceTL >= activeCoupon.columnFeeTotalTL

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Primary actions remain visible at the top instead of being hidden
        // among advanced quick actions further down the coupon page.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kupon Oluştur ve Kaydet", color = TextPrimary,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${activeCoupon.totalColumns} kolon", color = ElectricGreen,
                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenWizard,
                            colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_create_coupon_visible")
                        ) {
                            Text(
                                "Yeni Kupon Oluştur",
                                color = Color.Black,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )
                        }
                        Button(
                            onClick = { onSaveCouponToJson(activeCoupon) },
                            enabled = activeCoupon.totalColumns > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_save_coupon_visible")
                        ) {
                            Text(
                                "Kuponu Kaydet",
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Cloudflare Browser Run prepares the official Nesine form without
        // submitting a wager or charging the Toto wallet.
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = activeCoupon.totalColumns > 0) {
                        showCloudflareBridge = true
                    }
                    .testTag("btn_nesine_cloudflare_bridge"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102936)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "☁ Nesine • Bulut Chrome",
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Cloudflare Browser Run ile 1–4 kolonu forma aktar ve kontrol et. " +
                            "Oynama işlemi yapılmaz; Worker adresi ve erişim anahtarı gerekir.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Horizontal Saved Coupons Selector Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Oluşturulan Kuponlar (${coupons.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(coupons, key = { it.id }) { c ->
                            val isSelected = c.id == activeCoupon.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF261905) else Color(0xFF0F172A))
                                    .border(
                                        1.dp,
                                        if (isSelected) CasinoGold else DarkCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectCoupon(c) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${c.formulaType.badge} (${c.totalColumns} Kolon)",
                                    color = if (isSelected) CasinoGold else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. MATCH DISTRIBUTION MATRIX TABLE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SportsScore, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Maç Tercihi Dağılım Tablosu",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF261905))
                                .border(0.8.dp, CasinoGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable { onOpenWizard() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Formül Değiştir ⚡",
                                color = CasinoGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    MatchDistributionMatrix(distributions = activeCoupon.matchDistributions)
                }
            }
        }

        // 2. PRIMARY CASINO BETTOR PLAY CARD (No "Nesine", Just "OYNA", with 0.20 TL/Kolon Fee)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141D30)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, CasinoGold.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Fee Calculation Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Seçili Kupon Kolon Sayısı",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "${activeCoupon.totalColumns} Kolon (${activeCoupon.formulaType.badge})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "İşlem Bedeli (0.20 ₺/Kolon)",
                                fontSize = 11.sp,
                                color = CasinoGoldLight
                            )
                            Text(
                                text = "%.2f ₺".format(activeCoupon.columnFeeTotalTL),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = CasinoGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wallet Balance Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B101B))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cüzdan Bakiyeniz: %.2f ₺".format(walletBalanceTL),
                                color = if (canAfford) TextSecondary else Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = if (canAfford) "Yeterli Bakiye 🟢" else "Bakiye Yetersiz 🔴",
                            color = if (canAfford) ElectricGreen else Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // THE BIG "OYNA" ACTION BUTTON
                    Button(
                        onClick = {
                            if (!canAfford) {
                                Toast.makeText(context, "Yetersiz bakiye! Bu kupon için %.2f ₺ gereklidir.".format(activeCoupon.columnFeeTotalTL), Toast.LENGTH_SHORT).show()
                                onOpenWallet()
                            } else {
                                val success = onDeductFeeAndPlay(activeCoupon)
                                if (success) {
                                    Toast.makeText(context, "%.2f ₺ kolon bedeli tahsil edildi. Şimdi site seçiniz.".format(activeCoupon.columnFeeTotalTL), Toast.LENGTH_SHORT).show()
                                    showSiteSelectionDialog = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) CasinoGold else Color(0xFF241B08)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_play_coupon")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (canAfford) Color.Black else CasinoGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (canAfford) "OYNA ⚡ (%.2f ₺)".format(activeCoupon.columnFeeTotalTL) else "Google Play ile Bakiye Yükle 💰",
                                color = if (canAfford) Color.Black else CasinoGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (!canAfford) {
                                Toast.makeText(context, "Yetersiz bakiye! Cloudflare otomatik oynama için %.2f ₺ gereklidir.".format(activeCoupon.columnFeeTotalTL), Toast.LENGTH_SHORT).show()
                                onOpenWallet()
                            } else {
                                val success = onDeductFeeAndPlay(activeCoupon)
                                if (success) {
                                    selectedPlatformForPlay = BettingPlatform.NESINE
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F2942)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_play_cloudflare_nesine")
                    ) {
                        Text(
                            text = "☁️ Cloudflare Browser Run ile Nesine'de Oyna",
                            color = ElectricCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Site seçimi (İddaa, Nesine, Bilyoner vb.) oyna onayından sonra yapılacaktır.",
                        fontSize = 10.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. ACTION BUTTONS (Compact 2x2 Sportsbook Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPool)
                        .testTag("btn_open_coupon_pool"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10251F)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "🏆 Canlı Kupon Havuzu",
                            color = ElectricGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "Üyelerin anonim kuponları • Canlı en iyi kolonlar",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
                // Canlı Maç & Yaşayan Kolon Takip Bannerı
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToLiveResults() }
                        .testTag("btn_coupons_track_live"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1015)),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            listOf(VibrantRed.copy(alpha = 0.8f), CasinoGold.copy(alpha = 0.5f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "🔴", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "Canlı Skor & Yaşayan Kolon Takibi",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Maçlar oynandıkça 15, 14, 13 ikramiye durumunu izle",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Text(
                                text = "Canlı İzle ▶",
                                color = Color(0xFFFF727B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filtreler ile Kolonları Ele
                    CompactActionButton(
                        icon = Icons.Default.FilterAlt,
                        title = "Filtreler ile Ele",
                        badge = "Tasarruf ⚡",
                        accentColor = Color(0xFFFF727B),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenFilterDialog
                    )

                    // Kuponumu Forumda Paylaş
                    CompactActionButton(
                        icon = Icons.Default.Forum,
                        title = "Forumda Paylaş",
                        badge = "Topluluk",
                        accentColor = ElectricCyan,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onShareToForum(activeCoupon)
                            Toast.makeText(context, "Kuponunuz foruma başarıyla paylaşıldı!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kupon Linkini Paylaş
                    CompactActionButton(
                        icon = Icons.Default.Share,
                        title = "Link & Kod Paylaş",
                        badge = "Kupon No",
                        accentColor = CasinoGold,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val txt = PlatformAutomationManager.formatCouponForPlatform(activeCoupon, BettingPlatform.NESINE, savedCredentials)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, txt)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Kupon Linkini Paylaş"))
                        }
                    )

                    // Kuponları Excel Olarak İndir
                    CompactActionButton(
                        icon = Icons.Default.Download,
                        title = "Excel / TXT İndir",
                        badge = "Dışa Aktar",
                        accentColor = ElectricGreen,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val txt = FormulaEngine.exportToExtra1X2Txt(activeCoupon.columns)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Excel Spor Toto", txt))
                            Toast.makeText(context, "Kupon verisi Excel/TXT formatında panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Stored coupons remain accessible, while the Save action is at the top.
                CompactActionButton(
                    icon = Icons.Default.FolderOpen,
                    title = "Kayıtlı JSON Kuponları Aç",
                    badge = "Cihazdaki Kayıtlar",
                    accentColor = CasinoGold,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSavedJsonDialog = true }
                )

                // Benzer Kolonları Sil
                CleanDuplicatesBanner(
                    onCleanDuplicates = {
                        Toast.makeText(context, "Kupon tarandı: Tüm kolonlar benzersiz ve optimize!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 3.5. TOTAL PROBABILITY & DISTRIBUTION STATISTICS CARD (Requested by user)
        item {
            CouponProbabilityStatsCard(
                coupon = activeCoupon,
                matches = matches
            )
        }

        // 4. AUTHENTIC SPOR TOTO MATRIX TABLE VIEW (Requested by User)
        item {
            CouponMatrixTableView(
                coupon = activeCoupon,
                matches = matches,
                onDeleteCoupon = {
                    onDeleteCoupon(activeCoupon.id)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // SITE SELECTION MODAL (Appears AFTER clicking OYNA)
    if (showCloudflareBridge) {
        NesineCloudflareBridgeDialog(
            coupon = activeCoupon,
            onDismiss = { showCloudflareBridge = false }
        )
    }

    if (showSiteSelectionDialog) {
        SiteSelectionDialog(
            coupon = activeCoupon,
            onDismiss = { showSiteSelectionDialog = false },
            onSelectPlatform = { platform ->
                showSiteSelectionDialog = false
                selectedPlatformForPlay = platform
            }
        )
    }

    // Automated Play Dialog for the chosen site
    selectedPlatformForPlay?.let { platform ->
        PlatformAutoPlayDialog(
            coupon = activeCoupon,
            platform = platform,
            savedCredentials = savedCredentials,
            onDismiss = { selectedPlatformForPlay = null },
            onSaveCredentials = { creds ->
                onSaveCredentials(creds)
            }
        )
    }

    // SAVED JSON COUPONS MODAL DIALOG
    if (showSavedJsonDialog) {
        SavedJsonCouponsDialog(
            onDismiss = { showSavedJsonDialog = false },
            onLoadSavedList = onLoadSavedJsonCoupons,
            onLoadCouponFromFile = { file ->
                onLoadCouponFromFile(file)
                showSavedJsonDialog = false
            },
            onDeleteSavedFile = onDeleteSavedJsonFile
        )
    }
}

/**
 * Site Selection Dialog: Shown AFTER clicking Oyna and confirming the 0.20 TL/column fee.
 */
@Composable
private fun SiteSelectionDialog(
    coupon: Coupon,
    onDismiss: () -> Unit,
    onSelectPlatform: (BettingPlatform) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Column {
                Text(
                    text = "Yasal Bahis Sitesini Seçin",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Kolon bedeli tahsil edildi (${coupon.totalColumns} Kolon). Kuponun oynanacağı platformu seçiniz:",
                    fontSize = 11.sp,
                    color = CasinoGoldLight
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BettingPlatform.values().forEach { platform ->
                    val brandColor = Color(platform.brandColorHex)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E1524))
                            .border(1.dp, brandColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { onSelectPlatform(platform) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("select_site_${platform.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = platform.iconEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = platform.displayName,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = platform.domain,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(brandColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (platform == BettingPlatform.NESINE) "Cloudflare ⚡" else "Oyna ⚡",
                                    color = brandColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = TextMuted)
            }
        }
    )
}

@Composable
private fun MatchDistributionMatrix(
    distributions: List<Triple<Int, Int, Int>>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B101B))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Label Column (2, X, 1)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(modifier = Modifier.size(width = 24.dp, height = 18.dp), contentAlignment = Alignment.Center) {
                    Text(text = "M", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                DistributionRowHeader(label = "2", color = Color(0xFFEF4444))
                DistributionRowHeader(label = "X", color = ElectricCyan)
                DistributionRowHeader(label = "1", color = ElectricGreen)
            }

            // 15 Match Columns (1 to 15)
            distributions.forEachIndexed { idx, (h, d, a) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(modifier = Modifier.size(width = 28.dp, height = 18.dp), contentAlignment = Alignment.Center) {
                        Text(text = "${idx + 1}", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    DistributionCell(count = a, text = if (a > 0) "$a" else "0")
                    DistributionCell(count = d, text = if (d > 0) "$d" else "0")
                    DistributionCell(count = h, text = if (h > 0) "$h" else "0")
                }
            }
        }
    }
}

@Composable
private fun DistributionRowHeader(label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontWeight = FontWeight.Black, fontSize = 11.sp)
    }
}

@Composable
private fun DistributionCell(count: Int, text: String) {
    val highlight = count > 0
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (highlight) Color(0xFF1E273A) else Color(0xFF0F1626))
            .border(0.5.dp, if (highlight) CasinoGold.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (highlight) CasinoGoldLight else TextMuted,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CasinoActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A2338))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompactActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    badge: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141E31))
            .border(1.dp, Color(0xFF34445E), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(accentColor.copy(alpha = 0.17f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = badge,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CleanDuplicatesBanner(
    onCleanDuplicates: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0E2218))
            .border(1.dp, ElectricGreen.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .clickable(onClick = onCleanDuplicates)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                tint = ElectricGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Kuponlarımdaki Benzer Kolonları Sil",
                    color = ElectricGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
                Text(
                    text = "Gereksiz Kolonları Temizle & Bütçen Cebinde Kalsın",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun ModernColumnRow(column: TotoColumn) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "K.${column.index.toString().padStart(2, '0')}",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = CasinoGold,
                modifier = Modifier.width(36.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                column.predictions.forEach { p ->
                    val (badgeBg, badgeText) = when (p) {
                        PredictionOutcome.HOME -> Pair(Color(0xFF064E3B), ElectricGreen)
                        PredictionOutcome.DRAW -> Pair(Color(0xFF0C4A6E), ElectricCyan)
                        PredictionOutcome.AWAY -> Pair(Color(0xFF78350F), CasinoGold)
                    }
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.code,
                            color = badgeText,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Statistics panel showing the total probability distribution across all columns in the coupon.
 * Displays Home (1) %, Draw (X) %, Away (2) % totals, coupon confidence index, and favorite/surprise breakdown.
 */
@Composable
fun CouponProbabilityStatsCard(
    coupon: Coupon,
    matches: List<TotoMatch>,
    modifier: Modifier = Modifier
) {
    val totalCols = coupon.columns.size
    val totalPicks = (totalCols * 15).coerceAtLeast(1)

    var count1 = 0
    var countX = 0
    var count2 = 0
    var totalMatchProbSum = 0.0
    var favoritePicks = 0

    coupon.columns.forEach { col ->
        col.predictions.forEachIndexed { matchIdx, pred ->
            when (pred) {
                PredictionOutcome.HOME -> count1++
                PredictionOutcome.DRAW -> countX++
                PredictionOutcome.AWAY -> count2++
            }
            val match = matches.getOrNull(matchIdx)
            if (match != null) {
                val h = match.probability.home.firstOrNull() ?: 50
                val d = match.probability.draw.firstOrNull() ?: 25
                val a = match.probability.away.firstOrNull() ?: 25
                val pickProb = when (pred) {
                    PredictionOutcome.HOME -> h
                    PredictionOutcome.DRAW -> d
                    PredictionOutcome.AWAY -> a
                }
                totalMatchProbSum += pickProb
                val maxProb = maxOf(h, maxOf(d, a))
                if (pickProb == maxProb) {
                    favoritePicks++
                }
            }
        }
    }

    val pct1 = (count1 * 100.0) / totalPicks
    val pctX = (countX * 100.0) / totalPicks
    val pct2 = (count2 * 100.0) / totalPicks
    val confidenceScore = if (matches.isNotEmpty()) (totalMatchProbSum / totalPicks) else 65.0
    val favoriteRatio = (favoritePicks * 100.0) / totalPicks
    val surpriseRatio = (100.0 - favoriteRatio).coerceAtLeast(0.0)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CasinoGold.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().testTag("card_probability_stats")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Let the title and analysis badge wrap independently on narrow displays.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = CasinoGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Toplam İhtimal & Dağılım Paneli",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$totalCols Kolon Analizi",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1 - X - 2 Probability Distribution Bar Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Home (1)
                OutcomeStatBox(
                    label = "1 (Ev Sahibi)",
                    percentage = pct1,
                    count = count1,
                    accentColor = ElectricGreen,
                    modifier = Modifier.weight(1f)
                )

                // Draw (X)
                OutcomeStatBox(
                    label = "X (Beraberlik)",
                    percentage = pctX,
                    count = countX,
                    accentColor = CasinoGold,
                    modifier = Modifier.weight(1f)
                )

                // Away (2)
                OutcomeStatBox(
                    label = "2 (Deplasman)",
                    percentage = pct2,
                    count = count2,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-metrics Row (Confidence score, Favorite vs Surprise, Top column)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A101C))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Güven Endeksi", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = "%%%.1f".format(confidenceScore),
                        color = ElectricGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkCardBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Favori / Sürpriz", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = "%%%.0f / %%%.0f".format(favoriteRatio, surpriseRatio),
                        color = CasinoGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkCardBorder))

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Öncü Kolon", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = "Kolon #1 (Tavsiye)",
                        color = ElectricCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OutcomeStatBox(
    label: String,
    percentage: Double,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 110.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%%%.1f".format(percentage),
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Text(
                text = "$count tercih",
                color = TextSecondary,
                fontSize = 10.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (percentage / 100.0).toFloat().coerceIn(0.05f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }
        }
    }
}

/**
 * Modal dialog listing saved JSON coupons in device local storage,
 * allowing the user to inspect, load, and delete them.
 */
@Composable
private fun SavedJsonCouponsDialog(
    onDismiss: () -> Unit,
    onLoadSavedList: () -> List<SavedJsonCouponMeta>,
    onLoadCouponFromFile: (File) -> Unit,
    onDeleteSavedFile: (File) -> Unit
) {
    var savedList by remember { mutableStateOf(onLoadSavedList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = CasinoGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kayıtlı JSON Kuponları",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "${savedList.size} Dosya",
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            if (savedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📂", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Henüz cihaz hafızasına kaydedilmiş bir JSON kuponu bulunmuyor.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedList, key = { it.id }) { meta ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("saved_json_item_${meta.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = meta.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF261905))
                                            .border(0.8.dp, CasinoGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${meta.totalColumns} Kolon",
                                            color = CasinoGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${meta.formattedDate} • ${meta.file.length() / 1024} KB",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Delete file button
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF450A0A))
                                                .clickable {
                                                    onDeleteSavedFile(meta.file)
                                                    savedList = onLoadSavedList()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Sil",
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        // Load file button
                                        Button(
                                            onClick = { onLoadCouponFromFile(meta.file) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = "Aç 📂",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = CasinoGold, fontWeight = FontWeight.Bold)
            }
        }
    )
}

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
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsScore
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPlatformForPlay by remember { mutableStateOf<BettingPlatform?>(null) }
    var showSiteSelectionDialog by remember { mutableStateOf(false) }

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
                        text = "Kayıtlı Kuponlarınız (${coupons.size}):",
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

        // 3. ACTION BUTTONS (Share, Link, Excel)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Kuponumu Toplulukta Paylaş
                CasinoActionButton(
                    icon = Icons.Default.Forum,
                    title = "Kuponumu Topluluk / Forumda Paylaş",
                    onClick = {
                        onShareToForum(activeCoupon)
                        Toast.makeText(context, "Kuponunuz foruma başarıyla paylaşıldı!", Toast.LENGTH_SHORT).show()
                    }
                )

                // Kupon Linkini Paylaş
                CasinoActionButton(
                    icon = Icons.Default.Share,
                    title = "Kupon Linkini & Kodunu Paylaş",
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
                CasinoActionButton(
                    icon = Icons.Default.Download,
                    title = "Kuponları Excel / CSV Olarak İndir & Kopyala",
                    onClick = {
                        val txt = FormulaEngine.exportToExtra1X2Txt(activeCoupon.columns)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Excel Spor Toto", txt))
                        Toast.makeText(context, "Kupon verisi Excel/TXT formatında panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                    }
                )

                // Benzer Kolonları Sil
                CleanDuplicatesBanner(
                    onCleanDuplicates = {
                        Toast.makeText(context, "Kupon tarandı: Tüm kolonlar benzersiz ve optimize!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
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
                selectedPlatformForPlay = null
            }
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
                                    text = "Oyna ⚡",
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
private fun CleanDuplicatesBanner(
    onCleanDuplicates: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0E2218))
            .border(1.dp, ElectricGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onCleanDuplicates)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Kuponlarımdaki Benzer Kolonları Sil",
                    color = ElectricGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
                Text(
                    text = "Gereksiz Kolonları Temizle & Bütçen Cebinde Kalsın",
                    color = TextSecondary,
                    fontSize = 10.sp
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

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FormulaType
import com.example.model.UserProfile
import com.example.ui.components.AppNavTab
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

@Composable
fun DashboardScreen(
    userProfile: UserProfile,
    onFormulaSelected: (FormulaType) -> Unit,
    onNavigateTab: (AppNavTab) -> Unit,
    onOpenTxtImportDialog: () -> Unit,
    onOpenExcelImportDialog: () -> Unit,
    onOpenWizardDialog: () -> Unit,
    onOpenWallet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HERO JACKPOT CASINO CARD
        HeroJackpotCard(
            onQuickPlay = { onNavigateTab(AppNavTab.ANALYSIS) }
        )

        // 2. QUICK STATS (Bettor Theme)
        QuickStatsRow(
            totalCoupons = userProfile.totalAutoPlayedCoupons,
            walletBalanceTL = userProfile.walletBalanceTL,
            onOpenWallet = onOpenWallet
        )

        // 3. GUARANTEED FORMULA SECTION HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Garantili Formüller (12 - 15)",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF261905))
                    .border(1.dp, CasinoGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .clickable { onOpenWizardDialog() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Sihirbazı Aç ✨",
                    color = CasinoGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 15 GARANTI & 14 GARANTI TILES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GuaranteeTierCard(
                title = "15 Garanti",
                tagline = "Tam Kapsama (N-0)",
                description = "Seçimler tutarsa %100 kesin 15 Garanti ikramiye.",
                badge = "15 HEDEF",
                accentColor = CasinoGold,
                icon = Icons.Default.Casino,
                testTag = "card_formula_15",
                onClick = { onFormulaSelected(FormulaType.GUARANTEE_15) },
                modifier = Modifier.weight(1f)
            )

            GuaranteeTierCard(
                title = "14 Garanti",
                tagline = "N-1 Özel Sistem",
                description = "%80 bütçe tasarrufu ile garantili 14 kapsama.",
                badge = "%80 TASARRUF",
                accentColor = ElectricCyan,
                icon = Icons.Default.Shield,
                testTag = "card_formula_14",
                onClick = { onFormulaSelected(FormulaType.GUARANTEE_14) },
                modifier = Modifier.weight(1f)
            )
        }

        // 13 GARANTI & 12 GARANTI TILES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GuaranteeTierCard(
                title = "13 Garanti",
                tagline = "N-2 İndirgeme",
                description = "Yüksek kolon tasarrufu ile en az 13 garanti.",
                badge = "%92 TASARRUF",
                accentColor = ElectricGreen,
                icon = Icons.Default.Diamond,
                testTag = "card_formula_13",
                onClick = { onFormulaSelected(FormulaType.GUARANTEE_13) },
                modifier = Modifier.weight(1f)
            )

            GuaranteeTierCard(
                title = "12 Garanti",
                tagline = "N-3 Bütçe Dostu",
                description = "Minimum kolonla en ekonomik 12 garanti.",
                badge = "%97 TASARRUF",
                accentColor = Color(0xFFA855F7),
                icon = Icons.Default.Calculate,
                testTag = "card_formula_12",
                onClick = { onFormulaSelected(FormulaType.GUARANTEE_12) },
                modifier = Modifier.weight(1f)
            )
        }

        // 4. SPECIAL SYSTEMS ROW (9 KOLONLU SISTEM & YUZDESEL)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GuaranteeTierCard(
                title = "9 Kolonlu Sistem",
                tagline = "Dengeli 9 Kolon",
                description = "15 maçı 9 akıllı kolona bölen klasik toto sistemi.",
                badge = "9 KOLON",
                accentColor = CasinoGold,
                icon = Icons.Default.GridOn,
                testTag = "card_formula_9col",
                onClick = { onFormulaSelected(FormulaType.GUARANTEED_9_COL) },
                modifier = Modifier.weight(1f)
            )

            GuaranteeTierCard(
                title = "% Yüzdesel Formül",
                tagline = "Akıllı Olasılık",
                description = "Kamuoyu olasılıklarına göre yüksek yüzdeli eleme.",
                badge = "FİLTRE",
                accentColor = ElectricCyan,
                icon = Icons.Default.TableChart,
                testTag = "card_formula_percentage",
                onClick = { onFormulaSelected(FormulaType.PERCENTAGE_FORMULA) },
                modifier = Modifier.weight(1f)
            )
        }

        // 5. EXTERNAL IMPORT SECTION (TXT & EXCEL)
        Text(
            text = "Harici Kolon Yükleme & Oynama",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExternalImportTile(
                title = "Extra1X2 TXT",
                subtitle = "TXT Kolonlarını Yükle & Oyna",
                badge = "STANDART",
                accentColor = ElectricGreen,
                onClick = onOpenTxtImportDialog,
                modifier = Modifier.weight(1f)
            )

            ExternalImportTile(
                title = "Excel / CSV",
                subtitle = "Excel Kuponlarını Yükle & Oyna",
                badge = "EXCEL",
                accentColor = ElectricCyan,
                onClick = onOpenExcelImportDialog,
                modifier = Modifier.weight(1f)
            )
        }

        // 6. WALLET BALANCE & PLAY CARD (No "Nesine" hardcoding, pure "Oyna")
        WalletAndAutoPlayCard(
            userProfile = userProfile,
            onOpenWallet = onOpenWallet,
            onPlay = { onNavigateTab(AppNavTab.COUPONS) }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun GuaranteeTierCard(
    title: String,
    tagline: String,
    description: String,
    badge: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = tagline,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Oyna",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroJackpotCard(
    onQuickPlay: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF261905),
            Color(0xFF131D2D),
            Color(0xFF0F172A)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .border(1.dp, CasinoGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CasinoGold.copy(alpha = 0.2f))
                            .border(1.dp, CasinoGold, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "38. HAFTA CANLI BÜLTEN",
                            color = CasinoGoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VibrantRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kuponlar Açık",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tahmini Büyük İkramiye",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Text(
                    text = "45.000.000 ₺",
                    color = CasinoGoldLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onQuickPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_hero_go_analysis")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "15 Maç Analizi & Oyna",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatsRow(
    totalCoupons: Int,
    walletBalanceTL: Double,
    onOpenWallet: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill(title = "Oynanan Kupon", value = "$totalCoupons", modifier = Modifier.weight(1f))
        StatPill(title = "Kolon Ücreti", value = "0.20 ₺", modifier = Modifier.weight(1f))
        StatPill(
            title = "Cüzdan Bakiyesi",
            value = "%.2f ₺".format(walletBalanceTL),
            valueColor = CasinoGold,
            onClick = onOpenWallet,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    valueColor: Color = TextPrimary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
        }
    }
}

@Composable
private fun ExternalImportTile(
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = badge, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun WalletAndAutoPlayCard(
    userProfile: UserProfile,
    onOpenWallet: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CasinoGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CasinoGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = CasinoGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kupon Oynatma Bakiyesi",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "%.2f ₺".format(userProfile.walletBalanceTL),
                        color = CasinoGoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Kolon başı 0.20 ₺ ile otomatik oynama. Google Play ile bakiye yükleyip kuponunuzu hemen oynayabilirsiniz.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onOpenWallet,
                colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = "Yükle +", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Coupon
import com.example.model.CouponSurvivalReport
import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.PredictionOutcome
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantRed

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder

private enum class MatchFilterCategory(val label: String) {
    ALL("Tümü (15)"),
    FAVORITES("⭐ Takiptekiler"),
    LIVE("🔴 Canlı"),
    FINISHED("🏁 Bitenler"),
    UPCOMING("⏳ Başlamayanlar")
}

@Composable
fun MatchResultsScreen(
    liveScores: List<MatchLiveScore>,
    isPolling: Boolean,
    lastUpdateTime: String,
    isRefreshing: Boolean,
    currentCoupon: Coupon?,
    survivalReport: CouponSurvivalReport,
    favoritedMatchNumbers: Set<Int> = emptySet(),
    onToggleFavorite: (Int) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onTogglePolling: () -> Unit,
    onPollNow: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(MatchFilterCategory.ALL) }

    val liveCount = liveScores.count { it.isLive }
    val finishedCount = liveScores.count { it.status == MatchStatus.FINISHED }
    val upcomingCount = liveScores.count { it.status == MatchStatus.UPCOMING }
    val favCount = liveScores.count { favoritedMatchNumbers.contains(it.matchNumber) }

    val filteredMatches = when (selectedFilter) {
        MatchFilterCategory.ALL -> liveScores
        MatchFilterCategory.FAVORITES -> liveScores.filter { favoritedMatchNumbers.contains(it.matchNumber) }
        MatchFilterCategory.LIVE -> liveScores.filter { it.isLive }
        MatchFilterCategory.FINISHED -> liveScores.filter { it.status == MatchStatus.FINISHED }
        MatchFilterCategory.UPCOMING -> liveScores.filter { it.status == MatchStatus.UPCOMING }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Polling Control Bar & Title
        item {
            LivePollingControlHeader(
                isPolling = isPolling,
                lastUpdateTime = lastUpdateTime,
                isRefreshing = isRefreshing,
                favoritedCount = favCount,
                onTogglePolling = onTogglePolling,
                onPollNow = onPollNow,
                onOpenNotifications = onOpenNotifications
            )
        }

        // 2. Coupon Survival Card (Shows alive columns for 15, 14, 13, 12)
        item {
            CouponSurvivalCard(
                coupon = currentCoupon,
                report = survivalReport,
                onNavigateToCoupons = onNavigateToCoupons
            )
        }

        // 3. Filter Category Chips
        item {
            FilterCategoryChipsRow(
                selectedFilter = selectedFilter,
                liveCount = liveCount,
                finishedCount = finishedCount,
                upcomingCount = upcomingCount,
                favoritesCount = favCount,
                onFilterSelected = { selectedFilter = it }
            )
        }

        // 4. Matches List Header with count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bülten Maçları (${filteredMatches.size} / 15)",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (liveCount > 0) {
                    PulsingLiveBadge(liveCount = liveCount)
                }
            }
        }

        // 5. Match Cards
        if (filteredMatches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedFilter == MatchFilterCategory.FAVORITES)
                                "Henüz takip edilen maç yok. Maç kartlarındaki ⭐ simgesine dokunarak gol ve başlama bildirimlerini açabilirsiniz."
                            else
                                "Seçili filtreye uygun maç bulunamadı.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredMatches, key = { it.matchNumber }) { match ->
                MatchLiveScoreCard(
                    match = match,
                    currentCoupon = currentCoupon,
                    isFavorited = favoritedMatchNumbers.contains(match.matchNumber),
                    onToggleFavorite = { onToggleFavorite(match.matchNumber) }
                )
            }
        }
    }
}

@Composable
private fun LivePollingControlHeader(
    isPolling: Boolean,
    lastUpdateTime: String,
    isRefreshing: Boolean,
    favoritedCount: Int,
    onTogglePolling: () -> Unit,
    onPollNow: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isPolling) listOf(CasinoGold.copy(alpha = 0.6f), ElectricGreen.copy(alpha = 0.4f))
                else listOf(DarkCardBorder, DarkCardBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPolling) Color(0xFF1E3A2F) else Color(0xFF2B2520)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = "Canlı Skor",
                            tint = if (isPolling) ElectricGreen else CasinoGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Canlı Skor & Sonuçlar",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPolling) {
                                PulsingDot(color = ElectricGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Otomatik 5s Güncelleme Aktif",
                                    color = ElectricGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TextMuted)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Otomatik Takip Duraklatıldı",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Header Action Buttons (Notification Settings + Refresh)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .border(1.dp, CasinoGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("btn_open_match_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Gol ve Maç Bildirim Ayarları",
                            tint = CasinoGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPollNow,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .testTag("btn_refresh_live_scores")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = CasinoGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yenile",
                                tint = CasinoGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Notification Settings Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B2232))
                    .border(1.dp, CasinoGold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .clickable { onOpenNotifications() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("banner_notification_settings"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⭐ Takip & Bildirim:",
                        color = CasinoGoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$favoritedCount maç takipte (Gol & Başlama Uyarısı)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "Ayarla / Test ▶",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Polling Switch & Last Update Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Son Güncelleme: ",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = lastUpdateTime,
                        color = CasinoGoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Oto-Yenile",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isPolling,
                        onCheckedChange = { onTogglePolling() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CasinoGold,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        ),
                        modifier = Modifier
                            .size(height = 24.dp, width = 44.dp)
                            .testTag("switch_live_polling")
                    )
                }
            }
        }
    }
}

@Composable
private fun CouponSurvivalCard(
    coupon: Coupon?,
    report: CouponSurvivalReport,
    onNavigateToCoupons: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(CasinoGold.copy(alpha = 0.5f), ElectricCyan.copy(alpha = 0.3f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF261905)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ST",
                            color = CasinoGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Kupon Canlı İlerleme & İkramiye Durumu",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = coupon?.title ?: "Aktif Kupon Bulunamadı",
                            color = CasinoGoldLight,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                if (coupon != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F2617))
                            .border(1.dp, ElectricGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${report.totalColumns} Kolon",
                            color = ElectricGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (coupon == null) {
                // Empty state CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Henüz oluşturulmuş bir kuponunuz bulunmuyor.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToCoupons,
                            colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_go_to_coupons")
                        ) {
                            Text(
                                text = "Bültenden Kupon Oluştur",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // Progress Bar: Evaluated Matches (Finished + Live)
                val evaluated = report.finishedMatchesCount + report.liveMatchesCount
                val progressFraction = evaluated / 15f

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Maç İlerlemesi",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$evaluated / 15 Maç Değerlendirildi (%${(progressFraction * 100).toInt()})",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ElectricCyan,
                        trackColor = DarkSurface
                    )
                }

                // Grid of 4 Survival Tiers (15, 14, 13, 12)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SurvivalTierPill(
                        title = "15/15",
                        subtitle = "Büyük İkramiye",
                        aliveCount = report.aliveFor15,
                        totalCount = report.totalColumns,
                        badgeColor = CasinoGold,
                        modifier = Modifier.weight(1f)
                    )
                    SurvivalTierPill(
                        title = "14 Garanti",
                        subtitle = "N-1 Ödülü",
                        aliveCount = report.aliveFor14,
                        totalCount = report.totalColumns,
                        badgeColor = ElectricGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SurvivalTierPill(
                        title = "13 Garanti",
                        subtitle = "N-2 Ödülü",
                        aliveCount = report.aliveFor13,
                        totalCount = report.totalColumns,
                        badgeColor = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    SurvivalTierPill(
                        title = "12 Garanti",
                        subtitle = "N-3 Ödülü",
                        aliveCount = report.aliveFor12,
                        totalCount = report.totalColumns,
                        badgeColor = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "💡 Canlı maçlar oynandıkça ve maçlar tamamlandıkça yaşayan kolon sayılarınız anlık olarak filtrelenip hesaplanır.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SurvivalTierPill(
    title: String,
    subtitle: String,
    aliveCount: Int,
    totalCount: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val isAlive = aliveCount > 0
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isAlive) Color(0xFF161F2E) else Color(0xFF1E1E24))
            .border(
                1.dp,
                if (isAlive) badgeColor.copy(alpha = 0.7f) else DarkCardBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = if (isAlive) badgeColor else TextMuted,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
            Text(
                text = "$aliveCount Kolon",
                color = if (isAlive) TextPrimary else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = if (totalCount > 0) "%${(aliveCount * 100) / totalCount}" else "%0",
                color = if (isAlive) badgeColor else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FilterCategoryChipsRow(
    selectedFilter: MatchFilterCategory,
    liveCount: Int,
    finishedCount: Int,
    upcomingCount: Int,
    favoritesCount: Int,
    onFilterSelected: (MatchFilterCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChipItem(
                label = "Tümü (15)",
                isSelected = selectedFilter == MatchFilterCategory.ALL,
                accentColor = CasinoGold,
                onClick = { onFilterSelected(MatchFilterCategory.ALL) },
                testTag = "filter_chip_all"
            )
        }
        item {
            FilterChipItem(
                label = "⭐ Takiptekiler ($favoritesCount)",
                isSelected = selectedFilter == MatchFilterCategory.FAVORITES,
                accentColor = CasinoGoldLight,
                onClick = { onFilterSelected(MatchFilterCategory.FAVORITES) },
                testTag = "filter_chip_favorites"
            )
        }
        item {
            FilterChipItem(
                label = "🔴 Canlı ($liveCount)",
                isSelected = selectedFilter == MatchFilterCategory.LIVE,
                accentColor = VibrantRed,
                onClick = { onFilterSelected(MatchFilterCategory.LIVE) },
                testTag = "filter_chip_live"
            )
        }
        item {
            FilterChipItem(
                label = "🏁 Bitenler ($finishedCount)",
                isSelected = selectedFilter == MatchFilterCategory.FINISHED,
                accentColor = ElectricGreen,
                onClick = { onFilterSelected(MatchFilterCategory.FINISHED) },
                testTag = "filter_chip_finished"
            )
        }
        item {
            FilterChipItem(
                label = "⏳ Başlamayanlar ($upcomingCount)",
                isSelected = selectedFilter == MatchFilterCategory.UPCOMING,
                accentColor = ElectricCyan,
                onClick = { onFilterSelected(MatchFilterCategory.UPCOMING) },
                testTag = "filter_chip_upcoming"
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else DarkCard)
            .border(
                1.dp,
                if (isSelected) accentColor else DarkCardBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) accentColor else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MatchLiveScoreCard(
    match: MatchLiveScore,
    currentCoupon: Coupon?,
    isFavorited: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    val isLive = match.isLive
    val isFinished = match.status == MatchStatus.FINISHED
    val isUpcoming = match.status == MatchStatus.UPCOMING

    val cardBorderColor = when {
        isLive && isFavorited -> listOf(VibrantRed, CasinoGold)
        isLive -> listOf(VibrantRed.copy(alpha = 0.8f), VibrantRed.copy(alpha = 0.8f))
        isFinished && isFavorited -> listOf(ElectricGreen.copy(alpha = 0.5f), CasinoGold.copy(alpha = 0.4f))
        isFinished -> listOf(ElectricGreen.copy(alpha = 0.4f), ElectricGreen.copy(alpha = 0.4f))
        isFavorited -> listOf(CasinoGold.copy(alpha = 0.5f), DarkCardBorder)
        else -> listOf(DarkCardBorder, DarkCardBorder)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_card_${match.matchNumber}"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(cardBorderColor)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Match #, League, Favorite Toggle, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, CasinoGold.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${match.matchNumber}",
                            color = CasinoGoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.league,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Star Favorite Toggle Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_fav_match_${match.matchNumber}")
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorited) "Takibi Bırak" else "Takip Et & Bildirim Al",
                            tint = if (isFavorited) CasinoGold else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isFavorited) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CasinoGold.copy(alpha = 0.15f))
                                .border(1.dp, CasinoGold.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Takipte",
                                color = CasinoGoldLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                when (match.status) {
                    MatchStatus.LIVE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VibrantRed.copy(alpha = 0.2f))
                                .border(1.dp, VibrantRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            PulsingDot(color = VibrantRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CANLI • ${match.minuteText}",
                                color = VibrantRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                    MatchStatus.HALF_TIME -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonAmber.copy(alpha = 0.2f))
                                .border(1.dp, NeonAmber, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⏸️ İY (Devre)",
                                color = NeonAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                    MatchStatus.FINISHED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricGreen.copy(alpha = 0.2f))
                                .border(1.dp, ElectricGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🏁 MS (Bitti)",
                                color = ElectricGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                    MatchStatus.UPCOMING -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⏳ ${match.minuteText.ifBlank { match.matchDate.substringAfter(" ").take(5) }}",
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

            // Teams & Scoreboard Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = match.homeTeam,
                        color = if (match.winningOutcome == PredictionOutcome.HOME) CasinoGoldLight else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Oran: %.2f".format(match.homeWinOdd),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                // Digital Score Display
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLive) Color(0xFF261014) else Color(0xFF141923))
                        .border(
                            1.dp,
                            if (isLive) VibrantRed.copy(alpha = 0.8f) else DarkCardBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = match.scoreDisplay,
                        color = when {
                            isLive -> VibrantRed
                            isFinished -> ElectricGreen
                            else -> TextSecondary
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Away Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = match.awayTeam,
                        color = if (match.winningOutcome == PredictionOutcome.AWAY) CasinoGoldLight else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Text(
                        text = "Oran: %.2f".format(match.awayWinOdd),
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Current Winning Outcome Selector (1 - X - 2 indicator)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutcomeBadge(
                    outcomeCode = "1",
                    label = "Ev: %.2f".format(match.homeWinOdd),
                    isWinner = match.winningOutcome == PredictionOutcome.HOME,
                    isLive = isLive,
                    modifier = Modifier.weight(1f)
                )
                OutcomeBadge(
                    outcomeCode = "X",
                    label = "Ber: %.2f".format(match.drawOdd),
                    isWinner = match.winningOutcome == PredictionOutcome.DRAW,
                    isLive = isLive,
                    modifier = Modifier.weight(1f)
                )
                OutcomeBadge(
                    outcomeCode = "2",
                    label = "Dep: %.2f".format(match.awayWinOdd),
                    isWinner = match.winningOutcome == PredictionOutcome.AWAY,
                    isLive = isLive,
                    modifier = Modifier.weight(1f)
                )
            }

            // User's active coupon prediction feedback for this match
            if (currentCoupon != null) {
                UserCouponMatchInsight(
                    matchNumber = match.matchNumber,
                    winningOutcome = match.winningOutcome,
                    coupon = currentCoupon
                )
            }
        }
    }
}

@Composable
private fun OutcomeBadge(
    outcomeCode: String,
    label: String,
    isWinner: Boolean,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val activeBg = when {
        isWinner && isLive -> Color(0xFF3B151A)
        isWinner -> Color(0xFF0F2D1F)
        else -> DarkSurface
    }
    val activeBorder = when {
        isWinner && isLive -> VibrantRed
        isWinner -> ElectricGreen
        else -> DarkCardBorder
    }
    val activeText = when {
        isWinner && isLive -> VibrantRed
        isWinner -> ElectricGreen
        else -> TextMuted
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(activeBg)
            .border(1.dp, activeBorder, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = outcomeCode,
                color = if (isWinner) Color.White else TextSecondary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = activeText,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp
            )
            if (isWinner) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Önde/Kazandı",
                    tint = activeText,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun UserCouponMatchInsight(
    matchNumber: Int,
    winningOutcome: PredictionOutcome?,
    coupon: Coupon
) {
    val matchIdx = matchNumber - 1
    val dist = coupon.matchDistributions.getOrNull(matchIdx) ?: return
    val totalCols = coupon.totalColumns
    if (totalCols == 0) return

    val (homeCount, drawCount, awayCount) = dist
    val (homePct, drawPct, awayPct) = Triple(
        (homeCount * 100) / totalCols,
        (drawCount * 100) / totalCols,
        (awayCount * 100) / totalCols
    )

    val hasWinningPrediction = when (winningOutcome) {
        PredictionOutcome.HOME -> homeCount > 0
        PredictionOutcome.DRAW -> drawCount > 0
        PredictionOutcome.AWAY -> awayCount > 0
        null -> true // Upcoming match
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF131720))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Kupon Tercihleriniz: ",
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = "1: %$homePct | X: %$drawPct | 2: %$awayPct",
                color = CasinoGoldLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (winningOutcome != null) {
            if (hasWinningPrediction) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Başarılı",
                        tint = ElectricGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Kuponda Var",
                        color = ElectricGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Risk",
                        tint = VibrantRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Fire Verildi",
                        color = VibrantRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PulsingLiveBadge(liveCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VibrantRed.copy(alpha = 0.2f))
            .border(1.dp, VibrantRed.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        PulsingDot(color = VibrantRed)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$liveCount Canlı Maç",
            color = VibrantRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

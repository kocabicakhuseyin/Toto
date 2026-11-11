package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.FilterPreviewResult
import com.example.engine.TotoFilterEngine
import com.example.model.FilterApplicationMode
import com.example.model.TotoColumn
import com.example.model.TotoFilterCriteria
import com.example.model.TotoMatch
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricGreenGlow
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TotoFilterDialog(
    initialCriteria: TotoFilterCriteria,
    matches: List<TotoMatch>,
    candidateColumns: List<TotoColumn>,
    onDismiss: () -> Unit,
    onApplyFilters: (TotoFilterCriteria) -> Unit
) {
    var criteria by remember { mutableStateOf(initialCriteria) }
    var showStatsForTotalSurprise by remember { mutableStateOf(false) }

    // Live preview of reduction
    val previewResult: FilterPreviewResult = remember(criteria, candidateColumns) {
        TotoFilterEngine.previewFilter(candidateColumns, criteria, matches)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                // Android 15/16 edge-to-edge dialogs may extend beneath the
                // navigation bar; keep the fixed action row inside safe bounds.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 6.dp),
            color = DarkBackground,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. Top Header Bar (Dark Sportsbook Theme)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF131D30), Color(0xFF1C2A42))
                            )
                        )
                        .border(BorderStroke(1.dp, DarkCardBorder))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "≡  SPOR TOTO FİLTRE VE ELEME 🎯",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 2. Sleek Dark Collapse Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF162235), Color(0xFF0F1726))
                            )
                        )
                        .border(BorderStroke(1.dp, DarkCardBorder))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filtreleri gizlemek için tıklayınız",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("▲", color = CasinoGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1.5. 1-Click Preset Filter Profiles (Favori Ağırlıklı, Dengeli, Sürpriz Oran)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("card_preset_filter_profiles")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ Tek Tıkla Hazır Filtre Profilleri",
                                        color = CasinoGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Hızlı Seçim",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.model.FilterProfileType.values().forEach { profile ->
                                        val isCurrentProfile = when (profile) {
                                            com.example.model.FilterProfileType.FAVORITE_WEIGHTED ->
                                                criteria.allowedTotalSurprises == setOf(0, 1, 2)
                                            com.example.model.FilterProfileType.BALANCED ->
                                                criteria.allowedTotalSurprises == setOf(2, 3, 4)
                                            com.example.model.FilterProfileType.SURPRISE_WEIGHTED ->
                                                criteria.allowedTotalSurprises == setOf(4, 5, 6)
                                        }

                                        val accent = when (profile) {
                                            com.example.model.FilterProfileType.FAVORITE_WEIGHTED -> ElectricCyan
                                            com.example.model.FilterProfileType.BALANCED -> CasinoGold
                                            com.example.model.FilterProfileType.SURPRISE_WEIGHTED -> VibrantRed
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrentProfile) accent.copy(alpha = 0.2f) else Color(0xFF0F172A))
                                                .border(
                                                    1.dp,
                                                    if (isCurrentProfile) accent else DarkCardBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    criteria = profile.toCriteria()
                                                }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = when (profile) {
                                                        com.example.model.FilterProfileType.FAVORITE_WEIGHTED -> "🛡️ Favori"
                                                        com.example.model.FilterProfileType.BALANCED -> "⚖️ Dengeli"
                                                        com.example.model.FilterProfileType.SURPRISE_WEIGHTED -> "💥 Sürpriz"
                                                    },
                                                    color = if (isCurrentProfile) Color.White else TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = profile.badge,
                                                    color = accent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Application Mode Selector (İhtimalli vs Garantili)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Mode 1: İHTİMALLİ
                            val isStrict = criteria.applicationMode == FilterApplicationMode.PROBABILITY_STRICT
                            FilterModeButton(
                                title = "Filtrelerim Tam Uygulansın",
                                badgeText = "İHTİMALLİ",
                                isSelected = isStrict,
                                selectedAccentColor = VibrantRed,
                                selectedBgColor = Color(0xFF450A0A),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_strict_button"),
                                onClick = {
                                    criteria = criteria.copy(applicationMode = FilterApplicationMode.PROBABILITY_STRICT)
                                }
                            )

                            // Mode 2: GARANTİLİ
                            val isFlexible = criteria.applicationMode == FilterApplicationMode.GUARANTEE_FLEXIBLE
                            FilterModeButton(
                                title = "Filtrelerim Esnek Uygulansın",
                                badgeText = "GARANTİLİ",
                                isSelected = isFlexible,
                                selectedAccentColor = ElectricGreen,
                                selectedBgColor = Color(0xFF064E3B),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_guarantee_button"),
                                onClick = {
                                    criteria = criteria.copy(applicationMode = FilterApplicationMode.GUARANTEE_FLEXIBLE)
                                }
                            )
                        }
                    }

                    // 3. Info Notice & Section Header
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bilgi: Tercihlerinize göre filtreler dinamik olarak uygulanır.",
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1A263B), Color(0xFF131D2E))
                                    )
                                )
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡ Tüm Uygulanabilir Filtreler",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 4. Filter Card 1: 15 Maçta Toplam Sürpriz Sayısı
                    item {
                        FilterNumberCard(
                            title = "15 Maçta Toplam Sürpriz(Beraberlik ve Ters Sürpriz) Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3, 4, 5, 6),
                            selectedNumbers = criteria.allowedTotalSurprises,
                            onToggleNumber = { num ->
                                val updated = criteria.allowedTotalSurprises.toMutableSet()
                                if (updated.contains(num)) updated.remove(num) else updated.add(num)
                                criteria = criteria.copy(allowedTotalSurprises = updated)
                            },
                            statsToggleText = "Toplam Sürpriz Filtre İstatistiğini Görmek/Gizlemek için tıklayınız 📊",
                            isStatsVisible = showStatsForTotalSurprise,
                            onToggleStats = { showStatsForTotalSurprise = !showStatsForTotalSurprise }
                        )

                        AnimatedVisibility(visible = showStatsForTotalSurprise) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D30)),
                                border = BorderStroke(1.dp, Color(0xFF22314A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "📈 Sürpriz İstatistikleri (Geçmiş 50 Hafta Ortalaması):",
                                        color = CasinoGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• 2-4 Sürpriz gelme olasılığı: %68.4\n• 5+ Sürpriz gelme olasılığı: %14.2\n• 0-1 Sürpriz gelme olasılığı: %17.4",
                                        color = TextSecondary,
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // 5. Filter Card 2: 15 Maçta Beklediğiniz Beraberlik Sayısı
                    item {
                        FilterNumberCard(
                            title = "15 Maçta Beklediğiniz Beraberlik Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3, 4, 5, 6),
                            selectedNumbers = criteria.allowedTotalDraws,
                            onToggleNumber = { num ->
                                val updated = criteria.allowedTotalDraws.toMutableSet()
                                if (updated.contains(num)) updated.remove(num) else updated.add(num)
                                criteria = criteria.copy(allowedTotalDraws = updated)
                            }
                        )
                    }

                    // 6. Filter Card 3: 15 Maçta Beklenen Ters Sürpriz Sayısı
                    item {
                        FilterNumberCard(
                            title = "15 Maçta Beklenen Ters (Sert) Sürpriz Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3),
                            selectedNumbers = criteria.allowedTotalHardSurprises,
                            onToggleNumber = { num ->
                                val updated = criteria.allowedTotalHardSurprises.toMutableSet()
                                if (updated.contains(num)) updated.remove(num) else updated.add(num)
                                criteria = criteria.copy(allowedTotalHardSurprises = updated)
                            }
                        )
                    }

                    // 7. Filter Card 4: 15 Maçta 1'lerin Sayısı (7..14)
                    item {
                        FilterNumberCard(
                            title = "15 Maçta 1'lerin Sayısı (Ev Sahibi Galibiyeti)",
                            availableNumbers = listOf(7, 8, 9, 10, 11, 12, 13, 14),
                            selectedNumbers = criteria.allowedCountOf1s,
                            onToggleNumber = { num ->
                                val updated = criteria.allowedCountOf1s.toMutableSet()
                                if (updated.contains(num)) updated.remove(num) else updated.add(num)
                                criteria = criteria.copy(allowedCountOf1s = updated)
                            }
                        )
                    }

                    // 8. Filter Card 5: 15 Maçta 2'lerin Sayısı (1..5)
                    item {
                        FilterNumberCard(
                            title = "15 Maçta 2'lerin Sayısı (Deplasman Galibiyeti)",
                            availableNumbers = listOf(1, 2, 3, 4, 5),
                            selectedNumbers = criteria.allowedCountOf2s,
                            onToggleNumber = { num ->
                                val updated = criteria.allowedCountOf2s.toMutableSet()
                                if (updated.contains(num)) updated.remove(num) else updated.add(num)
                                criteria = criteria.copy(allowedCountOf2s = updated)
                            }
                        )
                    }

                    // 9. SECTION: 1-9 Maçları (Süper Lig) Filtreleri
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1E2D4A), Color(0xFF142036))
                                    )
                                )
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚽ 1-9 Maçları (Süper Lig) Filtreleri",
                                color = CasinoGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    item {
                        FilterNumberCard(
                            title = "1-9 Maçlarında beklediğiniz Toplam Sürpriz Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3, 4, 5),
                            selectedNumbers = criteria.allowedSurprises1To9,
                            onToggleNumber = { num ->
                                val s = criteria.allowedSurprises1To9.toMutableSet()
                                if (s.contains(num)) s.remove(num) else s.add(num)
                                criteria = criteria.copy(allowedSurprises1To9 = s)
                            }
                        )
                    }

                    item {
                        FilterNumberCard(
                            title = "1-9 Maçlarında beklediğiniz Beraberlik Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3, 4, 5),
                            selectedNumbers = criteria.allowedDraws1To9,
                            onToggleNumber = { num ->
                                val s = criteria.allowedDraws1To9.toMutableSet()
                                if (s.contains(num)) s.remove(num) else s.add(num)
                                criteria = criteria.copy(allowedDraws1To9 = s)
                            }
                        )
                    }

                    item {
                        FilterNumberCard(
                            title = "1-9 Maçlarında beklediğiniz Ters Sürpriz Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3),
                            selectedNumbers = criteria.allowedHardSurprises1To9,
                            onToggleNumber = { num ->
                                val s = criteria.allowedHardSurprises1To9.toMutableSet()
                                if (s.contains(num)) s.remove(num) else s.add(num)
                                criteria = criteria.copy(allowedHardSurprises1To9 = s)
                            }
                        )
                    }

                    // 10. SECTION: Avrupa Maçları (10-15) Filtreleri
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF064E3B), Color(0xFF0F2F24))
                                    )
                                )
                                .border(1.dp, ElectricGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🌍 Avrupa Maçları (10-15) Filtreleri",
                                color = ElectricGreenGlow,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    item {
                        FilterNumberCard(
                            title = "10-15 Maçlarında beklediğiniz Toplam Sürpriz Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3, 4, 5),
                            selectedNumbers = criteria.allowedSurprises10To15,
                            onToggleNumber = { num ->
                                val s = criteria.allowedSurprises10To15.toMutableSet()
                                if (s.contains(num)) s.remove(num) else s.add(num)
                                criteria = criteria.copy(allowedSurprises10To15 = s)
                            }
                        )
                    }

                    item {
                        FilterNumberCard(
                            title = "10-15 Maçlarında beklediğiniz Beraberlik Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3, 4, 5),
                            selectedNumbers = criteria.allowedDraws10To15,
                            onToggleNumber = { num ->
                                val s = criteria.allowedDraws10To15.toMutableSet()
                                if (s.contains(num)) s.remove(num) else s.add(num)
                                criteria = criteria.copy(allowedDraws10To15 = s)
                            }
                        )
                    }

                    item {
                        FilterNumberCard(
                            title = "10-15 Maçlarında beklediğiniz Ters Sürpriz Sayısı",
                            availableNumbers = listOf(0, 1, 2, 3),
                            selectedNumbers = criteria.allowedHardSurprises10To15,
                            onToggleNumber = { num ->
                                val s = criteria.allowedHardSurprises10To15.toMutableSet()
                                if (s.contains(num)) s.remove(num) else s.add(num)
                                criteria = criteria.copy(allowedHardSurprises10To15 = s)
                            }
                        )
                    }

                    // 11. Ard Arda Gelenler (Seri) Filtresi
                    item {
                        StreakFilterCard(
                            max1s = criteria.maxConsecutive1s,
                            maxDraws = criteria.maxConsecutiveDraws,
                            max2s = criteria.maxConsecutive2s,
                            onMax1sChanged = { criteria = criteria.copy(maxConsecutive1s = it) },
                            onMaxDrawsChanged = { criteria = criteria.copy(maxConsecutiveDraws = it) },
                            onMax2sChanged = { criteria = criteria.copy(maxConsecutive2s = it) }
                        )
                    }

                    // 12. Özel Sürpriz Filtreleme
                    item {
                        CustomMatchSurpriseFilterCard(
                            matches = matches,
                            selectedMatches = criteria.customSurpriseMatches,
                            allowedCount = criteria.allowedCustomSurprisesCount,
                            onToggleMatch = { matchNum ->
                                val updated = criteria.customSurpriseMatches.toMutableSet()
                                if (updated.contains(matchNum)) updated.remove(matchNum) else updated.add(matchNum)
                                criteria = criteria.copy(customSurpriseMatches = updated)
                            },
                            onToggleCount = { countNum ->
                                val updated = criteria.allowedCustomSurprisesCount.toMutableSet()
                                if (updated.contains(countNum)) updated.remove(countNum) else updated.add(countNum)
                                criteria = criteria.copy(allowedCustomSurprisesCount = updated)
                            }
                        )
                    }
                }

                // 13. Bottom Live Preview & Action Bar
                Surface(
                    color = DarkSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Reduction Status Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Başlangıç: ${previewResult.initialColumnCount} Kolon (${String.format("%.2f", previewResult.initialCostTL)} ₺)",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Kalan: ${previewResult.filteredColumnCount} Kolon (${String.format("%.2f", previewResult.filteredCostTL)} ₺)",
                                    color = ElectricGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }

                            if (previewResult.savingsPercentage > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF064E3B))
                                        .border(1.dp, ElectricGreen, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "%${previewResult.savingsPercentage} Tasarruf!",
                                        color = ElectricGreenGlow,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { criteria = TotoFilterCriteria() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(46.dp)
                                    .testTag("btn_reset_filters")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sıfırla", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    onApplyFilters(criteria)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.65f)
                                    .height(46.dp)
                                    .testTag("btn_apply_filter_elimination")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Filtreleri Uygula",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${previewResult.filteredColumnCount} kolon",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterModeButton(
    title: String,
    badgeText: String,
    isSelected: Boolean,
    selectedAccentColor: Color = VibrantRed,
    selectedBgColor: Color = Color(0xFF450A0A),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) selectedBgColor else DarkCard
    val textCol = if (isSelected) Color.White else TextSecondary
    val borderCol = if (isSelected) selectedAccentColor else DarkCardBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(selectedAccentColor.copy(alpha = 0.2f))
                            .border(1.dp, selectedAccentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = selectedAccentColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    color = textCol,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = badgeText,
                color = if (isSelected) selectedAccentColor else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterNumberCard(
    title: String,
    availableNumbers: List<Int>,
    selectedNumbers: Set<Int>,
    onToggleNumber: (Int) -> Unit,
    statsToggleText: String? = null,
    isStatsVisible: Boolean = false,
    onToggleStats: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header bar with title and play icon (matching dark theme)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E2D4A), Color(0xFF142036))
                        )
                    )
                    .border(1.dp, Color(0xFF2A3D62), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = CasinoGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wrap on narrow phones and large font sizes: fixed rows could
            // overflow horizontally and hide the last numerical choices.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableNumbers.forEach { num ->
                    CircleNumberButton(
                        number = num,
                        isSelected = selectedNumbers.contains(num),
                        onClick = { onToggleNumber(num) }
                    )
                }
            }

            if (statsToggleText != null && onToggleStats != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF162235))
                        .border(1.dp, Color(0xFF22314A), RoundedCornerShape(6.dp))
                        .clickable(onClick = onToggleStats)
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statsToggleText,
                        color = CasinoGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleNumberButton(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val outerTileColor = if (isSelected) Color(0xFF064E3B) else Color(0xFF141E31)
    val outerBorder = if (isSelected) ElectricGreen else Color(0xFF22314A)
    val innerCircleBg = if (isSelected) Color(0xFF047857) else Color(0xFF1E293B)
    val innerCircleBorder = if (isSelected) ElectricGreenGlow else Color(0xFF334155)
    val numberColor = if (isSelected) Color.White else TextPrimary

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(outerTileColor)
            .border(1.5.dp, outerBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(innerCircleBg)
                .border(1.dp, innerCircleBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = numberColor,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
    }
}


@Composable
private fun StreakFilterCard(
    max1s: Int?,
    maxDraws: Int?,
    max2s: Int?,
    onMax1sChanged: (Int?) -> Unit,
    onMaxDrawsChanged: (Int?) -> Unit,
    onMax2sChanged: (Int?) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E2D4A), Color(0xFF142036))
                        )
                    )
                    .border(1.dp, Color(0xFF2A3D62), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Ard Arda Gelenler (Seri) Filtresi",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            StreakSelectorRow("En Fazla Art Arda 1'ler:", listOf(2, 3, 4, 5), max1s, onMax1sChanged)
            Spacer(modifier = Modifier.height(6.dp))
            StreakSelectorRow("En Fazla Art Arda Beraberlikler:", listOf(1, 2, 3), maxDraws, onMaxDrawsChanged)
            Spacer(modifier = Modifier.height(6.dp))
            StreakSelectorRow("En Fazla Art Arda 2'ler:", listOf(1, 2, 3), max2s, onMax2sChanged)
        }
    }
}

@Composable
private fun StreakSelectorRow(
    label: String,
    options: List<Int>,
    current: Int?,
    onSelect: (Int?) -> Unit
) {
    Column {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Option "Sınırsız" (null)
            val isUnlimited = (current == null)
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isUnlimited) ElectricCyan else Color(0xFF1E293B))
                    .border(1.dp, if (isUnlimited) Color.White else Color(0xFF475569), RoundedCornerShape(6.dp))
                    .clickable { onSelect(null) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sınırsız",
                    color = if (isUnlimited) Color.Black else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            options.forEach { opt ->
                val isSel = (current == opt)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isSel) ElectricGreen else Color(0xFF1E293B))
                        .border(1.dp, if (isSel) ElectricGreenGlow else Color(0xFF475569), CircleShape)
                        .clickable { onSelect(opt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt.toString(),
                        color = if (isSel) Color.White else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomMatchSurpriseFilterCard(
    matches: List<TotoMatch>,
    selectedMatches: Set<Int>,
    allowedCount: Set<Int>,
    onToggleMatch: (Int) -> Unit,
    onToggleCount: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E2D4A), Color(0xFF142036))
                        )
                    )
                    .border(1.dp, Color(0xFF2A3D62), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Özel Sürpriz Filtreleme (Seçilen Maçlar)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sürpriz beklediğiniz maçları işaretleyin:",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                matches.forEach { match ->
                    val isSel = selectedMatches.contains(match.matchNumber)
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Color(0xFF064E3B) else Color(0xFF1E293B))
                            .border(1.dp, if (isSel) ElectricGreen else Color(0xFF475569), RoundedCornerShape(6.dp))
                            .clickable { onToggleMatch(match.matchNumber) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${match.matchNumber} ${match.homeTeam.take(3)}",
                            color = if (isSel) Color.White else TextPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bu maçlardan beklenen sürpriz sayısı:",
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 1, 2, 3, 4).forEach { c ->
                    val isSel = allowedCount.contains(c)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSel) ElectricGreen else Color(0xFF1E293B))
                            .border(1.dp, if (isSel) ElectricGreenGlow else Color(0xFF475569), CircleShape)
                            .clickable { onToggleCount(c) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = c.toString(),
                            color = if (isSel) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

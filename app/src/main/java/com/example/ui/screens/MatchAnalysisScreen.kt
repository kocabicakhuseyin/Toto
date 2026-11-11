package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterProfileType
import com.example.model.PredictionOutcome
import com.example.model.TotoMatch
import com.example.ui.components.TotoSelectionMatrixView
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Notifications

@Composable
fun MatchAnalysisScreen(
    matches: List<TotoMatch>,
    onTogglePrediction: (Int, PredictionOutcome) -> Unit,
    onSelectAllFavorites: () -> Unit,
    onApplyQuickDoubles: () -> Unit,
    onApplyQuickDoubles10To15: () -> Unit = {},
    onApplyQuickDoubles1To9: () -> Unit = {},
    onClearAll: () -> Unit,
    onGenerateCoupon: () -> Unit,
    onOpenFormulaWizard: () -> Unit,
    onOpenFilterDialog: () -> Unit = {},
    targetColumnCap: Int? = null,
    onSelectTargetColumnCap: (Int?) -> Unit = {},
    onApplyFilterProfile: (FilterProfileType) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefreshBulletin: () -> Unit = {},
    favoritedMatchNumbers: Set<Int> = emptySet(),
    onToggleFavorite: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalSelectedMatches = matches.count { it.selections.isNotEmpty() }
    var showAdvancedTools by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Keep the main Bülten screen focused on matches and the matrix.
        // Advanced controls are composed only when explicitly opened.
        Surface(
            color = DarkSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    enabled = !isRefreshing,
                    onClick = onRefreshBulletin,
                    label = {
                        Text(
                            if (isRefreshing) "Yenileniyor..." else "↻ Bülteni Yenile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkCard,
                        labelColor = ElectricCyan
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = !isRefreshing,
                        selected = false,
                        borderColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("chip_refresh_bulletin")
                )
                FilterChip(
                    selected = false,
                    onClick = { showAdvancedTools = true },
                    label = {
                        Text("⚙ Diğer Araçlar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkCard,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = DarkCardBorder
                    ),
                    modifier = Modifier.testTag("chip_more_bulletin_tools")
                )
            }
        }

        if (showAdvancedTools) {
            AdvancedBulletinToolsDialog(
                targetColumnCap = targetColumnCap,
                onSelectTargetColumnCap = onSelectTargetColumnCap,
                onSelectAllFavorites = onSelectAllFavorites,
                onApplyQuickDoubles = onApplyQuickDoubles,
                onApplyQuickDoubles1To9 = onApplyQuickDoubles1To9,
                onApplyQuickDoubles10To15 = onApplyQuickDoubles10To15,
                onApplyFilterProfile = onApplyFilterProfile,
                onClearAll = onClearAll,
                onDismiss = { showAdvancedTools = false }
            )
        }

        // 15 Matches list with sleek sports betting style & 1-0-2 Selection Matrix
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Interactive 15-Match Selection Matrix (1 - 0 - 2) Table (Matching user screenshot)
            item {
                TotoSelectionMatrixView(
                    matches = matches,
                    onToggleOutcome = onTogglePrediction
                )
            }

            // 2. Section Header for Match Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "15 Maç Detaylı Oran & Tercih Listesi:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "$totalSelectedMatches / 15 Seçildi",
                        color = if (totalSelectedMatches == 15) ElectricGreen else CasinoGoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. 15 Match Cards with 1, 0, 2 options
            items(matches, key = { it.id }) { match ->
                ModernMatchCard(
                    match = match,
                    isFavorited = favoritedMatchNumbers.contains(match.matchNumber),
                    onToggleFavorite = { onToggleFavorite(match.matchNumber) },
                    onToggleOutcome = { outcome -> onTogglePrediction(match.id, outcome) }
                )
            }
        }

        // Bottom Docked Summary & Play Bar
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenFilterDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_docked_filter_dialog")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Matris Filtrele",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onOpenFormulaWizard,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_docked_generate_coupon")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ Formüller",
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(7.dp))

                Button(
                    onClick = onGenerateCoupon,
                    colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_generate_coupon_direct")
                ) {
                    Text(
                        text = "Seçimlerden Kupon Oluştur",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "$totalSelectedMatches / 15 Maç Tercihi Belirlendi",
                        color = if (totalSelectedMatches == 15) ElectricGreen else NeonAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "0.20 ₺ / Kolon • 12, 13, 14, 15 Garanti",
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedBulletinToolsDialog(
    targetColumnCap: Int?,
    onSelectTargetColumnCap: (Int?) -> Unit,
    onSelectAllFavorites: () -> Unit,
    onApplyQuickDoubles: () -> Unit,
    onApplyQuickDoubles1To9: () -> Unit,
    onApplyQuickDoubles10To15: () -> Unit,
    onApplyFilterProfile: (FilterProfileType) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val columnCaps = listOf(
        com.example.engine.FormulaEngine.UNLIMITED_COLUMNS_CAP to "Sınırsız",
        null to "Otomatik",
        12 to "12",
        18 to "18",
        24 to "24",
        32 to "32",
        48 to "48",
        64 to "64",
        96 to "96",
        128 to "128"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Diğer Bülten Araçları", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("Toplu maç seçimi", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                BulletinDialogAction("⚡ Tüm Favorileri Seç") {
                    onSelectAllFavorites()
                    onDismiss()
                }
                BulletinDialogAction("⚽ Seçimleri 1–9 Dağıt") {
                    onApplyQuickDoubles1To9()
                    onDismiss()
                }
                BulletinDialogAction("🌍 Seçimleri 10–15 Dağıt") {
                    onApplyQuickDoubles10To15()
                    onDismiss()
                }
                BulletinDialogAction("🎯 Seçimleri 1–15 Dağıt") {
                    onApplyQuickDoubles()
                    onDismiss()
                }
                BulletinDialogAction("✕ Tüm Seçimleri Temizle") {
                    onClearAll()
                    onDismiss()
                }

                Spacer(modifier = Modifier.height(7.dp))
                Text("Kolon limiti", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Seçilen sınır yeni oluşturulacak kuponlarda kullanılır.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(columnCaps) { (cap, label) ->
                        val selected = cap == targetColumnCap
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTargetColumnCap(cap) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkCard,
                                labelColor = TextPrimary,
                                selectedContainerColor = Color(0xFF064E3B),
                                selectedLabelColor = ElectricGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(7.dp))
                Text("İsteğe bağlı filtre profilleri", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                BulletinDialogAction("🛡️ Favori Ağırlıklı") {
                    onApplyFilterProfile(FilterProfileType.FAVORITE_WEIGHTED)
                    onDismiss()
                }
                BulletinDialogAction("⚖️ Dengeli") {
                    onApplyFilterProfile(FilterProfileType.BALANCED)
                    onDismiss()
                }
                BulletinDialogAction("💥 Sürpriz Oran") {
                    onApplyFilterProfile(FilterProfileType.SURPRISE_WEIGHTED)
                    onDismiss()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat", color = ElectricCyan) }
        },
        containerColor = DarkSurface
    )
}

@Composable
private fun BulletinDialogAction(
    label: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = TextPrimary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ModernMatchCard(
    match: TotoMatch,
    isFavorited: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onToggleOutcome: (PredictionOutcome) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("modern_match_${match.matchNumber}"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFavorited) CasinoGold.copy(alpha = 0.65f) else DarkCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Match Number, Date, Favorite Toggle, League
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${match.matchNumber.toString().padStart(2, '0')}",
                            color = ElectricCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.matchDate,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Favorite Star Toggle
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_fav_analysis_match_${match.matchNumber}")
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorited) "Favorilerden Çıkar" else "Favoriye Ekle (Gol & Başlama Bildirimi)",
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

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, CasinoGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Spor Toto",
                        color = CasinoGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Teams Row (Bold & Clean)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.homeTeam,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "VS",
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Text(
                    text = match.awayTeam,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Modern Outcome Selection Chips (1 - X - 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val homeProb = match.probability.home.firstOrNull() ?: 50
                val drawProb = match.probability.draw.firstOrNull() ?: 25
                val awayProb = match.probability.away.firstOrNull() ?: 25

                ModernOutcomeChip(
                    label = "1",
                    teamTitle = "Ev Sahibi",
                    probability = homeProb,
                    isSelected = match.selections.contains(PredictionOutcome.HOME),
                    modifier = Modifier.weight(1f),
                    onClick = { onToggleOutcome(PredictionOutcome.HOME) }
                )

                ModernOutcomeChip(
                    label = "X",
                    teamTitle = "Beraberlik",
                    probability = drawProb,
                    isSelected = match.selections.contains(PredictionOutcome.DRAW),
                    modifier = Modifier.weight(1f),
                    onClick = { onToggleOutcome(PredictionOutcome.DRAW) }
                )

                ModernOutcomeChip(
                    label = "2",
                    teamTitle = "Deplasman",
                    probability = awayProb,
                    isSelected = match.selections.contains(PredictionOutcome.AWAY),
                    modifier = Modifier.weight(1f),
                    onClick = { onToggleOutcome(PredictionOutcome.AWAY) }
                )
            }
        }
    }
}

@Composable
private fun ModernOutcomeChip(
    label: String,
    teamTitle: String,
    probability: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Color(0xFF064E3B) else Color(0xFF0B1322)
    val border = if (isSelected) ElectricGreen else Color(0xFF22314A)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
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
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = ElectricGreenGlow,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = label,
                    color = if (isSelected) Color.White else TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$probability%",
                color = if (isSelected) ElectricGreenGlow else CasinoGoldLight,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Mini visual percentage progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (probability / 100f).coerceIn(0.05f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) ElectricGreen else ElectricCyan)
                )
            }
        }
    }
}

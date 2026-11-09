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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PredictionOutcome
import com.example.model.TotoMatch
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

@Composable
fun MatchAnalysisScreen(
    matches: List<TotoMatch>,
    onTogglePrediction: (Int, PredictionOutcome) -> Unit,
    onSelectAllFavorites: () -> Unit,
    onApplyQuickDoubles: () -> Unit,
    onClearAll: () -> Unit,
    onGenerateCoupon: () -> Unit,
    onOpenFormulaWizard: () -> Unit,
    isRefreshing: Boolean = false,
    onRefreshBulletin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalSelectedMatches = matches.count { it.selections.isNotEmpty() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Sticky Action Bar (Modern Filter Chips)
        Surface(
            color = DarkSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = isRefreshing,
                            onClick = onRefreshBulletin,
                            label = {
                                Text(
                                    if (isRefreshing) "Yenileniyor..." else "🔄 Bülteni Çek",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkCard,
                                labelColor = ElectricCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isRefreshing,
                                borderColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("chip_refresh_bulletin")
                        )
                    }

                    item {
                        FilterChip(
                            selected = false,
                            onClick = onSelectAllFavorites,
                            label = { Text("⚡ Tüm Favoriler", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkCard,
                                labelColor = ElectricCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = DarkCardBorder
                            ),
                            modifier = Modifier.testTag("chip_all_favorites")
                        )
                    }

                    item {
                        FilterChip(
                            selected = false,
                            onClick = onApplyQuickDoubles,
                            label = { Text("🎯 Çifte Şans Dağıt", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkCard,
                                labelColor = ElectricGreen
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = DarkCardBorder
                            ),
                            modifier = Modifier.testTag("chip_apply_doubles")
                        )
                    }

                    item {
                        FilterChip(
                            selected = false,
                            onClick = onClearAll,
                            label = { Text("✕ Temizle", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkCard,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = DarkCardBorder
                            ),
                            modifier = Modifier.testTag("chip_clear_all")
                        )
                    }
                }
            }
        }

        // 15 Matches list with sleek sports betting style
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(matches, key = { it.id }) { match ->
                ModernMatchCard(
                    match = match,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$totalSelectedMatches / 15 Maç Seçildi",
                        color = if (totalSelectedMatches == 15) ElectricGreen else NeonAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "14 Garanti N-1 İndirgeme",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onOpenFormulaWizard,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_docked_generate_coupon")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Garantili Formül & Oyna",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernMatchCard(
    match: TotoMatch,
    onToggleOutcome: (PredictionOutcome) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("modern_match_${match.matchNumber}"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Match Number, Date, League
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.matchDate,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
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

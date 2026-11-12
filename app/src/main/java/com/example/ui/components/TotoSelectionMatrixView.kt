package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PredictionOutcome
import com.example.model.TotoMatch
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantRed

private val HeaderRed = Color(0xFFC62828)
private val SelectedBlue = Color(0xFF1565C0)
private val UnselectedCellBg = Color(0xFF0F172A)
private val CellBorder = Color(0xFF1E293B)

/**
 * 15-Match Interactive Selection Matrix (1 - 0 - 2) for Spor Toto coupon creation.
 * Allows instant bird's-eye editing of home (1), draw (0 / X), and away (2) predictions.
 */
@Composable
fun TotoSelectionMatrixView(
    matches: List<TotoMatch>,
    onToggleOutcome: (matchId: Int, outcome: PredictionOutcome) -> Unit,
    onChangeFavorite: (matchId: Int, outcome: PredictionOutcome?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // Favorite editing is independent from selecting 1/0/2 outcomes for a coupon.
    var favoriteEditMode by remember { mutableStateOf(false) }

    // Calculate overall stats: singles, doubles, triples
    val (singlesCount, doublesCount, triplesCount) = remember(matches) {
        var singles = 0
        var doubles = 0
        var triples = 0
        matches.forEach { match ->
            when (match.selections.size) {
                1 -> singles++
                2 -> doubles++
                3 -> triples++
            }
        }
        Triple(singles, doubles, triples)
    }

    // Straight column count: 2^doubles * 3^triples
    val straightColumns = remember(doublesCount, triplesCount) {
        var cols = 1L
        repeat(doublesCount) { cols *= 2 }
        repeat(triplesCount) { cols *= 3 }
        cols
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("toto_selection_matrix_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Bar: "Maç Tercihi Dağılımı Tablosu" (Matching the user screenshot)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                        )
                    )
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Maç Tercihi Dağılımı Tablosu (1 - 0 - 2)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Both operations are available directly on the Bulletin matrix.
            // Explicit mode prevents an accidental change to a user's coupon.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    false to "Kupon Seçimi",
                    true to "★ Favori Seçimi"
                ).forEach { (mode, label) ->
                    val active = favoriteEditMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) {
                                    if (mode) Color(0xFF065F46) else SelectedBlue
                                } else Color(0xFF172033)
                            )
                            .border(
                                1.dp,
                                if (active) {
                                    if (mode) ElectricGreen else ElectricCyan
                                } else CellBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { favoriteEditMode = mode }
                            .testTag(if (mode) "matrix_mode_favorite" else "matrix_mode_coupon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (favoriteEditMode) {
                    "Favori modu: Her maçın 1 / 0 / 2 hücresine dokunun; kupon tercihi değişmez. Oto için Fav. satırına dokunun."
                } else {
                    "Kupon modu: 1 / 0 / 2 hücreleri kuponu düzenler. Favoriyi değiştirmek için ★ Favori Seçimi'ni açın."
                },
                color = if (favoriteEditMode) ElectricGreen else TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "15 maçın tamamı için tabloyu sağa kaydırın →",
                color = ElectricCyan,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            // Scrollable Matrix Grid: Row 2, Row 0, Row 1 across 15 matches + Totals
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // ROW 0: MATCH NUMBERS & TEAM ABBREVIATIONS
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header label corner cell
                        Box(
                            modifier = Modifier
                                .size(width = 38.dp, height = 34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Maç",
                                color = CasinoGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // 15 Match headers
                        matches.take(15).forEachIndexed { idx, match ->
                            val teamShort = match.homeTeam.take(3).uppercase()
                            val isFilled = match.selections.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .size(width = 28.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isFilled) Color(0xFF1E293B) else Color(0xFF0F172A))
                                    .border(0.5.dp, if (isFilled) ElectricCyan else CellBorder, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = if (isFilled) ElectricCyan else TextMuted,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = teamShort,
                                        color = TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Total column header
                        Box(
                            modifier = Modifier
                                .size(width = 34.dp, height = 34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Top.",
                                color = CasinoGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Favorite is distinct from the user's 1 / X / 2 predictions.
                    MatrixFavoriteRow(
                        matches = matches,
                        onChangeFavorite = onChangeFavorite
                    )

                    // ROW 1: "2" (Deplasman)
                    MatrixOutcomeRow(
                        outcome = PredictionOutcome.AWAY,
                        label = "2",
                        matches = matches,
                        favoriteEditMode = favoriteEditMode,
                        onToggle = { matchId -> onToggleOutcome(matchId, PredictionOutcome.AWAY) },
                        onFavoriteSelect = { matchId -> onChangeFavorite(matchId, PredictionOutcome.AWAY) }
                    )

                    // ROW 2: "0" (Beraberlik / X)
                    MatrixOutcomeRow(
                        outcome = PredictionOutcome.DRAW,
                        label = "0",
                        matches = matches,
                        favoriteEditMode = favoriteEditMode,
                        onToggle = { matchId -> onToggleOutcome(matchId, PredictionOutcome.DRAW) },
                        onFavoriteSelect = { matchId -> onChangeFavorite(matchId, PredictionOutcome.DRAW) }
                    )

                    // ROW 3: "1" (Ev Sahibi)
                    MatrixOutcomeRow(
                        outcome = PredictionOutcome.HOME,
                        label = "1",
                        matches = matches,
                        favoriteEditMode = favoriteEditMode,
                        onToggle = { matchId -> onToggleOutcome(matchId, PredictionOutcome.HOME) },
                        onFavoriteSelect = { matchId -> onChangeFavorite(matchId, PredictionOutcome.HOME) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Summary Bar: Combinations, Columns & Cost
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B1322))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text(
                        text = "Tercih Dağılımı:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "$singlesCount Tekli • $doublesCount Çifte • $triplesCount Kapalı",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Düz / Formüllü Kolon:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$straightColumns",
                            color = CasinoGoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = " Kolon",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixFavoriteRow(
    matches: List<TotoMatch>,
    onChangeFavorite: (Int, PredictionOutcome?) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 30.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF065F46)),
            contentAlignment = Alignment.Center
        ) {
            Text("Fav.", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        matches.take(15).forEach { match ->
            var expanded by remember(match.id) { mutableStateOf(false) }
            val isManual = match.favoriteOverride != null
            Box {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isManual) Color(0xFF78350F) else Color(0xFF064E3B))
                        .border(
                            1.dp,
                            if (isManual) CasinoGold else ElectricGreen,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { expanded = true }
                        .testTag("matrix_favorite_${match.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = match.favoriteOutcome.label + if (isManual) "*" else "",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Otomatik (${match.automaticFavoriteOutcome.label})") },
                        onClick = {
                            onChangeFavorite(match.id, null)
                            expanded = false
                        },
                        modifier = Modifier.testTag("matrix_favorite_auto_${match.id}")
                    )
                    listOf(PredictionOutcome.HOME, PredictionOutcome.DRAW, PredictionOutcome.AWAY).forEach { choice ->
                        DropdownMenuItem(
                            text = { Text("Elle: ${choice.label}") },
                            onClick = {
                                onChangeFavorite(match.id, choice)
                                expanded = false
                            },
                            modifier = Modifier.testTag("matrix_favorite_${choice.code}_${match.id}")
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 30.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${matches.count { it.favoriteOverride != null }}*",
                color = CasinoGoldLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MatrixOutcomeRow(
    outcome: PredictionOutcome,
    label: String,
    matches: List<TotoMatch>,
    favoriteEditMode: Boolean,
    onToggle: (matchId: Int) -> Unit,
    onFavoriteSelect: (matchId: Int) -> Unit
) {
    val totalCount = if (favoriteEditMode) {
        matches.count { it.favoriteOutcome == outcome }
    } else {
        matches.count { it.selections.contains(outcome) }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Header: Red cell with "2", "0", or "1" (Matches user screenshot)
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 30.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(HeaderRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }

        // 15 Match Cells: Interactive toggle button
        matches.take(15).forEach { match ->
            val isSelected = if (favoriteEditMode) {
                match.favoriteOutcome == outcome
            } else {
                match.selections.contains(outcome)
            }

            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            favoriteEditMode && isSelected -> Color(0xFF065F46)
                            isSelected -> SelectedBlue
                            else -> UnselectedCellBg
                        }
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.5.dp,
                        color = when {
                            favoriteEditMode && isSelected -> ElectricGreen
                            isSelected -> Color(0xFF60A5FA)
                            else -> CellBorder
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        if (favoriteEditMode) onFavoriteSelect(match.id) else onToggle(match.id)
                    }
                    .testTag(
                        if (favoriteEditMode) "matrix_favorite_direct_${match.id}_${outcome.code}"
                        else "matrix_cell_${match.id}_$label"
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                        text = if (isSelected) label else "·",
                    color = if (isSelected) Color.White else Color(0xFF475569),
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Right Total Count Cell
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 30.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (totalCount > 0) Color(0xFF1E293B) else UnselectedCellBg)
                .border(0.5.dp, if (totalCount > 0) CasinoGold else CellBorder, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$totalCount",
                color = if (totalCount > 0) CasinoGoldLight else TextMuted,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}

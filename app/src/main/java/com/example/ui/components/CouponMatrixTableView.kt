package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Coupon
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.model.TotoMatch
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
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

// Matrix colors for Spor Toto coupon table
private val TableBlue = Color(0xFF1565C0)
private val TableRed = Color(0xFFC62828)
private val TableGreen = Color(0xFF2E7D32)
private val TableMatchWinGreen = Color(0xFF16A34A)
private val TableMatchLossRed = Color(0xFF991B1B)
private val CellBorderColor = Color(0x33000000)

/**
 * Authentic Spor Toto Matrix Table with Live Match Results & Active Coupons Tracker.
 * Shows match outcomes as they are played and how many columns are still alive for 15, 14, 13, and 12.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CouponMatrixTableView(
    coupon: Coupon,
    matches: List<TotoMatch> = emptyList(),
    onDeleteCoupon: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayedCount = if (isExpanded) coupon.totalColumns else minOf(10, coupon.totalColumns)

    // Live Match Outcomes map (Match index 0..14 -> Outcome? null means upcoming)
    var liveResults by remember(coupon.id) {
        mutableStateOf(mapOf<Int, PredictionOutcome?>())
    }
    var showLiveSimControls by remember { mutableStateOf(false) }

    // 1. Calculate Live Stats for all columns
    val finishedMatchesCount = liveResults.count { it.value != null }
    val columnLiveStats = remember(coupon.columns, liveResults) {
        coupon.columns.map { col ->
            var correct = 0
            var wrong = 0
            for (i in 0 until 15) {
                val actual = liveResults[i]
                val predicted = col.predictions.getOrNull(i)
                if (actual != null && predicted != null) {
                    if (actual == predicted) {
                        correct++
                    } else {
                        wrong++
                    }
                }
            }
            Pair(correct, wrong)
        }
    }

    // Active Columns remaining calculations
    val totalCols = coupon.totalColumns
    val aliveFor15 = columnLiveStats.count { it.second == 0 }
    val aliveFor14 = columnLiveStats.count { it.second <= 1 }
    val aliveFor13 = columnLiveStats.count { it.second <= 2 }
    val aliveFor12 = columnLiveStats.count { it.second <= 3 }
    val bustedCols = columnLiveStats.count { it.second > 3 }

    // Compute primary summary label for each of the 15 matches (e.g. "1", "1X", "1X2", "X2")
    val matchPrimaryTags = remember(coupon, matches) {
        (0 until 15).map { matchIdx ->
            val match = matches.getOrNull(matchIdx)
            if (match != null && match.selections.isNotEmpty()) {
                val has1 = match.selections.contains(PredictionOutcome.HOME)
                val hasX = match.selections.contains(PredictionOutcome.DRAW)
                val has2 = match.selections.contains(PredictionOutcome.AWAY)
                buildTag(has1, hasX, has2)
            } else {
                val outcomes = coupon.columns.mapNotNull { it.predictions.getOrNull(matchIdx) }.toSet()
                val has1 = outcomes.contains(PredictionOutcome.HOME)
                val hasX = outcomes.contains(PredictionOutcome.DRAW)
                val has2 = outcomes.contains(PredictionOutcome.AWAY)
                buildTag(has1, hasX, has2)
            }
        }
    }

    // Match display titles (e.g. "1:FRANSA-TÜRKİYE")
    val matchTitles = remember(matches) {
        (0 until 15).map { idx ->
            val match = matches.getOrNull(idx)
            if (match != null) {
                "${idx + 1}:${match.homeTeam.uppercase()}-${match.awayTeam.uppercase()}"
            } else {
                "${idx + 1}:MAÇ ${idx + 1}"
            }
        }
    }

    val cellWidth = 20.dp
    val headerHeight = 175.dp
    val cellHeight = 25.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("coupon_matrix_table_view")
    ) {
        // 1. CANLI KUPON TAKİP VE DEVAM EDEN KOLONLAR KARTI (Requested by user)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2F)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, CasinoGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                                .background(Color(0xFFDC2626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("●", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CANLI MAÇ & KUPON TAKİBİ",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { showLiveSimControls = !showLiveSimControls }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showLiveSimControls) "Paneli Kapat ▲" else "Skor Simülasyonu ▼",
                            color = CasinoGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (finishedMatchesCount == 0) {
                        "Maçlar henüz başlamadı (15 maç beklemede). Skor simülasyonu ile kupon gidişatını test edebilirsiniz."
                    } else {
                        "$finishedMatchesCount / 15 Maç Sonuçlandı. Kuponlarınızın anlık ikramiye durumu:"
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Active Coupons Counters (15, 14, 13, 12)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveTierCounter(
                        tierName = "15 Yolunda",
                        aliveCount = aliveFor15,
                        totalCount = totalCols,
                        badgeColor = CasinoGold,
                        modifier = Modifier.weight(1f)
                    )
                    LiveTierCounter(
                        tierName = "14 Garanti",
                        aliveCount = aliveFor14,
                        totalCount = totalCols,
                        badgeColor = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    LiveTierCounter(
                        tierName = "13 Yolunda",
                        aliveCount = aliveFor13,
                        totalCount = totalCols,
                        badgeColor = ElectricGreen,
                        modifier = Modifier.weight(1f)
                    )
                    LiveTierCounter(
                        tierName = "Yanan / Elenen",
                        aliveCount = bustedCols,
                        totalCount = totalCols,
                        badgeColor = if (bustedCols > 0) VibrantRed else TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Interactive Live Score Simulation Bar (Appears when toggled)
                AnimatedVisibility(visible = showLiveSimControls) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF1E293B))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Maç Sonucu Değiştirmek İçin Numaralara Dokunun (1 ➔ X ➔ 2 ➔ -):",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 15 match result chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (0 until 15).forEach { mIdx ->
                                val res = liveResults[mIdx]
                                val label = res?.code ?: "-"
                                val chipBg = when (res) {
                                    PredictionOutcome.HOME -> Color(0xFF1E3A8A)
                                    PredictionOutcome.DRAW -> Color(0xFF0F766E)
                                    PredictionOutcome.AWAY -> Color(0xFF7C2D12)
                                    null -> Color(0xFF1E293B)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(chipBg)
                                        .border(0.8.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                        .clickable {
                                            val next = when (res) {
                                                null -> PredictionOutcome.HOME
                                                PredictionOutcome.HOME -> PredictionOutcome.DRAW
                                                PredictionOutcome.DRAW -> PredictionOutcome.AWAY
                                                PredictionOutcome.AWAY -> null
                                            }
                                            val updated = liveResults.toMutableMap()
                                            if (next != null) updated[mIdx] = next else updated.remove(mIdx)
                                            liveResults = updated
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "M${mIdx + 1}:$label",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Quick simulation: Random 6 matches finished
                            OutlinedButton(
                                onClick = {
                                    val sim = mutableMapOf<Int, PredictionOutcome?>()
                                    (0 until 15).forEach { i ->
                                        if (i < 8) {
                                            val match = matches.getOrNull(i)
                                            val h = match?.probability?.home?.firstOrNull() ?: 50
                                            val a = match?.probability?.away?.firstOrNull() ?: 25
                                            sim[i] = if (h > a) PredictionOutcome.HOME else PredictionOutcome.DRAW
                                        }
                                    }
                                    liveResults = sim
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("🎲 8 Maç Oynandı", fontSize = 10.5.sp)
                            }

                            // Quick simulation: All favorites won
                            OutlinedButton(
                                onClick = {
                                    val sim = mutableMapOf<Int, PredictionOutcome?>()
                                    (0 until 15).forEach { i ->
                                        val match = matches.getOrNull(i)
                                        val h = match?.probability?.home?.firstOrNull() ?: 50
                                        val a = match?.probability?.away?.firstOrNull() ?: 25
                                        sim[i] = if (h > a) PredictionOutcome.HOME else PredictionOutcome.AWAY
                                    }
                                    liveResults = sim
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("✅ Tüm Favoriler", fontSize = 10.5.sp)
                            }

                            // Reset
                            OutlinedButton(
                                onClick = { liveResults = emptyMap() },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(34.dp)
                            ) {
                                Text("🔄 Sıfırla", fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. STYLED SPORTSBOOK INFO BANNER (Replaces plain white card to match dark theme)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D30)),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22314A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${coupon.totalColumns} kolondan $displayedCount tanesi listelenmiştir.",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onDeleteCoupon() }
                        .testTag("btn_table_delete_coupon")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = VibrantRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Kuponu Sil",
                        fontSize = 12.sp,
                        color = VibrantRed,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. SCROLLABLE MATRIX TABLE (18 COLUMNS: Kupon No + 15 Matches + Doğru + Yanlış)
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 2.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width((cellWidth + 2.dp) * 18 + 4.dp)
            ) {
                // ROW A: TOP MINI SELECTION TAGS (Ana, 1, 1X2, ..., 0, 0)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Col 0: "Ana"
                    TextCell(
                        text = "Ana",
                        width = cellWidth,
                        height = 22.dp,
                        backgroundColor = TableBlue,
                        textColor = Color.White,
                        fontSize = 8.5.sp
                    )

                    // Cols 1..15: Selection tag (1, 1X, 1X2, etc.)
                    matchPrimaryTags.forEach { tag ->
                        TextCell(
                            text = tag,
                            width = cellWidth,
                            height = 22.dp,
                            backgroundColor = TableBlue,
                            textColor = Color.White,
                            fontSize = 8.sp
                        )
                    }

                    // Col 16: Doğru
                    TextCell(
                        text = "D",
                        width = cellWidth,
                        height = 22.dp,
                        backgroundColor = TableGreen,
                        textColor = Color.White,
                        fontSize = 9.sp
                    )

                    // Col 17: Yanlış
                    TextCell(
                        text = "Y",
                        width = cellWidth,
                        height = 22.dp,
                        backgroundColor = TableRed,
                        textColor = Color.White,
                        fontSize = 9.sp
                    )
                }

                // ROW B: VERTICAL ROTATED HEADERS (KUPON NUMARASI, Match Names, DOĞRU, YANLIŞ)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Col 0: "KUPON NUMARASI"
                    VerticalHeaderCanvasCell(
                        text = "KUPON NUMARASI",
                        width = cellWidth,
                        height = headerHeight,
                        backgroundColor = TableRed
                    )

                    // Cols 1..15: "1:FRANSA-TÜRKİYE", etc.
                    matchTitles.forEach { title ->
                        VerticalHeaderCanvasCell(
                            text = title,
                            width = cellWidth,
                            height = headerHeight,
                            backgroundColor = TableBlue
                        )
                    }

                    // Col 16: "DOĞRU MAÇ SAYISI"
                    VerticalHeaderCanvasCell(
                        text = "DOĞRU MAÇ SAYISI",
                        width = cellWidth,
                        height = headerHeight,
                        backgroundColor = TableGreen
                    )

                    // Col 17: "YANLIŞ MAÇ SAYISI"
                    VerticalHeaderCanvasCell(
                        text = "YANLIŞ MAÇ SAYISI",
                        width = cellWidth,
                        height = headerHeight,
                        backgroundColor = TableRed
                    )
                }

                // DATA ROWS 1..10 (with Live Results Highlight!)
                val firstBatch = coupon.columns.take(10)
                firstBatch.forEachIndexed { idx, column ->
                    val stats = columnLiveStats.getOrElse(idx) { Pair(0, 0) }
                    DataRowView(
                        column = column,
                        liveResults = liveResults,
                        correctCount = stats.first,
                        wrongCount = stats.second,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight
                    )
                }

                // EXPAND / COLLAPSE BUTTON (If coupon has more than 10 columns)
                if (coupon.totalColumns > 10) {
                    Button(
                        onClick = { isExpanded = !isExpanded },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TableRed
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .padding(vertical = 2.dp)
                            .testTag("btn_toggle_matrix_columns")
                    ) {
                        Text(
                            text = if (isExpanded) "KOLONLARI DEVAMINI GİZLE..." else "KOLONLARI DEVAMINI GÖSTER...",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // REMAINING ROWS (11..totalColumns) WHEN EXPANDED
                    if (isExpanded) {
                        val remainingBatch = coupon.columns.drop(10)
                        remainingBatch.forEachIndexed { rIdx, column ->
                            val actualIndex = 10 + rIdx
                            val stats = columnLiveStats.getOrElse(actualIndex) { Pair(0, 0) }
                            DataRowView(
                                column = column,
                                liveResults = liveResults,
                                correctCount = stats.first,
                                wrongCount = stats.second,
                                cellWidth = cellWidth,
                                cellHeight = cellHeight
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTierCounter(
    tierName: String,
    aliveCount: Int,
    totalCount: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val pct = if (totalCount > 0) ((aliveCount.toDouble() / totalCount) * 100).toInt() else 0

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.12f))
            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tierName,
                color = badgeColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$aliveCount",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                text = "%$pct",
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun DataRowView(
    column: TotoColumn,
    liveResults: Map<Int, PredictionOutcome?>,
    correctCount: Int,
    wrongCount: Int,
    cellWidth: Dp,
    cellHeight: Dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Col 0: Column Index (Red box)
        TextCell(
            text = "${column.index}",
            width = cellWidth,
            height = cellHeight,
            backgroundColor = TableRed,
            textColor = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        // Cols 1..15: Predictions (Color coded based on live score status!)
        (0 until 15).forEach { matchIdx ->
            val outcome = column.predictions.getOrNull(matchIdx)
            val code = outcome?.code ?: "-"
            val actual = liveResults[matchIdx]

            // Dynamic color feedback: Green if correct, Red/faded if wrong, Standard blue if pending
            val (bgColor, txtColor) = when {
                actual == null -> Pair(TableBlue, Color.White)
                actual == outcome -> Pair(TableMatchWinGreen, Color.White)
                else -> Pair(TableMatchLossRed, Color(0xFFFCA5A5))
            }

            TextCell(
                text = code,
                width = cellWidth,
                height = cellHeight,
                backgroundColor = bgColor,
                textColor = txtColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Col 16: Doğru (Green box displaying real correct count!)
        TextCell(
            text = "$correctCount",
            width = cellWidth,
            height = cellHeight,
            backgroundColor = TableGreen,
            textColor = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black
        )

        // Col 17: Yanlış (Red box displaying real wrong count!)
        TextCell(
            text = "$wrongCount",
            width = cellWidth,
            height = cellHeight,
            backgroundColor = TableRed,
            textColor = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun TextCell(
    text: String,
    width: Dp,
    height: Dp,
    backgroundColor: Color,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .border(0.5.dp, CellBorderColor, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Pixel-perfect vertical rotated text cell drawn via Native Canvas with -90 degree rotation.
 * Renders text vertically from bottom to top, exactly matching the screenshot.
 */
@Composable
private fun VerticalHeaderCanvasCell(
    text: String,
    width: Dp,
    height: Dp,
    backgroundColor: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { 8.5.sp.toPx() }

    val paint = remember(textSizePx, textColor) {
        Paint().apply {
            color = textColor.toArgb()
            textSize = textSizePx
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
    }

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .border(0.5.dp, CellBorderColor, RoundedCornerShape(3.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawContext.canvas.nativeCanvas.apply {
                save()
                translate(size.width / 2f + (textSizePx / 3.2f), size.height - 8f)
                rotate(-90f)
                drawText(text, 0f, 0f, paint)
                restore()
            }
        }
    }
}

private fun buildTag(has1: Boolean, hasX: Boolean, has2: Boolean): String {
    return buildString {
        if (has1) append("1")
        if (hasX) append("X")
        if (has2) append("2")
        if (isEmpty()) append("-")
    }
}

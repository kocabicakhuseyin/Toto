package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.TotoMatch

// Exact colors matching the Spor Toto Hedef 15 table screenshot
private val TableBlue = Color(0xFF1565C0)
private val TableRed = Color(0xFFC62828)
private val TableGreen = Color(0xFF2E7D32)
private val CellBorderColor = Color(0x33000000)

/**
 * Authentic Spor Toto Matrix Table component displaying 15-match coupon columns,
 * vertical rotated team headers, primary selections, and Doğru/Yanlış counters.
 */
@Composable
fun CouponMatrixTableView(
    coupon: Coupon,
    matches: List<TotoMatch> = emptyList(),
    onDeleteCoupon: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayedCount = if (isExpanded) coupon.totalColumns else minOf(10, coupon.totalColumns)

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
                // Derive from coupon column distinct values
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
        // 1. TOP INFO BANNER: "64 kolondan 10 tanesi listelenmiştir. (Kuponu Sil)"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${coupon.totalColumns} kolondan $displayedCount tanesi listelenmiştir. ",
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "(Kuponu Sil)",
                    fontSize = 14.sp,
                    color = TableRed,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { onDeleteCoupon() }
                        .testTag("btn_table_delete_coupon")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. SCROLLABLE MATRIX TABLE (18 COLUMNS: Kupon No + 15 Matches + Doğru + Yanlış)
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

                    // Col 16: Doğru "0"
                    TextCell(
                        text = "0",
                        width = cellWidth,
                        height = 22.dp,
                        backgroundColor = TableGreen,
                        textColor = Color.White,
                        fontSize = 9.sp
                    )

                    // Col 17: Yanlış "0"
                    TextCell(
                        text = "0",
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

                // DATA ROWS 1..10
                val firstBatch = coupon.columns.take(10)
                firstBatch.forEach { column ->
                    DataRowView(
                        column = column,
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
                        remainingBatch.forEach { column ->
                            DataRowView(
                                column = column,
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
private fun DataRowView(
    column: com.example.model.TotoColumn,
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

        // Cols 1..15: Predictions (Blue boxes)
        (0 until 15).forEach { matchIdx ->
            val outcome = column.predictions.getOrNull(matchIdx)
            val code = outcome?.code ?: "-"
            TextCell(
                text = code,
                width = cellWidth,
                height = cellHeight,
                backgroundColor = TableBlue,
                textColor = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Col 16: Doğru (Green box)
        TextCell(
            text = "0",
            width = cellWidth,
            height = cellHeight,
            backgroundColor = TableGreen,
            textColor = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )

        // Col 17: Yanlış (Red box)
        TextCell(
            text = "0",
            width = cellWidth,
            height = cellHeight,
            backgroundColor = TableRed,
            textColor = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
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
                // Move origin to the bottom center of the cell
                // Rotate -90 degrees so text flows vertically upwards from bottom to top
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

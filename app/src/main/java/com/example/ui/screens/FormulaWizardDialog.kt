package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.GuaranteeFormulaEngine
import com.example.model.FormulaType
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
import com.example.ui.theme.VibrantRed

data class FormulaProjection(
    val formulaType: FormulaType,
    val projectedColumns: Int,
    val projectedCostTL: Double,
    val savingsPercentage: Int,
    val guaranteeTier: Int,
    val guaranteeCoveragePercentage: Double,
    val chanceOf15Percentage: Double
)

@Composable
fun FormulaWizardDialog(
    initialFormula: FormulaType = FormulaType.GUARANTEE_14,
    matches: List<TotoMatch> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmFormula: (FormulaType) -> Unit
) {
    var selectedFormula by remember { mutableStateOf(initialFormula) }

    // 1. Calculate matrix selections summary from user picks
    val (totalSelected, selectionBreakdown, rawCombinations) = remember(matches) {
        var bankos = 0
        var doubles = 0
        var triples = 0
        var combinations = 1

        val list = matches.take(15)
        list.forEach { match ->
            when (match.selections.size) {
                0, 1 -> {
                    bankos++
                    combinations *= 1
                }
                2 -> {
                    doubles++
                    combinations = (combinations * 2).coerceAtMost(50_000)
                }
                else -> {
                    triples++
                    combinations = (combinations * 3).coerceAtMost(50_000)
                }
            }
        }
        val count = list.count { it.selections.isNotEmpty() }
        Triple(count, Triple(bankos, doubles, triples), combinations)
    }

    val (bankoCount, doubleCount, tripleCount) = selectionBreakdown

    // 2. Pre-calculate live projections for each formula tier
    val projections: Map<FormulaType, FormulaProjection> = remember(rawCombinations, doubleCount, tripleCount, matches) {
        val fullCombs = rawCombinations.coerceAtLeast(1)
        // Keep the dialog fast: do not run the expensive covering algorithm during composition.
        // The precise column count is determined only when the user generates a coupon.
        val smartCap = com.example.engine.FormulaEngine.calculateSmartCap(matches)
        val n1Cols = if (fullCombs <= 2) fullCombs
            else (fullCombs / (1 + doubleCount + 2 * tripleCount).coerceAtLeast(1)).coerceIn(1, smartCap)
        val n1Savings = if (fullCombs > 0) (((fullCombs - n1Cols).toDouble() / fullCombs) * 100).toInt().coerceAtLeast(0) else 0
        val n1Chance15 = (n1Cols.toDouble() / fullCombs * 100.0).coerceIn(0.0, 100.0)

        // 13 Garanti (N-2) projection
        val n2Cols = (n1Cols / 2).coerceIn(1, 24)
        val n2Savings = if (fullCombs > 0) (((fullCombs - n2Cols).toDouble() / fullCombs) * 100).toInt().coerceAtLeast(0) else 0
        val n2Chance15 = (n2Cols.toDouble() / fullCombs * 100.0).coerceIn(0.0, 100.0)

        // 12 Garanti (N-3) projection
        val n3Cols = (n2Cols / 2).coerceIn(1, 12)
        val n3Savings = if (fullCombs > 0) (((fullCombs - n3Cols).toDouble() / fullCombs) * 100).toInt().coerceAtLeast(0) else 0
        val n3Chance15 = (n3Cols.toDouble() / fullCombs * 100.0).coerceIn(0.0, 100.0)

        mapOf(
            FormulaType.GUARANTEE_15 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_15,
                projectedColumns = fullCombs.coerceAtMost(smartCap),
                projectedCostTL = fullCombs.coerceAtMost(smartCap) * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = (((fullCombs - fullCombs.coerceAtMost(smartCap)).toDouble() / fullCombs) * 100).toInt(),
                guaranteeTier = 15,
                guaranteeCoveragePercentage = 0.0,
                chanceOf15Percentage = fullCombs.coerceAtMost(smartCap).toDouble() / fullCombs * 100.0
            ),
            FormulaType.GUARANTEE_14 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_14,
                projectedColumns = n1Cols,
                projectedCostTL = n1Cols * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = n1Savings,
                guaranteeTier = 14,
                guaranteeCoveragePercentage = 0.0,
                chanceOf15Percentage = n1Chance15
            ),
            FormulaType.GUARANTEE_13 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_13,
                projectedColumns = n2Cols,
                projectedCostTL = n2Cols * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = n2Savings,
                guaranteeTier = 13,
                guaranteeCoveragePercentage = 0.0,
                chanceOf15Percentage = n2Chance15
            ),
            FormulaType.GUARANTEE_12 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_12,
                projectedColumns = n3Cols,
                projectedCostTL = n3Cols * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = n3Savings,
                guaranteeTier = 12,
                guaranteeCoveragePercentage = 0.0,
                chanceOf15Percentage = n3Chance15
            )
        )
    }

    val currentProjection = projections[selectedFormula] ?: projections[FormulaType.GUARANTEE_14]!!

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color(0xFF0B1322),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, CasinoGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                // Top Header Bar (Matching Pro Sportsbook dark theme)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF131D30), Color(0xFF1C2A42))
                            )
                        )
                        .border(BorderStroke(1.dp, DarkCardBorder))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CasinoGold.copy(alpha = 0.2f))
                                    .border(1.dp, CasinoGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎯", fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SPOR TOTO GARANTİLİ FORMÜL",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 2
                                )
                                Text(
                                    text = "Garantili İndirgeme ve Başarı İstatistik Paneli",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 14.sp,
                                    maxLines = 2
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_close_wizard_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.White
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(14.dp)) {

                    // 1. SEÇİM VE MATRİS ÖZET KARTI
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D30)),
                        border = BorderStroke(1.dp, Color(0xFF22314A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QueryStats,
                                        contentDescription = null,
                                        tint = CasinoGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "15 Maç Tercih Matrisi:",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }

                                Text(
                                    text = "$totalSelected / 15 Maç Seçildi",
                                    color = if (totalSelected == 15) ElectricGreen else NeonAmber,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Breakdown Chips: Banko, Çifte Şans, Kapatma
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SelectionBadge(label = "Banko", count = bankoCount, color = ElectricGreen, modifier = Modifier.weight(1f))
                                SelectionBadge(label = "Çifte Şans", count = doubleCount, color = ElectricCyan, modifier = Modifier.weight(1f))
                                SelectionBadge(label = "Kapatma (3)", count = tripleCount, color = Color(0xFFA855F7), modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Allow the combination count and cost to use the full card width.
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "İndirgemesiz tam kombinasyon",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "$rawCombinations kolon • ${String.format("%.2f", rawCombinations * GuaranteeFormulaEngine.COLUMN_FEE_TL)} ₺",
                                    color = CasinoGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. DETAYLI İSTATİSTİK VE BAŞARI ŞANSI PANELİ (Requested by user)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2338)),
                        border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${selectedFormula.title} • Önizleme",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        lineHeight = 19.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Yaklaşık %${currentProjection.savingsPercentage} kolon tasarrufu",
                                    color = ElectricGreenGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Kolon ve Maliyet Kıyaslama Kutusu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0B1728))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Tahmini kolon", color = TextSecondary, fontSize = 10.5.sp)
                                    Text(
                                        text = "${currentProjection.projectedColumns} kolon",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(0.8f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Birim fiyat", color = TextSecondary, fontSize = 10.5.sp)
                                    Text(text = "0.20 ₺", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(text = "Tahmini tutar", color = TextSecondary, fontSize = 10.5.sp)
                                    Text(
                                        text = "${String.format("%.2f", currentProjection.projectedCostTL)} ₺",
                                        color = ElectricGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Kapsama Özeti (Tahmini)",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // A preview is not a verified guarantee over the full matrix.
                            SuccessChanceBar(
                                label = "15 tam eşleşen kolon payı",
                                percentage = currentProjection.chanceOf15Percentage,
                                color = CasinoGold,
                                badgeText = "TAHMİNİ"
                            )

                            Spacer(modifier = Modifier.height(9.dp))

                            Text(
                                text = "Hedef: ${currentProjection.guaranteeTier} doğru. Önizleme tahminidir; kolon sınırı nedeniyle tüm kombinasyonların kapsandığı doğrulanmış değildir.",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Formül Seçeneğinizi Belirleyin:",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. FORMÜL SEÇENEKLERİ (15, 14, 13, 12 Garanti)
                    val guaranteeTiers = listOf(
                        FormulaType.GUARANTEE_15,
                        FormulaType.GUARANTEE_14,
                        FormulaType.GUARANTEE_13,
                        FormulaType.GUARANTEE_12
                    )

                    guaranteeTiers.forEach { formula ->
                        val isSelected = selectedFormula == formula
                        val proj = projections[formula] ?: return@forEach

                        val tierColor = when (formula) {
                            FormulaType.GUARANTEE_15 -> CasinoGold
                            FormulaType.GUARANTEE_14 -> ElectricCyan
                            FormulaType.GUARANTEE_13 -> ElectricGreen
                            FormulaType.GUARANTEE_12 -> Color(0xFFA855F7)
                            else -> ElectricCyan
                        }
                        val explanation = when (formula) {
                            FormulaType.GUARANTEE_15 -> "Tam kapsamayı hedefler; kolon limiti uygulanabilir"
                            FormulaType.GUARANTEE_14 -> "Bir hata toleransını hedefleyen indirgeme"
                            FormulaType.GUARANTEE_13 -> "İki hata toleransını hedefleyen indirgeme"
                            FormulaType.GUARANTEE_12 -> "Üç hata toleransını hedefleyen indirgeme"
                            else -> formula.description
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF16253D) else Color(0xFF131D30))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) tierColor else DarkCardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedFormula = formula }
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFormula = formula },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = tierColor,
                                        unselectedColor = TextMuted
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formula.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = explanation,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                    Text(
                                        text = "Tahmini: ${proj.projectedColumns} kolon • ${String.format("%.2f", proj.projectedCostTL)} ₺",
                                        color = tierColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                }
                }

                // Always-visible confirmation bar; options above remain scrollable.
                Surface(
                    color = Color(0xFF111B2D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(0.28f)
                                .heightIn(min = 56.dp)
                        ) {
                            Text("Vazgeç", color = TextSecondary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onConfirmFormula(selectedFormula)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.72f)
                                .heightIn(min = 56.dp)
                                .testTag("btn_confirm_wizard_formula")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Bu Formülle Kupon Üret",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Tahmini: ${currentProjection.projectedColumns} kolon • ${String.format("%.2f", currentProjection.projectedCostTL)} ₺",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
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

@Composable
private fun SelectionBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$count ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SuccessChanceBar(
    label: String,
    percentage: Double,
    color: Color,
    badgeText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${String.format("%.1f", percentage)}%",
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.18f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(text = badgeText, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percentage / 100.0).toFloat().coerceIn(0.0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

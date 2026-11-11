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
        val fullCost = fullCombs * GuaranteeFormulaEngine.COLUMN_FEE_TL

        // 14 Garanti (N-1) using algorithmic covering
        val smartCap = com.example.engine.FormulaEngine.calculateSmartCap(matches)
        val n1Result = if (matches.isNotEmpty()) {
            GuaranteeFormulaEngine.generateNMinus1Columns(matches, maxColumnsCap = smartCap)
        } else null

        val n1Cols = n1Result?.reducedColumns?.size ?: (fullCombs / (1 + doubleCount + 2 * tripleCount).coerceAtLeast(1)).coerceIn(1, smartCap)
        val n1Savings = if (fullCombs > 0) (((fullCombs - n1Cols).toDouble() / fullCombs) * 100).toInt().coerceAtLeast(0) else 0
        val n1Chance15 = if (fullCombs > 0) ((n1Cols.toDouble() / fullCombs) * 100.0).coerceIn(1.0, 100.0) else 100.0

        // 13 Garanti (N-2) projection
        val n2Cols = (n1Cols / 2).coerceIn(1, 24)
        val n2Savings = if (fullCombs > 0) (((fullCombs - n2Cols).toDouble() / fullCombs) * 100).toInt().coerceAtLeast(0) else 0
        val n2Chance15 = if (fullCombs > 0) ((n2Cols.toDouble() / fullCombs) * 100.0).coerceIn(0.5, 50.0) else 50.0

        // 12 Garanti (N-3) projection
        val n3Cols = (n2Cols / 2).coerceIn(1, 12)
        val n3Savings = if (fullCombs > 0) (((fullCombs - n3Cols).toDouble() / fullCombs) * 100).toInt().coerceAtLeast(0) else 0
        val n3Chance15 = if (fullCombs > 0) ((n3Cols.toDouble() / fullCombs) * 100.0).coerceIn(0.1, 25.0) else 25.0

        mapOf(
            FormulaType.GUARANTEE_15 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_15,
                projectedColumns = fullCombs.coerceAtMost(256),
                projectedCostTL = fullCombs.coerceAtMost(256) * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = 0,
                guaranteeTier = 15,
                guaranteeCoveragePercentage = 100.0,
                chanceOf15Percentage = 100.0
            ),
            FormulaType.GUARANTEE_14 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_14,
                projectedColumns = n1Cols,
                projectedCostTL = n1Cols * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = n1Savings,
                guaranteeTier = 14,
                guaranteeCoveragePercentage = 100.0,
                chanceOf15Percentage = n1Chance15
            ),
            FormulaType.GUARANTEE_13 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_13,
                projectedColumns = n2Cols,
                projectedCostTL = n2Cols * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = n2Savings,
                guaranteeTier = 13,
                guaranteeCoveragePercentage = 100.0,
                chanceOf15Percentage = n2Chance15
            ),
            FormulaType.GUARANTEE_12 to FormulaProjection(
                formulaType = FormulaType.GUARANTEE_12,
                projectedColumns = n3Cols,
                projectedCostTL = n3Cols * GuaranteeFormulaEngine.COLUMN_FEE_TL,
                savingsPercentage = n3Savings,
                guaranteeTier = 12,
                guaranteeCoveragePercentage = 100.0,
                chanceOf15Percentage = n3Chance15
            )
        )
    }

    val currentProjection = projections[selectedFormula] ?: projections[FormulaType.GUARANTEE_14]!!

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            color = Color(0xFF0B1322),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, CasinoGold.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Column {
                                Text(
                                    text = "SPOR TOTO GARANTİLİ FORMÜL",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Garantili İndirgeme ve Başarı İstatistik Paneli",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
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

                            // Raw Combinations vs Cost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "İndirgemesiz Tam Kombinasyon:",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "$rawCombinations Kolon (${String.format("%.2f", rawCombinations * GuaranteeFormulaEngine.COLUMN_FEE_TL)} ₺)",
                                    color = CasinoGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedFormula.title} Başarı Analizi",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF064E3B))
                                        .border(1.dp, ElectricGreen, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "%${currentProjection.savingsPercentage} Tasarruf",
                                        color = ElectricGreenGlow,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.5.sp
                                    )
                                }
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
                                Column {
                                    Text(text = "Üretilecek Kolon:", color = TextMuted, fontSize = 10.5.sp)
                                    Text(
                                        text = "${currentProjection.projectedColumns} Kolon",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Birim Kolon:", color = TextMuted, fontSize = 10.5.sp)
                                    Text(text = "0.20 ₺", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Kupon Tutarı:", color = TextMuted, fontSize = 10.5.sp)
                                    Text(
                                        text = "${String.format("%.2f", currentProjection.projectedCostTL)} ₺",
                                        color = ElectricGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "İkramiye Başarı Oranları (Seçimler Tuttuğunda):",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 15 Başarı İhtimali Çubuğu
                            SuccessChanceBar(
                                label = "15 Bilme Şansı (Büyük İkramiye)",
                                percentage = currentProjection.chanceOf15Percentage,
                                color = CasinoGold,
                                badgeText = if (currentProjection.guaranteeTier == 15) "TAM KAPSAM" else "İHTİMALLİ"
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 14 Garanti Çubuğu
                            SuccessChanceBar(
                                label = "14 Başarı Oranı (N-1 Kapsamı)",
                                percentage = if (currentProjection.guaranteeTier <= 14) 100.0 else 45.0,
                                color = ElectricCyan,
                                badgeText = if (currentProjection.guaranteeTier <= 14) "%100 KESİN GARANTİ" else "İHTİMALLİ"
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 13 ve 12 Garanti Çubuğu
                            SuccessChanceBar(
                                label = "13 & 12 Alt Kademe Garantisi",
                                percentage = 100.0,
                                color = ElectricGreen,
                                badgeText = "%100 KESİN"
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

                        val (tierColor, badgeText) = when (formula) {
                            FormulaType.GUARANTEE_15 -> Pair(CasinoGold, "Tam Kapsama")
                            FormulaType.GUARANTEE_14 -> Pair(ElectricCyan, "%${proj.savingsPercentage} Tasarruf")
                            FormulaType.GUARANTEE_13 -> Pair(ElectricGreen, "%${proj.savingsPercentage} Tasarruf")
                            FormulaType.GUARANTEE_12 -> Pair(Color(0xFFA855F7), "%${proj.savingsPercentage} Tasarruf")
                            else -> Pair(ElectricCyan, "")
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

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = formula.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(tierColor.copy(alpha = 0.2f))
                                                .border(1.dp, tierColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${proj.projectedColumns} Kolon (${String.format("%.2f", proj.projectedCostTL)} ₺)",
                                                color = tierColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = formula.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. ACTION BUTTONS
                    Button(
                        onClick = {
                            onConfirmFormula(selectedFormula)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_confirm_wizard_formula")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bu Formülle Kupon Üret (${currentProjection.projectedColumns} Kolon - ${String.format("%.2f", currentProjection.projectedCostTL)} ₺)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Vazgeç", color = TextMuted, fontSize = 12.5.sp)
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${String.format("%.1f", percentage)}%",
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = badgeText, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percentage / 100.0).toFloat().coerceIn(0.02f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

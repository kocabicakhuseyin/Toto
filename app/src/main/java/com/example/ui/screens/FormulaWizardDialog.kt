package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FormulaType
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FormulaWizardDialog(
    initialFormula: FormulaType = FormulaType.GUARANTEE_14,
    onDismiss: () -> Unit,
    onConfirmFormula: (FormulaType) -> Unit
) {
    var selectedFormula by remember { mutableStateOf(initialFormula) }

    val guaranteeTiers = listOf(
        FormulaType.GUARANTEE_15,
        FormulaType.GUARANTEE_14,
        FormulaType.GUARANTEE_13,
        FormulaType.GUARANTEE_12
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Garantili Formül Seçimi", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(text = "12, 13, 14 ve 15 Garanti İndirgeme Tiers", fontSize = 11.sp, color = TextMuted)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                guaranteeTiers.forEach { formula ->
                    val isSelected = selectedFormula == formula
                    val (tierColor, savingsText) = when (formula) {
                        FormulaType.GUARANTEE_15 -> Pair(NeonAmber, "Tam Kapsama")
                        FormulaType.GUARANTEE_14 -> Pair(ElectricCyan, "%80 Tasarruf")
                        FormulaType.GUARANTEE_13 -> Pair(ElectricGreen, "%92 Tasarruf")
                        FormulaType.GUARANTEE_12 -> Pair(Color(0xFFA855F7), "%97 Tasarruf")
                        else -> Pair(ElectricCyan, "")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(
                                1.dp,
                                if (isSelected) tierColor else Color(0xFFE2E8F0),
                                RoundedCornerShape(10.dp)
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

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = formula.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) tierColor else TextPrimary
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tierColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(text = savingsText, color = tierColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = formula.description,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmFormula(selectedFormula)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_wizard_formula")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bu Formülle Kupon Üret", color = Color.White, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = TextMuted)
            }
        }
    )
}

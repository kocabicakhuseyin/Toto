package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.GooglePlayBillingManager
import com.example.model.GooglePlayPackage
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GooglePlayBillingDialog(
    currentBalanceTL: Double,
    requiredFeeTL: Double? = null,
    onDismiss: () -> Unit,
    onPurchaseSuccess: (GooglePlayPackage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPackage by remember { mutableStateOf(GooglePlayBillingManager.packages[1]) } // 50 TL default
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CasinoGold, Color(0xFFD97706))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Google Play",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Google Play Bakiye Yükle",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Her 1 Kolon = 0.20 ₺ (20 Kuruş)",
                        fontSize = 11.sp,
                        color = CasinoGoldLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Balance Banner & Shortage Warning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Mevcut Bakiyeniz",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "%.2f ₺".format(currentBalanceTL),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = CasinoGold
                            )
                        }

                        if (requiredFeeTL != null && currentBalanceTL < requiredFeeTL) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Gereken Kolon Bedeli",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                                Text(
                                    text = "%.2f ₺".format(requiredFeeTL),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Bakiye Paketi Seçiniz:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                // Packages List
                GooglePlayBillingManager.packages.forEach { pkg ->
                    val isSelected = selectedPackage.id == pkg.id
                    val borderColor = if (isSelected) CasinoGold else DarkCardBorder
                    val bgColor = if (isSelected) Color(0xFF261D10) else Color(0xFF0D1424)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(1.2.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable(enabled = !isProcessing) { selectedPackage = pkg }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("package_${pkg.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPackage = pkg },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CasinoGold,
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
                                        text = pkg.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) CasinoGold else TextPrimary
                                    )

                                    if (pkg.bonusBadge != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ElectricGreen.copy(alpha = 0.2f))
                                                .border(0.8.dp, ElectricGreen, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = pkg.bonusBadge,
                                                color = ElectricGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${pkg.columnCount} Kolon Oynama Hakkı",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    Text(
                                        text = "%.2f ₺".format(pkg.amountTL),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = if (isSelected) CasinoGold else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Security & Google Play In-App Purchase Guarantee
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ElectricGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google Play Güvenli Ödeme Altyapısı (Anında Bakiye Yüklenir)",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isProcessing = true
                        delay(1200) // Realistic Google Play Billing sheet interaction
                        isProcessing = false
                        onPurchaseSuccess(selectedPackage)
                        Toast.makeText(
                            context,
                            "%.2f ₺ Google Play bakiyesi hesabınıza yüklendi! (${selectedPackage.columnCount} Kolon)".format(selectedPackage.amountTL),
                            Toast.LENGTH_LONG
                        ).show()
                        onDismiss()
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = CasinoGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_google_play_purchase")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Play Bağlanıyor...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Play ile %.2f ₺ Yükle".format(selectedPackage.amountTL),
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç", color = TextMuted)
                }
            }
        }
    )
}

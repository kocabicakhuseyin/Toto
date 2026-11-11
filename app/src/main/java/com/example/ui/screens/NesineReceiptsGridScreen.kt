package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NesineReceipt
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** GridView-style archive of *verified* real coupons, never generated Toto predictions. */
@Composable
fun NesineReceiptsGridScreen(receipts: List<NesineReceipt>, onBack: () -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var columnIndex by remember { mutableIntStateOf(0) }
    val selected = receipts.firstOrNull { it.couponId == selectedId }

    BackHandler { if (selected != null) selectedId = null else onBack() }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            IconButton(onClick = { if (selected != null) selectedId = null else onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(if (selected == null) "Oynanan Kuponlarım" else "Nesine Kuponu", fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Text(if (selected == null) "${receipts.size} doğrulanmış kupon" else "Nesine • ${selected.playedAt}", fontSize = 11.sp, color = TextMuted)
            }
        }

        if (selected == null) {
            Text(
                "Yalnızca Nesine paylaşım bağlantısıyla doğrulanıp bu cihaza eklenen kuponlar. " +
                    "Durum ve tutar son doğrulama anına aittir.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp, color = TextSecondary
            )
            if (receipts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(56.dp), tint = CasinoGold)
                    Text("Henüz doğrulanmış Nesine kuponu yok.", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Nesine'de oynadığınız Spor Toto kuponunu paylaşın, ardından 'Canlı Kupon Havuzu' ekranından bağlantıyı doğrulayın.", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(receipts, key = { it.couponId }) { receipt ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedId = receipt.couponId
                                columnIndex = 0
                            },
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("NESİNE • SPOR TOTO", fontSize = 10.sp, color = CasinoGold, fontWeight = FontWeight.Bold)
                                Text(receipt.playedAt, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                                Text(receipt.amount, fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Black, maxLines = 1)
                                Text("${receipt.columns.size} kolon • 15 maç", fontSize = 11.sp, color = TextSecondary)
                                Text(receipt.status, fontSize = 12.sp, color = ElectricGreen, fontWeight = FontWeight.SemiBold)
                                Text("Kupon ••••${receipt.couponId.takeLast(4)}", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        } else {
            val shownColumn = selected.columns.getOrNull(columnIndex) ?: selected.columns.first()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = DarkCard), border = BorderStroke(1.dp, DarkCardBorder)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Nesine tarafından doğrulanan kupon", color = CasinoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Tarih: ${selected.playedAt}", color = TextPrimary, fontSize = 13.sp)
                            Text("Tutar: ${selected.amount} • Durum: ${selected.status}", color = TextSecondary, fontSize = 12.sp)
                            Text("${selected.columns.size} kolon • 15 karşılaşma", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                item {
                    Text("Kolon seçin", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selected.columns.size) { index ->
                            val active = index == columnIndex
                            Card(
                                modifier = Modifier.clickable { columnIndex = index },
                                colors = CardDefaults.cardColors(containerColor = if (active) CasinoGold else DarkCard),
                                border = BorderStroke(1.dp, if (active) CasinoGold else DarkCardBorder)
                            ) {
                                Text(
                                    "${index + 1}. Kolon",
                                    color = if (active) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                items(selected.matchLabels.size) { index ->
                    val parts = selected.matchLabels[index].split('|')
                    Card(colors = CardDefaults.cardColors(containerColor = DarkCard), border = BorderStroke(1.dp, DarkCardBorder)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", color = CasinoGold, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${parts.getOrElse(0) { "Ev" }} - ${parts.getOrElse(1) { "Deplasman" }}", color = TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(parts.getOrElse(2) { "" }, color = TextMuted, fontSize = 10.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(shownColumn[index].toString(), color = ElectricGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

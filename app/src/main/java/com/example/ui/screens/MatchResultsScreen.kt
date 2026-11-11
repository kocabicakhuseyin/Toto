package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantRed

/**
 * Simple verified scoreboard. No notifications, predictions, odds or invented
 * fixture minutes are shown. Only verified provider responses populate cards.
 */
@Composable
fun MatchResultsScreen(
    liveScores: List<MatchLiveScore>,
    providerName: String,
    hasApiKey: Boolean = false,
    freeRequestsRemaining: Int? = null,
    onSaveApiKey: (String) -> Unit = {},
    lastUpdateTime: String,
    isRefreshing: Boolean,
    errorMessage: String?,
    onPollNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKeyEntry by remember { mutableStateOf(!hasApiKey) }
    var apiKeyDraft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Canlı Skorlar",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp
                        )
                        Text(
                            "$providerName • Ekran açıkken en sık 3 dk yenileme",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            "Son kontrol: $lastUpdateTime",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(
                        onClick = onPollNow,
                        enabled = hasApiKey && !isRefreshing,
                        modifier = Modifier.testTag("btn_refresh_live_scores")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(22.dp),
                                strokeWidth = 2.dp,
                                color = ElectricCyan
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Gerçek skorları yenile", tint = ElectricCyan)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "API-Football Ücretsiz Plan",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "100 istek/gün • Uygulama en fazla 80 istek kullanır • Maç dakikaları API verisidir.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (hasApiKey) {
                        Text(
                            "Bu cihazın kalan sorgu hakkı: ${freeRequestsRemaining ?: "—"}",
                            color = ElectricGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { showApiKeyEntry = !showApiKeyEntry }) {
                            Text(if (showApiKeyEntry) "Anahtar Alanını Kapat" else "API Anahtarını Değiştir")
                        }
                    }
                    if (!hasApiKey || showApiKeyEntry) {
                        OutlinedTextField(
                            value = apiKeyDraft,
                            onValueChange = { apiKeyDraft = it.trim() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_football_free_key"),
                            label = { Text("API-Football Anahtarı") },
                            placeholder = { Text("Ücretsiz plan API anahtarınızı girin") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                onSaveApiKey(apiKeyDraft)
                                apiKeyDraft = ""
                                showApiKeyEntry = false
                            },
                            enabled = apiKeyDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_save_api_football_key")
                        ) {
                            Text("Anahtarı Kaydet ve Canlı Skorları Aç", color = Color.White)
                        }
                    }
                }
            }
        }

        if (errorMessage != null || liveScores.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("live_score_empty"),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Text(
                        text = errorMessage ?: if (!hasApiKey)
                            "Canlı sonuçları görmek için yukarıdaki ücretsiz API-Football anahtarını girin."
                        else "Şu anda API-Football üzerinden doğrulanmış canlı karşılaşma bulunamadı.",
                        modifier = Modifier.padding(20.dp),
                        color = if (errorMessage != null) Color(0xFFFFB4A9) else TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(liveScores, key = { it.matchNumber }) { match ->
                RealLiveScoreCard(match)
            }
        }
    }
}

@Composable
private fun RealLiveScoreCard(match: MatchLiveScore) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("live_fixture_${match.matchNumber}"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.homeTeam,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        match.scoreDisplay,
                        color = if (match.isLive) VibrantRed else ElectricGreen,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    match.awayTeam,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            val minuteLabel = when (match.status) {
                MatchStatus.HALF_TIME -> "Devre arası"
                MatchStatus.FINISHED -> "Maç sona erdi"
                else -> if (match.minuteText == "—") "Dakika bilgisi yok" else "${match.minuteText} dakika"
            }
            Text(
                minuteLabel,
                modifier = Modifier.fillMaxWidth(),
                color = if (match.isLive) VibrantRed else TextMuted,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

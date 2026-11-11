package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.MatchAlertEvent
import com.example.model.MatchAlertEventType
import com.example.model.MatchLiveScore
import com.example.model.NotificationPreferences
import com.example.notification.MatchNotificationManager
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantRed

@Composable
fun MatchNotificationDialog(
    preferences: NotificationPreferences,
    history: List<MatchAlertEvent>,
    favoritedMatchNumbers: Set<Int>,
    matches: List<MatchLiveScore>,
    hasSystemPermission: Boolean,
    onRequestPermission: () -> Unit,
    onUpdatePreferences: (NotificationPreferences) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onSimulateTestAlert: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearHistory: () -> Unit,
    onNavigateToResults: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPreferences by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF261905)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Bildirimler",
                                tint = CasinoGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Canlı Maç Bildirimleri",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Goller & Maç Başlangıç Bildirimleri",
                                color = CasinoGoldLight,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .testTag("btn_close_notification_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 2. Permission Banner (if not granted)
                if (!hasSystemPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261014)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VibrantRed.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚠️ Bildirim İzni Gerekli",
                                    color = VibrantRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Gol olduğunda anında push bildirimi almak için sistem iznini açın.",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_request_notif_permission")
                            ) {
                                Text(
                                    text = "İzin Ver",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // 3. Quick Action Bar: Simulate Test Goal & Settings Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSimulateTestAlert,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A2F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_simulate_test_notification")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚽", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gol Bildirimi Test Et",
                                color = ElectricGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showPreferences = !showPreferences },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (showPreferences) CasinoGold else DarkCardBorder
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_toggle_notif_preferences")
                    ) {
                        Text(
                            text = if (showPreferences) "Ayarları Gizle ▲" else "Ayarlar ⚙️",
                            color = if (showPreferences) CasinoGold else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // 4. Expandable Settings Panel
                AnimatedVisibility(visible = showPreferences) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Bildirim Tercihleri",
                                color = CasinoGoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            NotifToggleItem(
                                title = "⚽ Gol Olduğunda Bildir",
                                subtitle = "Favori maçlarda gol atıldığında anlık uyarı",
                                isChecked = preferences.notifyOnGoal,
                                onCheckedChange = { onUpdatePreferences(preferences.copy(notifyOnGoal = it)) }
                            )

                            NotifToggleItem(
                                title = "📢 Maç Başladığında Bildir",
                                subtitle = "İlk düdük çaldığında maç başlama bildirimi",
                                isChecked = preferences.notifyOnMatchStart,
                                onCheckedChange = { onUpdatePreferences(preferences.copy(notifyOnMatchStart = it)) }
                            )

                            NotifToggleItem(
                                title = "🏁 Maç Bittiğinde Bildir",
                                subtitle = "Karşılaşma sona erdiğinde maç sonucu",
                                isChecked = preferences.notifyOnMatchEnd,
                                onCheckedChange = { onUpdatePreferences(preferences.copy(notifyOnMatchEnd = it)) }
                            )

                            NotifToggleItem(
                                title = "⭐ Sadece Favori Maçlar",
                                subtitle = "Açıksa yalnızca yıldızladığınız maçlar için bildirim gönderilir",
                                isChecked = preferences.notifyOnlyFavorites,
                                onCheckedChange = { onUpdatePreferences(preferences.copy(notifyOnlyFavorites = it)) }
                            )

                            NotifToggleItem(
                                title = "🔔 Ekran Üstü Canlı Banner",
                                subtitle = "Uygulama açıkken üstten kayan bildirim göster",
                                isChecked = preferences.inAppBannerEnabled,
                                onCheckedChange = { onUpdatePreferences(preferences.copy(inAppBannerEnabled = it)) }
                            )
                        }
                    }
                }

                // 5. Favorited Matches Chips Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bildirim Takibindeki Maçlar (${favoritedMatchNumbers.size})",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Yıldızla ekle/çıkar",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(matches, key = { it.matchNumber }) { match ->
                            val isFav = favoritedMatchNumbers.contains(match.matchNumber)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFav) Color(0xFF261905) else DarkCard)
                                    .border(
                                        1.dp,
                                        if (isFav) CasinoGold else DarkCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onToggleFavorite(match.matchNumber) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isFav) "⭐" else "☆",
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "#${match.matchNumber} ${match.homeTeam.take(6)}",
                                        color = if (isFav) CasinoGoldLight else TextSecondary,
                                        fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Recent Notification Alerts History
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Son Bildirimler (${history.size})",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        if (history.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Temizle",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clickable { onClearHistory() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Henüz yeni bir maç bildirimi yok.\nMaçlar başladığında veya favori maçlarınızda gol olduğunda burada listelenecek.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(history, key = { it.id }) { alert ->
                                NotificationHistoryItem(
                                    alert = alert,
                                    onViewLive = {
                                        onDismiss()
                                        onNavigateToResults()
                                    }
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
private fun NotifToggleItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 9.5.sp
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CasinoGold,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkBackground
            ),
            modifier = Modifier.size(height = 20.dp, width = 36.dp)
        )
    }
}

@Composable
private fun NotificationHistoryItem(
    alert: MatchAlertEvent,
    onViewLive: () -> Unit
) {
    val borderColor = when (alert.eventType) {
        MatchAlertEventType.GOAL_SCORED -> VibrantRed.copy(alpha = 0.6f)
        MatchAlertEventType.MATCH_STARTED -> ElectricCyan.copy(alpha = 0.6f)
        MatchAlertEventType.HALF_TIME -> NeonAmber.copy(alpha = 0.6f)
        MatchAlertEventType.MATCH_FINISHED -> ElectricGreen.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onViewLive() }
            .padding(10.dp)
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
                Text(text = alert.eventType.iconEmoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = alert.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = alert.message,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = alert.formattedTime,
                    color = TextMuted,
                    fontSize = 9.sp
                )
                Text(
                    text = "Canlı ▶",
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.CasinoGold
import com.example.ui.theme.CasinoGoldLight
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsScore
import com.example.ui.theme.VibrantRed

enum class AppNavTab(val label: String, val icon: ImageVector) {
    HOME("Panel", Icons.Default.Dashboard),
    ANALYSIS("Bülten", Icons.Default.Analytics),
    RESULTS("Canlı", Icons.Default.SportsScore),
    COUPONS("Kuponlarım", Icons.AutoMirrored.Filled.ReceiptLong),
    FORUM("Forum", Icons.Default.Forum),
    PROFILE("Profil", Icons.Default.AccountCircle)
}

@Composable
fun TopHeaderBar(
    userProfile: UserProfile,
    onProfileClick: () -> Unit,
    onOpenWallet: () -> Unit = {},
    onLiveClick: () -> Unit = {},
    liveMatchesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Brand with VIP Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CasinoGold, Color(0xFFD97706))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ST",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SPOR TOTO",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CasinoGold.copy(alpha = 0.2f))
                                .border(1.dp, CasinoGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "VIP",
                                color = CasinoGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = "0.20 ₺ / Kolon Otomasyon",
                        color = CasinoGoldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right: Live Matches Pill, Wallet Balance Pill & Profile
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live Matches Indicator Button
                if (liveMatchesCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2B0F15))
                            .border(1.dp, VibrantRed.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                            .clickable { onLiveClick() }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("btn_top_bar_live")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(VibrantRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$liveMatchesCount Canlı",
                            color = VibrantRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Gold Wallet Pill (Google Play)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF261905))
                        .border(1.dp, CasinoGold.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .clickable { onOpenWallet() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("btn_top_bar_wallet")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Cüzdan",
                        tint = CasinoGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "%.2f ₺".format(userProfile.walletBalanceTL),
                        color = CasinoGoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(CasinoGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Profile Avatar Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                        .clickable { onProfileClick() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    TeamBadgeView(team = userProfile.favoriteTeam, size = 24.dp)
                }
            }
        }
    }
}

@Composable
fun ModernBottomNavBar(
    currentTab: AppNavTab,
    couponsCount: Int,
    liveMatchesCount: Int = 0,
    onTabSelected: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        AppNavTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    when {
                        tab == AppNavTab.COUPONS && couponsCount > 0 -> {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = CasinoGold) {
                                        Text("$couponsCount", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(imageVector = tab.icon, contentDescription = tab.label)
                            }
                        }
                        tab == AppNavTab.RESULTS && liveMatchesCount > 0 -> {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = VibrantRed) {
                                        Text("$liveMatchesCount", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(imageVector = tab.icon, contentDescription = tab.label)
                            }
                        }
                        else -> {
                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                        }
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = CasinoGold,
                    indicatorColor = CasinoGold,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
            )
        }
    }
}

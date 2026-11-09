package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PlatformAutomationManager
import com.example.model.AutoPlayStepState
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.PlatformCredentials
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun PlatformAutoPlayDialog(
    coupon: Coupon,
    platform: BettingPlatform,
    savedCredentials: PlatformCredentials,
    onDismiss: () -> Unit,
    onSaveCredentials: (PlatformCredentials) -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf(savedCredentials.username.ifBlank { "kocabicakhuseyin" }) }
    var password by remember { mutableStateOf(savedCredentials.password.ifBlank { "sifre1234" }) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(savedCredentials.isRemembered) }

    var playState by remember { mutableStateOf(AutoPlayStepState.IDLE) }
    var stepIndex by remember { mutableIntStateOf(0) }

    val brandColor = Color(platform.brandColorHex)
    val brandTextColor = Color(platform.textColorHex)

    // Automated play sequence coroutine
    LaunchedEffect(playState) {
        if (playState == AutoPlayStepState.CONNECTING) {
            stepIndex = 1
            delay(700)
            stepIndex = 2
            delay(800)
            stepIndex = 3
            delay(900)
            stepIndex = 4
            playState = AutoPlayStepState.COMPLETED
            PlatformAutomationManager.executeAutoPlay(
                context,
                coupon,
                platform,
                PlatformCredentials(platform, username, password, rememberMe)
            )
            onSaveCredentials(PlatformCredentials(platform, username, password, rememberMe))
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (playState != AutoPlayStepState.CONNECTING) onDismiss()
        },
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brandColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = platform.iconEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${platform.displayName} ile Otomatik Oyna",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${coupon.formulaType.badge} • ${coupon.totalColumns} Kolon (${coupon.totalCostTL.toInt()} ₺)",
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                }
            }
        },
        text = {
            Column {
                if (playState == AutoPlayStepState.IDLE) {
                    Text(
                        text = "${platform.shortName} hesabınız üzerinden kuponun otomatik yatırılabilmesi için bilgilerinizi giriniz:",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("${platform.shortName} Kullanıcı Adı / T.C. / Üye No", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = brandColor,
                            unfocusedIndicatorColor = DarkCardBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_${platform.id}_username"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("${platform.shortName} Şifresi", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = brandColor,
                            unfocusedIndicatorColor = DarkCardBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_${platform.id}_password"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Remember Me Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = brandColor,
                                uncheckedColor = TextMuted
                            )
                        )
                        Text(
                            text = "${platform.shortName} giriş bilgilerimi güvenle hatırla",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Security assurance banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bilgileriniz sadece cihazınızda yerel saklanır, güvenli oturum için kullanılır.",
                            color = Color(0xFF0369A1),
                            fontSize = 10.sp
                        )
                    }
                } else {
                    // Automated Play In-Progress Console
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        AutomationStepRow(
                            stepNumber = 1,
                            title = "${platform.domain} SSL Bağlantısı Kuruluyor...",
                            isDone = stepIndex > 1,
                            isActive = stepIndex == 1,
                            brandColor = brandColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AutomationStepRow(
                            stepNumber = 2,
                            title = "'$username' Hesabı Doğrulanıyor...",
                            isDone = stepIndex > 2,
                            isActive = stepIndex == 2,
                            brandColor = brandColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AutomationStepRow(
                            stepNumber = 3,
                            title = "${coupon.totalColumns} Kolon ${platform.shortName} Sepetine Aktarılıyor...",
                            isDone = stepIndex > 3,
                            isActive = stepIndex == 3,
                            brandColor = brandColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AutomationStepRow(
                            stepNumber = 4,
                            title = "Kupon Hazır! ${platform.shortName} Başlatılıyor.",
                            isDone = stepIndex >= 4,
                            isActive = false,
                            brandColor = brandColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (playState == AutoPlayStepState.IDLE) {
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Lütfen kullanıcı adı ve şifrenizi giriniz", Toast.LENGTH_SHORT).show()
                        } else {
                            playState = AutoPlayStepState.CONNECTING
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_autoplay_${platform.id}")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = brandTextColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Otomatik Oyna", color = brandTextColor, fontWeight = FontWeight.Black)
                }
            } else if (playState == AutoPlayStepState.COMPLETED) {
                Button(
                    onClick = {
                        PlatformAutomationManager.executeAutoPlay(
                            context,
                            coupon,
                            platform,
                            PlatformCredentials(platform, username, password, rememberMe)
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_reopen_platform_${platform.id}")
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = brandTextColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${platform.shortName}'yi Aç", color = brandTextColor, fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            if (playState != AutoPlayStepState.CONNECTING) {
                TextButton(onClick = onDismiss) {
                    Text("Kapat", color = TextMuted)
                }
            }
        }
    )
}

@Composable
private fun AutomationStepRow(
    stepNumber: Int,
    title: String,
    isDone: Boolean,
    isActive: Boolean,
    brandColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isDone) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(brandColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
            }
        } else if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = brandColor,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$stepNumber", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            color = if (isDone) brandColor else if (isActive) TextPrimary else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal
        )
    }
}

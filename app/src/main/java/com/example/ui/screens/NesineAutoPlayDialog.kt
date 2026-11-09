package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.PlatformCredentials

@Composable
fun NesineAutoPlayDialog(
    coupon: Coupon,
    savedCredentials: PlatformCredentials,
    onDismiss: () -> Unit,
    onSaveCredentials: (PlatformCredentials) -> Unit
) {
    PlatformAutoPlayDialog(
        coupon = coupon,
        platform = BettingPlatform.NESINE,
        savedCredentials = savedCredentials,
        onDismiss = onDismiss,
        onSaveCredentials = onSaveCredentials
    )
}

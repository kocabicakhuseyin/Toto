package com.example.engine

import android.content.Context
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.PlatformCredentials

object NesineIntegrationManager {

    fun formatCouponForNesine(coupon: Coupon, credentials: PlatformCredentials? = null): String {
        return PlatformAutomationManager.formatCouponForPlatform(coupon, BettingPlatform.NESINE, credentials)
    }

    fun openNesineWithCoupon(context: Context, coupon: Coupon): Boolean {
        return PlatformAutomationManager.executeAutoPlay(
            context,
            coupon,
            BettingPlatform.NESINE,
            PlatformCredentials(BettingPlatform.NESINE)
        )
    }

    fun executeAutoPlay(context: Context, coupon: Coupon, credentials: PlatformCredentials): Boolean {
        return PlatformAutomationManager.executeAutoPlay(context, coupon, BettingPlatform.NESINE, credentials)
    }
}

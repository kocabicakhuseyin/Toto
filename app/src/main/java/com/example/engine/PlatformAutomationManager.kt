package com.example.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.model.BettingPlatform
import com.example.model.Coupon
import com.example.model.PlatformCredentials
import com.example.model.TotoColumn

object PlatformAutomationManager {

    /**
     * Formats coupon specifically for the selected betting platform.
     */
    fun formatCouponForPlatform(
        coupon: Coupon,
        platform: BettingPlatform,
        credentials: PlatformCredentials? = null
    ): String {
        val sb = StringBuilder()
        sb.append("=== ${platform.displayName.uppercase()} SPOR TOTO KUPONU ===\n")
        if (credentials != null && credentials.username.isNotBlank()) {
            sb.append("${platform.shortName} Kullanıcı: ${credentials.username}\n")
        }
        sb.append("Formül: ${coupon.formulaType.title}\n")
        sb.append("Kolon Sayısı: ${coupon.totalColumns} | Tutar: ${coupon.totalCostTL} TL\n")
        sb.append("---------------------------------\n")
        coupon.columns.forEach { col ->
            sb.append("Kolon ${col.index.toString().padStart(2, '0')}: ${col.asString}\n")
        }
        sb.append("---------------------------------\n")
        sb.append("Bu metin yalnızca kupon seçimlerinin kopyasıdır; bahis oynanmış veya onaylanmış değildir.\n")
        return sb.toString()
    }

    /**
     * Copies the selections and opens the external platform for the user to
     * log in, select the fixtures and confirm a bet. Does not place a bet.
     */
    fun executeAutoPlay(
        context: Context,
        coupon: Coupon,
        platform: BettingPlatform,
        credentials: PlatformCredentials
    ): Boolean {
        // 1. Copy formatted text to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val formattedText = formatCouponForPlatform(coupon, platform, credentials)
        val clip = ClipData.newPlainText("${platform.shortName} Spor Toto Kuponu", formattedText)
        clipboard?.setPrimaryClip(clip)

        // 2. Try launching platform's native Android App
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(platform.packageName)

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            // 3. Fallback to platform's Mobile Web Spor Toto page
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(platform.webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
                true
            } catch (e: Exception) {
                Toast.makeText(context, "${platform.shortName} açılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}

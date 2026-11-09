package com.example.billing

import com.example.model.COLUMN_FEE_TL
import com.example.model.GooglePlayPackage
import com.example.model.TransactionType
import com.example.model.WalletTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object GooglePlayBillingManager {

    val packages = listOf(
        GooglePlayPackage(
            id = "toto_balance_25",
            title = "Başlangıç Paketi",
            amountTL = 25.0,
            columnCount = (25.0 / COLUMN_FEE_TL).toInt(), // 125 Kolon
            bonusBadge = null,
            isPopular = false
        ),
        GooglePlayPackage(
            id = "toto_balance_50",
            title = "Standart Bahisçi",
            amountTL = 50.0,
            columnCount = (50.0 / COLUMN_FEE_TL).toInt(), // 250 Kolon
            bonusBadge = "EN ÇOK SEÇİLEN",
            isPopular = true
        ),
        GooglePlayPackage(
            id = "toto_balance_100",
            title = "Pro Kuponcu Paketi",
            amountTL = 100.0,
            columnCount = (100.0 / COLUMN_FEE_TL).toInt() + 25, // 500 + 25 Bonus Kolon
            bonusBadge = "+25 BONUS KOLON",
            isPopular = false
        ),
        GooglePlayPackage(
            id = "toto_balance_250",
            title = "VIP Toto Ustası",
            amountTL = 250.0,
            columnCount = (250.0 / COLUMN_FEE_TL).toInt() + 100, // 1250 + 100 Bonus Kolon
            bonusBadge = "VIP %15 İNDİRİM",
            isPopular = false
        )
    )

    fun createPurchaseTransaction(pkg: GooglePlayPackage): WalletTransaction {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        val randomGpaSuffix = (1000..9999).random().toString()
        val orderId = "GPA.${(1000..9999).random()}-${(1000..9999).random()}-${(1000..9999).random()}-$randomGpaSuffix"

        return WalletTransaction(
            id = UUID.randomUUID().toString(),
            title = "Google Play: ${pkg.title} (${pkg.columnCount} Kolon)",
            amountTL = pkg.amountTL,
            type = TransactionType.TOP_UP,
            dateText = dateFormat.format(Date()),
            orderId = orderId
        )
    }

    fun createColumnPlayTransaction(columnCount: Int, costTL: Double): WalletTransaction {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        return WalletTransaction(
            id = UUID.randomUUID().toString(),
            title = "$columnCount Kolon Oynatma Bedeli (0.20 ₺/Kolon)",
            amountTL = costTL,
            type = TransactionType.COLUMN_FEE,
            dateText = dateFormat.format(Date()),
            orderId = null
        )
    }
}

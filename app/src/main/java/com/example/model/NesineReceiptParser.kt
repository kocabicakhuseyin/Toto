package com.example.model

import java.util.Locale

/** Visible information from a public Nesine Spor Toto receipt, never an account password. */
data class NesineReceipt(
    val couponId: String,
    val status: String,
    val playedAt: String,
    val amount: String,
    val matchLabels: List<String>,
    val columns: List<String>,
    val weekKey: String
)

/**
 * A shared URL is evidence only after the real nesine.com receipt DOM has been loaded.
 * A copied prediction sheet, an external-app launch or a wallet charge is not evidence.
 */
object NesineReceiptParser {
    private val dateFormat = Regex("\\d{2}\\.\\d{2}\\.\\d{4}\\s+\\d{2}:\\d{2}")
    private val receiptId = Regex("\\d{12,24}")

    fun parse(
        couponId: String,
        status: String,
        playedAt: String,
        amount: String,
        rows: List<List<String>>
    ): Result<NesineReceipt> = runCatching {
        require(receiptId.matches(couponId)) { "Nesine kupon numarası doğrulanamadı." }
        // No broad 'success' fallback: cancelled, saved and unknown tickets are excluded.
        require(status.trim().equals("Bekliyor", ignoreCase = true)) {
            "Kuponun oynandığı doğrulanamadı (Nesine durumu: ${status.take(40)})."
        }
        require(dateFormat.matches(playedAt.trim())) { "Kuponun işlem zamanı bulunamadı." }
        require(rows.size == 15) { "Paylaşımda 15 karşılaşma bulunamadı." }

        val ordered = rows.mapIndexed { index, row ->
            require(row.size >= 6) { "${index + 1}. karşılaşmanın seçimleri eksik." }
            require(row[0].trim().toIntOrNull() == index + 1) { "Karşılaşma sırası hatalı." }
            require(dateFormat.matches(row[1].trim())) { "${index + 1}. karşılaşmanın tarihi hatalı." }
            val teams = row[2].trim().split('-', limit = 2)
            require(teams.size == 2 && teams.all { it.trim().isNotEmpty() }) {
                "${index + 1}. karşılaşmanın takımları ayrıştırılamadı."
            }
            val picks = row.drop(5).map { it.trim() }
            require(picks.isNotEmpty() && picks.all { it in setOf("1", "X", "2") }) {
                "${index + 1}. karşılaşmanın tahminleri geçersiz."
            }
            Triple("${teams[0].trim()}|${teams[1].trim()}|${row[1].trim()}", row[1].trim(), picks)
        }
        val pickCount = ordered.first().third.size
        require(ordered.all { it.third.size == pickCount }) { "Her maçta aynı sayıda kolon olmalı." }
        require(pickCount in 1..4096) { "Kolon sayısı geçersiz." }
        val columns = (0 until pickCount).map { index ->
            ordered.joinToString("") { it.third[index] }
        }
        val fixtureDates = ordered.map { row ->
            val parts = row.second.substring(0, 10).split('.')
            "${parts[2]}-${parts[1]}-${parts[0]}"
        }.sorted()
        val start = fixtureDates.first()
        val end = fixtureDates.last()
        NesineReceipt(
            couponId = couponId,
            status = status.trim(),
            playedAt = playedAt.trim(),
            amount = amount.trim(),
            matchLabels = ordered.map { it.first },
            columns = columns,
            weekKey = "nesine-$start-$end"
        )
    }

    fun isShareUrl(input: String): Boolean {
        val uri = try { java.net.URI(input.trim()) } catch (_: Exception) { return false }
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        if (uri.scheme?.lowercase(Locale.ROOT) != "https" || uri.userInfo != null) return false
        if (host == "nsn.li") return Regex("^/[A-Za-z0-9]{4,20}/?$").matches(uri.path.orEmpty())
        return (host == "nesine.com" || host == "www.nesine.com") &&
            uri.path.equals("/SporToto/ViewCoupon", ignoreCase = true) &&
            !uri.query.isNullOrBlank()
    }

    /** Stores a non-reversible identifier, not the public bearer-like receipt link. */
    fun sourceId(couponId: String): String = "nesine-" + java.security.MessageDigest
        .getInstance("SHA-256").digest(couponId.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

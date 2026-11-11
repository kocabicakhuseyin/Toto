package com.example.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * An on-device history of receipts fetched from a real, public Nesine coupon page.
 * Never saves passwords, session cookies, or the share URL (which may contain a token).
 * These are snapshots from the verification time, not continuously updated results.
 */
class NesineReceiptArchive(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "verified_nesine_receipts_v1", Context.MODE_PRIVATE
    )

    fun list(): List<NesineReceipt> {
        val json = preferences.getString("items", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                try {
                    val entry = array.getJSONObject(index)
                    val labels = entry.getJSONArray("matchLabels").toStringList()
                    val columns = entry.getJSONArray("columns").toStringList()
                    val id = entry.getString("couponId")
                    if (!ID_PATTERN.matches(id) || labels.size != 15 ||
                        columns.isEmpty() || columns.size > 4096 ||
                        columns.any { !COLUMN_PATTERN.matches(it) }) {
                        null
                    } else {
                        NesineReceipt(
                            couponId = id,
                            status = entry.getString("status"),
                            playedAt = entry.getString("playedAt"),
                            amount = entry.getString("amount"),
                            matchLabels = labels,
                            columns = columns,
                            weekKey = entry.getString("weekKey")
                        )
                    }
                } catch (_: Exception) {
                    null // Ignore a damaged individual entry instead of hiding the archive.
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(verifiedReceipt: NesineReceipt): List<NesineReceipt> {
        require(ID_PATTERN.matches(verifiedReceipt.couponId))
        require(verifiedReceipt.matchLabels.size == 15)
        require(verifiedReceipt.columns.isNotEmpty() && verifiedReceipt.columns.size <= 4096)
        require(verifiedReceipt.columns.all { COLUMN_PATTERN.matches(it) })

        val updated = merge(verifiedReceipt, list())
        val array = JSONArray()
        updated.forEach { receipt ->
            array.put(JSONObject().apply {
                put("couponId", receipt.couponId)
                put("status", receipt.status)
                put("playedAt", receipt.playedAt)
                put("amount", receipt.amount)
                put("matchLabels", JSONArray(receipt.matchLabels))
                put("columns", JSONArray(receipt.columns))
                put("weekKey", receipt.weekKey)
            })
        }
        // Commit before reporting success, so leaving the app cannot lose the receipt.
        check(preferences.edit().putString("items", array.toString()).commit()) {
            "Kupon cihaz hafızasına kaydedilemedi."
        }
        return updated
    }

    companion object {
        private val ID_PATTERN = Regex("\\d{12,24}")
        private val COLUMN_PATTERN = Regex("[1X2]{15}")
        private const val MAX_RECEIPTS = 100

        /** Newest verification first; repeated verification refreshes one existing card. */
        fun merge(receipt: NesineReceipt, existing: List<NesineReceipt>): List<NesineReceipt> =
            (listOf(receipt) + existing.filterNot { it.couponId == receipt.couponId })
                .take(MAX_RECEIPTS)
    }
}

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).map { index -> getString(index) }

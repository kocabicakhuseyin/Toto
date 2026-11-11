package com.example

import com.example.model.NesineReceipt
import com.example.model.NesineReceiptArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NesineReceiptArchiveTest {
    private fun receipt(id: Long, status: String = "Bekliyor") = NesineReceipt(
        couponId = id.toString().padStart(16, '0'),
        status = status,
        playedAt = "10.11.2026 21:14",
        amount = "40,00 TL",
        matchLabels = (1..15).map { "Ev$it|Dep$it|13.11.2026 20:00" },
        columns = listOf("1".repeat(15)),
        weekKey = "nesine-2026-11-13-2026-11-15"
    )

    @Test fun sameVerifiedCouponIsNotDuplicated() {
        val existing = listOf(receipt(123), receipt(456))
        val refreshed = receipt(456, status = "Bekliyor")
        val archive = NesineReceiptArchive.merge(refreshed, existing)

        assertEquals(2, archive.size)
        assertSame(refreshed, archive[0])
        assertEquals(existing[0], archive[1])
    }

    @Test fun archiveKeepsHundredNewestReceipts() {
        val prior = (1L..100L).map { receipt(it) }
        val archive = NesineReceiptArchive.merge(receipt(101), prior)

        assertEquals(100, archive.size)
        assertEquals(receipt(101).couponId, archive.first().couponId)
        assertEquals(receipt(99).couponId, archive.last().couponId)
    }
}

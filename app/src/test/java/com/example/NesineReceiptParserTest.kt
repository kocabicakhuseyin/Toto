package com.example

import com.example.model.NesineReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NesineReceiptParserTest {
    private val rows = (1..15).map { i ->
        val date = if (i == 1) "15.11.2026 22:45" else "13.11.2026 20:00"
        listOf(i.toString(), date, "Ev$i-Dep$i", "", "", "1", if (i == 2) "X" else "1")
    }

    @Test fun verifiedReceiptContainsActualColumnsAndFixtures() {
        val receipt = NesineReceiptParser.parse("4605410400000000", "Bekliyor", "10.11.2026 21:14",
            "40,00 TL", rows).getOrThrow()
        assertEquals(15, receipt.matchLabels.size)
        assertEquals(listOf("1".repeat(15), "1X" + "1".repeat(13)), receipt.columns)
        assertEquals("nesine-2026-11-13-2026-11-15", receipt.weekKey)
    }

    @Test fun cancelledOrUnverifiedCouponIsRejected() {
        assertTrue(NesineReceiptParser.parse("4605410400000000", "İptal Edildi", "10.11.2026 21:14",
            "40,00 TL", rows).isFailure)
        assertTrue(NesineReceiptParser.parse("4605410400000000", "", "10.11.2026 21:14",
            "40,00 TL", rows).isFailure)
        assertTrue(NesineReceiptParser.parse("4605410400000000", "Bekliyor", "10.11.2026 21:14",
            "40,00 TL", rows.dropLast(1)).isFailure)
    }

    @Test fun onlyGenuineHttpsNesineReceiptLinksAreAccepted() {
        assertTrue(NesineReceiptParser.isShareUrl("https://nsn.li/OmqWb7"))
        assertTrue(NesineReceiptParser.isShareUrl("https://www.nesine.com/SporToto/ViewCoupon?c=token"))
        assertFalse(NesineReceiptParser.isShareUrl("http://nsn.li/OmqWb7"))
        assertFalse(NesineReceiptParser.isShareUrl("https://nsn.li.attacker.tld/OmqWb7"))
        assertFalse(NesineReceiptParser.isShareUrl("https://www.nesine.com/iddaa"))
    }
}

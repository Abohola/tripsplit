package com.tripsplit.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptParserTest {
    @Test
    fun extractsItemLinesAndIgnoresTotals() {
        val text = """
            MARKET
            Milk 85.50
            2 x Chips 100.00
            SUBTOTAL 185.50
            VAT 25.00
            TOTAL EGP 210.50
        """.trimIndent()

        assertEquals(
            listOf(
                ParsedReceiptItem("Milk", 8_550L),
                ParsedReceiptItem("Chips", 10_000L),
            ),
            ReceiptParser.parse(text),
        )
        assertEquals(21_050L, ReceiptParser.findTotalCents(text))
    }
}

package com.tripsplit.receipt

data class ParsedReceiptItem(
    val name: String,
    val amountCents: Long,
)

object ReceiptParser {
    private val amountAtEnd = Regex("""(?:EGP|LE|L\.E\.?|\$)?\s*(-?\d[\d,]*(?:\.\d{1,2})?)\s*(?:EGP|LE|L\.E\.?)?$""", RegexOption.IGNORE_CASE)
    private val ignoredWords = listOf(
        "subtotal", "sub total", "total", "vat", "tax", "cash", "card", "visa", "mastercard",
        "change", "balance", "discount", "receipt", "invoice", "amount due", "tender", "payment",
    )

    fun parse(text: String): List<ParsedReceiptItem> = text
        .lineSequence()
        .map(String::trim)
        .filter { it.length >= 3 }
        .mapNotNull(::parseLine)
        .toList()

    fun findTotalCents(text: String): Long? = text
        .lineSequence()
        .map(String::trim)
        .filter { line ->
            val normalized = line.lowercase()
            normalized.contains("total") && !normalized.contains("subtotal") && !normalized.contains("sub total")
        }
        .mapNotNull { line ->
            amountAtEnd.find(line)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull()
        }
        .filter { it.signum() > 0 }
        .map { it.movePointRight(2).toLong() }
        .lastOrNull()

    private fun parseLine(line: String): ParsedReceiptItem? {
        val normalized = line.lowercase()
        if (ignoredWords.any { normalized.contains(it) }) return null
        val match = amountAtEnd.find(line) ?: return null
        val rawAmount = match.groupValues[1].replace(",", "")
        val amount = rawAmount.toBigDecimalOrNull() ?: return null
        if (amount.signum() <= 0) return null
        val name = line.substring(0, match.range.first)
            .trim(' ', '-', ':', '.', '*')
            .replace(Regex("""^\d+\s*[xX]\s*"""), "")
            .trim()
        if (name.length < 2 || name.none(Char::isLetter)) return null
        return ParsedReceiptItem(name = name, amountCents = amount.movePointRight(2).toLong())
    }
}

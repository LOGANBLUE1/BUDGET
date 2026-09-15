package com.example.budgetapp.util

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats and parses money for one chosen currency.
 *
 * Amounts are stored as whole cents (Long) everywhere so nothing drifts through floating
 * point; this class is the only place that converts to and from text.
 *
 * The currency the user picks and the locale used to lay numbers out are separate
 * concerns: picking INR on a US-locale phone should still render ₹ and follow Indian
 * digit grouping, so we look for a locale that actually uses the currency — preferring
 * one in the device's language — and fall back to the device locale if none does.
 * (Grouping style comes from the platform's locale data: Android's ICU groups INR as
 * ₹1,00,000.00, while a plain JVM groups it Western-style.)
 *
 * Fraction digits are pinned to 2 for every currency so what's displayed always matches
 * what the amount editor shows.
 */
class MoneyFormatter(
    requestedCode: String,
    baseLocale: Locale = Locale.getDefault(),
) {

    val currency: Currency = runCatching { Currency.getInstance(requestedCode) }
        .getOrElse { Currency.getInstance(FALLBACK_CODE) }

    val code: String get() = currency.currencyCode

    private val locale: Locale = localeFor(currency, baseLocale)

    val symbol: String = currency.getSymbol(locale)

    private val symbols = DecimalFormatSymbols.getInstance(locale)

    /** What the user's keyboard should produce for a decimal point in this locale. */
    val decimalSeparator: Char = symbols.decimalSeparator

    private val groupingSeparator: Char = symbols.groupingSeparator

    private val numberFormat: NumberFormat = NumberFormat.getCurrencyInstance(locale).also { nf ->
        // Order matters: setting the currency resets fraction digits to that currency's
        // defaults, so pin them afterwards.
        nf.currency = currency
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
    }

    fun format(cents: Long): String = numberFormat.format(cents / 100.0)

    /** Plain digits for an editable text field: no symbol, no grouping. */
    fun toEditableString(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = if (cents < 0) -cents else cents
        return "$sign${abs / 100}$decimalSeparator${(abs % 100).toString().padStart(2, '0')}"
    }

    /**
     * Parses user input into cents, or null when the text is not a usable amount.
     * Grouping separators are ignored, so "1,00,000.50" and "100000.5" both work.
     */
    fun parseToCents(input: String): Long? {
        val normalized = buildString {
            for (ch in input.trim()) {
                when {
                    ch in '0'..'9' -> append(ch)
                    ch == decimalSeparator -> append('.')
                    // Grouping separators, spaces and currency symbols are all noise.
                    else -> Unit
                }
            }
        }
        if (normalized.isEmpty()) return null

        val parts = normalized.split('.')
        if (parts.size > 2) return null

        val whole = parts[0].ifEmpty { "0" }
        val fraction = (parts.getOrNull(1) ?: "").take(2).padEnd(2, '0')

        val wholeValue = whole.toLongOrNull() ?: return null
        val fractionValue = fraction.toLongOrNull() ?: return null
        if (wholeValue > Long.MAX_VALUE / 100 - 1) return null

        return wholeValue * 100 + fractionValue
    }

    private companion object {
        const val FALLBACK_CODE = "INR"

        fun currencyOf(locale: Locale): Currency? =
            runCatching { Currency.getInstance(locale) }.getOrNull()

        fun localeFor(currency: Currency, base: Locale): Locale {
            if (currencyOf(base) == currency) return base

            val matches = Locale.getAvailableLocales().filter { currencyOf(it) == currency }
            return matches.firstOrNull { it.language == base.language }
                ?: matches.firstOrNull()
                ?: base
        }
    }
}

/** One row in the currency picker. */
data class CurrencyOption(
    val code: String,
    val displayName: String,
    val symbol: String,
) {
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return code.contains(q, ignoreCase = true) ||
            displayName.contains(q, ignoreCase = true) ||
            symbol.contains(q, ignoreCase = true)
    }

    companion object {
        /** Shown at the top of an unfiltered list, in this order. */
        val PINNED = listOf(
            "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD",
            "CHF", "CNY", "SGD", "AED", "SAR", "MYR", "ZAR",
        )

        private val cached: List<CurrencyOption> by lazy { build() }

        /** Every selectable currency: pinned ones first, then the rest alphabetically. */
        fun all(): List<CurrencyOption> = cached

        private fun build(): List<CurrencyOption> {
            // Currency.getAvailableCurrencies() includes withdrawn currencies such as
            // "Turkish Lira (1922-2005)". Keep only those some locale actually uses.
            val inUse = Locale.getAvailableLocales()
                .mapNotNullTo(HashSet()) { runCatching { Currency.getInstance(it) }.getOrNull() }

            return Currency.getAvailableCurrencies()
                .filter { it.currencyCode.length == 3 && it.currencyCode.all(Char::isLetter) }
                .filter { it in inUse || it.currencyCode in PINNED }
                .map {
                    CurrencyOption(
                        code = it.currencyCode,
                        displayName = it.displayName,
                        symbol = it.symbol,
                    )
                }
                .sortedWith(
                    compareBy(
                        { PINNED.indexOf(it.code).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE },
                        { it.code },
                    )
                )
        }
    }
}

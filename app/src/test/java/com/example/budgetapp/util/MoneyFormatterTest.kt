package com.example.budgetapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {

    private val inr = MoneyFormatter("INR", Locale("en", "IN"))
    private val usd = MoneyFormatter("USD", Locale.US)
    private val eur = MoneyFormatter("EUR", Locale.GERMANY)

    @Test
    fun `parses whole amounts`() {
        assertEquals(1200L, inr.parseToCents("12"))
        assertEquals(0L, inr.parseToCents("0"))
    }

    @Test
    fun `parses decimals and pads short fractions`() {
        assertEquals(1250L, inr.parseToCents("12.50"))
        assertEquals(1250L, inr.parseToCents("12.5"))
        assertEquals(1200L, inr.parseToCents("12."))
        assertEquals(5L, inr.parseToCents("0.05"))
    }

    @Test
    fun `truncates beyond two decimal places`() {
        assertEquals(1299L, inr.parseToCents("12.999"))
    }

    @Test
    fun `ignores grouping separators`() {
        assertEquals(100000L, inr.parseToCents("1,000"))
        assertEquals(10000050L, inr.parseToCents("1,00,000.50"))
        assertEquals(100000L, usd.parseToCents("1,000"))
    }

    @Test
    fun `follows the locale's decimal separator`() {
        // German uses ',' as the decimal point and '.' for grouping.
        assertEquals(1250L, eur.parseToCents("12,50"))
        assertEquals(125000L, eur.parseToCents("1.250"))
    }

    @Test
    fun `strips currency symbols and spaces`() {
        assertEquals(45000L, usd.parseToCents(" $450.00 "))
        assertEquals(45000L, inr.parseToCents(" ₹450.00 "))
    }

    @Test
    fun `rejects unusable input`() {
        assertNull(inr.parseToCents(""))
        assertNull(inr.parseToCents("   "))
        assertNull(inr.parseToCents("abc"))
        assertNull(inr.parseToCents("1.2.3"))
        assertNull(inr.parseToCents("99999999999999999999"))
    }

    @Test
    fun `round-trips through the editable string form`() {
        listOf(0L, 5L, 99L, 100L, 123456L).forEach { cents ->
            assertEquals(cents, inr.parseToCents(inr.toEditableString(cents)))
            assertEquals(cents, eur.parseToCents(eur.toEditableString(cents)))
        }
    }

    @Test
    fun `formats in the selected currency, not the device one`() {
        assertTrue(inr.format(123456L).contains("1,234.56"))
        assertEquals("₹", inr.symbol)
        assertTrue(usd.format(123456L).contains("1,234.56"))
        assertEquals("$", usd.symbol)
    }

    @Test
    fun `uses the chosen currency's conventions, not the device's`() {
        // Digit grouping style is platform locale data, but the currency itself must not
        // follow the device: INR on a US-locale phone is still rupees.
        val inrOnUsDevice = MoneyFormatter("INR", Locale.US)
        assertEquals("INR", inrOnUsDevice.code)
        assertEquals("₹", inrOnUsDevice.symbol)
        assertTrue("was: ${inrOnUsDevice.format(100L)}", inrOnUsDevice.format(100L).contains("₹"))
    }

    @Test
    fun `falls back to the default currency for an unknown code`() {
        assertEquals("INR", MoneyFormatter("XYZZY", Locale.US).code)
    }
}

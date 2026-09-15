package com.example.budgetapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyOptionTest {

    private val all = CurrencyOption.all()

    @Test
    fun `pinned currencies lead the list, in order`() {
        assertEquals(CurrencyOption.PINNED, all.take(CurrencyOption.PINNED.size).map { it.code })
    }

    @Test
    fun `the default currency is first`() {
        assertEquals("INR", all.first().code)
    }

    @Test
    fun `the rest are alphabetical`() {
        val rest = all.drop(CurrencyOption.PINNED.size).map { it.code }
        assertEquals(rest.sorted(), rest)
    }

    @Test
    fun `withdrawn currencies are left out`() {
        val codes = all.map { it.code }
        assertFalse("TRL is the pre-2005 Turkish lira", codes.contains("TRL"))
        assertFalse("ZWD was withdrawn in 2009", codes.contains("ZWD"))
        assertTrue(codes.contains("INR"))
        assertTrue(codes.contains("AED"))
        assertTrue(codes.contains("USD"))
    }

    @Test
    fun `every pinned currency is present`() {
        val codes = all.map { it.code }
        CurrencyOption.PINNED.forEach { assertTrue("missing $it", codes.contains(it)) }
    }

    @Test
    fun `search matches code, name and symbol`() {
        val inr = all.first { it.code == "INR" }
        assertTrue(inr.matches("inr"))
        assertTrue(inr.matches("Rupee"))
        assertTrue(inr.matches("₹"))
        assertTrue(inr.matches(""))
        assertFalse(inr.matches("peso"))
    }
}

package com.example.budgetapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySpendTest {
    private val food = Category(id = 1, name = "Food", createdAt = 0)
    private val stay = Category(id = 2, name = "Stay", createdAt = 0)
    private fun e(cents: Long, cat: Long?) = Expense(budgetId = 1, amountCents = cents, categoryId = cat)

    @Test
    fun `groups by category, largest first, uncategorised separate`() {
        val result = categoryBreakdown(
            listOf(e(100, 1), e(300, 2), e(50, null), e(50, 1)),
            listOf(food, stay),
        )
        assertEquals(listOf("Stay", "Food", UNCATEGORISED), result.map { it.name })
        assertEquals(listOf(300L, 150L, 50L), result.map { it.spentCents })
        assertEquals(2, result[1].expenseCount)
        assertEquals(0.6f, result[0].share, 0.0001f)
    }

    @Test
    fun `a deleted category counts as uncategorised`() {
        val result = categoryBreakdown(listOf(e(100, 9), e(100, null)), listOf(food))
        assertEquals(1, result.size)
        assertEquals(null, result.single().categoryId)
        assertEquals(2, result.single().expenseCount)
    }

    @Test
    fun `nothing to show without spending`() {
        assertTrue(categoryBreakdown(emptyList(), listOf(food)).isEmpty())
    }
}

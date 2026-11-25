package com.example.budgetapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseTest {

    @Test
    fun `an expense saved with just an amount shows a placeholder name`() {
        val expense = Expense(budgetId = 1, amountCents = 500)
        assertFalse(expense.hasTitle)
        assertEquals(Expense.UNTITLED, expense.displayTitle)
    }

    @Test
    fun `a blank name counts as no name`() {
        assertEquals(Expense.UNTITLED, Expense(budgetId = 1, title = "   ", amountCents = 1).displayTitle)
    }

    @Test
    fun `a named expense shows its name`() {
        val expense = Expense(budgetId = 1, title = "Coffee", amountCents = 120)
        assertTrue(expense.hasTitle)
        assertEquals("Coffee", expense.displayTitle)
    }
}

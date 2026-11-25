package com.example.budgetapp.data.backup

import com.example.budgetapp.data.Budget
import com.example.budgetapp.data.Category
import com.example.budgetapp.data.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileTest {

    private val budgets = listOf(
        Budget(id = 1, name = "Goa trip", limitCents = 50_000_00, currencyCode = "INR", createdAt = 1_000, rating = 4),
        Budget(id = 2, name = "Groceries", limitCents = null, currencyCode = "EUR", createdAt = 2_000),
    )
    private val expenses = listOf(
        Expense(id = 10, budgetId = 1, title = "Hotel", amountCents = 12_000_00, createdAt = 1_500, rating = 5),
        Expense(id = 11, budgetId = 1, title = "", amountCents = 250_00, createdAt = 1_600),
        Expense(id = 12, budgetId = 2, title = "Bread", amountCents = 3_50, createdAt = 2_500, rating = 2, categoryId = 7),
    )
    private val categories = listOf(
        Category(id = 7, name = "Food", createdAt = 100),
        Category(id = 8, name = "Stay", createdAt = 200),
    )

    @Test
    fun `round trips budgets and expenses through JSON`() {
        val original = BackupFile.of(budgets, expenses, categories, now = 9_999)
        val restored = BackupFile.fromJson(original.toJson())

        assertEquals(original, restored)
        assertEquals(budgets, restored.budgets.map { it.toEntity() })
        assertEquals(expenses, restored.expenses.map { it.toEntity() })
        assertEquals(categories, restored.categories.map { it.toEntity() })
    }

    @Test
    fun `keeps a budget without a limit and an untitled expense`() {
        val restored = BackupFile.fromJson(BackupFile.of(budgets, expenses).toJson())

        assertNull(restored.budgets.single { it.id == 2L }.limitCents)
        assertEquals("", restored.expenses.single { it.id == 11L }.title)
    }

    @Test
    fun `reads a backup written before ratings existed`() {
        val old = """
            {"version":1,"exportedAt":5,
             "budgets":[{"id":1,"name":"Old","limitCents":null,"currencyCode":"USD","createdAt":1}],
             "expenses":[{"id":2,"budgetId":1,"title":"Tea","amountCents":100,"createdAt":2,"someFutureField":true}]}
        """.trimIndent()

        val restored = BackupFile.fromJson(old)

        assertNull(restored.budgets.single().rating)
        assertNull(restored.expenses.single().rating)
        assertEquals("Tea", restored.expenses.single().title)
        assertNull(restored.expenses.single().categoryId)
        assertTrue(restored.categories.isEmpty())
    }
}

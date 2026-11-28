package com.example.budgetapp.data

/** How much of a budget went to one category. [categoryId] null = uncategorised. */
data class CategorySpend(
    val categoryId: Long?,
    val name: String,
    val spentCents: Long,
    val expenseCount: Int,
    /** Share of the budget's total, 0f..1f. */
    val share: Float,
)

/**
 * Spending per category, largest first, with uncategorised expenses as their own line.
 * Expenses whose category no longer exists count as uncategorised.
 */
fun categoryBreakdown(expenses: List<Expense>, categories: List<Category>): List<CategorySpend> {
    val total = expenses.sumOf { it.amountCents }
    if (total <= 0L) return emptyList()
    val names = categories.associate { it.id to it.name }
    return expenses
        .groupBy { e -> e.categoryId?.takeIf { it in names } }
        .map { (id, group) ->
            val spent = group.sumOf { it.amountCents }
            CategorySpend(
                categoryId = id,
                name = id?.let(names::getValue) ?: UNCATEGORISED,
                spentCents = spent,
                expenseCount = group.size,
                share = spent.toFloat() / total.toFloat(),
            )
        }
        .sortedWith(compareByDescending<CategorySpend> { it.spentCents }.thenBy { it.name })
}

const val UNCATEGORISED = "Uncategorised"

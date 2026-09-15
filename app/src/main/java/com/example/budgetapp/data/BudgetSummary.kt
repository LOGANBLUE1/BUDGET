package com.example.budgetapp.data

/** A budget plus the aggregates the list screen needs, computed by SQL. */
data class BudgetSummary(
    val id: Long,
    val name: String,
    val limitCents: Long?,
    val currencyCode: String,
    val createdAt: Long,
    val spentCents: Long,
    val expenseCount: Int,
) {
    val remainingCents: Long? get() = limitCents?.let { it - spentCents }

    /** 0f..1f for the progress bar; null when the budget has no limit. */
    val progress: Float?
        get() = limitCents?.let { limit ->
            if (limit <= 0L) null else (spentCents.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        }

    val isOverBudget: Boolean get() = limitCents != null && spentCents > limitCents

    fun toBudget(): Budget = Budget(
        id = id,
        name = name,
        limitCents = limitCents,
        currencyCode = currencyCode,
        createdAt = createdAt,
    )
}

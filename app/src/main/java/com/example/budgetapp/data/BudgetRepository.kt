package com.example.budgetapp.data

import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
) {
    fun observeSummaries(): Flow<List<BudgetSummary>> = budgetDao.observeSummaries()

    fun observeBudget(id: Long): Flow<Budget?> = budgetDao.observeBudget(id)

    fun observeExpenses(budgetId: Long): Flow<List<Expense>> = expenseDao.observeForBudget(budgetId)

    suspend fun addBudget(name: String, limitCents: Long?, currencyCode: String): Long =
        budgetDao.insert(
            Budget(name = name, limitCents = limitCents, currencyCode = currencyCode)
        )

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    suspend fun addExpense(budgetId: Long, title: String, amountCents: Long) =
        expenseDao.insert(Expense(budgetId = budgetId, title = title, amountCents = amountCents))

    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    /**
     * Re-inserts a deleted expense with its original id, backing the undo action.
     * Fails harmlessly if the parent budget was deleted in the meantime.
     */
    suspend fun restoreExpense(expense: Expense) {
        runCatching { expenseDao.insert(expense) }
    }
}

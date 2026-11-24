package com.example.budgetapp.data

import androidx.room.withTransaction
import com.example.budgetapp.data.backup.BackupFile
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val db: BudgetDatabase,
) {
    private val budgetDao = db.budgetDao()
    private val expenseDao = db.expenseDao()

    fun observeSummaries(): Flow<List<BudgetSummary>> = budgetDao.observeSummaries()

    fun observeBudget(id: Long): Flow<Budget?> = budgetDao.observeBudget(id)

    fun observeExpenses(budgetId: Long): Flow<List<Expense>> = expenseDao.observeForBudget(budgetId)

    suspend fun addBudget(name: String, limitCents: Long?, currencyCode: String, rating: Int?): Long =
        budgetDao.insert(
            Budget(name = name, limitCents = limitCents, currencyCode = currencyCode, rating = rating)
        )

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    suspend fun addExpense(budgetId: Long, title: String, amountCents: Long, rating: Int?) =
        expenseDao.insert(
            Expense(budgetId = budgetId, title = title, amountCents = amountCents, rating = rating)
        )

    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    /**
     * Re-inserts a deleted expense with its original id, backing the undo action.
     * Fails harmlessly if the parent budget was deleted in the meantime.
     */
    suspend fun restoreExpense(expense: Expense) {
        runCatching { expenseDao.insert(expense) }
    }

    suspend fun isEmpty(): Boolean = budgetDao.count() == 0

    /** A snapshot of everything, read in one transaction so budgets and expenses agree. */
    suspend fun exportBackup(): BackupFile = db.withTransaction {
        BackupFile.of(budgetDao.getAll(), expenseDao.getAll())
    }

    /** Replaces all local data with [backup], keeping its ids so expenses stay attached. */
    suspend fun replaceAll(backup: BackupFile) = db.withTransaction {
        budgetDao.deleteAll() // expenses are removed by ON DELETE CASCADE
        budgetDao.insertAll(backup.budgets.map { it.toEntity() })
        val budgetIds = backup.budgets.mapTo(HashSet()) { it.id }
        // Skip any orphans rather than failing the whole restore on a foreign key.
        expenseDao.insertAll(backup.expenses.filter { it.budgetId in budgetIds }.map { it.toEntity() })
    }
}

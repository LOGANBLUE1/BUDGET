package com.example.budgetapp.data

import androidx.room.withTransaction
import com.example.budgetapp.data.backup.BackupFile
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val db: BudgetDatabase,
) {
    private val budgetDao = db.budgetDao()
    private val expenseDao = db.expenseDao()
    private val categoryDao = db.categoryDao()

    fun observeSummaries(): Flow<List<BudgetSummary>> = budgetDao.observeSummaries()

    fun observeBudget(id: Long): Flow<Budget?> = budgetDao.observeBudget(id)

    fun observeExpenses(budgetId: Long): Flow<List<Expense>> = expenseDao.observeForBudget(budgetId)

    suspend fun addBudget(name: String, limitCents: Long?, currencyCode: String, rating: Int?): Long =
        budgetDao.insert(
            Budget(name = name, limitCents = limitCents, currencyCode = currencyCode, rating = rating)
        )

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    suspend fun addExpense(
        budgetId: Long,
        title: String,
        amountCents: Long,
        rating: Int?,
        categoryId: Long?,
    ) = expenseDao.insert(
        Expense(
            budgetId = budgetId,
            title = title,
            amountCents = amountCents,
            rating = rating,
            categoryId = categoryId,
        )
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

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    fun observeCategoryUsage(): Flow<List<CategoryUsage>> = categoryDao.observeUsage()

    /**
     * Creates a category, or returns the existing one with the same name (ignoring case),
     * so the list never ends up with both "Food" and "food".
     */
    suspend fun addCategory(name: String): Long = db.withTransaction {
        val trimmed = name.trim()
        categoryDao.findByName(trimmed)?.id ?: categoryDao.insert(Category(name = trimmed))
    }

    /** Renames a category. Returns false if another category already has that name. */
    suspend fun renameCategory(category: Category, name: String): Boolean = db.withTransaction {
        val trimmed = name.trim()
        val clash = categoryDao.findByName(trimmed)
        if (clash != null && clash.id != category.id) return@withTransaction false
        categoryDao.update(category.copy(name = trimmed))
        true
    }

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun isEmpty(): Boolean = budgetDao.count() == 0

    /** A snapshot of everything, read in one transaction so budgets and expenses agree. */
    suspend fun exportBackup(): BackupFile = db.withTransaction {
        BackupFile.of(budgetDao.getAll(), expenseDao.getAll(), categoryDao.getAll())
    }

    /** Replaces all local data with [backup], keeping its ids so expenses stay attached. */
    suspend fun replaceAll(backup: BackupFile) = db.withTransaction {
        budgetDao.deleteAll() // expenses are removed by ON DELETE CASCADE
        categoryDao.deleteAll()
        categoryDao.insertAll(backup.categories.map { it.toEntity() })
        budgetDao.insertAll(backup.budgets.map { it.toEntity() })
        val budgetIds = backup.budgets.mapTo(HashSet()) { it.id }
        val categoryIds = backup.categories.mapTo(HashSet()) { it.id }
        // Skip orphans and drop dangling categories rather than failing the whole restore.
        expenseDao.insertAll(
            backup.expenses
                .filter { it.budgetId in budgetIds }
                .map { dto ->
                    dto.toEntity().let { e ->
                        if (e.categoryId != null && e.categoryId !in categoryIds) e.copy(categoryId = null) else e
                    }
                }
        )
    }
}

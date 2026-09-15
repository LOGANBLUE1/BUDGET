package com.example.budgetapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query(
        """
        SELECT b.id AS id,
               b.name AS name,
               b.limitCents AS limitCents,
               b.currencyCode AS currencyCode,
               b.createdAt AS createdAt,
               COALESCE(SUM(e.amountCents), 0) AS spentCents,
               COUNT(e.id) AS expenseCount
        FROM budgets b
        LEFT JOIN expenses e ON e.budgetId = b.id
        GROUP BY b.id
        ORDER BY b.createdAt DESC
        """
    )
    fun observeSummaries(): Flow<List<BudgetSummary>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun observeBudget(id: Long): Flow<Budget?>

    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}

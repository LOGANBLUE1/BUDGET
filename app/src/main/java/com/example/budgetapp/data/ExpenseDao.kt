package com.example.budgetapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE budgetId = :budgetId ORDER BY createdAt DESC, id DESC")
    fun observeForBudget(budgetId: Long): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Insert
    suspend fun insertAll(expenses: List<Expense>)

    @Query("SELECT * FROM expenses ORDER BY id")
    suspend fun getAll(): List<Expense>

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}

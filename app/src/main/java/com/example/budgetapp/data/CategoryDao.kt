package com.example.budgetapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A category plus how many expenses use it, for the manage screen. */
data class CategoryUsage(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val expenseCount: Int,
) {
    fun toCategory() = Category(id = id, name = name, createdAt = createdAt)
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Category>>

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.createdAt AS createdAt, COUNT(e.id) AS expenseCount
        FROM categories c
        LEFT JOIN expenses e ON e.categoryId = c.id
        GROUP BY c.id
        ORDER BY c.name COLLATE NOCASE
        """
    )
    fun observeUsage(): Flow<List<CategoryUsage>>

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Category?

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAll(): List<Category>

    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    /** Expenses that used it keep existing, with categoryId set to NULL. */
    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

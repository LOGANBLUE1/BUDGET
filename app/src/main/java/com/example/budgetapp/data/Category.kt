package com.example.budgetapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A label such as "Food" or "Transport" that expenses can optionally carry.
 * Categories are shared by every budget. Deleting one leaves its expenses uncategorised.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

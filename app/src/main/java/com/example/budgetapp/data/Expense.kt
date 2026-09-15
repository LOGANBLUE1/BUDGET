package com.example.budgetapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("budgetId")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val title: String,
    val amountCents: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

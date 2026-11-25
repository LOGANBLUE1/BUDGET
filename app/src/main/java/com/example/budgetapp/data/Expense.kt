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
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("budgetId"), Index("categoryId")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    /** Optional: an empty string means the expense was saved with just an amount. */
    val title: String = "",
    val amountCents: Long,
    val createdAt: Long = System.currentTimeMillis(),
    /** 1..5 stars, or null when unrated. */
    val rating: Int? = null,
    /** Optional [Category]; null when uncategorised. */
    val categoryId: Long? = null,
) {
    val hasTitle: Boolean get() = title.isNotBlank()

    /** What to show for an expense that may have been saved without a name. */
    val displayTitle: String get() = title.ifBlank { UNTITLED }

    companion object {
        const val UNTITLED = "Expense"
    }
}

package com.example.budgetapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A spending bucket. [limitCents] is optional: a budget without a limit is just a
 * container that tracks a running total.
 *
 * [currencyCode] is picked when the budget is created and then fixed — every expense
 * inside a budget is in the budget's currency, so amounts inside it are always comparable.
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val limitCents: Long? = null,
    @ColumnInfo(defaultValue = "INR")
    val currencyCode: String = DEFAULT_CURRENCY,
    val createdAt: Long = System.currentTimeMillis(),
    /** Overall 1..5 star rating for the budget / trip, or null when unrated. */
    val rating: Int? = null,
) {
    companion object {
        const val DEFAULT_CURRENCY = "INR"
    }
}

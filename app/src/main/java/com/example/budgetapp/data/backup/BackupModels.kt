package com.example.budgetapp.data.backup

import com.example.budgetapp.data.Budget
import com.example.budgetapp.data.Category
import com.example.budgetapp.data.Expense
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The whole database as one JSON document. This is what goes to Google Drive.
 *
 * The DTOs mirror the Room entities on purpose but stay separate from them, so a later
 * schema change can't silently change the backup format. Older backups simply miss the
 * newer fields, and the defaults fill them in.
 */
@Serializable
data class BackupFile(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val budgets: List<BudgetDto>,
    val expenses: List<ExpenseDto>,
    /** Added after the first release; older backups have none. */
    val categories: List<CategoryDto> = emptyList(),
) {
    fun toJson(): String = json.encodeToString(this)

    companion object {
        const val CURRENT_VERSION = 1

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromJson(text: String): BackupFile = json.decodeFromString(serializer(), text)

        fun of(
            budgets: List<Budget>,
            expenses: List<Expense>,
            categories: List<Category> = emptyList(),
            now: Long = System.currentTimeMillis(),
        ) = BackupFile(
            exportedAt = now,
            budgets = budgets.map(BudgetDto::from),
            expenses = expenses.map(ExpenseDto::from),
            categories = categories.map(CategoryDto::from),
        )
    }
}

@Serializable
data class BudgetDto(
    val id: Long,
    val name: String,
    val limitCents: Long? = null,
    val currencyCode: String = Budget.DEFAULT_CURRENCY,
    val createdAt: Long,
    val rating: Int? = null,
    /** Added in schema v5; older backups have none, so their budgets restore as live. */
    val closedAt: Long? = null,
) {
    fun toEntity() = Budget(
        id = id,
        name = name,
        limitCents = limitCents,
        currencyCode = currencyCode,
        createdAt = createdAt,
        rating = rating,
        closedAt = closedAt,
    )

    companion object {
        fun from(b: Budget) =
            BudgetDto(b.id, b.name, b.limitCents, b.currencyCode, b.createdAt, b.rating, b.closedAt)
    }
}

@Serializable
data class ExpenseDto(
    val id: Long,
    val budgetId: Long,
    val title: String = "",
    val amountCents: Long,
    val createdAt: Long,
    val rating: Int? = null,
    val categoryId: Long? = null,
) {
    fun toEntity() = Expense(
        id = id,
        budgetId = budgetId,
        title = title,
        amountCents = amountCents,
        createdAt = createdAt,
        rating = rating,
        categoryId = categoryId,
    )

    companion object {
        fun from(e: Expense) =
            ExpenseDto(e.id, e.budgetId, e.title, e.amountCents, e.createdAt, e.rating, e.categoryId)
    }
}

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val createdAt: Long,
) {
    fun toEntity() = Category(id = id, name = name, createdAt = createdAt)

    companion object {
        fun from(c: Category) = CategoryDto(c.id, c.name, c.createdAt)
    }
}

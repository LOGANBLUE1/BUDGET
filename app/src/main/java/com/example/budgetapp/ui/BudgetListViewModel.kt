package com.example.budgetapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetapp.BudgetApplication
import com.example.budgetapp.data.BudgetRepository
import com.example.budgetapp.data.BudgetSummary
import com.example.budgetapp.data.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Spending summed over the budgets sharing one currency. */
data class CurrencyTotal(
    val currencyCode: String,
    val spentCents: Long,
    val limitCents: Long?,
    val budgetCount: Int,
)

data class BudgetListUiState(
    val budgets: List<BudgetSummary> = emptyList(),
    /** For the quick-add expense dialog. */
    val categories: List<Category> = emptyList(),
    val loading: Boolean = true,
) {
    /** Budgets still in use, newest first: the left tab. */
    val live: List<BudgetSummary> get() = budgets.filterNot { it.isClosed }

    /** Closed budgets, most recently closed first: the right tab. */
    val closed: List<BudgetSummary>
        get() = budgets.filter { it.isClosed }.sortedByDescending { it.closedAt }
}

/**
 * Budgets in different currencies can't be added together, so the header reports one
 * total per currency rather than a single meaningless number.
 */
fun currencyTotals(budgets: List<BudgetSummary>): List<CurrencyTotal> =
    budgets
        .groupBy { it.currencyCode }
        .map { (code, group) ->
            CurrencyTotal(
                currencyCode = code,
                spentCents = group.sumOf { it.spentCents },
                limitCents = group.mapNotNull { it.limitCents }
                    .takeIf { it.isNotEmpty() }?.sum(),
                budgetCount = group.size,
            )
        }
        .sortedByDescending { it.spentCents }

class BudgetListViewModel(
    private val repository: BudgetRepository,
    private val quickAdd: QuickAddShortcut,
) : ViewModel() {

    val uiState: StateFlow<BudgetListUiState> =
        combine(repository.observeSummaries(), repository.observeCategories()) { budgets, categories ->
            BudgetListUiState(budgets = budgets, categories = categories, loading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetListUiState(),
        )

    fun addBudget(name: String, limitCents: Long?, currencyCode: String, rating: Int?) {
        viewModelScope.launch { repository.addBudget(name.trim(), limitCents, currencyCode, rating) }
    }

    /** Name, limit and rating are editable after creation; the currency is not. */
    fun editBudget(summary: BudgetSummary, name: String, limitCents: Long?, rating: Int?) {
        viewModelScope.launch {
            repository.updateBudget(
                summary.toBudget().copy(name = name.trim(), limitCents = limitCents, rating = rating)
            )
        }
    }

    /** Quick add from a budget card, without opening the budget. */
    fun addExpense(
        summary: BudgetSummary,
        title: String,
        amountCents: Long,
        rating: Int?,
        categoryId: Long?,
        createdAt: Long,
    ) {
        viewModelScope.launch {
            repository.addExpense(summary.id, title.trim(), amountCents, rating, categoryId, createdAt)
            quickAdd.remember(summary.toBudget())
        }
    }

    suspend fun createCategory(name: String): Long = repository.addCategory(name)

    fun closeBudget(summary: BudgetSummary) {
        viewModelScope.launch { repository.closeBudget(summary.id) }
    }

    fun reopenBudget(summary: BudgetSummary) {
        viewModelScope.launch { repository.reopenBudget(summary.id) }
    }

    fun deleteBudget(summary: BudgetSummary) {
        viewModelScope.launch { repository.deleteBudget(summary.toBudget()) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BudgetApplication
                BudgetListViewModel(app.repository, app.quickAdd)
            }
        }
    }
}

package com.example.budgetapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetapp.BudgetApplication
import com.example.budgetapp.data.BudgetRepository
import com.example.budgetapp.data.BudgetSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    val loading: Boolean = true,
) {
    /**
     * Budgets in different currencies can't be added together, so the header reports one
     * total per currency rather than a single meaningless number.
     */
    val totals: List<CurrencyTotal>
        get() = budgets
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
}

class BudgetListViewModel(private val repository: BudgetRepository) : ViewModel() {

    val uiState: StateFlow<BudgetListUiState> = repository.observeSummaries()
        .map { BudgetListUiState(budgets = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetListUiState(),
        )

    fun addBudget(name: String, limitCents: Long?, currencyCode: String) {
        viewModelScope.launch { repository.addBudget(name.trim(), limitCents, currencyCode) }
    }

    /** Name and limit are editable after creation; the currency is not. */
    fun editBudget(summary: BudgetSummary, name: String, limitCents: Long?) {
        viewModelScope.launch {
            repository.updateBudget(
                summary.toBudget().copy(name = name.trim(), limitCents = limitCents)
            )
        }
    }

    fun deleteBudget(summary: BudgetSummary) {
        viewModelScope.launch { repository.deleteBudget(summary.toBudget()) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BudgetApplication
                BudgetListViewModel(app.repository)
            }
        }
    }
}

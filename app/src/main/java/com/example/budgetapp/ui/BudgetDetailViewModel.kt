package com.example.budgetapp.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetapp.BudgetApplication
import com.example.budgetapp.data.Budget
import com.example.budgetapp.data.BudgetRepository
import com.example.budgetapp.data.Expense
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetDetailUiState(
    val budget: Budget? = null,
    val expenses: List<Expense> = emptyList(),
    val loading: Boolean = true,
) {
    val spentCents: Long get() = expenses.sumOf { it.amountCents }
    val remainingCents: Long? get() = budget?.limitCents?.let { it - spentCents }
    val isOverBudget: Boolean get() = remainingCents?.let { it < 0 } == true

    val progress: Float?
        get() = budget?.limitCents?.let { limit ->
            if (limit <= 0L) null else (spentCents.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        }

    val averageCents: Long get() = if (expenses.isEmpty()) 0L else spentCents / expenses.size

    /** True once the budget is known to be gone (deleted), so the screen can pop. */
    val deleted: Boolean get() = !loading && budget == null
}

class BudgetDetailViewModel(
    private val repository: BudgetRepository,
    val budgetId: Long,
) : ViewModel() {

    val uiState: StateFlow<BudgetDetailUiState> = combine(
        repository.observeBudget(budgetId),
        repository.observeExpenses(budgetId),
    ) { budget, expenses ->
        BudgetDetailUiState(budget = budget, expenses = expenses, loading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetDetailUiState(),
    )

    fun addExpense(title: String, amountCents: Long) {
        viewModelScope.launch { repository.addExpense(budgetId, title.trim(), amountCents) }
    }

    fun updateExpense(expense: Expense, title: String, amountCents: Long) {
        viewModelScope.launch {
            repository.updateExpense(expense.copy(title = title.trim(), amountCents = amountCents))
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun restoreExpense(expense: Expense) {
        viewModelScope.launch { repository.restoreExpense(expense) }
    }

    fun updateBudget(name: String, limitCents: Long?) {
        val current = uiState.value.budget ?: return
        viewModelScope.launch {
            repository.updateBudget(current.copy(name = name.trim(), limitCents = limitCents))
        }
    }

    fun deleteBudget() {
        val current = uiState.value.budget ?: return
        viewModelScope.launch { repository.deleteBudget(current) }
    }

    companion object {
        const val BUDGET_ID_ARG = "budgetId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BudgetApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<Long>(BUDGET_ID_ARG) ?: 0L
                BudgetDetailViewModel(app.repository, id)
            }
        }
    }
}

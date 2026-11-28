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
import com.example.budgetapp.data.Category
import com.example.budgetapp.data.CategorySpend
import com.example.budgetapp.data.UNCATEGORISED
import com.example.budgetapp.data.categoryBreakdown
import com.example.budgetapp.data.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which expenses the list shows: one category (null id = uncategorised). */
data class CategoryFilter(val categoryId: Long?)

data class BudgetDetailUiState(
    val budget: Budget? = null,
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: CategoryFilter? = null,
    val loading: Boolean = true,
) {
    /** Spending per category, for the "By category" card. */
    val breakdown: List<CategorySpend> by lazy { categoryBreakdown(expenses, categories) }

    /** Worth showing only when at least one expense has a category. */
    val showBreakdown: Boolean get() = breakdown.any { it.categoryId != null }

    /** The expenses the list shows after [filter]. Totals always cover all of them. */
    val visibleExpenses: List<Expense> by lazy {
        val f = filter ?: return@lazy expenses
        val known = categories.mapTo(HashSet()) { it.id }
        expenses.filter { e -> e.categoryId?.takeIf { it in known } == f.categoryId }
    }

    val filterName: String?
        get() = filter?.let { f -> breakdown.firstOrNull { it.categoryId == f.categoryId }?.name ?: UNCATEGORISED }

    val spentCents: Long get() = expenses.sumOf { it.amountCents }
    val remainingCents: Long? get() = budget?.limitCents?.let { it - spentCents }
    val isOverBudget: Boolean get() = remainingCents?.let { it < 0 } == true

    val progress: Float?
        get() = budget?.limitCents?.let { limit ->
            if (limit <= 0L) null else (spentCents.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        }

    private val categoryNames: Map<Long, String> by lazy { categories.associate { it.id to it.name } }

    fun categoryName(expense: Expense): String? = expense.categoryId?.let(categoryNames::get)

    val averageCents: Long get() = if (expenses.isEmpty()) 0L else spentCents / expenses.size

    /** A closed budget is read-only: no adding, editing or deleting expenses. */
    val isClosed: Boolean get() = budget?.isClosed == true

    /** True once the budget is known to be gone (deleted), so the screen can pop. */
    val deleted: Boolean get() = !loading && budget == null
}

class BudgetDetailViewModel(
    private val repository: BudgetRepository,
    private val quickAdd: QuickAddShortcut,
    val budgetId: Long,
    /** Opened from the launcher shortcut: show the Add expense dialog straight away. */
    val openAddExpense: Boolean = false,
) : ViewModel() {

    private val filter = MutableStateFlow<CategoryFilter?>(null)

    val uiState: StateFlow<BudgetDetailUiState> = combine(
        repository.observeBudget(budgetId),
        repository.observeExpenses(budgetId),
        repository.observeCategories(),
        filter,
    ) { budget, expenses, categories, filter ->
        BudgetDetailUiState(
            budget = budget,
            expenses = expenses,
            categories = categories,
            filter = filter,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetDetailUiState(),
    )

    /** Tapping the selected category again clears the filter. */
    fun toggleFilter(categoryId: Long?) {
        filter.update { if (it?.categoryId == categoryId && it != null) null else CategoryFilter(categoryId) }
    }

    fun clearFilter() {
        filter.value = null
    }

    fun addExpense(title: String, amountCents: Long, rating: Int?, categoryId: Long?, createdAt: Long) {
        viewModelScope.launch {
            repository.addExpense(budgetId, title.trim(), amountCents, rating, categoryId, createdAt)
            uiState.value.budget?.let(quickAdd::remember)
        }
    }

    fun updateExpense(
        expense: Expense,
        title: String,
        amountCents: Long,
        rating: Int?,
        categoryId: Long?,
        createdAt: Long,
    ) {
        viewModelScope.launch {
            repository.updateExpense(
                expense.copy(
                    title = title.trim(),
                    amountCents = amountCents,
                    rating = rating,
                    categoryId = categoryId,
                    createdAt = createdAt,
                )
            )
        }
    }

    /** Creates (or reuses) a category by name and returns its id. */
    suspend fun createCategory(name: String): Long = repository.addCategory(name)

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun restoreExpense(expense: Expense) {
        viewModelScope.launch { repository.restoreExpense(expense) }
    }

    fun updateBudget(name: String, limitCents: Long?, rating: Int?) {
        val current = uiState.value.budget ?: return
        viewModelScope.launch {
            repository.updateBudget(
                current.copy(name = name.trim(), limitCents = limitCents, rating = rating)
            )
        }
    }

    fun closeBudget() {
        viewModelScope.launch { repository.closeBudget(budgetId) }
    }

    fun reopenBudget() {
        viewModelScope.launch { repository.reopenBudget(budgetId) }
    }

    fun deleteBudget() {
        val current = uiState.value.budget ?: return
        viewModelScope.launch { repository.deleteBudget(current) }
    }

    companion object {
        const val BUDGET_ID_ARG = "budgetId"
        const val ADD_EXPENSE_ARG = "addExpense"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BudgetApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<Long>(BUDGET_ID_ARG) ?: 0L
                val add = handle.get<Boolean>(ADD_EXPENSE_ARG) ?: false
                BudgetDetailViewModel(app.repository, app.quickAdd, id, add)
            }
        }
    }
}

package com.example.budgetapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetapp.BudgetApplication
import com.example.budgetapp.data.BudgetRepository
import com.example.budgetapp.data.CategoryUsage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<CategoryUsage> = emptyList(),
    val loading: Boolean = true,
)

class CategoriesViewModel(private val repository: BudgetRepository) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = repository.observeCategoryUsage()
        .map { CategoriesUiState(categories = it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun add(name: String) {
        viewModelScope.launch { repository.addCategory(name) }
    }

    fun rename(category: CategoryUsage, name: String) {
        viewModelScope.launch {
            if (!repository.renameCategory(category.toCategory(), name)) {
                _messages.send("A category called \"${name.trim()}\" already exists")
            }
        }
    }

    fun delete(category: CategoryUsage) {
        viewModelScope.launch { repository.deleteCategory(category.toCategory()) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BudgetApplication
                CategoriesViewModel(app.repository)
            }
        }
    }
}

package com.example.budgetapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.data.Budget
import com.example.budgetapp.data.Expense
import com.example.budgetapp.util.Dates
import com.example.budgetapp.util.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    onBack: () -> Unit,
    viewModel: BudgetDetailViewModel = viewModel(factory = BudgetDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Every amount on this screen — and in the dialogs it opens — is in the budget's
    // own currency, which was fixed when the budget was created.
    val currencyCode = state.budget?.currencyCode ?: Budget.DEFAULT_CURRENCY
    val formatter = remember(currencyCode) { MoneyFormatter(currencyCode) }

    var showAddExpense by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingBudget by remember { mutableStateOf(false) }
    var confirmDeleteBudget by remember { mutableStateOf(false) }

    // The budget row is gone (deleted here or elsewhere) — leave the screen.
    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    fun deleteExpense(expense: Expense) {
        viewModel.deleteExpense(expense)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${expense.title}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreExpense(expense)
            }
        }
    }

    CompositionLocalProvider(LocalMoneyFormatter provides formatter) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.budget?.name ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { editingBudget = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit budget")
                        }
                        IconButton(onClick = { confirmDeleteBudget = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete budget")
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddExpense = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add expense") },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") { SummaryCard(state) }

                if (state.expenses.isEmpty()) {
                    item(key = "empty") { EmptyExpenses() }
                } else {
                    item(key = "header") {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(state.expenses, key = { it.id }) { expense ->
                        ExpenseRow(
                            expense = expense,
                            onClick = { editingExpense = expense },
                            onDelete = { deleteExpense(expense) },
                        )
                    }
                }
            }
        }

        if (showAddExpense) {
            ExpenseEditorDialog(
                title = "Add expense",
                confirmLabel = "Add",
                onDismiss = { showAddExpense = false },
                onConfirm = { title, cents ->
                    viewModel.addExpense(title, cents)
                    showAddExpense = false
                },
            )
        }

        editingExpense?.let { target ->
            ExpenseEditorDialog(
                title = "Edit expense",
                confirmLabel = "Save",
                initialTitle = target.title,
                initialAmountCents = target.amountCents,
                onDismiss = { editingExpense = null },
                onConfirm = { title, cents ->
                    viewModel.updateExpense(target, title, cents)
                    editingExpense = null
                },
                onDelete = {
                    editingExpense = null
                    deleteExpense(target)
                },
            )
        }

        val budgetBeingEdited = state.budget
        if (editingBudget && budgetBeingEdited != null) {
            BudgetEditorDialog(
                title = "Edit budget",
                confirmLabel = "Save",
                editing = true,
                initialName = budgetBeingEdited.name,
                initialLimitCents = budgetBeingEdited.limitCents,
                initialCurrencyCode = budgetBeingEdited.currencyCode,
                onDismiss = { editingBudget = false },
                onConfirm = { name, limit, _ ->
                    viewModel.updateBudget(name, limit)
                    editingBudget = false
                },
            )
        }

        if (confirmDeleteBudget) {
            ConfirmDialog(
                title = "Delete this budget?",
                message = "The budget and all ${state.expenses.size} of its expenses will be removed.",
                onDismiss = { confirmDeleteBudget = false },
                onConfirm = {
                    confirmDeleteBudget = false
                    viewModel.deleteBudget()
                },
            )
        }
    }
}

@Composable
private fun SummaryCard(state: BudgetDetailUiState) {
    val formatter = LocalMoneyFormatter.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Spent", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatter.format(state.spentCents),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )

            val limit = state.budget?.limitCents
            if (limit != null) {
                Spacer(Modifier.height(4.dp))
                Text("of ${formatter.format(limit)}", style = MaterialTheme.typography.bodyMedium)

                state.progress?.let { progress ->
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (state.isOverBudget) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Stat(label = "Expenses", value = state.expenses.size.toString())
                Stat(label = "Average", value = formatter.format(state.averageCents))
                val remaining = state.remainingCents
                Stat(
                    label = if (remaining != null && remaining < 0) "Over by" else "Left",
                    value = when {
                        remaining == null -> "—"
                        remaining < 0 -> formatter.format(-remaining)
                        else -> formatter.format(remaining)
                    },
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val formatter = LocalMoneyFormatter.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Dates.format(expense.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatter.format(expense.amountCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${expense.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyExpenses() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text("No expenses yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap Add expense to record your first one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

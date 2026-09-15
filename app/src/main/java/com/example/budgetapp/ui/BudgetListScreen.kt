package com.example.budgetapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.data.BudgetSummary
import com.example.budgetapp.util.MoneyFormatter
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    onOpenBudget: (Long) -> Unit,
    viewModel: BudgetListViewModel = viewModel(factory = BudgetListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BudgetSummary?>(null) }
    var deleting by remember { mutableStateOf<BudgetSummary?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budgets") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New budget") },
            )
        },
    ) { padding ->
        if (!state.loading && state.budgets.isEmpty()) {
            EmptyBudgets(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.budgets.isNotEmpty()) {
                    item(key = "totals") { GrandTotalCard(state) }
                }
                items(state.budgets, key = { it.id }) { summary ->
                    BudgetCard(
                        summary = summary,
                        onClick = { onOpenBudget(summary.id) },
                        onEdit = { editing = summary },
                        onDelete = { deleting = summary },
                    )
                }
            }
        }
    }

    if (showCreate) {
        BudgetEditorDialog(
            title = "New budget",
            confirmLabel = "Create",
            editing = false,
            onDismiss = { showCreate = false },
            onConfirm = { name, limit, currency ->
                viewModel.addBudget(name, limit, currency)
                showCreate = false
            },
        )
    }

    editing?.let { target ->
        BudgetEditorDialog(
            title = "Edit budget",
            confirmLabel = "Save",
            editing = true,
            initialName = target.name,
            initialLimitCents = target.limitCents,
            initialCurrencyCode = target.currencyCode,
            onDismiss = { editing = null },
            onConfirm = { name, limit, _ ->
                viewModel.editBudget(target, name, limit)
                editing = null
            },
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = "Delete \"${target.name}\"?",
            message = "This also deletes its ${target.expenseCount} expense" +
                if (target.expenseCount == 1) "." else "s.",
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.deleteBudget(target)
                deleting = null
            },
        )
    }
}

@Composable
private fun GrandTotalCard(state: BudgetListUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Total spent", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            state.totals.forEachIndexed { index, total ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                val formatter = remember(total.currencyCode) { MoneyFormatter(total.currencyCode) }
                Text(
                    text = formatter.format(total.spentCents),
                    style = if (state.totals.size == 1) MaterialTheme.typography.headlineMedium
                    else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val detail = buildString {
                    total.limitCents?.let { append("of ${formatter.format(it)} budgeted ") }
                    append("across ${total.budgetCount} budget")
                    if (total.budgetCount != 1) append("s")
                }
                Text(text = detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BudgetCard(
    summary: BudgetSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formatter = remember(summary.currencyCode) { MoneyFormatter(summary.currencyCode) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = summary.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${summary.currencyCode} · ${summary.expenseCount} expense" +
                            if (summary.expenseCount == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Budget options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = formatter.format(summary.spentCents),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (summary.isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
                summary.limitCents?.let { limit ->
                    Text(
                        text = "of ${formatter.format(limit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val progress = summary.progress
            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (summary.isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                val remaining = summary.remainingCents ?: 0L
                Text(
                    text = if (remaining >= 0) "${formatter.format(remaining)} left"
                    else "${formatter.format(-remaining)} over budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyBudgets(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("No budgets yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Create a budget, then add expenses to it to watch the total add up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

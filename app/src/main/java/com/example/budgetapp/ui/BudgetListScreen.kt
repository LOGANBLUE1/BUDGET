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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.data.BudgetSummary
import com.example.budgetapp.util.Dates
import com.example.budgetapp.util.MoneyFormatter
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    onOpenBudget: (Long) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenCategories: () -> Unit,
    viewModel: BudgetListViewModel = viewModel(factory = BudgetListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BudgetSummary?>(null) }
    var deleting by remember { mutableStateOf<BudgetSummary?>(null) }
    var addingTo by remember { mutableStateOf<BudgetSummary?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Page 0 (left) holds live budgets, page 1 (right) closed ones.
    val pagerState = rememberPagerState(pageCount = { 2 })

    fun closeBudget(summary: BudgetSummary) {
        viewModel.closeBudget(summary)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Closed \"${summary.name}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.reopenBudget(summary)
        }
    }

    fun reopenBudget(summary: BudgetSummary) {
        viewModel.reopenBudget(summary)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Re-opened \"${summary.name}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.closeBudget(summary)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                actions = {
                    IconButton(onClick = onOpenCategories) {
                        Icon(Icons.Outlined.Category, contentDescription = "Categories")
                    }
                    IconButton(onClick = onOpenBackup) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Backup & sync")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // New budgets are always live, so the button only belongs on the Live tab.
            if (pagerState.currentPage == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New budget") },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                listOf("Live" to state.live.size, "Closed" to state.closed.size)
                    .forEachIndexed { index, (label, count) ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(if (state.loading) label else "$label ($count)") },
                        )
                    }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val closedTab = page == 1
                val budgets = if (closedTab) state.closed else state.live
                if (!state.loading && budgets.isEmpty()) {
                    if (closedTab) EmptyClosedBudgets() else EmptyBudgets()
                } else {
                    BudgetPage(
                        budgets = budgets,
                        closedTab = closedTab,
                        onOpenBudget = onOpenBudget,
                        onEdit = { editing = it },
                        onAddExpense = { addingTo = it },
                        onClose = ::closeBudget,
                        onReopen = ::reopenBudget,
                        onDelete = { deleting = it },
                    )
                }
            }
        }
    }

    addingTo?.let { target ->
        val formatter = remember(target.currencyCode) { MoneyFormatter(target.currencyCode) }
        CompositionLocalProvider(LocalMoneyFormatter provides formatter) {
            ExpenseEditorDialog(
                title = "Add to ${target.name}",
                confirmLabel = "Add",
                categories = state.categories,
                onCreateCategory = viewModel::createCategory,
                onDismiss = { addingTo = null },
                onConfirm = { title, cents, rating, categoryId, createdAt ->
                    viewModel.addExpense(target, title, cents, rating, categoryId, createdAt)
                    addingTo = null
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Added ${formatter.format(cents)} to ${target.name}")
                    }
                },
            )
        }
    }

    if (showCreate) {
        BudgetEditorDialog(
            title = "New budget",
            confirmLabel = "Create",
            editing = false,
            onDismiss = { showCreate = false },
            onConfirm = { name, limit, currency, rating ->
                viewModel.addBudget(name, limit, currency, rating)
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
            initialRating = target.rating,
            onDismiss = { editing = null },
            onConfirm = { name, limit, _, rating ->
                viewModel.editBudget(target, name, limit, rating)
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
private fun BudgetPage(
    budgets: List<BudgetSummary>,
    closedTab: Boolean,
    onOpenBudget: (Long) -> Unit,
    onEdit: (BudgetSummary) -> Unit,
    onAddExpense: (BudgetSummary) -> Unit,
    onClose: (BudgetSummary) -> Unit,
    onReopen: (BudgetSummary) -> Unit,
    onDelete: (BudgetSummary) -> Unit,
) {
    val totals = remember(budgets) { currencyTotals(budgets) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (totals.isNotEmpty()) {
            item(key = "totals") {
                GrandTotalCard(
                    label = if (closedTab) "Spent in closed budgets" else "Total spent",
                    totals = totals,
                )
            }
        }
        items(budgets, key = { it.id }) { summary ->
            BudgetCard(
                summary = summary,
                onClick = { onOpenBudget(summary.id) },
                onEdit = { onEdit(summary) },
                onAddExpense = { onAddExpense(summary) },
                onClose = { onClose(summary) },
                onReopen = { onReopen(summary) },
                onDelete = { onDelete(summary) },
            )
        }
    }
}

@Composable
private fun GrandTotalCard(label: String, totals: List<CurrencyTotal>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            totals.forEachIndexed { index, total ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                val formatter = remember(total.currencyCode) { MoneyFormatter(total.currencyCode) }
                Text(
                    text = formatter.format(total.spentCents),
                    style = if (totals.size == 1) MaterialTheme.typography.headlineMedium
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
    onAddExpense: () -> Unit,
    onClose: () -> Unit,
    onReopen: () -> Unit,
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
                    summary.closedAt?.let { closedAt ->
                        Text(
                            text = "Closed ${Dates.format(closedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (summary.rating != null) {
                        Spacer(Modifier.height(4.dp))
                        StarRatingDisplay(rating = summary.rating, size = 16.dp)
                    }
                }
                if (!summary.isClosed) {
                    FilledTonalIconButton(onClick = onAddExpense) {
                        Icon(Icons.Default.Add, contentDescription = "Add expense to ${summary.name}")
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Budget options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (summary.isClosed) {
                            DropdownMenuItem(
                                text = { Text("Re-open") },
                                onClick = {
                                    menuOpen = false
                                    onReopen()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    menuOpen = false
                                    onEdit()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Close") },
                                onClick = {
                                    menuOpen = false
                                    onClose()
                                },
                            )
                        }
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
    EmptyState(
        icon = Icons.Outlined.AccountBalanceWallet,
        title = "No live budgets",
        message = "Create a budget, then add expenses to it to watch the total add up.",
        modifier = modifier,
    )
}

@Composable
private fun EmptyClosedBudgets(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Inventory2,
        title = "No closed budgets",
        message = "Close a budget from its ⋮ menu when you're done with it. It moves here, and you can re-open it any time.",
        modifier = modifier,
    )
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

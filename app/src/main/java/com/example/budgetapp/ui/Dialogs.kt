package com.example.budgetapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.budgetapp.data.Budget
import com.example.budgetapp.util.MoneyFormatter

/**
 * Create or edit a budget.
 *
 * The currency is chosen here, at creation, and is then fixed for the life of the budget
 * so every expense inside it stays comparable — so [editing] hides the picker and shows
 * the currency as plain text instead.
 */
@Composable
fun BudgetEditorDialog(
    title: String,
    confirmLabel: String,
    editing: Boolean,
    initialName: String = "",
    initialLimitCents: Long? = null,
    initialCurrencyCode: String = Budget.DEFAULT_CURRENCY,
    onDismiss: () -> Unit,
    onConfirm: (name: String, limitCents: Long?, currencyCode: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var currencyCode by remember { mutableStateOf(initialCurrencyCode) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    val formatter = remember(currencyCode) { MoneyFormatter(currencyCode) }
    var limit by remember {
        mutableStateOf(initialLimitCents?.let { formatter.toEditableString(it) } ?: "")
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val limitCents = if (limit.isBlank()) null else formatter.parseToCents(limit)
    val limitValid = limit.isBlank() || limitCents != null
    val canConfirm = name.isNotBlank() && limitValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget name") },
                    placeholder = { Text("Groceries, Trip to Goa…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                if (editing) {
                    CurrencyLocked(formatter)
                } else {
                    CurrencySelector(
                        formatter = formatter,
                        onClick = { showCurrencyPicker = true },
                    )
                }

                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Limit (optional)") },
                    placeholder = { Text("0${formatter.decimalSeparator}00") },
                    prefix = { Text(formatter.symbol) },
                    singleLine = true,
                    isError = !limitValid,
                    supportingText = {
                        Text(
                            if (limitValid) "Leave empty to just track spending"
                            else "Enter a valid amount"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = { onConfirm(name.trim(), limitCents, currencyCode) },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            selectedCode = currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { code ->
                currencyCode = code
                showCurrencyPicker = false
            },
        )
    }
}

@Composable
private fun CurrencySelector(formatter: MoneyFormatter, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${formatter.symbol}  ${formatter.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change currency")
        }
    }
}

@Composable
private fun CurrencyLocked(formatter: MoneyFormatter) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Currency",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${formatter.symbol}  ${formatter.code}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "· fixed at creation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Create / edit an expense, always in its budget's currency. [onDelete] shows when editing. */
@Composable
fun ExpenseEditorDialog(
    title: String,
    confirmLabel: String,
    initialTitle: String = "",
    initialAmountCents: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amountCents: Long) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val formatter = LocalMoneyFormatter.current

    var expenseTitle by remember { mutableStateOf(initialTitle) }
    var amount by remember {
        mutableStateOf(initialAmountCents?.let { formatter.toEditableString(it) } ?: "")
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val amountCents = formatter.parseToCents(amount)
    val amountValid = amount.isBlank() || amountCents != null
    val canConfirm = expenseTitle.isNotBlank() && amountCents != null && amountCents > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = expenseTitle,
                    onValueChange = { expenseTitle = it },
                    label = { Text("What was it for?") },
                    placeholder = { Text("Coffee, bus fare…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (${formatter.code})") },
                    placeholder = { Text("0${formatter.decimalSeparator}00") },
                    prefix = { Text(formatter.symbol) },
                    singleLine = true,
                    isError = !amountValid,
                    supportingText = {
                        if (!amountValid) Text("Enter a valid amount")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = { onConfirm(expenseTitle.trim(), amountCents ?: 0L) },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

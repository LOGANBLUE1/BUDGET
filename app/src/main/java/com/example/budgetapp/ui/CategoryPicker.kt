package com.example.budgetapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.budgetapp.data.Category
import kotlinx.coroutines.launch

/**
 * Optional category for an expense: tap a chip to pick it, tap it again to clear it.
 * "New" adds a category in place and selects it, so you never have to leave the dialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: suspend (String) -> Long,
) {
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    fun submit() {
        val name = newName.trim()
        if (name.isEmpty()) return
        scope.launch {
            onSelect(onCreate(name))
            newName = ""
            adding = false
        }
    }

    Column {
        Text(
            text = "Category (optional)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            categories.forEach { category ->
                val selected = category.id == selectedId
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(if (selected) null else category.id) },
                    label = { Text(category.name) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
            if (!adding) {
                AssistChip(
                    onClick = { adding = true },
                    label = { Text("New") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) },
                )
            }
        }

        if (adding) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New category") },
                placeholder = { Text("Food, Transport…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    if (newName.isBlank()) {
                        IconButton(onClick = { adding = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel new category")
                        }
                    } else {
                        IconButton(onClick = { submit() }) {
                            Icon(Icons.Default.Check, contentDescription = "Add category")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        }
    }
}

/** A small tag that shows an expense's category in lists. */
@Composable
fun CategoryTag(name: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

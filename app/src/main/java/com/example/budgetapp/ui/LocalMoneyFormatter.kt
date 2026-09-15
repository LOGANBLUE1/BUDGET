package com.example.budgetapp.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.budgetapp.data.Budget
import com.example.budgetapp.util.MoneyFormatter

/**
 * The formatter for the budget currently in scope. Provided per budget — the detail
 * screen wraps its content in its budget's currency, so every amount and every dialog
 * below it formats and parses the same way.
 */
val LocalMoneyFormatter = staticCompositionLocalOf {
    MoneyFormatter(Budget.DEFAULT_CURRENCY)
}

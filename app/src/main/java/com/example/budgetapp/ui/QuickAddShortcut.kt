package com.example.budgetapp.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.budgetapp.MainActivity
import com.example.budgetapp.R
import com.example.budgetapp.data.Budget

/**
 * The "Add expense" launcher shortcut (long-press the app icon). It adds to the budget an
 * expense was last added to, so that's remembered here and shown in the shortcut's label.
 */
class QuickAddShortcut(context: Context) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences("quick_add", Context.MODE_PRIVATE)

    val lastBudgetId: Long?
        get() = prefs.getLong(KEY_BUDGET, 0L).takeIf { it != 0L }

    /** Call after an expense is added to [budget]. */
    fun remember(budget: Budget) {
        prefs.edit().putLong(KEY_BUDGET, budget.id).apply()
        val intent = Intent(context, MainActivity::class.java).setAction(ACTION_ADD_EXPENSE)
        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel("Add expense")
            .setLongLabel("Add expense to ${budget.name}")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_add))
            .setIntent(intent)
            .build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
    }

    companion object {
        const val ACTION_ADD_EXPENSE = "com.example.budgetapp.action.ADD_EXPENSE"
        private const val SHORTCUT_ID = "add_expense"
        private const val KEY_BUDGET = "last_budget_id"
    }
}

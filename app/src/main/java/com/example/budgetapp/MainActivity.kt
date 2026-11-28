package com.example.budgetapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.budgetapp.ui.BudgetNavHost
import com.example.budgetapp.ui.QuickAddShortcut
import com.example.budgetapp.ui.theme.BudgetAppTheme

class MainActivity : ComponentActivity() {

    /** Bumped for each "Add expense" launcher shortcut launch. */
    private var addExpenseRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after rotation the dialog is already open (or was closed).
        if (savedInstanceState == null) handle(intent)
        setContent {
            BudgetAppTheme {
                BudgetNavHost(addExpenseRequest = addExpenseRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == QuickAddShortcut.ACTION_ADD_EXPENSE) addExpenseRequests++
    }
}

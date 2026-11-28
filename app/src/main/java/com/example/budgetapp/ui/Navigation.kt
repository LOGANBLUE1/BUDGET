package com.example.budgetapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.budgetapp.BudgetApplication

object Routes {
    const val BUDGET_LIST = "budgets"
    const val BACKUP = "backup"
    const val CATEGORIES = "categories"
    const val BUDGET_DETAIL =
        "budgets/{${BudgetDetailViewModel.BUDGET_ID_ARG}}?${BudgetDetailViewModel.ADD_EXPENSE_ARG}=" +
            "{${BudgetDetailViewModel.ADD_EXPENSE_ARG}}"

    fun budgetDetail(id: Long, addExpense: Boolean = false) =
        "budgets/$id?${BudgetDetailViewModel.ADD_EXPENSE_ARG}=$addExpense"
}

/**
 * @param addExpenseRequest goes up each time the "Add expense" launcher shortcut is used;
 * the app then opens the last-used budget with its Add expense dialog.
 */
@Composable
fun BudgetNavHost(addExpenseRequest: Int = 0) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as BudgetApplication

    LaunchedEffect(addExpenseRequest) {
        if (addExpenseRequest == 0) return@LaunchedEffect
        val id = app.quickAdd.lastBudgetId ?: return@LaunchedEffect
        // The budget may have been deleted or closed since; then just show the list.
        val budget = app.repository.getBudget(id)
        if (budget == null || budget.isClosed) return@LaunchedEffect
        navController.navigate(Routes.budgetDetail(id, addExpense = true)) {
            popUpTo(Routes.BUDGET_LIST)
        }
    }

    NavHost(navController = navController, startDestination = Routes.BUDGET_LIST) {
        composable(Routes.BUDGET_LIST) {
            BudgetListScreen(
                onOpenBudget = { id -> navController.navigate(Routes.budgetDetail(id)) },
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.BUDGET_DETAIL,
            arguments = listOf(
                navArgument(BudgetDetailViewModel.BUDGET_ID_ARG) { type = NavType.LongType },
                navArgument(BudgetDetailViewModel.ADD_EXPENSE_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            BudgetDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

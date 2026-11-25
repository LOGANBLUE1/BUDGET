package com.example.budgetapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val BUDGET_LIST = "budgets"
    const val BACKUP = "backup"
    const val CATEGORIES = "categories"
    const val BUDGET_DETAIL = "budgets/{${BudgetDetailViewModel.BUDGET_ID_ARG}}"

    fun budgetDetail(id: Long) = "budgets/$id"
}

@Composable
fun BudgetNavHost() {
    val navController = rememberNavController()

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
                navArgument(BudgetDetailViewModel.BUDGET_ID_ARG) { type = NavType.LongType }
            ),
        ) {
            BudgetDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

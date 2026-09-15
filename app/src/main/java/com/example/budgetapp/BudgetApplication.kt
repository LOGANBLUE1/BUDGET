package com.example.budgetapp

import android.app.Application
import com.example.budgetapp.data.BudgetDatabase
import com.example.budgetapp.data.BudgetRepository

class BudgetApplication : Application() {

    val repository: BudgetRepository by lazy {
        val db = BudgetDatabase.get(this)
        BudgetRepository(db.budgetDao(), db.expenseDao())
    }
}

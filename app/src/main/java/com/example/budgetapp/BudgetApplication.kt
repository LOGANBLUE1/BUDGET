package com.example.budgetapp

import android.app.Application
import com.example.budgetapp.data.BudgetDatabase
import com.example.budgetapp.data.BudgetRepository
import com.example.budgetapp.data.backup.BackupManager
import com.example.budgetapp.data.backup.GoogleAuth
import com.example.budgetapp.data.backup.SyncPrefs

class BudgetApplication : Application() {

    private val database by lazy { BudgetDatabase.get(this) }

    val repository: BudgetRepository by lazy { BudgetRepository(database) }

    val syncPrefs: SyncPrefs by lazy { SyncPrefs(this) }

    val googleAuth: GoogleAuth by lazy { GoogleAuth(this) }

    val backupManager: BackupManager by lazy {
        BackupManager(this, database, repository, syncPrefs, googleAuth)
    }

    override fun onCreate() {
        super.onCreate()
        backupManager.start()
    }
}

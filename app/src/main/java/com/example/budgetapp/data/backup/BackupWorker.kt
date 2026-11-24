package com.example.budgetapp.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budgetapp.BudgetApplication
import java.io.IOException

/** Runs one scheduled Drive backup. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BudgetApplication
        if (!app.syncPrefs.current.syncEnabled) return Result.success()

        return try {
            app.backupManager.backupNow()
            Result.success()
        } catch (e: NeedsSignInException) {
            app.syncPrefs.recordError(e.message)
            Result.failure()
        } catch (e: DriveHttpException) {
            // 5xx and 429 are worth retrying; other errors won't fix themselves.
            if (e.code >= 500 || e.code == 429) {
                Result.retry()
            } else {
                app.syncPrefs.recordError("Backup failed (${e.code})")
                Result.failure()
            }
        } catch (e: IOException) {
            Result.retry()
        }
    }
}

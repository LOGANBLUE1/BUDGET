package com.example.budgetapp.data.backup

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.budgetapp.data.BudgetDatabase
import com.example.budgetapp.data.BudgetRepository
import java.util.concurrent.TimeUnit

/** Google wants the user to sign in again, so nothing can be done in the background. */
class NeedsSignInException : Exception("Sign in to Google again to keep backing up")

/**
 * Backs the whole database up to a single file in Google Drive and restores from it.
 *
 * While sync is on, any change to budgets or expenses schedules a backup a few seconds
 * later. Rapid edits fold into one upload, and WorkManager retries while offline.
 */
class BackupManager(
    context: Context,
    private val db: BudgetDatabase,
    private val repository: BudgetRepository,
    private val prefs: SyncPrefs,
    private val auth: GoogleAuth,
    private val drive: DriveBackupClient = DriveBackupClient(),
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** Starts watching the database; call once from Application.onCreate. */
    fun start() {
        db.invalidationTracker.addObserver(object : InvalidationTracker.Observer(arrayOf("budgets", "expenses")) {
            override fun onInvalidated(tables: Set<String>) {
                if (prefs.current.syncEnabled) scheduleBackup()
            }
        })
    }

    fun scheduleBackup(delaySeconds: Long = DEBOUNCE_SECONDS) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelScheduled() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    /** The backup currently in Drive, or null if there isn't one yet. */
    suspend fun findBackup(): RemoteBackup? = withToken { drive.find(it) }

    /**
     * Uploads a fresh snapshot. If [force] is false, an empty database never overwrites
     * an existing backup, so a fresh install can't wipe the cloud copy before it's restored.
     *
     * @return the upload time, or null if it was skipped.
     */
    suspend fun backupNow(force: Boolean = false): Long? {
        val snapshot = repository.exportBackup()
        val uploaded = withToken { token ->
            val existing = drive.find(token)
            if (!force && existing != null && snapshot.budgets.isEmpty()) return@withToken null
            drive.upload(token, snapshot.toJson(), existing)
        } ?: return null
        prefs.recordBackup(uploaded.modifiedAt)
        return uploaded.modifiedAt
    }

    /** Replaces local data with the Drive backup. Returns false if there was none. */
    suspend fun restore(): Boolean {
        val backup = withToken { token ->
            val remote = drive.find(token) ?: return@withToken null
            BackupFile.fromJson(drive.download(token, remote.fileId))
        } ?: return false
        repository.replaceAll(backup)
        return true
    }

    /** Runs [block] with an access token, minting a new one once if Drive rejects it. */
    private suspend fun <T> withToken(block: suspend (String) -> T): T {
        val token = auth.tokenOrNull() ?: throw NeedsSignInException()
        return try {
            block(token)
        } catch (e: DriveHttpException) {
            if (e.code != 401) throw e
            auth.invalidate(token)
            block(auth.tokenOrNull() ?: throw NeedsSignInException())
        }
    }

    companion object {
        const val WORK_NAME = "drive-backup"
        const val DEBOUNCE_SECONDS = 10L
    }
}

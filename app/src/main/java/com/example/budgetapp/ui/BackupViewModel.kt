package com.example.budgetapp.ui

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetapp.BudgetApplication
import com.example.budgetapp.data.BudgetRepository
import com.example.budgetapp.data.backup.BackupFile
import com.example.budgetapp.data.backup.BackupManager
import com.example.budgetapp.data.backup.GoogleAuth
import com.example.budgetapp.data.backup.NeedsSignInException
import com.example.budgetapp.data.backup.SyncPrefs
import com.example.budgetapp.data.backup.SyncState
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** A Drive backup exists but this device has no data. Offer to bring it back. */
data class RestoreOffer(val backupTime: Long, val enableSyncAfter: Boolean)

/** A backup file was picked and read; waiting for the user to confirm replacing everything. */
data class PendingImport(val backup: BackupFile) {
    val budgetCount: Int get() = backup.budgets.size
    val expenseCount: Int get() = backup.expenses.size
}

data class BackupUiState(
    val sync: SyncState = SyncState(),
    val busy: Boolean = false,
    val restoreOffer: RestoreOffer? = null,
    val pendingImport: PendingImport? = null,
)

private data class Transient(
    val busy: Boolean = false,
    val restoreOffer: RestoreOffer? = null,
    val pendingImport: PendingImport? = null,
)

class BackupViewModel(
    private val repository: BudgetRepository,
    private val prefs: SyncPrefs,
    private val auth: GoogleAuth,
    private val backups: BackupManager,
    private val files: ContentResolver,
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<BackupUiState> = combine(prefs.state, transient) { sync, t ->
        BackupUiState(sync = sync, busy = t.busy, restoreOffer = t.restoreOffer, pendingImport = t.pendingImport)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState(sync = prefs.current))

    private val _consentRequests = Channel<IntentSender>(Channel.BUFFERED)
    /** The screen launches each of these as the Google account / consent UI. */
    val consentRequests = _consentRequests.receiveAsFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun signIn() = launchBusy {
        when (val outcome = auth.authorize()) {
            is GoogleAuth.Outcome.Granted -> onGranted(outcome)
            is GoogleAuth.Outcome.NeedsUser -> _consentRequests.send(outcome.intentSender)
        }
    }

    fun onConsentResult(resultCode: Int, data: Intent?) = launchBusy {
        if (resultCode != Activity.RESULT_OK) {
            _messages.send("Sign-in cancelled")
            return@launchBusy
        }
        when (val outcome = auth.outcomeFromIntent(data)) {
            is GoogleAuth.Outcome.Granted -> onGranted(outcome)
            is GoogleAuth.Outcome.NeedsUser -> _messages.send("Google sign-in didn't finish. Try again.")
        }
    }

    private suspend fun onGranted(outcome: GoogleAuth.Outcome.Granted) {
        prefs.setSignedIn(outcome.email)
        _messages.send("Signed in" + (outcome.email?.let { " as $it" } ?: ""))
        offerRestoreIfUseful(enableSyncAfter = false)
    }

    fun setSyncEnabled(enabled: Boolean) {
        if (!enabled) {
            prefs.setSyncEnabled(false)
            backups.cancelScheduled()
            return
        }
        launchBusy {
            // On a fresh install, restore before syncing so the cloud copy isn't replaced.
            if (offerRestoreIfUseful(enableSyncAfter = true)) return@launchBusy
            enableSync()
        }
    }

    private fun enableSync() {
        prefs.setSyncEnabled(true)
        backups.scheduleBackup(delaySeconds = 0)
    }

    /** @return true if a restore offer is now showing. */
    private suspend fun offerRestoreIfUseful(enableSyncAfter: Boolean): Boolean {
        val remote = backups.findBackup() ?: return false
        if (!repository.isEmpty()) return false
        transient.update { it.copy(restoreOffer = RestoreOffer(remote.modifiedAt, enableSyncAfter)) }
        return true
    }

    fun acceptRestoreOffer() {
        val offer = transient.value.restoreOffer ?: return
        transient.update { it.copy(restoreOffer = null) }
        launchBusy {
            if (backups.restore()) _messages.send("Restored from Google Drive")
            if (offer.enableSyncAfter) enableSync()
        }
    }

    fun declineRestoreOffer() {
        val offer = transient.value.restoreOffer ?: return
        transient.update { it.copy(restoreOffer = null) }
        if (offer.enableSyncAfter) enableSync()
    }

    fun backupNow() = launchBusy {
        val at = backups.backupNow()
        _messages.send(
            if (at != null) "Backed up to Google Drive"
            else "Nothing to back up yet. Your Drive backup was left as it is."
        )
    }

    fun restoreNow() = launchBusy {
        _messages.send(
            if (backups.restore()) "Restored from Google Drive" else "No backup found in Google Drive"
        )
    }

    /** Writes the same JSON the Drive backup uses to a file the user picked. */
    fun exportToFile(uri: Uri) = launchBusy {
        val backup = repository.exportBackup()
        withContext(Dispatchers.IO) {
            val out = files.openOutputStream(uri, "wt") ?: throw FileException("Couldn't open that file")
            out.bufferedWriter().use { it.write(backup.toJson()) }
        }
        _messages.send(
            "Exported ${backup.budgets.size} budget" + (if (backup.budgets.size == 1) "" else "s") +
                " and ${backup.expenses.size} expense" + (if (backup.expenses.size == 1) "" else "s")
        )
    }

    /** Reads a backup file and asks for confirmation before replacing anything. */
    fun readImportFile(uri: Uri) = launchBusy {
        val backup = withContext(Dispatchers.IO) {
            val text = files.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw FileException("Couldn't open that file")
            try {
                BackupFile.fromJson(text)
            } catch (e: SerializationException) {
                throw FileException("That file isn't a Budget backup")
            } catch (e: IllegalArgumentException) {
                throw FileException("That file isn't a Budget backup")
            }
        }
        transient.update { it.copy(pendingImport = PendingImport(backup)) }
    }

    fun confirmImport() {
        val pending = transient.value.pendingImport ?: return
        transient.update { it.copy(pendingImport = null) }
        launchBusy {
            repository.replaceAll(pending.backup)
            _messages.send("Imported ${pending.budgetCount} budget" + if (pending.budgetCount == 1) "" else "s")
        }
    }

    fun cancelImport() {
        transient.update { it.copy(pendingImport = null) }
    }

    fun signOut() = launchBusy {
        backups.cancelScheduled()
        auth.revoke()
        prefs.signOut()
        _messages.send("Signed out. Your data stays on this phone and in Drive.")
    }

    /** Runs [block] with a busy indicator, turning failures into a readable message. */
    private fun launchBusy(block: suspend () -> Unit) {
        if (transient.value.busy) return
        viewModelScope.launch {
            transient.update { it.copy(busy = true) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.send(describe(e))
            } finally {
                transient.update { it.copy(busy = false) }
            }
        }
    }

    private fun describe(e: Exception): String = when (e) {
        is NeedsSignInException -> "Please sign in with Google again"
        is ApiException -> when (e.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR ->
                "Google sign-in isn't set up for this build yet (see README: Google Drive setup)"
            CommonStatusCodes.NETWORK_ERROR -> "No internet connection"
            else -> "Google sign-in failed (code ${e.statusCode})"
        }
        is FileException -> e.message ?: "Couldn't use that file"
        is IOException -> "Couldn't reach Google Drive. Check your connection."
        else -> e.message ?: "Something went wrong"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BudgetApplication
                BackupViewModel(app.repository, app.syncPrefs, app.googleAuth, app.backupManager, app.contentResolver)
            }
        }
    }
}

/** A problem with a picked file, with a message fit to show as is. */
private class FileException(message: String) : IOException(message)

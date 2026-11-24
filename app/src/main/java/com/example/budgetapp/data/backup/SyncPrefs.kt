package com.example.budgetapp.data.backup

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SyncState(
    /** Set once the user signs in with Google; null when signed out. */
    val accountEmail: String? = null,
    val signedIn: Boolean = false,
    val syncEnabled: Boolean = false,
    val lastBackupAt: Long? = null,
    /** The last background backup failure, shown on the settings screen. */
    val lastError: String? = null,
)

/** Small persisted state for Drive backup. It's device-local and never itself backed up. */
class SyncPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("drive_sync", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    val current: SyncState get() = _state.value

    fun setSignedIn(email: String?) = edit {
        it.copy(signedIn = true, accountEmail = email, lastError = null)
    }

    fun signOut() = edit { SyncState() }

    fun setSyncEnabled(enabled: Boolean) = edit { it.copy(syncEnabled = enabled, lastError = null) }

    fun recordBackup(at: Long) = edit { it.copy(lastBackupAt = at, lastError = null) }

    fun recordError(message: String?) = edit { it.copy(lastError = message) }

    @Synchronized
    private fun edit(change: (SyncState) -> SyncState) {
        _state.update(change)
        val s = _state.value
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, s.signedIn)
            .putString(KEY_EMAIL, s.accountEmail)
            .putBoolean(KEY_ENABLED, s.syncEnabled)
            .putLong(KEY_LAST_BACKUP, s.lastBackupAt ?: 0L)
            .putString(KEY_ERROR, s.lastError)
            .apply()
    }

    private fun read() = SyncState(
        signedIn = prefs.getBoolean(KEY_SIGNED_IN, false),
        accountEmail = prefs.getString(KEY_EMAIL, null),
        syncEnabled = prefs.getBoolean(KEY_ENABLED, false),
        lastBackupAt = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0L },
        lastError = prefs.getString(KEY_ERROR, null),
    )

    private companion object {
        const val KEY_SIGNED_IN = "signed_in"
        const val KEY_EMAIL = "email"
        const val KEY_ENABLED = "enabled"
        const val KEY_LAST_BACKUP = "last_backup"
        const val KEY_ERROR = "last_error"
    }
}

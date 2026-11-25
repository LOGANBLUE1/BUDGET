package com.example.budgetapp.data.backup

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google sign-in for Drive backup, built on the Google Identity `AuthorizationClient`.
 *
 * The app asks only for `drive.appdata`, a hidden per-app folder in the user's Drive,
 * so it can never see or touch the user's other Drive files.
 *
 * Nothing is configured in code: Google recognises the app by its package name and
 * signing-certificate SHA-1, registered once as an Android OAuth client (see README).
 */
class GoogleAuth(context: Context) {

    private val appContext = context.applicationContext
    private val client = Identity.getAuthorizationClient(appContext)

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE), Scope(EMAIL_SCOPE)))
        .build()

    sealed interface Outcome {
        data class Granted(val accessToken: String, val email: String?) : Outcome
        /** The user has to pick an account or consent: launch [intentSender]. */
        data class NeedsUser(val intentSender: IntentSender) : Outcome
    }

    /** Starts (or silently completes) authorization. Throws if Play services fails. */
    suspend fun authorize(): Outcome = client.authorize(request).await().toOutcome()

    /** Turns the result Intent of the consent screen into an outcome. */
    fun outcomeFromIntent(data: Intent?): Outcome =
        client.getAuthorizationResultFromIntent(data).toOutcome()

    /**
     * An access token if the user already granted access, or null if they need to sign in
     * again. Never shows UI, so it's safe from background work.
     */
    suspend fun tokenOrNull(): String? =
        (runCatching { authorize() }.getOrNull() as? Outcome.Granted)?.accessToken

    /** Drops a token Google rejected so the next [authorize] mints a fresh one. */
    suspend fun invalidate(token: String) = withContext(Dispatchers.IO) {
        runCatching { GoogleAuthUtil.clearToken(appContext, token) }
    }

    /** Revokes the app's access on Google's side. Best effort: offline sign-out still works locally. */
    suspend fun revoke() = withContext(Dispatchers.IO) {
        val token = tokenOrNull() ?: return@withContext
        runCatching {
            val conn = URL("https://oauth2.googleapis.com/revoke").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.outputStream.use {
                    it.write("token=${URLEncoder.encode(token, "UTF-8")}".toByteArray())
                }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }
        invalidate(token)
    }

    private fun AuthorizationResult.toOutcome(): Outcome {
        val pending = pendingIntent
        if (hasResolution() && pending != null) return Outcome.NeedsUser(pending.intentSender)
        val token = accessToken ?: error("Google returned no access token")
        @Suppress("DEPRECATION")
        val email = runCatching { toGoogleSignInAccount()?.email }.getOrNull()
        return Outcome.Granted(token, email)
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val EMAIL_SCOPE = "email"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

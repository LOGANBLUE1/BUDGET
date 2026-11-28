package com.example.budgetapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.util.Dates
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sync = state.sync
    val snackbar = remember { SnackbarHostState() }
    var confirmRestore by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> viewModel.onConsentResult(result.resultCode, result.data) }

    LaunchedEffect(viewModel) {
        viewModel.consentRequests.collect { sender ->
            consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportToFile) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::readImportFile) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.busy) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (sync.signedIn && sync.syncEnabled) Icons.Outlined.CloudDone
                            else Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Drive",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = when {
                                    !sync.signedIn -> "Not signed in"
                                    sync.accountEmail != null -> sync.accountEmail
                                    else -> "Signed in"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Your budgets and expenses are saved to a private app folder in your " +
                            "Google Drive. The app can't see any of your other Drive files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!sync.signedIn) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::signIn,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Sign in with Google") }
                    }
                }
            }

            if (sync.signedIn) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Sync to Google Drive", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (sync.syncEnabled) "Backs up automatically after every change"
                                    else "Off: nothing is uploaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = sync.syncEnabled,
                                onCheckedChange = viewModel::setSyncEnabled,
                                enabled = !state.busy,
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Last backup: " + (sync.lastBackupAt?.let(Dates::format) ?: "never"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        sync.lastError?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = viewModel::backupNow,
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                            ) { Text("Back up now") }
                            OutlinedButton(
                                onClick = { confirmRestore = true },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                            ) { Text("Restore") }
                        }
                    }
                }

                TextButton(
                    onClick = { confirmSignOut = true },
                    enabled = !state.busy,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Sign out") }
            }

            FileCard(
                busy = state.busy,
                onExport = { exportLauncher.launch("budget-backup-${LocalDate.now()}.json") },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            )
        }
    }

    state.pendingImport?.let { pending ->
        ConfirmDialog(
            title = "Import this backup?",
            message = "It has ${pending.budgetCount} budget" + (if (pending.budgetCount == 1) "" else "s") +
                " and ${pending.expenseCount} expense" + (if (pending.expenseCount == 1) "" else "s") +
                ". Everything on this phone will be replaced with it." +
                if (sync.signedIn && sync.syncEnabled) " Your Drive backup will be updated to match." else "",
            confirmLabel = "Import",
            onDismiss = viewModel::cancelImport,
            onConfirm = viewModel::confirmImport,
        )
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "Restore from Google Drive?",
            message = "Everything on this phone will be replaced with your last Drive backup.",
            confirmLabel = "Restore",
            onDismiss = { confirmRestore = false },
            onConfirm = {
                confirmRestore = false
                viewModel.restoreNow()
            },
        )
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out of Google?",
            message = "Sync stops. Your data stays on this phone, and the last backup stays in Drive.",
            confirmLabel = "Sign out",
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
        )
    }

    state.restoreOffer?.let { offer ->
        ConfirmDialog(
            title = "Backup found",
            message = "There's a backup in your Google Drive from ${Dates.format(offer.backupTime)}. " +
                "Restore it to this phone?" +
                if (offer.enableSyncAfter) "\n\nIf you skip, your next change replaces that backup." else "",
            confirmLabel = "Restore",
            dismissLabel = if (offer.enableSyncAfter) "Skip" else "Not now",
            onDismiss = viewModel::declineRestoreOffer,
            onConfirm = viewModel::acceptRestoreOffer,
        )
    }
}

@Composable
private fun FileCard(busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Backup file",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Save everything to a file you choose (the same format as the Drive backup), " +
                    "or load one. Loading a file replaces everything on this phone. No sign-in needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onExport,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("Export") }
                OutlinedButton(
                    onClick = onImport,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("Import") }
            }
        }
    }
}

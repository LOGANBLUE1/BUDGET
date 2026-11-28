# AGENTS.md

Context for any agent (or person) picking this project up in a new session. Read this
first; the [README](README.md) has the user-facing feature list and full file tree.

## Project

**Budget** is an Android budgeting app: budgets that hold expenses, with live totals.
It works fully offline, with an optional Google Drive backup.

| | |
| --- | --- |
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Package / applicationId | `com.example.budgetapp` |
| SDK | minSdk 26, targetSdk / compileSdk 35, JDK 17 |
| Storage | Room, **schema v4** (`data/BudgetDatabase.kt`) |
| Current version | **1.1.0** (versionCode 2), released on GitHub |

## Where things are

All source is under `app/src/main/java/com/example/budgetapp/`.

- `BudgetApplication.kt` owns the database, repository, `SyncPrefs`, `GoogleAuth` and
  `BackupManager`. ViewModel factories pull their dependencies from here.
- `data/`
  - Entities: `Budget`, `Expense`, `Category`
  - DAOs
  - `BudgetRepository` (also does the backup export and replace-all restore)
  - `BudgetDatabase` (all migrations)
- `data/backup/`
  - `BackupModels` (JSON format)
  - `GoogleAuth` (sign-in, `drive.appdata` scope)
  - `DriveBackupClient` (Drive REST v3)
  - `BackupManager` (watches Room, schedules uploads, restores)
  - `BackupWorker` (WorkManager job)
  - `SyncPrefs`
- `ui/`: one `XScreen.kt` + `XViewModel.kt` per screen (BudgetList, BudgetDetail, Backup,
  Categories), plus `Navigation.kt`, `Dialogs.kt`, `StarRating.kt`, `CategoryPicker.kt` and
  `CurrencyPickerDialog.kt`.
- `util/`: `MoneyFormatter.kt`, which is the only place text becomes money and back.
- Tests are in `app/src/test/` (JVM only).
- `.github/workflows/release.yml` is the release pipeline.

## Rules and conventions

- **Money** is stored as whole cents in a `Long`, and is parsed and formatted only
  through `MoneyFormatter`. No floating point.
- **A budget's currency is fixed** at creation; every expense in it uses that currency.
- **Optional fields:** an expense needs only an amount. Its name (`""` = none, shown as
  *Expense*), rating (1–5 or null) and category (nullable FK, `ON DELETE SET NULL`) are
  optional. A budget's limit and rating are optional.
- **Schema changes** bump the version and add a `Migration` in `BudgetDatabase.kt`.
  Never use destructive migration: users have real data on their phones. To add a
  foreign key, rebuild the table, as `MIGRATION_3_4` does.
- **Backup format** (`BackupModels.kt`) must stay readable by newer versions. A new field
  needs a default, and `ignoreUnknownKeys` stays on. Add a test in `BackupFileTest`.
- **Secrets:** never commit keystores, `keystore.properties`, or passwords. They are in
  `.gitignore`.
- Keep the README's feature list and file tree up to date when you change features.

## Build and test

```sh
./gradlew testDebugUnitTest   # JVM unit tests
./gradlew assembleDebug       # debug APK
```

**Cloud sessions can't build Android.** Their network blocks `dl.google.com`, which
serves the Android SDK and Google Maven. Pure-Kotlin code (entities, `BackupModels`, the
tests) can still be checked there: make a scratch Kotlin-JVM Gradle project with
kotlinx-serialization and stub `androidx.room` annotations, copy those files in, and run
`gradle test`. Say plainly which parts were not compiled.

## Releasing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add `release-notes/v<version>.md`; it becomes the release body. Update `CHANGELOG.md`.
3. Merge to `main`.
4. On GitHub, go to **Actions → Release → Run workflow** and enter the version. The
   workflow tests, builds, signs, tags `v<version>` and publishes the release with
   `budget-v<version>.apk`.

The four signing secrets are `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
`KEY_PASSWORD`. The owner holds the keystore. The current signing cert SHA-1 is
`2E:52:24:4A:58:54:F3:2D:C4:96:13:B1:EB:3F:8A:90:E2:E3:70:72`. **The 1.0 key was lost**,
so 1.0 and 1.1.0+ are signed with different keys and can't update in place. Every
release from 1.1.0 on must use the current key.

## Google Drive backup

- The backup is one JSON file in Drive's hidden `appDataFolder` (scope `drive.appdata`).
- Sign-in works only for builds whose package + signing SHA-1 are registered as an
  Android OAuth client in Google Cloud. The owner has registered the release key. Debug
  builds need their own SHA-1 added (`./gradlew signingReport`).
- An empty local database never overwrites an existing backup.
- On a fresh install, the app offers to restore after sign-in.

## History

| Version | What |
| --- | --- |
| 1.0.0 | Budgets, expenses, per-budget currency, undo, live totals |
| 1.1.0 | Amount-first expenses with an optional name, ★ ratings, categories, Drive backup; new signing key; release workflow |

## Ideas not started

- Spending-by-category breakdown and a category filter in a budget
- An expense date field (expenses currently always get "now")
- Import/export to a file
- A real applicationId (not `com.example.*`) before any Play Store publishing. Changing
  it means a new Google OAuth client.

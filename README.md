# Budget

An Android budgeting app: create budgets, add expenses inside them, and watch the totals
update. Everything lives in a local Room (SQLite) database on the device. No server and no
account are needed. An **optional** Google Drive backup keeps a copy in your own Drive.

## What it does

**Budgets list**
- Create a budget with a name, a currency, an optional spending limit and an optional
  overall ★ rating (handy for rating a whole trip)
- Each card shows its star rating, total spent, the limit, a progress bar and what's left (or how far over)
- A card at the top totals spending — one line per currency, since budgets in different
  currencies can't be added together
- Edit or delete a budget from the ⋮ menu (deleting removes its expenses too)

**Inside a budget**
- Every amount is in the budget's own currency, fixed when the budget was created
- Summary card: total spent, limit, progress, expense count, average expense, amount left
- Add an expense: the **amount** comes first and is the only required field. A name, a
  category and a 1–5 ★ rating are optional. An expense without a name shows as *Expense*.
- Each expense row shows its category tag and rating, if it has them
- Tap any expense to edit it, or delete it from the row / the edit dialog
- Deleting an expense shows an **Undo** snackbar
- Going over the limit turns the amounts and progress bar red

**Categories** (tag icon on the Budgets screen)
- Categories such as *Food* or *Stay* are shared by all budgets. Using one is always optional.
- Pick a category from the chips in the expense dialog, or tap **New** to create one right
  there. Tap the selected chip again to clear it.
- The Categories screen lists each one with how many expenses use it, and lets you add,
  rename or delete categories. Names are unique, ignoring case.
- Deleting a category keeps its expenses; they just become uncategorised.

**Backup & sync** (cloud icon on the Budgets screen)
- Sign in with Google, then switch **Sync to Google Drive** on or off
- While it's on, every change is backed up automatically a few seconds later (retried when
  you're back online). You can also back up on demand with **Back up now**.
- **Restore** replaces what's on the phone with the last backup. On a fresh install, after
  you sign in, the app offers to restore if it finds a backup.
- The backup is one JSON file in Drive's hidden *app data* folder. The app only asks for
  the `drive.appdata` scope, so it can't see any of your other Drive files.
- An empty phone never overwrites an existing backup, so reinstalling can't wipe it

## Tech

| Piece | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3 (dynamic color on Android 12+) |
| State | ViewModel + StateFlow, `collectAsStateWithLifecycle` |
| Storage | Room (schema v4), Flow-backed queries so the UI updates itself |
| Backup | Google Identity `AuthorizationClient` + Drive REST v3, WorkManager, kotlinx.serialization |
| Navigation | navigation-compose, two destinations |
| Min / target SDK | 26 / 35 |

### Currency

Currency is chosen **per budget, at creation, and then fixed** — every expense inside a
budget is in that budget's currency, so its totals are always comparable. Editing a budget
lets you change the name and limit but shows the currency as read-only. The default is INR.

[MoneyFormatter.kt](app/src/main/java/com/example/budgetapp/util/MoneyFormatter.kt) is the
only place text becomes money or vice versa. Amounts are stored as whole cents in a `Long`
so nothing drifts through floating point, and parsing follows the currency's locale, so
`1,00,000.50` works for rupees and `1.250,50` works for euros. Picking a currency also
picks the locale its amounts are laid out in, so INR renders as ₹ with Indian grouping
even on a device set to en-US.

Totals per budget are computed in SQL (`SUM`/`COUNT` in
[BudgetDao.kt](app/src/main/java/com/example/budgetapp/data/BudgetDao.kt)) rather than in Kotlin,
so the list stays cheap as expenses pile up.

## Google Drive setup (one time)

Sign-in only works once Google knows about the app. No client ID or secret goes in the
code: Google matches the app by its package name and signing certificate.

1. In the [Google Cloud console](https://console.cloud.google.com/), create a project and
   enable the **Google Drive API**.
2. Under **Google Auth Platform → Branding / Audience**, set up the OAuth consent screen
   (External). While it's in *Testing*, add your Google account under **Test users**.
3. Under **Data access**, add the scope `.../auth/drive.appdata`.
4. Under **Clients**, create an **Android** OAuth client:
   - Package name: `com.example.budgetapp`
   - SHA-1: run `./gradlew signingReport` and copy the SHA-1 of each variant you install.
     The debug and release keystores differ, so add one client per keystore.
5. Build, install, open **Backup & sync** and tap **Sign in with Google**.

If sign-in says it "isn't set up for this build yet", the package name or SHA-1 doesn't
match a registered Android client.

## Running it

Requires **JDK 17+** and the **Android SDK** (platform 35).

Easiest path — open the project folder in Android Studio (Ladybug or newer), let it sync,
press Run.

From the command line, with `ANDROID_HOME` set and a device or emulator connected:

```sh
./gradlew installDebug     # build and install
./gradlew assembleDebug    # just build the APK
```

## Layout

```
app/src/main/java/com/example/budgetapp/
├── BudgetApplication.kt        # owns the database + repository
├── MainActivity.kt
├── data/
│   ├── Budget.kt               # entity, owns the currency
│   ├── Expense.kt              # entity, CASCADE-deleted with its budget
│   ├── BudgetSummary.kt        # budget + SQL aggregates for the list
│   ├── BudgetDao.kt
│   ├── ExpenseDao.kt
│   ├── Category.kt             # entity; expenses.categoryId is ON DELETE SET NULL
│   ├── CategoryDao.kt          # + per-category expense counts
│   ├── BudgetDatabase.kt       # v1->v2 currencyCode, v2->v3 ratings, v3->v4 categories
│   ├── BudgetRepository.kt     # also export / replace-all for backups
│   └── backup/
│       ├── BackupModels.kt     # the JSON backup format
│       ├── GoogleAuth.kt       # Google sign-in, drive.appdata scope
│       ├── DriveBackupClient.kt# Drive REST calls (appDataFolder)
│       ├── BackupManager.kt    # watches the DB, schedules / runs backup + restore
│       ├── BackupWorker.kt     # WorkManager job
│       └── SyncPrefs.kt        # signed-in account, sync on/off, last backup
├── ui/
│   ├── Navigation.kt
│   ├── BudgetListScreen.kt     + BudgetListViewModel.kt
│   ├── BudgetDetailScreen.kt   + BudgetDetailViewModel.kt
│   ├── BackupScreen.kt         + BackupViewModel.kt
│   ├── CategoriesScreen.kt     + CategoriesViewModel.kt
│   ├── CategoryPicker.kt       # chips + inline "New" in the expense dialog
│   ├── Dialogs.kt              # add/edit budget, add/edit expense, confirm delete
│   ├── StarRating.kt           # star input + read-only stars
│   ├── CurrencyPickerDialog.kt # searchable currency list
│   ├── LocalMoneyFormatter.kt  # the in-scope budget's currency
│   └── theme/
└── util/
    ├── MoneyFormatter.kt       # cents <-> text for one currency, + the currency list
    └── Dates.kt
```

## Tests

`./gradlew testDebugUnitTest` runs the JVM unit tests. They cover amount parsing and
formatting across locales, the currency list, the backup JSON format (including categories and
reading older backups) and how untitled expenses are shown.

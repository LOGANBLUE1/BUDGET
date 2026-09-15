# Budget

A fully client-side Android budgeting app: create budgets, add expenses (title + amount)
inside them, and watch the totals update. Everything lives in a local Room (SQLite)
database on the device — no server, no account, no network permission.

## What it does

**Budgets list**
- Create a budget with a name, a currency and an optional spending limit
- Each card shows total spent, the limit, a progress bar and what's left (or how far over)
- A card at the top totals spending — one line per currency, since budgets in different
  currencies can't be added together
- Edit or delete a budget from the ⋮ menu (deleting removes its expenses too)

**Inside a budget**
- Every amount is in the budget's own currency, fixed when the budget was created
- Summary card: total spent, limit, progress, expense count, average expense, amount left
- Add an expense with a title and an amount
- Tap any expense to edit it, or delete it from the row / the edit dialog
- Deleting an expense shows an **Undo** snackbar
- Going over the limit turns the amounts and progress bar red

## Tech

| Piece | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3 (dynamic color on Android 12+) |
| State | ViewModel + StateFlow, `collectAsStateWithLifecycle` |
| Storage | Room (schema v2), Flow-backed queries so the UI updates itself |
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
│   ├── BudgetDatabase.kt       # v1 -> v2 migration adds budgets.currencyCode
│   └── BudgetRepository.kt
├── ui/
│   ├── Navigation.kt
│   ├── BudgetListScreen.kt     + BudgetListViewModel.kt
│   ├── BudgetDetailScreen.kt   + BudgetDetailViewModel.kt
│   ├── Dialogs.kt              # add/edit budget, add/edit expense, confirm delete
│   ├── CurrencyPickerDialog.kt # searchable currency list
│   ├── LocalMoneyFormatter.kt  # the in-scope budget's currency
│   └── theme/
└── util/
    ├── MoneyFormatter.kt       # cents <-> text for one currency, + the currency list
    └── Dates.kt
```

## Tests

`./gradlew testDebugUnitTest` — 17 JVM unit tests covering amount parsing/formatting
across locales and the currency list.

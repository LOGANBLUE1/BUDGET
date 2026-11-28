# Changelog

## 1.2.0

- Close a budget when you're done with it, and re-open it later. Budgets are split into
  **Live** and **Closed** tabs; a closed budget is read-only.
- Expenses get a date (defaults to today; pick any past day). Rows show the date only.
- **By category** card inside a budget (collapsible); tap a category to filter the expense list
- Quick add: a **+** on each live budget card, and an **Add expense** launcher shortcut
  for the last-used budget. The number keyboard opens with the add dialog.
- **Export / Import** the backup JSON to a file from Backup & sync (import replaces everything)
- Database schema v4 → v5 (`budgets.closedAt`); the backup format gains `closedAt`
- Debug builds install as a separate app (`com.example.budgetapp.debug`)

Full notes: [release-notes/v1.2.0.md](release-notes/v1.2.0.md)

## 1.1.0

- Expense dialog asks for the amount first; the expense name is now optional
- Optional 1–5 star rating on expenses and an overall rating on budgets
- Optional categories, shared across budgets, with a screen to add, rename and delete them
- Optional Google Drive backup & sync (sign in with Google, on/off switch, back up now,
  restore, restore offer on a fresh install)
- Adds the internet permission, used only by the Drive backup
- Database schema v2 → v4
- Signed with a new release key: uninstall 1.0 before installing (this deletes 1.0's data)

Full notes: [release-notes/v1.1.0.md](release-notes/v1.1.0.md)

## 1.0.0

- Budgets with a name, optional spending limit and a currency fixed at creation
- Expenses with a title and amount; edit, delete with undo
- Live totals, remaining amount and over-budget state; per-currency grand totals
- Material 3 with dynamic color and dark theme

# Changelog

## 1.1.0

- Expense dialog asks for the amount first; the expense name is now optional
- Optional 1–5 star rating on expenses and an overall rating on budgets
- Optional categories, shared across budgets, with a screen to add, rename and delete them
- Optional Google Drive backup & sync (sign in with Google, on/off switch, back up now,
  restore, restore offer on a fresh install)
- Adds the internet permission, used only by the Drive backup
- Database schema v2 → v4; existing data is kept

Full notes: [release-notes/v1.1.0.md](release-notes/v1.1.0.md)

## 1.0.0

- Budgets with a name, optional spending limit and a currency fixed at creation
- Expenses with a title and amount; edit, delete with undo
- Live totals, remaining amount and over-budget state; per-currency grand totals
- Material 3 with dynamic color and dark theme

package com.example.budgetapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Budget::class, Expense::class, Category::class],
    version = 5,
    exportSchema = false,
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: BudgetDatabase? = null

        /** v2 gave each budget its own currency; existing budgets keep the old default. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE budgets ADD COLUMN currencyCode TEXT NOT NULL " +
                        "DEFAULT '${Budget.DEFAULT_CURRENCY}'"
                )
            }
        }

        /** v3 adds an optional 1..5 star rating to budgets and expenses. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN rating INTEGER")
                db.execSQL("ALTER TABLE expenses ADD COLUMN rating INTEGER")
            }
        }

        /**
         * v4 adds categories and an optional expenses.categoryId. SQLite can't add a
         * foreign key to an existing table, so expenses is rebuilt and its rows copied over.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expenses_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`budgetId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`amountCents` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`rating` INTEGER, " +
                        "`categoryId` INTEGER, " +
                        "FOREIGN KEY(`budgetId`) REFERENCES `budgets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL(
                    "INSERT INTO `expenses_new` (`id`, `budgetId`, `title`, `amountCents`, `createdAt`, `rating`) " +
                        "SELECT `id`, `budgetId`, `title`, `amountCents`, `createdAt`, `rating` FROM `expenses`"
                )
                db.execSQL("DROP TABLE `expenses`")
                db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_budgetId` ON `expenses` (`budgetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
            }
        }

        /** v5 lets a budget be closed: closedAt is null while it is live. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN closedAt INTEGER")
            }
        }

        fun get(context: Context): BudgetDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BudgetDatabase::class.java,
                "budget.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
        }
    }
}

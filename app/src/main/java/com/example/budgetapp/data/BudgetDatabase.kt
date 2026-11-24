package com.example.budgetapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Budget::class, Expense::class],
    version = 3,
    exportSchema = false,
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao
    abstract fun expenseDao(): ExpenseDao

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

        fun get(context: Context): BudgetDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BudgetDatabase::class.java,
                "budget.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}

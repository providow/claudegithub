package com.gcashagent.tracker.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gcashagent.tracker.core.data.local.entity.ChargeConfigEntity
import com.gcashagent.tracker.core.data.local.entity.FeeBracketEntity
import com.gcashagent.tracker.core.data.local.entity.GCashNumberEntity
import com.gcashagent.tracker.core.data.local.entity.TransactionEntity

@Database(
    entities = [GCashNumberEntity::class, TransactionEntity::class, FeeBracketEntity::class, ChargeConfigEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gcashNumberDao(): GCashNumberDao
    abstract fun transactionDao(): TransactionDao
    abstract fun feeBracketDao(): FeeBracketDao
    abstract fun chargeConfigDao(): ChargeConfigDao

    companion object {
        /** v2 adds per-transaction charge tracking and the fee_brackets table. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `chargeCentavos` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fee_brackets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`gcashNumberId` INTEGER NOT NULL, " +
                        "`flow` TEXT NOT NULL, " +
                        "`minCentavos` INTEGER NOT NULL, " +
                        "`maxCentavos` INTEGER NOT NULL, " +
                        "`feeCentavos` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`gcashNumberId`) REFERENCES `gcash_numbers`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_fee_brackets_gcashNumberId` " +
                        "ON `fee_brackets` (`gcashNumberId`)"
                )
            }
        }

        /** v3 adds per-number/direction charge configuration (brackets vs percentage). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `charge_configs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`gcashNumberId` INTEGER NOT NULL, " +
                        "`flow` TEXT NOT NULL, " +
                        "`mode` TEXT NOT NULL, " +
                        "`percentBasisPoints` INTEGER NOT NULL, " +
                        "`minChargeCentavos` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`gcashNumberId`) REFERENCES `gcash_numbers`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_charge_configs_gcashNumberId_flow` " +
                        "ON `charge_configs` (`gcashNumberId`, `flow`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gcash_tracker.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

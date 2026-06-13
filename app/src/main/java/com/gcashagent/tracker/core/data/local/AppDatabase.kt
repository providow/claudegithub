package com.gcashagent.tracker.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gcashagent.tracker.core.data.local.entity.GCashNumberEntity
import com.gcashagent.tracker.core.data.local.entity.TransactionEntity

@Database(
    entities = [GCashNumberEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gcashNumberDao(): GCashNumberDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gcash_tracker.db"
                ).build().also { INSTANCE = it }
            }
    }
}

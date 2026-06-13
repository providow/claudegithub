package com.gcashagent.tracker.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcashagent.tracker.core.data.local.entity.GCashNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GCashNumberDao {

    @Query("SELECT * FROM gcash_numbers ORDER BY alias COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GCashNumberEntity>>

    @Query("SELECT * FROM gcash_numbers WHERE id = :id")
    fun observeById(id: Long): Flow<GCashNumberEntity?>

    @Query("SELECT * FROM gcash_numbers WHERE id = :id")
    suspend fun getById(id: Long): GCashNumberEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: GCashNumberEntity): Long

    @Update
    suspend fun update(entity: GCashNumberEntity)

    @Delete
    suspend fun delete(entity: GCashNumberEntity)

    @Query("SELECT COUNT(*) FROM gcash_numbers WHERE phoneNumber = :phoneNumber AND id != :excludeId")
    suspend fun countByPhoneNumber(phoneNumber: String, excludeId: Long): Int
}

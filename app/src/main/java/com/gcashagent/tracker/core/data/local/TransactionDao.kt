package com.gcashagent.tracker.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcashagent.tracker.core.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE gcashNumberId = :numberId ORDER BY dateTime DESC")
    fun observeForNumber(numberId: Long): Flow<List<TransactionEntity>>

    /** Transactions for one number within [start, end). */
    @Query(
        """
        SELECT * FROM transactions
        WHERE gcashNumberId = :numberId AND dateTime >= :start AND dateTime < :end
        ORDER BY dateTime DESC
        """
    )
    fun observeForNumberInRange(numberId: Long, start: Long, end: Long): Flow<List<TransactionEntity>>

    /** Transactions across all numbers within [start, end) — for the combined report. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime DESC
        """
    )
    fun observeAllInRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)
}

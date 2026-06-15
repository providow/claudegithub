package com.gcashagent.tracker.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcashagent.tracker.core.data.local.entity.FeeBracketEntity
import com.gcashagent.tracker.core.domain.model.CashFlow
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeBracketDao {

    @Query("SELECT * FROM fee_brackets WHERE gcashNumberId = :numberId AND flow = :flow ORDER BY minCentavos ASC")
    fun observeForFlow(numberId: Long, flow: CashFlow): Flow<List<FeeBracketEntity>>

    @Query("SELECT * FROM fee_brackets WHERE gcashNumberId = :numberId")
    fun observeForNumber(numberId: Long): Flow<List<FeeBracketEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FeeBracketEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<FeeBracketEntity>)

    @Update
    suspend fun update(entity: FeeBracketEntity)

    @Delete
    suspend fun delete(entity: FeeBracketEntity)

    @Query("DELETE FROM fee_brackets WHERE gcashNumberId = :numberId AND flow = :flow")
    suspend fun clearFlow(numberId: Long, flow: CashFlow)
}

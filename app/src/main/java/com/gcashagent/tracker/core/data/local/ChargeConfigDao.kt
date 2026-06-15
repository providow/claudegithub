package com.gcashagent.tracker.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcashagent.tracker.core.data.local.entity.ChargeConfigEntity
import com.gcashagent.tracker.core.domain.model.CashFlow
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargeConfigDao {

    @Query("SELECT * FROM charge_configs WHERE gcashNumberId = :numberId AND flow = :flow")
    fun observe(numberId: Long, flow: CashFlow): Flow<ChargeConfigEntity?>

    @Query("SELECT * FROM charge_configs WHERE gcashNumberId = :numberId AND flow = :flow")
    suspend fun get(numberId: Long, flow: CashFlow): ChargeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ChargeConfigEntity): Long

    @Update
    suspend fun update(entity: ChargeConfigEntity)
}

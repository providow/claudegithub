package com.gcashagent.tracker.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gcashagent.tracker.core.domain.model.ChargeMode
import com.gcashagent.tracker.core.domain.model.CashFlow

@Entity(
    tableName = "charge_configs",
    foreignKeys = [
        ForeignKey(
            entity = GCashNumberEntity::class,
            parentColumns = ["id"],
            childColumns = ["gcashNumberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gcashNumberId", "flow"], unique = true)]
)
data class ChargeConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gcashNumberId: Long,
    val flow: CashFlow,
    val mode: ChargeMode,
    val percentBasisPoints: Long,
    val minChargeCentavos: Long
)

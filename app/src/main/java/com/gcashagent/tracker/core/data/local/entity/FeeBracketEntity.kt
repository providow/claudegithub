package com.gcashagent.tracker.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gcashagent.tracker.core.domain.model.CashFlow

@Entity(
    tableName = "fee_brackets",
    foreignKeys = [
        ForeignKey(
            entity = GCashNumberEntity::class,
            parentColumns = ["id"],
            childColumns = ["gcashNumberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gcashNumberId")]
)
data class FeeBracketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gcashNumberId: Long,
    val flow: CashFlow,
    val minCentavos: Long,
    val maxCentavos: Long,
    val feeCentavos: Long
)

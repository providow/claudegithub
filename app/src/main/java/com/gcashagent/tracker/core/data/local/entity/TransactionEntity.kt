package com.gcashagent.tracker.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gcashagent.tracker.core.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = GCashNumberEntity::class,
            parentColumns = ["id"],
            childColumns = ["gcashNumberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gcashNumberId"), Index("dateTime")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gcashNumberId: Long,
    val dateTime: Long,
    val type: TransactionType,
    val amountCentavos: Long,
    val counterpartyNumber: String?,
    val referenceNumber: String?,
    val screenshotPath: String?,
    val createdAt: Long
)

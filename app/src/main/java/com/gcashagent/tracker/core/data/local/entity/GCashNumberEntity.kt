package com.gcashagent.tracker.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gcash_numbers",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class GCashNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val phoneNumber: String,
    val createdAt: Long
)

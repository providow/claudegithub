package com.gcashagent.tracker.core.data.local

import androidx.room.TypeConverter
import com.gcashagent.tracker.core.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

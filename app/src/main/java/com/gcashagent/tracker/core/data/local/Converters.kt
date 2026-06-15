package com.gcashagent.tracker.core.data.local

import androidx.room.TypeConverter
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.ChargeMode
import com.gcashagent.tracker.core.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCashFlow(flow: CashFlow): String = flow.name

    @TypeConverter
    fun toCashFlow(value: String): CashFlow = CashFlow.valueOf(value)

    @TypeConverter
    fun fromChargeMode(mode: ChargeMode): String = mode.name

    @TypeConverter
    fun toChargeMode(value: String): ChargeMode = ChargeMode.valueOf(value)
}

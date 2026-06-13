package com.gcashagent.tracker.core.data.repository

import com.gcashagent.tracker.core.data.local.entity.GCashNumberEntity
import com.gcashagent.tracker.core.data.local.entity.TransactionEntity
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.Transaction

fun GCashNumberEntity.toDomain() = GCashNumber(
    id = id,
    alias = alias,
    phoneNumber = phoneNumber,
    createdAt = createdAt
)

fun GCashNumber.toEntity() = GCashNumberEntity(
    id = id,
    alias = alias,
    phoneNumber = phoneNumber,
    createdAt = createdAt
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    gcashNumberId = gcashNumberId,
    dateTime = dateTime,
    type = type,
    amountCentavos = amountCentavos,
    counterpartyNumber = counterpartyNumber,
    referenceNumber = referenceNumber,
    screenshotPath = screenshotPath,
    createdAt = createdAt
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    gcashNumberId = gcashNumberId,
    dateTime = dateTime,
    type = type,
    amountCentavos = amountCentavos,
    counterpartyNumber = counterpartyNumber,
    referenceNumber = referenceNumber,
    screenshotPath = screenshotPath,
    createdAt = createdAt
)

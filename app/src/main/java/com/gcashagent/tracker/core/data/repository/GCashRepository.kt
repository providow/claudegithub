package com.gcashagent.tracker.core.data.repository

import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.FeeBracket
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Single point of access to app data. UI/ViewModels depend only on this
 * interface, so storage can later evolve (cloud sync, multi-device) without
 * touching the feature layer.
 */
interface GCashRepository {

    // --- GCash numbers ---
    fun observeNumbers(): Flow<List<GCashNumber>>
    fun observeNumber(id: Long): Flow<GCashNumber?>
    suspend fun getNumber(id: Long): GCashNumber?

    /** @throws DuplicateNumberException if [GCashNumber.phoneNumber] already exists. */
    suspend fun addNumber(number: GCashNumber): Long
    suspend fun updateNumber(number: GCashNumber)
    suspend fun deleteNumber(number: GCashNumber)

    // --- Transactions ---
    fun observeTransactions(numberId: Long): Flow<List<Transaction>>
    fun observeTransactionsInRange(numberId: Long, start: Long, end: Long): Flow<List<Transaction>>
    fun observeAllTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>>
    suspend fun getTransaction(id: Long): Transaction?
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)

    // --- Charge brackets (per number, per direction) ---
    fun observeBrackets(numberId: Long, flow: CashFlow): Flow<List<FeeBracket>>
    suspend fun getBrackets(numberId: Long, flow: CashFlow): List<FeeBracket>
    suspend fun upsertBracket(bracket: FeeBracket)
    suspend fun deleteBracket(bracket: FeeBracket)
    /** Replace all brackets for a number+direction with [brackets]. */
    suspend fun setBrackets(numberId: Long, flow: CashFlow, brackets: List<FeeBracket>)
    /** Replace a number+direction's brackets with the default template. */
    suspend fun loadDefaultTemplate(numberId: Long, flow: CashFlow)
}

/** Thrown when enrolling/editing a number whose phone number is already in use. */
class DuplicateNumberException(message: String) : Exception(message)

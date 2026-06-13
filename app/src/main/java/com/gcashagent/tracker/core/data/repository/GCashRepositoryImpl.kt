package com.gcashagent.tracker.core.data.repository

import com.gcashagent.tracker.core.data.local.GCashNumberDao
import com.gcashagent.tracker.core.data.local.TransactionDao
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GCashRepositoryImpl(
    private val numberDao: GCashNumberDao,
    private val transactionDao: TransactionDao
) : GCashRepository {

    override fun observeNumbers(): Flow<List<GCashNumber>> =
        numberDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeNumber(id: Long): Flow<GCashNumber?> =
        numberDao.observeById(id).map { it?.toDomain() }

    override suspend fun getNumber(id: Long): GCashNumber? =
        numberDao.getById(id)?.toDomain()

    override suspend fun addNumber(number: GCashNumber): Long {
        requireUniquePhone(number.phoneNumber, excludeId = 0)
        return numberDao.insert(number.toEntity())
    }

    override suspend fun updateNumber(number: GCashNumber) {
        requireUniquePhone(number.phoneNumber, excludeId = number.id)
        numberDao.update(number.toEntity())
    }

    override suspend fun deleteNumber(number: GCashNumber) =
        numberDao.delete(number.toEntity())

    private suspend fun requireUniquePhone(phoneNumber: String, excludeId: Long) {
        if (numberDao.countByPhoneNumber(phoneNumber, excludeId) > 0) {
            throw DuplicateNumberException("This GCash number is already enrolled.")
        }
    }

    override fun observeTransactions(numberId: Long): Flow<List<Transaction>> =
        transactionDao.observeForNumber(numberId).map { list -> list.map { it.toDomain() } }

    override fun observeTransactionsInRange(numberId: Long, start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.observeForNumberInRange(numberId, start, end).map { list -> list.map { it.toDomain() } }

    override fun observeAllTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.observeAllInRange(start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun getTransaction(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun addTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.delete(transaction.toEntity())
}

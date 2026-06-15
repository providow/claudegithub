package com.gcashagent.tracker.core.data.repository

import com.gcashagent.tracker.core.data.local.ChargeConfigDao
import com.gcashagent.tracker.core.data.local.FeeBracketDao
import com.gcashagent.tracker.core.data.local.GCashNumberDao
import com.gcashagent.tracker.core.data.local.TransactionDao
import com.gcashagent.tracker.core.data.local.entity.ChargeConfigEntity
import com.gcashagent.tracker.core.data.local.entity.FeeBracketEntity
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.ChargeConfig
import com.gcashagent.tracker.core.domain.model.FeeBracket
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.util.DefaultFeeTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GCashRepositoryImpl(
    private val numberDao: GCashNumberDao,
    private val transactionDao: TransactionDao,
    private val feeBracketDao: FeeBracketDao,
    private val chargeConfigDao: ChargeConfigDao
) : GCashRepository {

    override fun observeNumbers(): Flow<List<GCashNumber>> =
        numberDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeNumber(id: Long): Flow<GCashNumber?> =
        numberDao.observeById(id).map { it?.toDomain() }

    override suspend fun getNumber(id: Long): GCashNumber? =
        numberDao.getById(id)?.toDomain()

    override suspend fun addNumber(number: GCashNumber): Long {
        requireUniquePhone(number.phoneNumber, excludeId = 0)
        val id = numberDao.insert(number.toEntity())
        // Seed both directions with the default charge template; agents can edit later.
        seedTemplate(id, CashFlow.CASH_IN)
        seedTemplate(id, CashFlow.CASH_OUT)
        return id
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

    override fun observeBrackets(numberId: Long, flow: CashFlow): Flow<List<FeeBracket>> =
        feeBracketDao.observeForFlow(numberId, flow).map { list -> list.map { it.toDomain() } }

    override suspend fun getBrackets(numberId: Long, flow: CashFlow): List<FeeBracket> =
        feeBracketDao.getForFlow(numberId, flow).map { it.toDomain() }

    override suspend fun setBrackets(numberId: Long, flow: CashFlow, brackets: List<FeeBracket>) {
        feeBracketDao.clearFlow(numberId, flow)
        feeBracketDao.insertAll(
            brackets.map {
                FeeBracketEntity(
                    gcashNumberId = numberId,
                    flow = flow,
                    minCentavos = it.minCentavos,
                    maxCentavos = it.maxCentavos,
                    feeCentavos = it.feeCentavos
                )
            }
        )
    }

    override suspend fun upsertBracket(bracket: FeeBracket) {
        if (bracket.id == 0L) feeBracketDao.insert(bracket.toEntity())
        else feeBracketDao.update(bracket.toEntity())
    }

    override suspend fun deleteBracket(bracket: FeeBracket) =
        feeBracketDao.delete(bracket.toEntity())

    override suspend fun loadDefaultTemplate(numberId: Long, flow: CashFlow) {
        feeBracketDao.clearFlow(numberId, flow)
        seedTemplate(numberId, flow)
    }

    override fun observeChargeConfig(numberId: Long, flow: CashFlow): Flow<ChargeConfig> =
        chargeConfigDao.observe(numberId, flow).map { it?.toDomain() ?: ChargeConfig.DEFAULT }

    override suspend fun getChargeConfig(numberId: Long, flow: CashFlow): ChargeConfig =
        chargeConfigDao.get(numberId, flow)?.toDomain() ?: ChargeConfig.DEFAULT

    override suspend fun setChargeConfig(numberId: Long, flow: CashFlow, config: ChargeConfig) {
        val existing = chargeConfigDao.get(numberId, flow)
        val entity = ChargeConfigEntity(
            id = existing?.id ?: 0,
            gcashNumberId = numberId,
            flow = flow,
            mode = config.mode,
            percentBasisPoints = config.percentBasisPoints,
            minChargeCentavos = config.minChargeCentavos
        )
        if (existing == null) chargeConfigDao.insert(entity) else chargeConfigDao.update(entity)
    }

    private suspend fun seedTemplate(numberId: Long, flow: CashFlow) {
        val brackets = DefaultFeeTemplate.bracketsCentavos().map { (min, max, fee) ->
            FeeBracketEntity(
                gcashNumberId = numberId,
                flow = flow,
                minCentavos = min,
                maxCentavos = max,
                feeCentavos = fee
            )
        }
        feeBracketDao.insertAll(brackets)
    }
}

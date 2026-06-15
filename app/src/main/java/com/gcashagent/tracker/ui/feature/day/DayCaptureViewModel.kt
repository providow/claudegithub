package com.gcashagent.tracker.ui.feature.day

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gcashagent.tracker.core.data.repository.GCashRepository
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.ChargeConfig
import com.gcashagent.tracker.core.domain.model.FeeBracket
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.ReportSummary
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.ocr.ReceiptScanner
import com.gcashagent.tracker.core.util.ChargeCalculator
import com.gcashagent.tracker.core.util.DateRange
import com.gcashagent.tracker.core.util.ImageStore
import com.gcashagent.tracker.core.util.PhDateTime
import com.gcashagent.tracker.core.util.ReportExporter
import com.gcashagent.tracker.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DayCaptureViewModel(
    private val repository: GCashRepository,
    private val imageStore: ImageStore,
    private val scanner: ReceiptScanner,
    private val exporter: ReportExporter,
    private val numberId: Long
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now(PhDateTime.ZONE))
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _flow = MutableStateFlow(CashFlow.CASH_IN)
    val flow: StateFlow<CashFlow> = _flow.asStateFlow()

    /** Number of screenshots currently being read by OCR. */
    private val _processing = MutableStateFlow(0)
    val processing: StateFlow<Int> = _processing.asStateFlow()

    val number: StateFlow<GCashNumber?> =
        repository.observeNumber(numberId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transactions: StateFlow<List<Transaction>> =
        _date.flatMapLatest { d ->
            val r = DateRange.of(d, d)
            repository.observeTransactionsInRange(numberId, r.startMillis, r.endExclusiveMillis)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<ReportSummary> =
        transactions
            .map { ReportSummary.from(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportSummary.EMPTY)

    /** This number's charge brackets, keyed by direction, for in-memory charge lookup. */
    private val brackets: StateFlow<Map<CashFlow, List<FeeBracket>>> =
        combine(
            repository.observeBrackets(numberId, CashFlow.CASH_IN),
            repository.observeBrackets(numberId, CashFlow.CASH_OUT)
        ) { cashIn, cashOut ->
            mapOf(CashFlow.CASH_IN to cashIn, CashFlow.CASH_OUT to cashOut)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** This number's charge config (brackets vs percentage), keyed by direction. */
    private val configs: StateFlow<Map<CashFlow, ChargeConfig>> =
        combine(
            repository.observeChargeConfig(numberId, CashFlow.CASH_IN),
            repository.observeChargeConfig(numberId, CashFlow.CASH_OUT)
        ) { cashIn, cashOut ->
            mapOf(CashFlow.CASH_IN to cashIn, CashFlow.CASH_OUT to cashOut)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setDate(d: LocalDate) { _date.value = d }
    fun setFlow(f: CashFlow) { _flow.value = f }

    /** Charge for an amount under the given direction's configuration (brackets or %). */
    fun chargeFor(flow: CashFlow, amountCentavos: Long): Long =
        ChargeCalculator.charge(
            configs.value[flow] ?: ChargeConfig.DEFAULT,
            brackets.value[flow].orEmpty(),
            amountCentavos
        )

    /**
     * Persist each screenshot and OCR it for amount + reference, creating a
     * transaction with the currently selected Cash In/Out direction and date.
     * Misses are saved with amount 0 so the user can correct them in the list.
     */
    fun addScreenshots(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val type = _flow.value.transactionType
        val day = _date.value
        viewModelScope.launch {
            _processing.update { it + uris.size }
            for (uri in uris) {
                try {
                    val path = imageStore.saveFromUri(uri)
                    val parsed = scanner.scan(uri)
                    val amount = parsed.amountCentavos ?: 0L
                    val dateTime = day.atTime(LocalTime.now(PhDateTime.ZONE))
                        .atZone(PhDateTime.ZONE).toInstant().toEpochMilli()
                    repository.addTransaction(
                        Transaction(
                            gcashNumberId = numberId,
                            dateTime = dateTime,
                            type = type,
                            amountCentavos = amount,
                            chargeCentavos = chargeFor(_flow.value, amount),
                            referenceNumber = parsed.referenceNumber,
                            screenshotPath = path
                        )
                    )
                } catch (_: Exception) {
                    // ignore a single bad image; keep processing the rest
                } finally {
                    _processing.update { it - 1 }
                }
            }
        }
    }

    fun updateTransaction(transaction: Transaction, amountCentavos: Long, chargeCentavos: Long, reference: String?) {
        viewModelScope.launch {
            repository.updateTransaction(
                transaction.copy(
                    amountCentavos = amountCentavos,
                    chargeCentavos = chargeCentavos,
                    referenceNumber = reference?.trim()?.ifBlank { null }
                )
            )
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            transaction.screenshotPath?.let { runCatching { File(it).delete() } }
        }
    }

    fun export(onReady: (File) -> Unit, onError: (String) -> Unit) {
        val n = number.value
        val day = _date.value
        val txns = transactions.value
        viewModelScope.launch {
            try {
                val range = DateRange.of(day, day)
                val label = n?.let { "${it.alias} (${it.formattedNumber})" } ?: "GCash"
                val map = if (n != null) mapOf(n.id to n) else emptyMap()
                val file = withContext(Dispatchers.IO) { exporter.export(label, range, txns, map) }
                onReady(file)
            } catch (e: Exception) {
                onError("Could not create the Excel file.")
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, numberId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    DayCaptureViewModel(
                        container.repository,
                        container.imageStore,
                        container.receiptScanner,
                        container.reportExporter,
                        numberId
                    )
                }
            }
    }
}

package com.gcashagent.tracker.ui.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gcashagent.tracker.core.data.repository.GCashRepository
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.ReportSummary
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.util.DateRange
import com.gcashagent.tracker.core.util.ReportExporter
import com.gcashagent.tracker.di.AppContainer
import com.gcashagent.tracker.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

enum class RangePreset { TODAY, WEEK, MONTH, CUSTOM }

data class ReportUiState(
    val scopeLabel: String = "",
    val preset: RangePreset = RangePreset.TODAY,
    val range: DateRange = DateRange.today(),
    val transactions: List<Transaction> = emptyList(),
    val summary: ReportSummary = ReportSummary.EMPTY,
    val numbersById: Map<Long, GCashNumber> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    private val repository: GCashRepository,
    private val exporter: ReportExporter,
    private val numberId: Long
) : ViewModel() {

    private val isAllNumbers = numberId == Routes.ALL_NUMBERS

    private val _preset = MutableStateFlow(RangePreset.TODAY)
    private val _range = MutableStateFlow(DateRange.today())

    private val transactionsFlow = _range.flatMapLatest { r ->
        if (isAllNumbers) repository.observeAllTransactionsInRange(r.startMillis, r.endExclusiveMillis)
        else repository.observeTransactionsInRange(numberId, r.startMillis, r.endExclusiveMillis)
    }

    val uiState: StateFlow<ReportUiState> =
        combine(
            repository.observeNumbers(),
            _preset,
            _range,
            transactionsFlow
        ) { numbers, preset, range, txns ->
            val byId = numbers.associateBy { it.id }
            val scope = if (isAllNumbers) "All Numbers"
            else byId[numberId]?.let { "${it.alias} (${it.formattedNumber})" } ?: "GCash Number"
            ReportUiState(
                scopeLabel = scope,
                preset = preset,
                range = range,
                transactions = txns,
                summary = ReportSummary.from(txns),
                numbersById = byId
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun setPreset(preset: RangePreset) {
        _preset.value = preset
        when (preset) {
            RangePreset.TODAY -> _range.value = DateRange.today()
            RangePreset.WEEK -> _range.value = DateRange.thisWeek()
            RangePreset.MONTH -> _range.value = DateRange.thisMonth()
            RangePreset.CUSTOM -> Unit // range set via setCustomRange
        }
    }

    fun setCustomRange(from: LocalDate, to: LocalDate) {
        _preset.value = RangePreset.CUSTOM
        _range.value = DateRange.of(from, to)
    }

    /** Build the .xlsx on a background thread and hand the file back. */
    fun export(onReady: (File) -> Unit, onError: (String) -> Unit) {
        val state = uiState.value
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    exporter.export(state.scopeLabel, state.range, state.transactions, state.numbersById)
                }
                onReady(file)
            } catch (e: Exception) {
                onError("Could not create the Excel file.")
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, numberId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { ReportViewModel(container.repository, container.reportExporter, numberId) }
        }
    }
}

package com.gcashagent.tracker.ui.feature.charges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gcashagent.tracker.core.data.repository.GCashRepository
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.FeeBracket
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.util.FeeLadderGenerator
import com.gcashagent.tracker.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChargesViewModel(
    private val repository: GCashRepository,
    private val numberId: Long
) : ViewModel() {

    private val _flow = MutableStateFlow(CashFlow.CASH_IN)
    val flow: StateFlow<CashFlow> = _flow.asStateFlow()

    val number: StateFlow<GCashNumber?> =
        repository.observeNumber(numberId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val brackets: StateFlow<List<FeeBracket>> =
        _flow.flatMapLatest { repository.observeBrackets(numberId, it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Other enrolled numbers, for "copy charges from". */
    val otherNumbers: StateFlow<List<GCashNumber>> =
        repository.observeNumbers()
            .map { list -> list.filter { it.id != numberId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFlow(f: CashFlow) { _flow.value = f }

    /** Replace the current direction's table with a generated linear ladder (inputs in pesos). */
    fun generateLadder(startPeso: Long, stepPeso: Long, firstFeePeso: Long, feeStepPeso: Long, uptoPeso: Long) {
        val flow = _flow.value
        val brackets = FeeLadderGenerator
            .generate(startPeso, stepPeso, firstFeePeso, feeStepPeso, uptoPeso)
            .map { (min, max, fee) ->
                FeeBracket(gcashNumberId = numberId, flow = flow, minCentavos = min, maxCentavos = max, feeCentavos = fee)
            }
        viewModelScope.launch { repository.setBrackets(numberId, flow, brackets) }
    }

    /** Copy the current direction's brackets from another number into this one. */
    fun copyFrom(otherNumberId: Long) {
        val flow = _flow.value
        viewModelScope.launch {
            val source = repository.getBrackets(otherNumberId, flow)
            repository.setBrackets(numberId, flow, source)
        }
    }

    /** Insert (id == 0) or update a bracket for the current direction. */
    fun save(existing: FeeBracket?, minCentavos: Long, maxCentavos: Long, feeCentavos: Long) {
        viewModelScope.launch {
            repository.upsertBracket(
                FeeBracket(
                    id = existing?.id ?: 0,
                    gcashNumberId = numberId,
                    flow = _flow.value,
                    minCentavos = minCentavos,
                    maxCentavos = maxCentavos,
                    feeCentavos = feeCentavos
                )
            )
        }
    }

    fun delete(bracket: FeeBracket) {
        viewModelScope.launch { repository.deleteBracket(bracket) }
    }

    fun loadDefaultTemplate() {
        viewModelScope.launch { repository.loadDefaultTemplate(numberId, _flow.value) }
    }

    companion object {
        fun factory(container: AppContainer, numberId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ChargesViewModel(container.repository, numberId) }
            }
    }
}

package com.gcashagent.tracker.ui.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gcashagent.tracker.core.data.repository.GCashRepository
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.ReportSummary
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val number: GCashNumber? = null,
    val transactions: List<Transaction> = emptyList(),
    val summary: ReportSummary = ReportSummary.EMPTY
)

class TransactionsViewModel(
    private val repository: GCashRepository,
    private val numberId: Long
) : ViewModel() {

    val uiState: StateFlow<TransactionsUiState> =
        combine(
            repository.observeNumber(numberId),
            repository.observeTransactions(numberId)
        ) { number, transactions ->
            TransactionsUiState(
                number = number,
                transactions = transactions,
                summary = ReportSummary.from(transactions)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            // Also remove the stored screenshot to avoid orphaned files.
            transaction.screenshotPath?.let { path ->
                runCatching { java.io.File(path).delete() }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, numberId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { TransactionsViewModel(container.repository, numberId) }
        }
    }
}

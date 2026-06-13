package com.gcashagent.tracker.ui.feature.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gcashagent.tracker.core.data.repository.DuplicateNumberException
import com.gcashagent.tracker.core.data.repository.GCashRepository
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.domain.model.ReportSummary
import com.gcashagent.tracker.core.util.DateRange
import com.gcashagent.tracker.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NumberRow(
    val number: GCashNumber,
    val todaySummary: ReportSummary
)

class NumbersViewModel(
    private val repository: GCashRepository
) : ViewModel() {

    private val today = DateRange.today()

    val rows: StateFlow<List<NumberRow>> =
        combine(
            repository.observeNumbers(),
            repository.observeAllTransactionsInRange(today.startMillis, today.endExclusiveMillis)
        ) { numbers, todayTxns ->
            val byNumber = todayTxns.groupBy { it.gcashNumberId }
            numbers.map { number ->
                NumberRow(
                    number = number,
                    todaySummary = ReportSummary.from(byNumber[number.id].orEmpty())
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Validate and save a number (insert when [editing] is null, else update).
     * On success reports the saved number's id (the new id when inserting).
     * Reports a friendly message through [onError] on validation/duplicate failure.
     */
    fun save(
        editing: GCashNumber?,
        alias: String,
        phoneNumber: String,
        onSuccess: (id: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedAlias = alias.trim()
        val digits = phoneNumber.filter { it.isDigit() }

        when {
            trimmedAlias.isEmpty() -> return onError("Please enter a label (e.g. \"Main\").")
            digits.length != 11 || !digits.startsWith("09") ->
                return onError("Enter a valid 11-digit GCash number starting with 09.")
        }

        viewModelScope.launch {
            try {
                val id = if (editing == null) {
                    repository.addNumber(GCashNumber(alias = trimmedAlias, phoneNumber = digits))
                } else {
                    repository.updateNumber(editing.copy(alias = trimmedAlias, phoneNumber = digits))
                    editing.id
                }
                onSuccess(id)
            } catch (e: DuplicateNumberException) {
                onError(e.message ?: "This GCash number is already enrolled.")
            } catch (e: Exception) {
                onError("Could not save. Please try again.")
            }
        }
    }

    fun delete(number: GCashNumber) {
        viewModelScope.launch { repository.deleteNumber(number) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { NumbersViewModel(container.repository) }
        }
    }
}

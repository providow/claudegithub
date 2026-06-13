package com.gcashagent.tracker.ui.feature.transactions

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gcashagent.tracker.core.data.repository.GCashRepository
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.domain.model.TransactionType
import com.gcashagent.tracker.core.util.ImageStore
import com.gcashagent.tracker.core.util.PesoFormatter
import com.gcashagent.tracker.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionEntryViewModel(
    private val repository: GCashRepository,
    private val imageStore: ImageStore,
    private val numberId: Long,
    private val transactionId: Long?
) : ViewModel() {

    val isEditing: Boolean = transactionId != null

    private val _editing = MutableStateFlow<Transaction?>(null)
    val editing: StateFlow<Transaction?> = _editing.asStateFlow()

    init {
        if (transactionId != null) {
            viewModelScope.launch { _editing.value = repository.getTransaction(transactionId) }
        }
    }

    /**
     * Validate and persist the transaction.
     * @param newImageUri a freshly picked screenshot to copy in, or null to keep [existingPath].
     */
    fun save(
        dateTime: Long,
        type: TransactionType,
        amountText: String,
        counterparty: String,
        reference: String,
        newImageUri: Uri?,
        existingPath: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val centavos = PesoFormatter.parseToCentavos(amountText)
        if (centavos == null || centavos <= 0L) {
            return onError("Enter a valid amount greater than ₱0.")
        }

        viewModelScope.launch {
            try {
                val screenshotPath = when {
                    newImageUri != null -> imageStore.saveFromUri(newImageUri) ?: existingPath
                    else -> existingPath
                }
                // If the screenshot was replaced, clean up the previous file.
                if (newImageUri != null && existingPath != null && existingPath != screenshotPath) {
                    imageStore.delete(existingPath)
                }

                val current = _editing.value
                val transaction = Transaction(
                    id = current?.id ?: 0,
                    gcashNumberId = numberId,
                    dateTime = dateTime,
                    type = type,
                    amountCentavos = centavos,
                    counterpartyNumber = counterparty.trim().ifBlank { null },
                    referenceNumber = reference.trim().ifBlank { null },
                    screenshotPath = screenshotPath,
                    createdAt = current?.createdAt ?: System.currentTimeMillis()
                )
                if (current == null) repository.addTransaction(transaction)
                else repository.updateTransaction(transaction)
                onSuccess()
            } catch (e: Exception) {
                onError("Could not save the transaction. Please try again.")
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, numberId: Long, transactionId: Long?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    TransactionEntryViewModel(container.repository, container.imageStore, numberId, transactionId)
                }
            }
    }
}

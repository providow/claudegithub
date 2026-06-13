package com.gcashagent.tracker.ui.feature.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.util.PesoFormatter
import com.gcashagent.tracker.core.util.PhDateTime
import com.gcashagent.tracker.di.appContainer
import com.gcashagent.tracker.ui.components.CashFlowChip
import com.gcashagent.tracker.ui.components.EmptyState
import com.gcashagent.tracker.ui.components.SummaryCard
import com.gcashagent.tracker.ui.theme.CashInGreen
import com.gcashagent.tracker.ui.theme.CashOutRed
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    numberId: Long,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onOpenReport: () -> Unit
) {
    val container = appContainer()
    val vm: TransactionsViewModel = viewModel(factory = TransactionsViewModel.factory(container, numberId))
    val state by vm.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.number?.alias ?: "Transactions", maxLines = 1)
                        state.number?.let {
                            Text(
                                it.formattedNumber,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenReport) {
                        Icon(Icons.Filled.Assessment, contentDescription = "Report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add") }
            )
        }
    ) { padding ->
        if (state.transactions.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "No transactions yet",
                subtitle = "Tap Add to record a send or receive. Remember: you sending = Cash In, you receiving = Cash Out.",
                modifier = Modifier.padding(padding)
            )
        } else {
            val groups = remember(state.transactions) {
                state.transactions
                    .groupBy { PhDateTime.toLocalDate(it.dateTime) }
                    .toSortedMap(compareByDescending { it })
                    .map { (_, txns) -> txns.first().dateTime to txns }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SummaryCard(summary = state.summary, modifier = Modifier.padding(bottom = 8.dp))
                }
                groups.forEach { (dayMillis, txns) ->
                    item {
                        Text(
                            text = PhDateTime.formatDayHeader(dayMillis),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(txns.size) { i ->
                        val txn = txns[i]
                        TransactionRow(
                            transaction = txn,
                            onClick = { onEditTransaction(txn.id) },
                            onLongPress = { pendingDelete = txn }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("Delete this ${target.cashFlow.label} of ${PesoFormatter.format(target.amountCentavos)}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(target); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val amountColor = if (transaction.cashFlow == CashFlow.CASH_IN) CashInGreen else CashOutRed
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenshotThumb(path = transaction.screenshotPath)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CashFlowChip(transaction.cashFlow)
                    Text(
                        text = "  ${PhDateTime.formatTime(transaction.dateTime)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                transaction.counterpartyNumber?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = (if (transaction.cashFlow == CashFlow.CASH_IN) "To: " else "From: ") + it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                transaction.referenceNumber?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Ref: $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PesoFormatter.format(transaction.amountCentavos),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                TextButton(onClick = onLongPress, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Delete", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ScreenshotThumb(path: String?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = "Screenshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Filled.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

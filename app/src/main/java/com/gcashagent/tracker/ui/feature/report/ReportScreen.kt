package com.gcashagent.tracker.ui.feature.report

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.util.PesoFormatter
import com.gcashagent.tracker.core.util.PhDateTime
import com.gcashagent.tracker.di.appContainer
import com.gcashagent.tracker.ui.components.CashFlowChip
import com.gcashagent.tracker.ui.components.SummaryCard
import com.gcashagent.tracker.ui.theme.CashInGreen
import com.gcashagent.tracker.ui.theme.CashOutRed
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    numberId: Long,
    onBack: () -> Unit
) {
    val container = appContainer()
    val context = LocalContext.current
    val vm: ReportViewModel = viewModel(factory = ReportViewModel.factory(container, numberId))
    val state by vm.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Report", maxLines = 1)
                        Text(
                            state.scopeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip("Today", state.preset == RangePreset.TODAY) { vm.setPreset(RangePreset.TODAY) }
                    PresetChip("This Week", state.preset == RangePreset.WEEK) { vm.setPreset(RangePreset.WEEK) }
                    PresetChip("This Month", state.preset == RangePreset.MONTH) { vm.setPreset(RangePreset.MONTH) }
                    PresetChip("Custom", state.preset == RangePreset.CUSTOM) { showRangePicker = true }
                }
            }
            item {
                val r = state.range
                val label = if (r.isSingleDay) PhDateTime.formatDate(r.startMillis)
                else "${PhDateTime.formatDate(r.startMillis)} – ${PhDateTime.formatDate(r.endExclusiveMillis - 1)}"
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { SummaryCard(summary = state.summary) }
            item {
                OutlinedButton(
                    onClick = {
                        vm.export(
                            onReady = { file -> shareExcel(context, file) },
                            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                        )
                    },
                    enabled = state.transactions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text("  Export to Excel (.xlsx)")
                }
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            if (state.transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions in this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(state.transactions, key = { it.id }) { txn ->
                    ReportRow(txn, showNumber = numberId < 0, numberLabel = {
                        state.numbersById[txn.gcashNumberId]?.alias ?: "—"
                    })
                }
            }
        }
    }

    if (showRangePicker) {
        val rangeState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMs = rangeState.selectedStartDateMillis
                        val endMs = rangeState.selectedEndDateMillis ?: startMs
                        if (startMs != null && endMs != null) {
                            val from = Instant.ofEpochMilli(startMs).atZone(ZoneOffset.UTC).toLocalDate()
                            val to = Instant.ofEpochMilli(endMs).atZone(ZoneOffset.UTC).toLocalDate()
                            vm.setCustomRange(from, to)
                        }
                        showRangePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showRangePicker = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = rangeState, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun ReportRow(
    transaction: Transaction,
    showNumber: Boolean,
    numberLabel: () -> String
) {
    val amountColor = if (transaction.cashFlow == CashFlow.CASH_IN) CashInGreen else CashOutRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CashFlowChip(transaction.cashFlow)
                Text(
                    "  ${PhDateTime.formatDateTime(transaction.dateTime)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showNumber) {
                Text(numberLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            transaction.referenceNumber?.takeIf { it.isNotBlank() }?.let {
                Text("Ref: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = PesoFormatter.format(transaction.amountCentavos),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

private fun shareExcel(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share report").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

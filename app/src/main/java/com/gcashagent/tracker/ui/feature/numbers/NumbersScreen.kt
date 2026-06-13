package com.gcashagent.tracker.ui.feature.numbers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcashagent.tracker.core.domain.model.GCashNumber
import com.gcashagent.tracker.core.util.PesoFormatter
import com.gcashagent.tracker.di.appContainer
import com.gcashagent.tracker.ui.components.EmptyState
import com.gcashagent.tracker.ui.theme.CashInGreen
import com.gcashagent.tracker.ui.theme.CashOutRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersScreen(
    onOpenNumber: (Long) -> Unit,
    onOpenCombinedReport: () -> Unit
) {
    val container = appContainer()
    val vm: NumbersViewModel = viewModel(factory = NumbersViewModel.factory(container))
    val rows by vm.rows.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var editingNumber by remember { mutableStateOf<GCashNumber?>(null) }
    var sheetError by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<GCashNumber?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GCash Agent Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (rows.isNotEmpty()) {
                        IconButton(onClick = onOpenCombinedReport) {
                            Icon(Icons.Filled.Assessment, contentDescription = "All numbers report")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingNumber = null; sheetError = null; showSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Number") }
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "No GCash numbers yet",
                subtitle = "Add your first GCash number to start recording cash in and cash out transactions.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rows, key = { it.number.id }) { row ->
                    NumberCard(
                        row = row,
                        onClick = { onOpenNumber(row.number.id) },
                        onEdit = { editingNumber = row.number; sheetError = null; showSheet = true },
                        onDelete = { pendingDelete = row.number }
                    )
                }
            }
        }
    }

    if (showSheet) {
        NumberEditSheet(
            editing = editingNumber,
            errorMessage = sheetError,
            onDismiss = { showSheet = false; sheetError = null },
            onSubmit = { alias, phone ->
                val wasAdd = editingNumber == null
                vm.save(
                    editing = editingNumber,
                    alias = alias,
                    phoneNumber = phone,
                    onSuccess = { id ->
                        showSheet = false
                        sheetError = null
                        if (wasAdd) onOpenNumber(id)
                    },
                    onError = { sheetError = it }
                )
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete number?") },
            text = { Text("Delete \"${target.alias}\" (${target.formattedNumber}) and all of its transactions? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(target); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NumberCard(
    row: NumberRow,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.number.alias, style = MaterialTheme.typography.titleMedium)
                    Text(
                        row.number.formattedNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TodayStat("Today In", PesoFormatter.format(row.todaySummary.totalCashInCentavos), CashInGreen, Modifier.weight(1f))
                TodayStat("Today Out", PesoFormatter.format(row.todaySummary.totalCashOutCentavos), CashOutRed, Modifier.weight(1f))
                TodayStat(
                    "Net",
                    PesoFormatter.formatSigned(row.todaySummary.netCentavos),
                    if (row.todaySummary.netCentavos < 0) CashOutRed else CashInGreen,
                    Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${row.todaySummary.transactionCount} transaction(s) today · tap to view",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

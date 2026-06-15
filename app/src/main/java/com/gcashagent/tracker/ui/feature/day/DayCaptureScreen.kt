package com.gcashagent.tracker.ui.feature.day

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.Transaction
import com.gcashagent.tracker.core.util.PesoFormatter
import com.gcashagent.tracker.core.util.PhDateTime
import com.gcashagent.tracker.di.appContainer
import com.gcashagent.tracker.ui.components.SummaryCard
import com.gcashagent.tracker.ui.theme.CashInGreen
import com.gcashagent.tracker.ui.theme.CashOutRed
import com.gcashagent.tracker.ui.util.shareExcelFile
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCaptureScreen(
    numberId: Long,
    onBack: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenCharges: () -> Unit
) {
    val container = appContainer()
    val context = LocalContext.current
    val vm: DayCaptureViewModel = viewModel(factory = DayCaptureViewModel.factory(container, numberId))

    val number by vm.number.collectAsState()
    val date by vm.date.collectAsState()
    val flow by vm.flow.collectAsState()
    val processing by vm.processing.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val summary by vm.summary.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    val dateMillis = remember(date) {
        date.atStartOfDay(PhDateTime.ZONE).toInstant().toEpochMilli()
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> -> vm.addScreenshots(uris) }

    fun launchPicker() = picker.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(number?.alias ?: "Transactions", maxLines = 1)
                        number?.let {
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
                    IconButton(onClick = onOpenCharges) {
                        Icon(Icons.Filled.Percent, contentDescription = "Charges")
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
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  " + PhDateTime.formatDate(dateMillis))
                }
            }
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = flow == CashFlow.CASH_IN,
                        onClick = { vm.setFlow(CashFlow.CASH_IN) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("Cash In") }
                    )
                    SegmentedButton(
                        selected = flow == CashFlow.CASH_OUT,
                        onClick = { vm.setFlow(CashFlow.CASH_OUT) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("Cash Out") }
                    )
                }
            }
            item {
                Button(onClick = { launchPicker() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                    Text("  Upload ${flow.label} screenshots")
                }
            }
            if (processing > 0) {
                item {
                    Column {
                        Text(
                            "Reading $processing screenshot(s)…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                }
            }
            item { SummaryCard(summary = summary) }
            item {
                OutlinedButton(
                    onClick = {
                        vm.export(
                            onReady = { file -> shareExcelFile(context, file) },
                            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                        )
                    },
                    enabled = transactions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text("  Download Report (.xlsx)")
                }
            }
            item {
                TextButton(onClick = onOpenReport, modifier = Modifier.fillMaxWidth()) {
                    Text("View date-range report")
                }
            }
            item { HorizontalDivider() }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions for this day yet. Choose Cash In or Cash Out, then upload your GCash screenshots — the amount is read automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(transactions, key = { it.id }) { txn ->
                    DayTransactionRow(
                        transaction = txn,
                        onEdit = { editing = txn },
                        onDelete = { vm.delete(txn) }
                    )
                }
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    editing?.let { txn ->
        EditAmountDialog(
            transaction = txn,
            computeCharge = { amountCentavos -> vm.chargeFor(txn.cashFlow, amountCentavos) },
            onDismiss = { editing = null },
            onSave = { centavos, charge, ref ->
                vm.updateTransaction(txn, centavos, charge, ref)
                editing = null
            }
        )
    }
}

@Composable
private fun DayTransactionRow(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIn = transaction.cashFlow == CashFlow.CASH_IN
    val amountColor = if (isIn) CashInGreen else CashOutRed
    val needsAmount = transaction.amountCentavos <= 0L
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (transaction.screenshotPath != null) {
                    AsyncImage(
                        model = File(transaction.screenshotPath),
                        contentDescription = "Screenshot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                if (needsAmount) {
                    Text(
                        "Tap to set amount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        PesoFormatter.format(transaction.amountCentavos),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }
                Text(
                    "${PhDateTime.formatTime(transaction.dateTime)} · Charge ${PesoFormatter.format(transaction.chargeCentavos)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transaction.referenceNumber?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        "Ref: $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EditAmountDialog(
    transaction: Transaction,
    computeCharge: (Long) -> Long,
    onDismiss: () -> Unit,
    onSave: (centavos: Long, charge: Long, ref: String?) -> Unit
) {
    fun pesos(centavos: Long) =
        if (centavos > 0) String.format(java.util.Locale.US, "%.2f", centavos / 100.0) else ""

    var amount by remember { mutableStateOf(pesos(transaction.amountCentavos)) }
    var charge by remember { mutableStateOf(pesos(transaction.chargeCentavos)) }
    var ref by remember { mutableStateOf(transaction.referenceNumber ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Amount (₱)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = charge,
                    onValueChange = { input -> charge = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Charge / income (₱)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                TextButton(
                    onClick = {
                        val amt = PesoFormatter.parseToCentavos(amount)
                        if (amt != null) charge = pesos(computeCharge(amt))
                    }
                ) { Text("Use charge from table") }
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("Reference number (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val centavos = PesoFormatter.parseToCentavos(amount)
                if (centavos == null || centavos <= 0L) {
                    error = "Enter a valid amount greater than ₱0."
                } else {
                    val chargeCentavos = PesoFormatter.parseToCentavos(charge) ?: 0L
                    onSave(centavos, chargeCentavos, ref)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

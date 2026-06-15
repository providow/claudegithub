package com.gcashagent.tracker.ui.feature.charges

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcashagent.tracker.core.domain.model.CashFlow
import com.gcashagent.tracker.core.domain.model.FeeBracket
import com.gcashagent.tracker.core.util.PesoFormatter
import com.gcashagent.tracker.di.appContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargesScreen(
    numberId: Long,
    onBack: () -> Unit
) {
    val container = appContainer()
    val vm: ChargesViewModel = viewModel(factory = ChargesViewModel.factory(container, numberId))
    val number by vm.number.collectAsState()
    val flow by vm.flow.collectAsState()
    val brackets by vm.brackets.collectAsState()

    var editing by remember { mutableStateOf<FeeBracket?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var confirmTemplate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Charges", maxLines = 1)
                        number?.let {
                            Text(
                                "${it.alias} · ${it.formattedNumber}",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Bracket") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                Text(
                    "Amounts that fall in a bracket earn its charge (your income). Amounts outside every bracket earn ₱0.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                TextButton(onClick = { confirmTemplate = true }) {
                    Text("Load sample template")
                }
            }

            if (brackets.isEmpty()) {
                item {
                    Text(
                        "No brackets for ${flow.label} yet. Add one, or load the sample template.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(brackets, key = { it.id }) { b ->
                    BracketRow(
                        bracket = b,
                        onEdit = { editing = b; showDialog = true },
                        onDelete = { vm.delete(b) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        BracketDialog(
            existing = editing,
            onDismiss = { showDialog = false; editing = null },
            onSave = { min, max, fee ->
                vm.save(editing, min, max, fee)
                showDialog = false
                editing = null
            }
        )
    }

    if (confirmTemplate) {
        AlertDialog(
            onDismissRequest = { confirmTemplate = false },
            title = { Text("Load sample template?") },
            text = { Text("This replaces all ${flow.label} brackets for this number with the standard ₱5-per-₱500 template.") },
            confirmButton = {
                TextButton(onClick = { vm.loadDefaultTemplate(); confirmTemplate = false }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { confirmTemplate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BracketRow(
    bracket: FeeBracket,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${PesoFormatter.format(bracket.minCentavos)} – ${PesoFormatter.format(bracket.maxCentavos)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Charge ${PesoFormatter.format(bracket.feeCentavos)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun BracketDialog(
    existing: FeeBracket?,
    onDismiss: () -> Unit,
    onSave: (minCentavos: Long, maxCentavos: Long, feeCentavos: Long) -> Unit
) {
    fun pesos(centavos: Long) =
        if (centavos > 0) String.format(java.util.Locale.US, "%.2f", centavos / 100.0) else ""

    var min by remember { mutableStateOf(existing?.let { pesos(it.minCentavos) } ?: "") }
    var max by remember { mutableStateOf(existing?.let { pesos(it.maxCentavos) } ?: "") }
    var fee by remember { mutableStateOf(existing?.let { pesos(it.feeCentavos) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add bracket" else "Edit bracket") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = min,
                        onValueChange = { input -> min = input.filter { it.isDigit() || it == '.' } },
                        label = { Text("From (₱)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = max,
                        onValueChange = { input -> max = input.filter { it.isDigit() || it == '.' } },
                        label = { Text("To (₱)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = fee,
                    onValueChange = { input -> fee = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Charge (₱)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minC = PesoFormatter.parseToCentavos(min)
                val maxC = PesoFormatter.parseToCentavos(max)
                val feeC = PesoFormatter.parseToCentavos(fee)
                when {
                    minC == null || maxC == null || feeC == null ->
                        error = "Enter valid amounts."
                    minC > maxC -> error = "\"From\" must be less than or equal to \"To\"."
                    else -> onSave(minC, maxC, feeC)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

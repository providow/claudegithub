package com.gcashagent.tracker.ui.feature.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcashagent.tracker.core.domain.model.TransactionType
import com.gcashagent.tracker.core.util.PhDateTime
import com.gcashagent.tracker.di.appContainer
import com.gcashagent.tracker.ui.components.CashFlowChip
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    numberId: Long,
    transactionId: Long?,
    onDone: () -> Unit
) {
    val container = appContainer()
    val vm: TransactionEntryViewModel =
        viewModel(factory = TransactionEntryViewModel.factory(container, numberId, transactionId))
    val editing by vm.editing.collectAsState()

    val now = remember { LocalDate.now(PhDateTime.ZONE) }
    var date by remember { mutableStateOf(now) }
    var time by remember { mutableStateOf(LocalTime.now(PhDateTime.ZONE).withSecond(0).withNano(0)) }
    var type by remember { mutableStateOf(TransactionType.SEND) }
    var amount by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var existingPath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var populated by remember { mutableStateOf(false) }

    // Populate fields once when an existing transaction is loaded (edit mode).
    LaunchedEffect(editing) {
        val t = editing
        if (t != null && !populated) {
            val zoned = java.time.Instant.ofEpochMilli(t.dateTime).atZone(PhDateTime.ZONE)
            date = zoned.toLocalDate()
            time = zoned.toLocalTime().withSecond(0).withNano(0)
            type = t.type
            amount = String.format(java.util.Locale.US, "%.2f", t.amountPesos)
            counterparty = t.counterpartyNumber ?: ""
            reference = t.referenceNumber ?: ""
            existingPath = t.screenshotPath
            populated = true
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedUri = uri }

    val dateTimeMillis = remember(date, time) {
        date.atTime(time).atZone(PhDateTime.ZONE).toInstant().toEpochMilli()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Type selector with live cash-flow preview
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, t ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t },
                        shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size),
                        label = { Text(if (t == TransactionType.SEND) "I Sent" else "I Received") }
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This counts as ", style = MaterialTheme.typography.bodyMedium)
                CashFlowChip(type.cashFlow)
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Amount (₱)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  " + PhDateTime.formatDate(dateTimeMillis), maxLines = 1)
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  " + PhDateTime.formatTime(dateTimeMillis), maxLines = 1)
                }
            }

            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text(if (type == TransactionType.SEND) "Send to (number)" else "Received from (number)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference number (optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            ScreenshotPicker(
                pickedUri = pickedUri,
                existingPath = existingPath,
                onPick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClear = { pickedUri = null; existingPath = null },
                modifier = Modifier.padding(top = 16.dp)
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Button(
                onClick = {
                    vm.save(
                        dateTime = dateTimeMillis,
                        type = type,
                        amountText = amount,
                        counterparty = counterparty,
                        reference = reference,
                        newImageUri = pickedUri,
                        existingPath = existingPath,
                        onSuccess = onDone,
                        onError = { error = it }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(if (vm.isEditing) "Save Changes" else "Save Transaction")
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = false
        )
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTime = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = state)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTime = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            time = LocalTime.of(state.hour, state.minute)
                            showTime = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotPicker(
    pickedUri: Uri?,
    existingPath: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasImage = pickedUri != null || existingPath != null
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Screenshot (optional)", style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(if (hasImage) 200.dp else 96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            when {
                pickedUri != null -> AsyncImage(
                    model = pickedUri,
                    contentDescription = "Screenshot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                existingPath != null -> AsyncImage(
                    model = File(existingPath),
                    contentDescription = "Screenshot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Tap to attach a screenshot", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (hasImage) {
            TextButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Remove screenshot")
            }
        }
    }
}

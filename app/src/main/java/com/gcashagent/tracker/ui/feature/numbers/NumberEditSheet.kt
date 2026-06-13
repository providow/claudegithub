package com.gcashagent.tracker.ui.feature.numbers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gcashagent.tracker.core.domain.model.GCashNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberEditSheet(
    editing: GCashNumber?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (alias: String, phone: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var alias by remember { mutableStateOf(editing?.alias ?: "") }
    var phone by remember { mutableStateOf(editing?.phoneNumber ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (editing == null) "Add GCash Number" else "Edit GCash Number",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Label (e.g. Main, Store 2)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { input -> phone = input.filter { it.isDigit() }.take(11) },
                label = { Text("GCash Number (09XXXXXXXXX)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Button(
                onClick = { onSubmit(alias, phone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(if (editing == null) "Add Number" else "Save Changes")
            }
        }
    }
}

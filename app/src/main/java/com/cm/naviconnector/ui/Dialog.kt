package com.cm.naviconnector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceListDialog(onDismiss: () -> Unit, onDeviceSelected: (String) -> Unit) {
    val dummyDevices = List(5) { "Device ${it + 1}" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a device") },
        text = {
            LazyColumn {
                items(dummyDevices) {
                    Text(
                        it,
                        modifier = Modifier
                            .clickable { onDeviceSelected(it) }
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
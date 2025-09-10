package com.cm.naviconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cm.naviconnector.feature.control.AppEvent
import com.cm.naviconnector.feature.control.AppUiState
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.MainViewModel
import com.cm.naviconnector.ui.design.AppBackground
import com.cm.naviconnector.ui.design.CircleButton
import com.cm.naviconnector.ui.design.PlaylistPanel
import com.cm.naviconnector.ui.design.TopBar
import com.cm.naviconnector.ui.dial.CircularDial
import com.cm.naviconnector.ui.theme.NaviConnectorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NaviConnectorTheme {
                val uiState by viewModel.uiState.collectAsState()
                AppBackground {
                    MainScreen(uiState = uiState, onEvent = viewModel::onEvent)
                }
            }
        }
    }
}

@Composable
fun MainScreen(uiState: AppUiState, onEvent: (AppEvent) -> Unit) {
    val activeColor = Color(0xFF0F2A5F)
    val inactiveColor = Color.Gray

    if (uiState.showDeviceListDialog) {
        DeviceListDialog(onDismiss = { onEvent(AppEvent.OnDismissDeviceDialog) }) {
            onEvent(AppEvent.OnDeviceChosen(it))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TopBar()

        // Top 4 buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleButton(painter = painterResource(id = R.drawable.ic_launcher_foreground), onClick = {}, enabled = uiState.top1PowerActive, tint = if(uiState.top1PowerActive) activeColor else inactiveColor)
            CircleButton(painter = painterResource(id = R.drawable.ic_launcher_foreground), onClick = {}, onLongClick = { onEvent(AppEvent.OnBtLongPress) }, enabled = true, tint = if(uiState.isConnected) activeColor else inactiveColor)
            CircleButton(painter = painterResource(id = R.drawable.ic_launcher_foreground), onClick = {}, enabled = uiState.isConnected, tint = if(uiState.isConnected) activeColor else inactiveColor)
            CircleButton(painter = painterResource(id = R.drawable.ic_launcher_foreground), onClick = {}, enabled = uiState.isConnected, tint = if(uiState.isConnected) activeColor else inactiveColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        val currentFeatureState = uiState.features[uiState.currentFeature]
        val dialColor = if (currentFeatureState?.isActive == true) activeColor else inactiveColor

        var level by remember { mutableIntStateOf(10) }
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFEFF2FF)),
            contentAlignment = Alignment.Center
        ) {
            CircularDial(
                value = level,
                onValueChange = { level = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Feature 4 buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Feature.values().forEach { feature ->
                val featureState = uiState.features[feature]
                val color = if (featureState?.isActive == true) activeColor else inactiveColor
                CircleButton(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    onClick = { onEvent(AppEvent.OnFeatureTapped(feature)) },
                    tint = color,
                    enabled = uiState.isConnected
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom control bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column {
                CircleButton(painter = painterResource(id = R.drawable.ic_launcher_foreground), onClick = { onEvent(AppEvent.OnPlayClicked) }, modifier = Modifier.size(40.dp), enabled = uiState.isConnected)
                CircleButton(painter = painterResource(id = R.drawable.ic_launcher_foreground), onClick = { onEvent(AppEvent.OnPauseClicked) }, modifier = Modifier.size(40.dp), enabled = uiState.isConnected)
            }
            Spacer(modifier = Modifier.width(8.dp))
            PlaylistPanel(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DeviceListDialog(onDismiss: () -> Unit, onDeviceSelected: (String) -> Unit) {
    val dummyDevices = List(5) { "Device ${it + 1}" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a device") },
        text = {
            LazyColumn {
                items(dummyDevices) {
                    Text(it, modifier = Modifier.clickable { onDeviceSelected(it) }.fillMaxWidth().padding(16.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
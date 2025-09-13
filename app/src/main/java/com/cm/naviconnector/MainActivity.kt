package com.cm.naviconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.Feature
import com.cm.naviconnector.feature.MainViewModel
import com.cm.naviconnector.ui.design.AppBackground
import com.cm.naviconnector.ui.design.CircleButton
import com.cm.naviconnector.ui.design.PlaylistPanel
import com.cm.naviconnector.ui.design.TopBar
import com.cm.naviconnector.ui.dial.CircularDial
import com.cm.naviconnector.ui.theme.LightGrayishBlue
import com.cm.naviconnector.ui.theme.NaviConnectorTheme
import com.cm.naviconnector.ui.theme.Navy

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
    val activeColor = Navy
    val inactiveColor = LightGrayishBlue

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
        Spacer(modifier = Modifier.height(24.dp))
        TopBar()
        Spacer(modifier = Modifier.height(36.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CircleButton(
                painter = painterResource(id = R.drawable.power),
                onClick = {},
                enabled = uiState.powerActive,
                tint = if (uiState.powerActive) activeColor else inactiveColor
            )
            CircleButton(
                painter = painterResource(id = R.drawable.bluetooth),
                onClick = {},
                onLongClick = { onEvent(AppEvent.OnBtLongPress) },
                enabled = true,
                tint = if (uiState.isConnected) activeColor else inactiveColor
            )
            CircleButton(
                painter = painterResource(id = R.drawable.wifi),
                onClick = {},
                enabled = uiState.isConnected,
                tint = if (uiState.isConnected) activeColor else inactiveColor
            )
            CircleButton(
                painter = painterResource(id = R.drawable.upload),
                onClick = {},
                enabled = uiState.isConnected,
                tint = if (uiState.isConnected) activeColor else inactiveColor
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        val currentFeatureState = uiState.features[uiState.currentFeature]
        val dialColor = if (currentFeatureState?.isActive == true) activeColor else inactiveColor
        var level by remember { mutableIntStateOf(0) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.cat), contentDescription = "Cat")
            CircularDial(value = level, onValueChange = { level = it })
            Image(painter = painterResource(id = R.drawable.dog), contentDescription = "Dog")
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Feature.entries.forEach { feature ->
                CircleButton(
                    painter = painterResource(id = feature.resourceId),
                    onClick = { onEvent(AppEvent.OnFeatureTapped(feature)) },
                    tint = dialColor,
                    enabled = uiState.isConnected
                )
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                CircleButton(
                    painter = painterResource(id = R.drawable.play),
                    onClick = { onEvent(AppEvent.OnPlayClicked) },
                    modifier = Modifier.size(60.dp),
                    enabled = uiState.isConnected
                )
                CircleButton(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    onClick = { onEvent(AppEvent.OnPauseClicked) },
                    modifier = Modifier.size(60.dp),
                    enabled = uiState.isConnected
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            PlaylistPanel(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(30.dp))
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
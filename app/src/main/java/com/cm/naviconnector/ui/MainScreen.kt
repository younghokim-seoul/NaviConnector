package com.cm.naviconnector.ui

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cm.naviconnector.R
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.TopButtonType
import com.cm.naviconnector.ui.component.CircleButton
import com.cm.naviconnector.ui.component.PlaylistPanel
import com.cm.naviconnector.ui.component.TopBar
import com.cm.naviconnector.ui.component.CircularSeekbar
import com.cm.naviconnector.ui.dialog.AudioListDialog
import com.cm.naviconnector.ui.dialog.DeviceListDialog
import com.cm.naviconnector.ui.theme.LightGrayishBlue
import com.cm.naviconnector.ui.theme.Navy

@Composable
fun MainScreen(uiState: AppUiState, onEvent: (AppEvent) -> Unit) {
    val activeColor = Navy
    val inactiveColor = LightGrayishBlue

    if (uiState.showDeviceListDialog) {
        DeviceListDialog(
            devices = emptyList(), // TODO: Pass actual device list
            onConnectClick = { device ->
//                onEvent(AppEvent.OnDeviceSelected())
            }
        )
    }

    if (uiState.showAudioListDialog) {
        AudioListDialog(
            audioFiles = emptyList(), // TODO: Pass actual audio file list
            onDismiss = { onEvent(AppEvent.SetAudioDialogVisibility(false)) },
            onAudioFileClick = { /* TODO: Handle audio file click */ }
        )
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
            TopButtonType.entries.forEach { buttonType ->
                val tint = when (buttonType) {
                    TopButtonType.POWER -> if (uiState.powerActive) activeColor else inactiveColor
                    else -> if (uiState.isConnected) activeColor else inactiveColor
                }
                val enabled = when (buttonType) {
                    TopButtonType.POWER -> uiState.powerActive
                    TopButtonType.BLUETOOTH -> true
                    else -> uiState.isConnected
                }

                CircleButton(
                    painter = painterResource(id = buttonType.icon),
                    onClick = {
                        when (buttonType) {
                            TopButtonType.AUDIO -> onEvent(AppEvent.SetAudioDialogVisibility(true))
                            TopButtonType.POWER -> TODO()
                            TopButtonType.BLUETOOTH -> TODO()
                            TopButtonType.WIFI -> TODO()
                            TopButtonType.UPLOAD -> TODO()
                        }
                    },
                    onLongClick = if (buttonType == TopButtonType.BLUETOOTH) {
                        { onEvent(AppEvent.OnBtLongPress) }
                    } else null,
                    enabled = enabled,
                    tint = tint
                )
            }
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
            CircularSeekbar(value = level, onValueChange = { level = it })
            Image(painter = painterResource(id = R.drawable.dog), contentDescription = "Dog")
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Feature.entries.forEach { feature ->
                CircleButton(
                    painter = painterResource(id = feature.icon),
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
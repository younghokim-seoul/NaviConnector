package com.cm.naviconnector.ui

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.cm.naviconnector.MainViewModel
import com.cm.naviconnector.R
import com.cm.naviconnector.feature.AppEffect
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.TopButtonType
import com.cm.naviconnector.ui.component.CircleButton
import com.cm.naviconnector.ui.component.CircularSeekbar
import com.cm.naviconnector.ui.component.PlaylistPanel
import com.cm.naviconnector.ui.component.TopBar
import com.cm.naviconnector.ui.dialog.AudioListDialog
import com.cm.naviconnector.ui.dialog.DeviceListDialog
import com.cm.naviconnector.ui.theme.LightGrayishBlue
import com.cm.naviconnector.ui.theme.Navy

@Composable
fun MainRoute(vm: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val currentContext by rememberUpdatedState(newValue = context)

    var showDeviceDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is AppEffect.SetDeviceDialogVisible -> showDeviceDialog = effect.visible
                is AppEffect.SetAudioDialogVisible -> showAudioDialog = effect.visible
                is AppEffect.ShowToast -> Toast.makeText(
                    currentContext,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (showDeviceDialog) {
        val devices by vm.scannedDevices.collectAsStateWithLifecycle()
        DeviceListDialog(
            devices = devices,
            onConnectClick = { vm.onClickDevice(it) }
        )
    }

    if (showAudioDialog) {
        val audioFiles = vm.audioPaging.collectAsLazyPagingItems()
        AudioListDialog(
            audioFiles = audioFiles,
            onAudioFileClick = { vm.onAudioFileClick(it) },
        )
    }

    val uiState = vm.uiState.collectAsStateWithLifecycle().value

    MainScreen(
        uiState = uiState,
        onEvent = vm::onEvent
    )
}

@Composable
fun MainScreen(
    uiState: AppUiState,
    onEvent: (AppEvent) -> Unit,
) {
    val activeColor = Navy
    val inactiveColor = LightGrayishBlue

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
                    TopButtonType.POWER -> if (uiState.isPowerOn) activeColor else inactiveColor
                    else -> if (uiState.isConnected) activeColor else inactiveColor
                }
                val enabled = when (buttonType) {
                    TopButtonType.POWER -> uiState.isPowerOn
                    else -> uiState.isConnected
                }

                CircleButton(
                    painter = painterResource(id = buttonType.icon),
                    onClick = { onEvent(AppEvent.OnTopButtonTapped(buttonType)) },
                    enabled = enabled,
                    tint = tint
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        val currentFeature = uiState.currentFeature
        val currentFeatureState = uiState.features[currentFeature]
        val pointerShadowColor = currentFeature.color
        val buttonColor = if (currentFeatureState?.isActive == true) activeColor else inactiveColor
        val level = currentFeatureState?.level ?: 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.cat), contentDescription = "Cat")
            CircularSeekbar(
                value = level,
                onValueChange = { onEvent(AppEvent.OnDialChanged(it)) },
                pointerShadowColor = pointerShadowColor
            )
            Image(painter = painterResource(id = R.drawable.dog), contentDescription = "Dog")
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Feature.entries.forEach { feature ->
                CircleButton(
                    painter = painterResource(id = feature.icon),
                    onClick = { onEvent(AppEvent.OnFeatureTapped(feature)) },
                    tint = buttonColor,
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
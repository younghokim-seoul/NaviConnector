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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.graphics.Color
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
import com.cm.naviconnector.feature.control.BottomButtonType
import com.cm.naviconnector.feature.control.MainFeature
import com.cm.naviconnector.feature.control.PlaylistItem
import com.cm.naviconnector.feature.control.SubFeature
import com.cm.naviconnector.feature.control.TopButtonType
import com.cm.naviconnector.feature.upload.UploadState
import com.cm.naviconnector.ui.component.CircleButton
import com.cm.naviconnector.ui.component.CircularSeekbar
import com.cm.naviconnector.ui.component.PlaylistPanel
import com.cm.naviconnector.ui.component.RectangleButton
import com.cm.naviconnector.ui.component.Label
import com.cm.naviconnector.ui.component.TopBar
import com.cm.naviconnector.ui.dialog.AudioListDialog
import com.cm.naviconnector.ui.dialog.DeviceListDialog
import com.cm.naviconnector.ui.dialog.UploadProgressDialog
import com.cm.naviconnector.ui.theme.LightGrayishBlue
import com.cm.naviconnector.ui.theme.Navy

@Composable
fun MainRoute(vm: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val currentContext by rememberUpdatedState(newValue = context)

    var showDeviceDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioDialog by rememberSaveable { mutableStateOf(false) }
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }

    val uiState = vm.uiState.collectAsStateWithLifecycle().value
    val playlist = vm.playlist.collectAsStateWithLifecycle().value

    HandleDialogs(
        vm = vm,
        showDeviceDialog = showDeviceDialog,
        showAudioDialog = showAudioDialog,
        showUploadDialog = showUploadDialog
    )

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is AppEffect.SetDeviceDialogVisible -> showDeviceDialog = effect.visible
                is AppEffect.SetAudioDialogVisible -> showAudioDialog = effect.visible
                is AppEffect.SetUploadDialogVisible -> showUploadDialog = effect.visible
                is AppEffect.ShowToast -> Toast.makeText(
                    currentContext,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    MainScreen(
        uiState = uiState,
        onEvent = vm::onEvent,
        playlist = playlist
    )
}

@Composable
private fun HandleDialogs(
    vm: MainViewModel, // TODO: vm 분리
    showDeviceDialog: Boolean,
    showAudioDialog: Boolean,
    showUploadDialog: Boolean
) {
    if (showDeviceDialog) {
        val devices by vm.scannedDevices.collectAsStateWithLifecycle()
        DeviceListDialog(
            devices = devices,
            onConnectClick = { vm.onEvent(AppEvent.DeviceConnectClicked(it)) },
            onDismissRequest = { vm.onEvent(AppEvent.DeviceDialogDismissed) }
        )
    }

    if (showAudioDialog) {
        val audioFiles = vm.audioPaging.collectAsLazyPagingItems()
        AudioListDialog(
            audioFiles = audioFiles,
            onUploadClick = { vm.onEvent(AppEvent.AudioUploadClicked(it)) },
            onDismissRequest = { vm.onEvent(AppEvent.AudioDialogDismissed) }
        )
    }

    if (showUploadDialog) {
        val uploadState = vm.uiState.collectAsStateWithLifecycle().value.uploadState
        when (uploadState) {
            is UploadState.InProgress -> UploadProgressDialog(progress = uploadState.progress)
            is UploadState.Idle -> Unit
        }
    }
}

@Composable
fun MainScreen(
    uiState: AppUiState,
    onEvent: (AppEvent) -> Unit,
    playlist: List<PlaylistItem>,
) {
    val activeColor = Navy
    val inactiveColor = LightGrayishBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TopBar(isLowBattery = uiState.isLowBattery)
        Spacer(modifier = Modifier.height(30.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TopButtonType.entries.forEach { buttonType ->
                val tint = when (buttonType) {
                    TopButtonType.POWER -> if (uiState.isPowerOn) activeColor else inactiveColor
                    else -> if (uiState.isConnected) activeColor else inactiveColor
                }
                val enabled = when (buttonType) {
                    TopButtonType.BLUETOOTH -> true
                    else -> uiState.isConnected
                }

                CircleButton(
                    painter = painterResource(id = buttonType.icon),
                    onClick = { onEvent(AppEvent.TopButtonClicked(buttonType)) },
                    enabled = enabled,
                    tint = tint
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        val currentFeature = uiState.currentFeature
        val currentFeatureState = uiState.features[currentFeature]
        val accentColor = currentFeature?.color ?: Color.LightGray
        val level = currentFeatureState?.level ?: 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.cat), contentDescription = "Cat")
            CircularSeekbar(
                value = level,
                onValueChangeFinished = { onEvent(AppEvent.DialChanged(it)) },
                accentColor = accentColor,
                enabled = uiState.currentFeature != null && uiState.features[uiState.currentFeature]?.enabled == true
            )
            Image(painter = painterResource(id = R.drawable.dog), contentDescription = "Dog")
        }

        Spacer(modifier = Modifier.height(5.dp))

        MainFeatures(
            uiState = uiState,
            onEvent = onEvent,
            activeColor = activeColor,
            inactiveColor = inactiveColor
        )

        Spacer(modifier = Modifier.height(50.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                BottomButtonType.entries.forEach { buttonType ->
                    val painterId =
                        if (uiState.isConnected) buttonType.enabledIcon else buttonType.disabledIcon

                    RectangleButton(
                        painter = painterResource(id = painterId),
                        onClick = { onEvent(AppEvent.BottomButtonClicked(buttonType)) },
                        modifier = Modifier.size(60.dp),
                        enabled = uiState.isConnected
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            PlaylistPanel(
                modifier = Modifier.weight(1f),
                playlist = playlist,
                selectedFileName = uiState.selectedFileName,
                onItemClick = { onEvent(AppEvent.PlaylistItemClicked(it)) }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MainFeatures(
    uiState: AppUiState,
    onEvent: (AppEvent) -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    Row( // TODO: Row 중첩 필요?
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        MainFeature.mainFeatures.forEach { mainFeature ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    val isSelected = when (val current = uiState.currentFeature) {
                        is MainFeature -> current == mainFeature
                        is SubFeature -> mainFeature.subFeatures.contains(current)
                        else -> false
                    }

                    if (isSelected) {
                        mainFeature.subFeatures.forEach { subFeature ->
                            val subFeatureState = uiState.features[subFeature]
                            val subFeatureEnabled = subFeatureState?.enabled == true

                            Label(
                                text = subFeature.id,
                                tint = if (subFeatureEnabled) activeColor else inactiveColor,
                                onClick = { onEvent(AppEvent.FeatureToggled(subFeature)) }
                            )
                        }
                    }
                }

                val featureState = uiState.features[mainFeature]
                val enabled = featureState?.enabled == true
                val tint = if (enabled) activeColor else inactiveColor

                CircleButton(
                    painter = painterResource(id = mainFeature.icon),
                    onClick = { onEvent(AppEvent.FeatureToggled(mainFeature)) },
                    tint = tint,
                    enabled = uiState.isConnected
                )
            }
        }
    }
}
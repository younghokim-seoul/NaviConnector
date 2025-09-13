package com.cm.naviconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cm.naviconnector.ui.MainScreen
import com.cm.naviconnector.ui.design.AppBackground
import com.cm.naviconnector.ui.theme.NaviConnectorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
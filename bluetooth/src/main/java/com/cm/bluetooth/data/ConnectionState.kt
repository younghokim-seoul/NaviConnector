package com.cm.bluetooth.data

import android.bluetooth.BluetoothDevice

data class ConnectionState(
    val state: Int,
    val previousState: Int,
    val bluetoothDevice: BluetoothDevice?
)

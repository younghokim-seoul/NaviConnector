package com.cm.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build

fun Intent.getBluetoothDeviceExtra(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
    }
}
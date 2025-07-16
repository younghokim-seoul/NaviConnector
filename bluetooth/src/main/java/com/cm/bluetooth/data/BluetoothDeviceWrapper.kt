package com.cm.bluetooth.data

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BluetoothDeviceWrapper(val bluetoothDevice: BluetoothDevice, val rssi: Int) : Parcelable

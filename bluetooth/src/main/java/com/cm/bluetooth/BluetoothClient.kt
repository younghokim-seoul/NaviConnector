package com.cm.bluetooth
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context

@SuppressLint("StaticFieldLeak")

class BluetoothClient(private val context : Context) {

    companion object {

        @Volatile
        private var INSTANCE: BluetoothClient? = null

        fun getInstance(context: Context): BluetoothClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothClient(context).also { INSTANCE = it }
            }
        }
    }

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter

    @Volatile
    var bluetoothSocket: BluetoothSocket? = null


}
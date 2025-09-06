package com.cm.bluetooth

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.io.IOException

class BluetoothIO(val bluetoothSocket: BluetoothSocket?) {
    private val inputStream = getSocketStream { bluetoothSocket?.inputStream }
    private val outputStream = getSocketStream { bluetoothSocket?.outputStream }

    fun sendPacket(data: ByteArray): Boolean {
        if (bluetoothSocket?.isConnected != true) return false
        return try {
            val stream = outputStream
            if (stream == null) {
                Timber.e("Bluetooth OutputStream is null.")
                return false
            }
            stream.write(data)
            stream.flush()
            true
        } catch (e: IOException) {
            Timber.e(e, "Error sending data over Bluetooth")
            false
        }
    }

    fun receivePacket() = channelFlow {
        if (inputStream == null) {
            throw NullPointerException("inputStream is null. Perhaps bluetoothSocket is also null")
        }

        while (isActive) {
            try {
                trySend(inputStream.read().toByte())
            } catch (e: IOException) {
                error("Couldn't read bytes from flow. Disconnected")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun closeConnections() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


private fun <T> getSocketStream(block: () -> T?): T? {
    return try {
        block()
    } catch (e: IOException) {
        Timber.e(e, "Couldn't open Bluetooth socket stream")
        throw IOException("Couldn't open Bluetooth socket stream: ${e.message}")
    }
}
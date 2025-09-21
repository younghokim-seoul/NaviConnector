package com.cm.bluetooth

import android.bluetooth.BluetoothSocket
import com.cm.bluetooth.data.reqeust.RequestPacket
import com.cm.bluetooth.data.response.NabiPacket
import com.cm.bluetooth.data.response.NabiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException

class BluetoothConnection(val bluetoothSocket: BluetoothSocket?) {
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

    @ExperimentalCoroutinesApi
    fun receivePacket(
        bufferCapacity: Int = 1024,
    ): Flow<NabiPacket> = channelFlow {

        if (inputStream == null) {
            throw NullPointerException("inputStream is null. Perhaps bluetoothSocket is also null")
        }

        val byteAccumulator = ByteArrayOutputStream()
        val buffer = ByteArray(bufferCapacity)

        // 코루틴이 살아있는 동안 계속해서 블루투스 데이터를 읽어온다.
        while (isActive) {
            try {
                val numBytes = inputStream.read(buffer)
                if (numBytes < 0) break

                byteAccumulator.write(buffer, 0, numBytes)

                // --- 안쪽 루프 ---
                // 방금 읽어온 데이터를 포함한 전체 버퍼에서
                // 완성된 패킷이 여러 개일 수 있으므로, 전부 처리하기 위해 반복
                while (true) {
                    val accumulatedBytes = byteAccumulator.toByteArray()
                    if (accumulatedBytes.isEmpty()) break

                    val rawPacket = findCompletePacket(accumulatedBytes)

                    if (rawPacket != null) {
                        // 패킷을 찾았으면 파싱해서 전달하고, 버퍼에서 제거
                        val nabiPacket = NabiParser.parse(rawPacket)
                        trySend(nabiPacket)

                        // 처리한 패킷만큼 버퍼에서 제거합니다.
                        val remainingBytes = accumulatedBytes.drop(rawPacket.size).toByteArray()
                        byteAccumulator.reset()
                        byteAccumulator.write(remainingBytes)
                    } else {
                        // 버퍼에 더 이상 완성된 패킷이 없으면 안쪽 루프를 종료
                        break
                    }
                }
            } catch (e: IOException) {
                close(e)
                break // 예외 발생 시 루프 종료
            }
        }
    }.flowOn(Dispatchers.IO)


    private fun findCompletePacket(bytes: ByteArray): ByteArray? {
        val startIndex = bytes.indexOf(RequestPacket.STX)
        if (startIndex == -1) return null

        if (bytes.size < startIndex + 3) return null

        val dataLength = ((bytes[startIndex + 1].toInt() and 0xFF) shl 8) or (bytes[startIndex + 2].toInt() and 0xFF)

        val totalPacketLength = 7 + dataLength

        if (bytes.size < startIndex + totalPacketLength) return null

        val potentialPacket = bytes.copyOfRange(startIndex, startIndex + totalPacketLength)

        return if (potentialPacket.last() == RequestPacket.ETX) {
            potentialPacket
        } else {
            null
        }
    }

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
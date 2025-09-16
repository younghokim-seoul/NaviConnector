package com.cm.bluetooth.data.response

import com.cm.bluetooth.data.reqeust.ModbusCrc

object NabiParser {
    fun parse(packet: ByteArray) : NabiPacket {
        val len = ((packet[1].toInt() and 0xFF) shl 8) or (packet[2].toInt() and 0xFF)
        val cmd = packet[3]
        val data = if (len > 0) packet.copyOfRange(4, 4 + len) else byteArrayOf()
        val crcReceived = ((packet[4 + len].toInt() and 0xFF) shl 8) or (packet[5 + len].toInt() and 0xFF)


        // CRC 검증
        val crcCalculated = ModbusCrc.compute(packet.copyOfRange(3, 4 + len))

        if (crcReceived != crcCalculated) {
            return InvalidPacket.CrcError(packet.toList())
        }

        return InvalidPacket.Unknown(packet.toList(),cmd)
    }
}
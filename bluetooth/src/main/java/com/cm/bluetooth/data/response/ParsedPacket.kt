package com.cm.bluetooth.data.response

sealed class ParsedPacket : NabiPacket {
}


sealed class InvalidPacket : NabiPacket {
    // CRC 체크 실패
    data class CrcError(
        override val raw: List<Byte>,
    ) : InvalidPacket() {
        override val command: Byte = -1
    }

    // 알 수 없는 CMD
    data class Unknown(
        override val raw: List<Byte>,
        override val command: Byte
    ) : InvalidPacket()
}
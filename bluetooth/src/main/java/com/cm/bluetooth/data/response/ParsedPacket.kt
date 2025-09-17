package com.cm.bluetooth.data.response

sealed class ParsedPacket : NabiPacket {

    // STATUS_INFO (0x02) 응답
    data class StatusInfo(
        override val raw: List<Byte>,
        val battery: Int,
        val filmStatus: Int,
        val fanStatus: Int,
        val heaterStatus: Int,
        val isAudioPlaying: Boolean,
        val volume: Int
    ) : ParsedPacket() {
        override val command: Byte = 0x02
    }
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
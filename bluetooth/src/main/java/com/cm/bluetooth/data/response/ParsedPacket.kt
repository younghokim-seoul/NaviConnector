package com.cm.bluetooth.data.response

import com.cm.bluetooth.data.reqeust.CMD

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
        override val command: Byte = CMD.STATUS_INFO.toByte()
    }

    // LOW_BATTERY (0x21) 알림
    data class LowBattery(
        override val raw: List<Byte>,
        val battery: Int
    ) : ParsedPacket() {
        override val command: Byte = CMD.LOW_BATTERY.toByte()
    }

    // ACK 응답 (CONTROL, SET_VOLUME 등)
    data class Ack(
        override val raw: List<Byte>,
        override val command: Byte // 어떤 명령어에 대한 ACK인지 구분
    ) : ParsedPacket()
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
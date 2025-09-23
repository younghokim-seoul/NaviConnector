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

    // GET_AUDIO_LIST (0x04) 응답
    data class AudioList(
        override val raw: List<Byte>,
        val currentPage: Int,
        val totalPages: Int,
        val fileCountInPage: Int,
        val fileNames: List<String>
    ) : ParsedPacket() {
        override val command: Byte = CMD.GET_AUDIO_LIST.toByte()
    }

    //PLAY_AUDIO (0x05) 응답
    data class PlayAudioResult(
        override val raw: List<Byte>,
        val resultCode: ResultCode
    ) : ParsedPacket() {
        override val command: Byte = CMD.PLAY_AUDIO.toByte()

        enum class ResultCode(val value: Byte) {
            SUCCESS(0x01),
            SD_CARD_NOT_FOUND(0xFF.toByte()),
            PLAYBACK_FAILED(0x02),
            UNKNOWN(0x00); // 예외 처리용

            companion object {
                fun fromByte(value: Byte): ResultCode =
                    entries.find { it.value == value } ?: UNKNOWN
            }
        }
    }

    //UPLOAD_DOING (0x12) 응답
    data class UploadDoingAck(
        override val raw: List<Byte>,
        val frameNumber: Int
    ) : ParsedPacket() {
        override val command: Byte = CMD.UPLOAD_DOING.toByte()
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
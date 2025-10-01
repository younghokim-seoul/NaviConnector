package com.cm.bluetooth.data.response

import com.cm.bluetooth.data.reqeust.CMD
import com.cm.bluetooth.data.reqeust.ModbusCrc
import com.cm.bluetooth.data.toHex
import timber.log.Timber
import java.nio.charset.Charset

object NabiParser {
    fun parse(packet: ByteArray): NabiPacket {
        Timber.tag("packet").d("NabiParser: packet to hex: ${packet.toHex()}")

        val len = ((packet[1].toInt() and 0xFF) shl 8) or (packet[2].toInt() and 0xFF)
        val cmd = packet[3]
        val data = if (len > 0) packet.copyOfRange(4, 4 + len) else byteArrayOf()
        val crcReceived =
            ((packet[4 + len].toInt() and 0xFF) shl 8) or (packet[5 + len].toInt() and 0xFF)


        // CRC 검증
        val crcCalculated = ModbusCrc.compute(packet.copyOfRange(3, 4 + len))

        if (crcReceived != crcCalculated) {
            return InvalidPacket.CrcError(packet.toList())
        }

        return when (cmd) {
            CMD.CONTROL.toByte(),         // 0x01
            CMD.TRAINING_MODE.toByte(),   // 0x03
            CMD.STOP_AUDIO.toByte(),      // 0x06
            CMD.SET_VOLUME.toByte(),      // 0x07
            CMD.UPLOAD_START.toByte(),    // 0x11
            CMD.UPLOAD_END.toByte() -> {  // 0x13
                if (data.isEmpty()) {
                    ParsedPacket.Ack(packet.toList(), cmd)
                } else {
                    InvalidPacket.Unknown(packet.toList(), cmd)
                }
            }

            CMD.STATUS_INFO.toByte() -> {
                ParsedPacket.StatusInfo(
                    raw = packet.toList(),
                    battery = data[0].toInt() and 0xFF,
                    filmStatus = data[1].toInt() and 0xFF,
                    fanStatus = data[2].toInt() and 0xFF,
                    heaterStatus = data[3].toInt() and 0xFF,
                    isAudioPlaying = data[4] == 0x01.toByte(),
                    volume = data[5].toInt() and 0xFF
                )
            }

            CMD.GET_AUDIO_LIST.toByte() -> {
                if (data.size > 3) {
                    val fileNamesBytes = data.drop(3).toByteArray()
                    // 쉼표로 구분된 문자열을 파싱하여 리스트로 만듬
                    val fileNames = String(fileNamesBytes, Charset.defaultCharset())
                        .split(',')
                        .filter { it.isNotEmpty() }
                    ParsedPacket.AudioList(
                        raw = packet.toList(),
                        currentPage = data[0].toInt() and 0xFF,
                        totalPages = data[1].toInt() and 0xFF,
                        fileCountInPage = data[2].toInt() and 0xFF,
                        fileNames = fileNames
                    )
                } else {
                    InvalidPacket.Unknown(packet.toList(), cmd)
                }
            }

            CMD.PLAY_AUDIO.toByte() -> {
                if (data.size == 1) {
                    ParsedPacket.PlayAudioResult(
                        raw = packet.toList(),
                        resultCode = ParsedPacket.PlayAudioResult.ResultCode.fromByte(data[0])
                    )
                } else {
                    InvalidPacket.Unknown(packet.toList(), cmd)
                }
            }

            CMD.UPLOAD_DOING.toByte() -> {
                if (data.size == 2) {
                    val frameNumber = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                    ParsedPacket.UploadDoingAck(
                        raw = packet.toList(),
                        frameNumber = frameNumber
                    )
                } else {
                    InvalidPacket.Unknown(packet.toList(), CMD.UPLOAD_DOING.toByte())
                }
            }

            CMD.LOW_BATTERY.toByte() -> if (data.size == 1) {
                ParsedPacket.LowBattery(
                    raw = packet.toList(),
                    battery = data[0].toInt() and 0xFF
                )
            } else {
                InvalidPacket.Unknown(packet.toList(), CMD.LOW_BATTERY.toByte())
            }


            else -> InvalidPacket.Unknown(packet.toList(), cmd)
        }
    }
}
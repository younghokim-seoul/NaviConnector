package com.cm.bluetooth.data.response

interface NabiPacket {
    val raw: List<Byte>
    val command: Byte
}
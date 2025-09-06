package com.cm.bluetooth.data.reqeust

object ModbusCrc {
    fun compute(data: ByteArray): Int {
        var crc = 0xFFFF
        for (raw in data) {
            val v = raw.toInt() and 0xFF
            crc = crc xor v
            repeat(8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xFFFF
    }
}
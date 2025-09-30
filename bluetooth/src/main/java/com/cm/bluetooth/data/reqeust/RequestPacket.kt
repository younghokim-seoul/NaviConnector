package com.cm.bluetooth.data.reqeust

abstract class RequestPacket(private val cmd: Int) {
    init {
        require(cmd in 0x00..0xFF) { "CMD must be 1 byte" }
    }

    protected abstract fun data(): ByteArray


    /**
     * 패킷 프레임 구성
    구분 STX LEN CMD Data CRC16 ETX
    길이  1  2    1   N   2     1
     */
    fun toByteArray(): ByteArray {
        val d = data()
        require(d.size in 0..2052) { "Data length out of range (0..2052)" }

        val payload = ByteArray(1 + d.size)
        payload[0] = cmd.toByte()
        if (d.isNotEmpty()) System.arraycopy(d, 0, payload, 1, d.size)
        val crc = ModbusCrc.compute(payload) // 0..0xFFFF


        val out = ByteArray(1 + 2 + 1 + d.size + 2 + 1)
        var p = 0
        // STX
        out[p++] = STX
        // LEN (Big-Endian)
        val len = d.size
        out[p++] = ((len ushr 8) and 0xFF).toByte()
        out[p++] = (len and 0xFF).toByte()
        // CMD + Data
        out[p++] = cmd.toByte()
        if (d.isNotEmpty()) {
            System.arraycopy(d, 0, out, p, d.size)
            p += d.size
        }
        // CRC16 (Big-Endian: MSB → LSB)
        out[p++] = ((crc ushr 8) and 0xFF).toByte()
        out[p++] = (crc and 0xFF).toByte()
        // ETX
        out[p] = ETX


        return out
    }


    companion object {
        const val STX: Byte = 0x02
        const val ETX: Byte = 0x03
    }
}
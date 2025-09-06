package com.cm.bluetooth.data

fun ByteArray.toHex(): String {
    return this.joinToString("") { byte ->
        String.format("%02X", byte.toInt() and 0xFF)
    }
}

fun String.toUtf8Fixed(fixedLen: Int = 64): ByteArray {
    val raw = this.toByteArray(Charsets.UTF_8)
    require(raw.size <= fixedLen) { "String too long: ${raw.size} > $fixedLen" }
    if (raw.size == fixedLen) return raw
    val out = ByteArray(fixedLen)
    System.arraycopy(raw, 0, out, 0, raw.size)
    return out
}

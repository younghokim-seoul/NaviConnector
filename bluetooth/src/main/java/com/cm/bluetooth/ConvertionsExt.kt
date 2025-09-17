package com.cm.bluetooth


val ByteArray.toHexUpper
    inline get() = this.joinToString(separator = "") {
        String.format(
            "%02X",
            (it.toInt() and 0xFF)
        )
    }
val ByteArray.toHexString inline get() = this.toHexUpper

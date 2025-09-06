package com.cm.bluetooth.data.reqeust

object CMD {
    const val CONTROL = 0x01
    const val STATUS_INFO = 0x02
    const val TRAINING_MODE = 0x03
    const val GET_AUDIO_LIST = 0x04
    const val PLAY_AUDIO = 0x05
    const val STOP_AUDIO = 0x06
    const val SET_VOLUME = 0x07
    const val UPLOAD_START = 0x11
    const val UPLOAD_DOING = 0x12
    const val UPLOAD_END = 0x13
    const val LOW_BATTERY = 0x21
}

enum class ControlTarget(val code: Int) {
    FILM_AND_FAN(0x00),
    FILM(0x01),
    FAN(0x02),
    HEATER(0x03)
}

enum class TrainingMode(val code: Int) {
    SLOW(1),
    RANDOM(2)
}
package com.cm.bluetooth.data.reqeust

import com.cm.bluetooth.data.toUtf8Fixed


/** CONTROL (0x01) – target :
0x00: 필름, 팬 동시제어
0x01: 필름
0x02: 팬
0x03: 히터,
value : 제어 값: 0 ~ 255(0의경우 OFF)
 */
class ControlPacket(
    private val target: ControlTarget,
    private val value: Int
) : RequestPacket(CMD.CONTROL) {
    init {
        require(value in 0..255) { "Control value must be 0..255" }
    }

    override fun data(): ByteArray = byteArrayOf(
        target.code.toByte(),
        value.toByte()
    )
}

/** STATUS_INFO (0x02)
 * 장비 상태정보 조회 요청. Data 없음(LEN=0).
 */
class StatusInfoRequest : RequestPacket(CMD.STATUS_INFO) {
    override fun data(): ByteArray = byteArrayOf()
}

/** TRAINING_MODE (0x03) – mode = 1: Training slow 모드 2: Training Random 모드, seconds : Training 시간: 0~100초 */
class TrainingModeRequest(
    private val mode: TrainingMode,
    private val seconds: Int = 50
) : RequestPacket(CMD.TRAINING_MODE) {
    init {
        require(seconds in 0..100) { "seconds must be 0..100" }
    }

    override fun data(): ByteArray = byteArrayOf(
        mode.code.toByte(),
        seconds.toByte()
    )
}

/** GET_AUDIO_LIST (0x04)
 * SD카드 사운드 파일 목록 조회. page [페이지번호(1: 1~255)].
 */
class GetAudioListRequest(
    private val page: Int
) : RequestPacket(CMD.GET_AUDIO_LIST) {
    init {
        require(page in 1..255) { "page must be 1..255" }
    }

    override fun data(): ByteArray = byteArrayOf(page.toByte())
}

/** PLAY_AUDIO (0x05)
 * 지정한 파일명을 재생. 파일명은 최대 64바이트.
 */
class PlayAudioRequest(
    private val filename: String
) : RequestPacket(CMD.PLAY_AUDIO) {
    override fun data(): ByteArray = filename.toUtf8Fixed()
}


/** STOP_AUDIO (0x06)
 * 재생 중인 오디오 중지. Data 없음.
 */
class StopAudioRequest : RequestPacket(CMD.STOP_AUDIO) {
    override fun data(): ByteArray = byteArrayOf()
}

/** SET_VOLUME (0x07)
 * 오디오 볼륨 설정(0~10). 0은 음소거.
 */
class SetVolumeRequest(
    private val volume: Int
) : RequestPacket(CMD.SET_VOLUME) {
    init {
        require(volume in 0..10) { "volume must be 0..10" }
    }

    override fun data(): ByteArray = byteArrayOf(volume.toByte())
}
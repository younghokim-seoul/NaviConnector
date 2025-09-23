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


/**
 * UPLOAD_START (0x11)
 * 오디오 파일 전송 시작을 알림.
 * @param filename 전송할 파일명 (최대 64바이트)
 * @param totalFrames 전체 프레임 개수
 */
class UploadStartRequest(
    private val filename: String,
    private val totalFrames: Int
) : RequestPacket(CMD.UPLOAD_START) {
    init {
        require(totalFrames in 0..65535) { "totalFrames must be 0..65535" }
    }

    override fun data(): ByteArray {
        val fileBytes = filename.toUtf8Fixed(64)
        val frameBytes = byteArrayOf(
            ((totalFrames ushr 8) and 0xFF).toByte(),
            (totalFrames and 0xFF).toByte()
        )
        return fileBytes + frameBytes
    }
}

/**
 * UPLOAD_DOING (0x12)
 * 실제 파일 데이터를 프레임 단위로 전송.
 * @param frameNumber 현재 프레임의 인덱스 (0부터 시작)
 * @param frameData 실제 데이터 (최대 2048바이트)
 */
class UploadDoingRequest(
    private val frameNumber: Int,
    private val frameData: ByteArray
) : RequestPacket(CMD.UPLOAD_DOING) {

    init {
        require(frameNumber in 0..65535) { "frameNumber must be 0..65535" }
        require(frameData.size in 1..2048) { "frameData size must be 1..2048" }
    }

    override fun data(): ByteArray {
        val header = byteArrayOf(
            ((frameNumber ushr 8) and 0xFF).toByte(),
            (frameNumber and 0xFF).toByte(),
            ((frameData.size ushr 8) and 0xFF).toByte(),
            (frameData.size and 0xFF).toByte()
        )
        // 데이터 프레임은 2048바이트로 고정, 실제 데이터가 작으면 0으로 패딩됨
        val paddedData = ByteArray(2048)
        System.arraycopy(frameData, 0, paddedData, 0, frameData.size)

        return header + paddedData
    }
}

/**
 * UPLOAD_END (0x13)
 * 파일 전송 완료를 알림.
 * @param filename 전송 완료한 파일명 (최대 64바이트)
 */
class UploadEndRequest(
    private val filename: String
) : RequestPacket(CMD.UPLOAD_END) {
    override fun data(): ByteArray = filename.toUtf8Fixed(64)
}
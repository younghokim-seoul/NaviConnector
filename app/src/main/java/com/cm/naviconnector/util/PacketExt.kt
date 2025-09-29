package com.cm.naviconnector.util

import com.cm.bluetooth.data.reqeust.CMD
import com.cm.bluetooth.data.reqeust.ControlPacket
import com.cm.bluetooth.data.reqeust.GetAudioListRequest
import com.cm.bluetooth.data.reqeust.PlayAudioRequest
import com.cm.bluetooth.data.reqeust.RequestPacket
import com.cm.bluetooth.data.reqeust.SetVolumeRequest
import com.cm.bluetooth.data.reqeust.StatusInfoRequest
import com.cm.bluetooth.data.reqeust.StopAudioRequest
import com.cm.bluetooth.data.reqeust.TrainingModeRequest
import com.cm.bluetooth.data.reqeust.UploadDoingRequest
import com.cm.bluetooth.data.reqeust.UploadEndRequest
import com.cm.bluetooth.data.reqeust.UploadStartRequest

fun RequestPacket.commandByte(): Byte? = when (this) {
    is StatusInfoRequest -> CMD.STATUS_INFO
    is ControlPacket -> CMD.CONTROL
    is SetVolumeRequest -> CMD.SET_VOLUME
    is GetAudioListRequest -> CMD.GET_AUDIO_LIST
    is PlayAudioRequest -> CMD.PLAY_AUDIO
    is StopAudioRequest -> CMD.STOP_AUDIO
    is UploadStartRequest -> CMD.UPLOAD_START
    is UploadDoingRequest -> CMD.UPLOAD_DOING
    is UploadEndRequest -> CMD.UPLOAD_END
    is TrainingModeRequest -> CMD.TRAINING_MODE
    else -> null
}?.toByte()
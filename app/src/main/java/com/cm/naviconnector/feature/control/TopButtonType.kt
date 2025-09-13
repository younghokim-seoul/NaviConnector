package com.cm.naviconnector.feature.control

import androidx.annotation.DrawableRes
import com.cm.naviconnector.R

enum class TopButtonType(@DrawableRes val icon: Int) {
    POWER(R.drawable.power),
    BLUETOOTH(R.drawable.bluetooth),
    WIFI(R.drawable.wifi),
    UPLOAD(R.drawable.upload)
}
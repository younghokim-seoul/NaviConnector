package com.cm.naviconnector.util

import kotlinx.coroutines.channels.Channel

suspend fun <T> Channel<T>.sendAll(vararg items: T) {
    for (i in items) send(i)
}
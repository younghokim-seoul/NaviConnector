package com.cm.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import com.cm.bluetooth.data.BluetoothDeviceWrapper
import com.cm.bluetooth.data.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.Short.Companion.MIN_VALUE
import timber.log.Timber

@SuppressLint("StaticFieldLeak", "HardwareIds", "MissingPermission")
class BluetoothClient(private val context: Context) {

    companion object {

        @Volatile
        private var INSTANCE: BluetoothClient? = null

        fun getInstance(context: Context): BluetoothClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothClient(context).also { INSTANCE = it }
            }
        }
    }

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val bluetoothAdapter = bluetoothManager?.adapter


    var bluetoothSocket: BluetoothSocket? = null

    fun isBluetoothAvailable() = bluetoothAdapter != null && bluetoothAdapter.address.isNotEmpty()
    fun isBluetoothEnabled() = bluetoothAdapter?.isEnabled

    fun isLocationServiceEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER

        )


    //이미 페어링(연결/등록)된 블루투스 기기들의 목록
    fun bondedDevices(): Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

    //원격 장치 탐색(검색) 프로세스를 시작
    fun startScan() = bluetoothAdapter?.startDiscovery()


    //블루투스 디바이스 탐색 결과
    fun collectScanDevices() = channelFlow {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        Timber.i("FOUND DEVICE")

                        val device = intent.getBluetoothDeviceExtra()

                        val rssi =
                            intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, MIN_VALUE).toInt()

                        device?.let { trySend(BluetoothDeviceWrapper(it, rssi)) }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Timber.i("DISCOVERY STARTED")
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Timber.i("DISCOVERY FINISHED")
                    }
                }
            }
        }

        context.registerReceiver(receiver, filter)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Timber.e("Receiver was not registered or already unregistered $e")
            }
        }
    }.flowOn(Dispatchers.IO)

    fun collectConnectionState() = channelFlow {
        val filter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val status = it.getIntExtra(
                        BluetoothAdapter.EXTRA_CONNECTION_STATE,
                        BluetoothAdapter.STATE_DISCONNECTED
                    )
                    val previousStatus = it.getIntExtra(
                        BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE,
                        BluetoothAdapter.STATE_DISCONNECTED
                    )
                    val device = intent.getBluetoothDeviceExtra()
                    trySend(ConnectionState(status, previousStatus, device))
                }
            }
        }

        context.registerReceiver(receiver, filter)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Timber.e("Receiver was not registered or already unregistered $e")
            }
        }
    }.flowOn(Dispatchers.IO)


}
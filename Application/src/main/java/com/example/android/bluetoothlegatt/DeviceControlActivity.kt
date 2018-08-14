/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import org.jetbrains.anko.doAsync
import java.util.*

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with `BluetoothLeService`, which in turn interacts with the
 * Bluetooth LE API.
 */
class DeviceControlActivity : Activity() {
    private var mConnectionState: TextView? = null
    private var mPendingField: TextView? = null
    private var mFaceField: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mConnected = false

    private var pending = -1
    private var pendingTime = System.currentTimeMillis()
    private var face = -1

    private val toggl = Toggl()

    companion object {
        private val TAG = DeviceControlActivity::class.java!!.simpleName
        private const val CHANGE_TIME = 3000

        @JvmField var EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        @JvmField var EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }

        private val faces: Array<Vector> = arrayOf(
                Vector(74, 16, 443),
                Vector(-105, -418, 196),
                Vector(397, -273, 139),
                Vector(415, 258, 106),
                Vector(-81, 441, 143),
                Vector(-402, 25, 200),
                Vector(-22, -24, -565),
                Vector(161, 404, -318),
                Vector(450, -44, -318),
                Vector(121, -454, -262),
                Vector(-369, -266, -230),
                Vector(-348, 263, -263)
        )
        private val scales: Array<Vector> = arrayOf(
                Vector(-505, -521, -620),
                Vector(549, 506, 476)
        )
        private val scaledFaces = faces.map { v-> v.scale(scales[0], scales[1]) }.toTypedArray()
        private val quaternions = scaledFaces.map { v-> Quaternion.fromYawPitchRoll(v.scale(Math.PI / 180.0)) }.toTypedArray()
    }

    // Code to manage Service lifecycle.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
                updateConnectionState(R.string.connected)
                invalidateOptionsMenu()
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                updateConnectionState(R.string.disconnected)
                invalidateOptionsMenu()
                clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                val gattService = mBluetoothLeService!!.gattService(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
                val characteristic = gattService!!.getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"))
                mBluetoothLeService!!.setCharacteristicNotification(characteristic, true)
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                handleData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }

    private fun clearUI() {
        mPendingField!!.setText(R.string.no_data)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gatt_services_characteristics)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        findViewById<TextView>(R.id.device_address).text = mDeviceAddress
        mConnectionState = findViewById(R.id.connection_state)
        mPendingField = findViewById(R.id.pending_value)
        mFaceField = findViewById(R.id.face_value)

        //println(mPendingField)

        actionBar!!.title = mDeviceName
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.d(TAG, "Connect request result=" + result)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (mConnected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                mBluetoothLeService!!.connect(mDeviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                mBluetoothLeService!!.disconnect()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState!!.setText(resourceId) }
    }

    private fun handleData(data: ByteArray?) {
        // reporting frequency is approximately 7 hz

        if (data == null || data.size != 6) {
            return
        }

        val scaled = Vector(
                shortBytesToDouble(data[0], data[1]) * 1000.0,
                shortBytesToDouble(data[2], data[3]) * 1000.0,
                shortBytesToDouble(data[4], data[5]) * 1000.0
        ).scale(scales[0], scales[1])
        val quaternion = Quaternion.fromYawPitchRoll(scaled.scale(Math.PI / 180.0))
        var closest = -1
        var minDistance = Double.MAX_VALUE
        quaternions.forEachIndexed {i, q->
            val distance = q.distance(quaternion)
            if (distance < minDistance) {
                minDistance = distance
                closest = i
            }
        }

        if (pending != closest) {
            pending = closest
            pendingTime = System.currentTimeMillis()
            mPendingField!!.text = closest.toString()
        } else if (face != pending && System.currentTimeMillis() - pendingTime > CHANGE_TIME) {
            face = pending
            mFaceField!!.text = face.toString()
            doAsync {
                toggl.start("Project $face")
            }
        }
    }

    private fun shortBytesToDouble(b1: Byte, b2: Byte): Double {
        return (((b1.toInt() and 255) shl 8) or (b2.toInt() and 255)).toShort() / Short.MAX_VALUE.toDouble()
    }
}

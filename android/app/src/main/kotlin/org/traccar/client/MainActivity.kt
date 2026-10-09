package org.traccar.client

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "org.traccar.client/device"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "getDeviceId") {
                result.success(getDeviceIdentifier())
            } else {
                result.notImplemented()
            }
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun getDeviceIdentifier(): String {
        // 1. Try Hardware Serial Number (Build.getSerial)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val serial = Build.getSerial()
                if (!serial.isNullOrEmpty() && serial != Build.UNKNOWN) {
                    return serial
                }
            } else {
                @Suppress("DEPRECATION")
                val serial = Build.SERIAL
                if (!serial.isNullOrEmpty() && serial != Build.UNKNOWN) {
                    return serial
                }
            }
        } catch (_: SecurityException) {} catch (_: Exception) {}

        // 2. Try Headwind MDM Content Provider (serial, device_number, etc.)
        try {
            val hmdmUris = arrayOf(
                "content://com.hmdm.launcher.provider/serial",
                "content://com.hmdm.launcher.provider/device_number",
                "content://com.hmdm.launcher.provider/device_id",
                "content://com.hmdm.launcher.provider/imei"
            )
            for (uriStr in hmdmUris) {
                try {
                    contentResolver.query(Uri.parse(uriStr), null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getString(0)
                            if (!id.isNullOrEmpty()) return id
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // 3. Try TelephonyManager for IMEI or Device ID
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val imei = telephonyManager.imei
                        if (!imei.isNullOrEmpty()) return imei
                    } catch (_: SecurityException) {}
                }
                try {
                    @Suppress("DEPRECATION")
                    val devId = telephonyManager.deviceId
                    if (!devId.isNullOrEmpty()) return devId
                } catch (_: SecurityException) {}
            }
        } catch (_: Exception) {}

        // 4. Fallback to Settings.Secure.ANDROID_ID
        try {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrEmpty()) {
                return androidId
            }
        } catch (_: Exception) {}

        return ""
    }
}

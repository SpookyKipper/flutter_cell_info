package com.spookysrv.imsinfo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodChannel

/**
 * Centralized IMS voice network type channel setup.
 * Keeps implementation isolated under `com.spookysrv.imsinfo`.
 */
object ImsInfoChannel {
    private const val CHANNEL_NAME = "com.spookysrv.imsinfo/telephony"

    private var channel: MethodChannel? = null
    private var appContext: Context? = null

    fun attach(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "getVoiceNetworkType" -> {
                    val ctx = appContext
                    if (ctx == null) {
                        result.error("NO_CONTEXT", "Context not available", null)
                        return@setMethodCallHandler
                    }

                    val hasPermission = ContextCompat.checkSelfPermission(
                        ctx,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        result.success("PERMISSION_DENIED")
                        return@setMethodCallHandler
                    }

                    val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        telephonyManager.voiceNetworkType
                    } else {
                        telephonyManager.networkType
                    }

                    // Also read current data network type to complement voice state on devices
                    // that report IWLAN for data but do not propagate it to voice (e.g. Samsung).
                    val dataType = telephonyManager.dataNetworkType

                    var voiceType = when (networkType) {
                        TelephonyManager.NETWORK_TYPE_LTE -> "VoLTE"
                        TelephonyManager.NETWORK_TYPE_NR -> "VoNR"
                        TelephonyManager.NETWORK_TYPE_IWLAN -> "VoWiFi"
                        TelephonyManager.NETWORK_TYPE_GSM,
                        TelephonyManager.NETWORK_TYPE_GPRS,
                        TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                        TelephonyManager.NETWORK_TYPE_UMTS,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                        else -> "Unknown/Other"
                    }

                    // Fallback: if data is IWLAN, treat voice as VoWiFi.
                    // Some Samsung devices set Data Network Type to IWLAN when VoWiFi is active but Voice at 4G/5G
                    if (dataType == TelephonyManager.NETWORK_TYPE_IWLAN) {
                        voiceType = "VoWiFi"
                    }

                    result.success(voiceType)
                }
                else -> result.notImplemented()
            }
        }
    }

    fun detach() {
        channel?.setMethodCallHandler(null)
        channel = null
        appContext = null
    }
}

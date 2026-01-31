package com.spookysrv.celldetect

import android.content.Context
import android.telephony.TelephonyManager
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.plugins.FlutterPlugin
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat



/**
 * rrc
 * Keeps implementation isolated under `com.spookysrv.celldetect`.
 */
object RrcInfoChannel {
    private const val CHANNEL_NAME = "com.spookysrv.celldetect/rrcinfo"

    private var channel: MethodChannel? = null
    private var appContext: Context? = null

    fun attach(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "getRrcStatus" -> {
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
                    val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
                    // Note: This requires READ_PHONE_STATE permission in Manifest
                    // and runtime permission request in Flutter.
                    val state = tm.dataState
                    
                    var rrcStatus = when (state) {
                        TelephonyManager.DATA_DISCONNECTED -> "RRC_IDLE_OR_DISCONNECTED"
                        TelephonyManager.DATA_CONNECTING -> "RRC_CONNECTING"
                        TelephonyManager.DATA_CONNECTED -> "RRC_CONNECTED"
                        TelephonyManager.DATA_SUSPENDED -> "SUSPENDED"
                        else -> "UNKNOWN"
                    }

                    result.success(rrcStatus)
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

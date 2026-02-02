package com.spookysrv.celldetect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodChannel



/**
 * Centralized IMS voice network type channel setup.
 * Keeps implementation isolated under `com.spookysrv.celldetect`.
 */
object ServiceStateChannel {
    private const val CHANNEL_NAME = "com.spookysrv.celldetect/serviceState"

    private var channel: MethodChannel? = null
    private var appContext: Context? = null

    fun attach(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "getServiceState" -> {

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
                    // Logic: Fetch ServiceState Async -> Parse String -> Return Boolean
                    
                    // Helper to parse the state once received
                    fun parseAndReply(serviceState: ServiceState) {
                        val ssString = serviceState.toString()
                      
                        try {
                            result.success(ssString)
                        } catch (e: Exception) {
                            // Handle edge case where result might be replied to already
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // --- Android 12+ (API 31+) ---
                        try {
                            val executor = ctx.mainExecutor
                            telephonyManager.registerTelephonyCallback(executor, object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                                override fun onServiceStateChanged(serviceState: ServiceState) {
                                    // Stop listening immediately to save battery
                                    telephonyManager.unregisterTelephonyCallback(this)
                                    parseAndReply(serviceState)
                                }
                            })
                        } catch (e: Exception) {
                            // Fallback if permission/API fails
                            result.error("API_ERROR", e.message, null)
                        }
                    } else {
                        // --- Android 11 and Below ---
                        val listener = object : PhoneStateListener() {
                            override fun onServiceStateChanged(serviceState: ServiceState) {
                                // Stop listening immediately
                                telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE)
                                parseAndReply(serviceState)
                            }
                        }
                        telephonyManager.listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE)
                    }
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

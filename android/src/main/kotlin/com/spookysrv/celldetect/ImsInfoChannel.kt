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
object ImsInfoChannel {
    private const val CHANNEL_NAME = "com.spookysrv.celldetect/telephony"

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
                "isImsRegistered" -> {

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
                        
                        // Check for explicit SUCCESS flags
                        val isRegistered = ssString.contains("imsRegState=1") ||       // Samsung/Qualcomm
                                           ssString.contains("mIsImsRegistered=true")  // Pixel/AOSP

                        // Note: You can add the "Veto" logic here if you want to be extra strict
                        // e.g. if (ssString.contains("imsRegState=0")) isRegistered = false

                        // Send result to Flutter
                        // Ensure this runs on the main thread (MethodChannel requirement)
                        // Telephony callbacks usually run on the main thread/looper provided.
                        try {
                            result.success(isRegistered)
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

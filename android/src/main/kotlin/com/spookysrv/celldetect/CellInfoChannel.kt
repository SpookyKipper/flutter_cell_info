package com.spookysrv.celldetect

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.telephony.PhysicalChannelConfig
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.plugins.FlutterPlugin
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.telephony.PhoneStateListener


/**
 * rrc
 * Keeps implementation isolated under `com.spookysrv.celldetect`.
 */
object CellInfoChannel {
    private const val CHANNEL_NAME = "com.spookysrv.celldetect/cellinfo"

    private var channel: MethodChannel? = null
    private var appContext: Context? = null

    // 1. Variable to cache the latest value
    private var latestOverrideType: Int = 0 
    
    // 2. Track if we are currently listening to avoid duplicate registrations
    private var isListening = false
    
    // 3. Hold references to listeners to prevent garbage collection issues
    private var telephonyCallback: Any? = null

    fun attach(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "getOverrideNetworkType" -> {
                    val context = appContext
                    if (context == null) {
                        result.error("NO_CONTEXT", "Context is null", null)
                        return@setMethodCallHandler
                    }

                    // Permission Check
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                         result.error("PERMISSION_DENIED", "READ_PHONE_STATE permission denied", null)
                         return@setMethodCallHandler
                    }
                    
                    startListening(binding)
                    result.success(getOverrideTypeName(latestOverrideType))
                }
                else -> result.notImplemented()
            }
        }
    }

    fun detach() {
        channel?.setMethodCallHandler(null)
        channel = null
        cleanup(appContext)
        appContext = null
    }


    // Helper to convert Int -> String
    private fun getOverrideTypeName(type: Int): String {
        return when (type) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE -> "NONE"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "LTE_CA"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "LTE_ADVANCED_PRO"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "NR_ADVANCED"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "NR_NSA"
            // API 31+ constant, check for existence or use raw value 5
            5 -> "NR_ADVANCED" 
            else -> "UNKNOWN"
        }
    }

    private fun startListening(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
val telephonyManager = appContext!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        // val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Permission Check
        val hasPermission = ContextCompat.checkSelfPermission(
                        appContext!!,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        return
                    }

        // Avoid registering twice
        if (isListening) return 

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ (Android 12)
            val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                    latestOverrideType = telephonyDisplayInfo.overrideNetworkType
                }
            }
            val mainExecutor = appContext!!.mainExecutor
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            telephonyCallback = callback
            isListening = true
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            // // API 30 (Android 11)
            // val listener = object : PhoneStateListener() {
            //         var latestOverrideType = TelephonyDisplayInfo..
            //     }
            // }
            // telephonyManager.listen(listener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
            // telephonyCallback = listener
            // isListening = true
            // lastOverrideType = 0 // No override info available (ANDROID 12+ only)
        }
    }

    fun cleanup(appContext: Context?) {
        // super.onDestroy()
        // Cleanup when the app closes
        val telephonyManager = appContext!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            (telephonyCallback as? PhoneStateListener)?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }



}
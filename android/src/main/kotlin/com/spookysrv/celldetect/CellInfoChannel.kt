// package com.spookysrv.celldetect

// import android.content.Context
// import android.os.Build
// import android.telephony.TelephonyCallback
// import android.telephony.TelephonyDisplayInfo
// import android.telephony.TelephonyManager
// import android.telephony.PhysicalChannelConfig
// import androidx.annotation.RequiresApi
// import io.flutter.embedding.android.FlutterActivity
// import io.flutter.embedding.engine.FlutterEngine
// import io.flutter.plugin.common.MethodChannel
// import io.flutter.embedding.engine.plugins.FlutterPlugin
// import androidx.core.content.ContextCompat
// import android.Manifest
// import android.content.pm.PackageManager

// /**
//  * rrc
//  * Keeps implementation isolated under `com.spookysrv.celldetect`.
//  */
// object CellInfoChannel {
//     private const val CHANNEL_NAME = "com.spookysrv.celldetect/cellinfo"

//     private var channel: MethodChannel? = null
//     private var appContext: Context? = null

//     fun attach(binding: FlutterPlugin.FlutterPluginBinding) {
//         appContext = binding.applicationContext
//         channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
//         channel?.setMethodCallHandler { call, result ->
//             when (call.method) {
//                 "getDetailedNetworkType" -> {
//                     val ctx = appContext
//                                         val telephonyManager = ctx!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

//                     try {
//         // 1. Create a variable to track if we've already replied (to prevent double replies)
//         var hasReplied = false

//         // 2. Create the callback that implements BOTH listeners
//         val callback = object : TelephonyCallback(), 
//             TelephonyCallback.PhysicalChannelConfigListener, 
//             TelephonyCallback.DisplayInfoListener {
            
//             // Store the display info when it arrives
//             var currentDisplayInfo: TelephonyDisplayInfo? = null

//             override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
//                 currentDisplayInfo = telephonyDisplayInfo
//                 // We don't reply here, we wait for the physical config to trigger the logic
//             }

//             override fun onPhysicalChannelConfigChanged(configs: List<PhysicalChannelConfig>) {
//                 if (hasReplied) return // Safety check

//                 // --- STEP 1: Count Physical Resources ---
//                 var lteCcCount = 0
//                 var nrCcCount = 0

//                 for (config in configs) {
//                     when (config.networkType) {
//                         TelephonyManager.NETWORK_TYPE_LTE -> lteCcCount++
//                         TelephonyManager.NETWORK_TYPE_NR -> nrCcCount++
//                     }
//                 }

//                 // --- STEP 2: Use the cached DisplayInfo (or 0 if not arrived yet) ---
//                 // Note: Android usually sends onDisplayInfoChanged immediately before this, 
//                 // but if it's null, we assume no override.
//                 val override = currentDisplayInfo?.overrideNetworkType ?: 0
                
//                 val isNrAvailable = override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
//                                     override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED

//                 // --- STEP 3: Get Base Radio Technology ---
//                                     val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

//                 val baseNetworkType = telephonyManager?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                
//                 // --- STEP 4: Decision Tree ---
//                 val status = when {
//                     // === 5G SA ===
//                     baseNetworkType == TelephonyManager.NETWORK_TYPE_NR || (nrCcCount > 0 && lteCcCount == 0) -> {
//                         if (nrCcCount > 1) "NR-SA-CA" else "NR-SA"
//                     }

//                     // === 5G NSA ===
//                     nrCcCount > 0 && lteCcCount > 0 -> {
//                         if (lteCcCount > 1 || nrCcCount > 1) "NR-NSA-CA" else "NR-NSA"
//                     }

//                     // === LTE / LTE-A ===
//                     baseNetworkType == TelephonyManager.NETWORK_TYPE_LTE || lteCcCount > 0 -> {
//                         if (isNrAvailable) {
//                             if (lteCcCount > 1) "LTE-A-NRANCHOR" else "LTE-NRANCHOR"
//                         } else {
//                             if (lteCcCount > 1) "LTE-A" else "LTE"
//                         }
//                     }

//                     // === Legacy ===
//                     baseNetworkType == TelephonyManager.NETWORK_TYPE_HSPAP ||
//                     baseNetworkType == TelephonyManager.NETWORK_TYPE_HSPA ||
//                     baseNetworkType == TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                    
//                     baseNetworkType == TelephonyManager.NETWORK_TYPE_EDGE ||
//                     baseNetworkType == TelephonyManager.NETWORK_TYPE_GPRS -> "2G"

//                     else -> "Unknown"
//                 }

//                 // Send Result and Unregister
//                 hasReplied = true
//                 result.success(status)
//                 telephonyManager?.unregisterTelephonyCallback(this)
//             }
//         }

//         val mainExecutor = ctx.mainExecutor
//         // 3. Register the callback
//         telephonyManager?.registerTelephonyCallback(mainExecutor, callback)

//     } catch (e: SecurityException) {
//         result.error("PERMISSION", e.message, null)
//     } catch (e: Exception) {
//         result.error("ERROR", e.message, null)
//     }
//                 }

//                 // ---------------------------------------------------------
//                 // METHOD 2: Get Raw CA Combos (Visible on Qualcomm)
//                 // ---------------------------------------------------------
//                 // "getPhysicalCells" -> {
//                 //     netMonster.getPhysicalChannelConfiguration(subId) { configList ->
//                 //         val processedData = configList.map { config ->
//                 //             val statusStr = when(config.connectionStatus) {
//                 //                 PhysicalChannelConfig.ConnectionStatus.PRIMARY -> "Primary"
//                 //                 PhysicalChannelConfig.ConnectionStatus.SECONDARY -> "Secondary"
//                 //                 else -> "Unknown"
//                 //             }
                            
//                 //             mapOf(
//                 //                 "band" to (config.band?.name ?: "Unknown"), // e.g., "2100", "700"
//                 //                 "number" to (config.band?.number ?: 0),     // e.g., 1, 3, 78
//                 //                 "bandwidth" to config.bandwidth,            // kHz
//                 //                 "pci" to (config.pci ?: -1),
//                 //                 "status" to statusStr,
//                 //                 "type" to if (config.band is BandNr) "NR" else "LTE"
//                 //             )
//                 //         }
//                 //          result.success(processedData) 
//                 //     }
//                 // }
                
//                 else -> result.notImplemented()
//             }
//         }
//     }

//     fun detach() {
//         channel?.setMethodCallHandler(null)
//         channel = null
//         appContext = null
//     }
// }

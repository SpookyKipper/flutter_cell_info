import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class ImsService {
  // Ensure this string matches the CHANNEL in your MainActivity.kt
  static const MethodChannel _channel = MethodChannel('com.spookysrv.celldetect/telephony');

  /// Returns: 'VoLTE', 'VoNR', 'VoWiFi', '2G', '3G', or 'Unknown'
  /// Throws: PlatformException if the native call fails.
  static Future<String> getNetworkType() async {
    // 1. Check/Request Permissions explicitly before calling native code
    final status = await Permission.phone.status;
    if (!status.isGranted) {
      final result = await Permission.phone.request();
      if (!result.isGranted) {
        return "PERMISSION_DENIED";
      }
    }

    try {
      // 2. Invoke the native Android method
      final String networkType = await _channel.invokeMethod('getVoiceNetworkType');
      return networkType;
    } on PlatformException catch (e) {
      // Log error or handle specific native exceptions
      print("Error fetching IMS status: ${e.message}");
      return "ERROR";
    }
  }
}
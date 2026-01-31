import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class RrcService {
  // Ensure this string matches the CHANNEL in your MainActivity.kt
  static const MethodChannel _channel =
      MethodChannel('com.spookysrv.celldetect/rrcinfo');

  static Future<String> getRrcStatus() async {
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
      final String rrcStatus = 
          await _channel.invokeMethod('getRrcStatus');
      return rrcStatus;
    } on PlatformException catch (e) {
      // Log error or handle specific native exceptions
      print("Error fetching RRC status: ${e.message}");
      return "ERROR";
    }
  }
}

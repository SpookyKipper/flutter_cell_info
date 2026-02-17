import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class CellService {
  // Ensure this string matches the CHANNEL in your MainActivity.kt
  static const MethodChannel _channel =
      MethodChannel('com.spookysrv.celldetect/cellinfo');

  static Future<String> getDetailedNetworkType() async {
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
      final String networkType =
          await _channel.invokeMethod('getDetailedNetworkType');
      return networkType;
    } on PlatformException catch (e) {
      // Log error or handle specific native exceptions
      print("Error fetching NetworkType: ${e.message}");
      return "ERROR";
    }
  }

  static Future<List<Map<String, dynamic>>> getCaCombos() async {
    try {
      final List<dynamic> result =
          await _channel.invokeMethod('getPhysicalCells');

      // Cast standard List<dynamic> to List<Map<String, dynamic>>
      return result.map((e) => Map<String, dynamic>.from(e)).toList();
    } on PlatformException catch (e) {
      print("CA Error: ${e.message}");
      return [];
    }
  }

  static Future<String> getOverrideNetworkType() async {
    try {
      final String result =
          await _channel.invokeMethod('getOverrideNetworkType');
      return result;
    } on PlatformException catch (e) {
      print("CA Error: ${e.message}");
      return "ERROR";}
  }
}

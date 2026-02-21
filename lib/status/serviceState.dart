import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class ServiceStateService {
  // Ensure this string matchBwes the CHANNEL in your MainActivity.kt
  static const MethodChannel _channel =
      MethodChannel('com.spookysrv.celldetect/serviceState');

  static Future<bool> searchServiceState(String search) async {
    // 1. Check/Request Permissions explicitly before calling native code
    final status = await Permission.phone.status;
    if (!status.isGranted) {
      final result = await Permission.phone.request();
      if (!result.isGranted) {
        return Future.error("PERMISSION_DENIED");
      }
    }

    try {
      // 2. Invoke the native Android method
      final String serviceState =
          await _channel.invokeMethod('getServiceState');
      if (serviceState.contains(search)) {
        return true;
      } else {
        return false;
      }
    } on PlatformException catch (e) {
      // Log error or handle specific native exceptions
      print("Error fetching RRC status: ${e.message}");
      return Future.error("ERROR");
    }
  }

  static Future<List<double>> getBandwidths() async {
    // included in all
    // 1. Check/Request Permissions explicitly before calling native code
    final status = await Permission.phone.status;
    if (!status.isGranted) {
      final result = await Permission.phone.request();
      if (!result.isGranted) {
        return Future.error("PERMISSION_DENIED");
      }
    }

    try {
      // 2. Invoke the native Android method
      final String serviceState =
          await _channel.invokeMethod('getServiceState');
      RegExp regExpBw = RegExp(r"mCellBandwidths=\[(.*?)\]");
      Match? matchBw = regExpBw.firstMatch(serviceState);

      List<double> bandwidths = [];

      if (matchBw != null) {
        // 提取括號內的字串 (例如 "20000, 20000, 50000")
        String? content = matchBw.group(1);

        if (content != null && content.isNotEmpty) {
          // 3. 以逗號分割並轉換為整數列表
          bandwidths = content
              .split(',')
              .map((e) => int.parse(e.trim()) / 1000)
              .toList();
        }
      }

      return bandwidths;
    } on PlatformException catch (e) {
      // Log error or handle specific native exceptions
      print("Error fetching RRC status: ${e.message}");
      return Future.error("ERROR");
    }
  }

  static _getSerivceStateVal(String key, String serviceState) {
    RegExp regExp = RegExp("$key=(.*?)[,}]");
    Match? match = regExp.firstMatch(serviceState);
    return match?.group(1)?.trim() ?? "Unknown";
  }

  static Future<Map> getAllInfoFromSerivceState() async {
    // 1. Check/Request Permissions explicitly before calling native code
    final status = await Permission.phone.status;
    if (!status.isGranted) {
      final result = await Permission.phone.request();
      if (!result.isGranted) {
        return Future.error("PERMISSION_DENIED");
      }
    }

    try {
      // 2. Invoke the native Android method
      final String serviceState =
          await _channel.invokeMethod('getServiceState');
      RegExp regExpBw = RegExp(r"mCellBandwidths=\[(.*?)\]");
      Match? matchBw = regExpBw.firstMatch(serviceState);

      List<double> bandwidths = [];

      if (matchBw != null) {
        // 提取括號內的字串 (例如 "20000, 20000, 50000")
        String? content = matchBw.group(1);

        if (content != null && content.isNotEmpty) {
          // 3. 以逗號分割並轉換為整數列表
          bandwidths = content
              .split(',')
              .map((e) => int.parse(e.trim()) / 1000)
              .toList();
        }
      }

      bool usingCa = serviceState.contains("isUsingCarrierAggregation=true");
      bool nrAvailable = serviceState.contains("isNrAvailable = true");
      bool endcAvailable = serviceState.contains("isEnDcAvailable = true");
      bool imsRegistered = serviceState.contains("imsRegState=1") ||
          serviceState.contains("mIsImsRegistered=true") ||
          serviceState.contains("mVoiceRegState=0(IN_SERVICE)");

      String carrierName =
          _getSerivceStateVal("mOperatorAlphaLongRaw", serviceState);
      String mvnoName = _getSerivceStateVal("mOperatorAlphaLong", serviceState);
      if (mvnoName == carrierName) {
        mvnoName = "";
      }

      String dataRadioTech =
          _getSerivceStateVal("getRilDataRadioTechnology", serviceState);
      String voiceRadioTech =
          _getSerivceStateVal("getRilVoiceRadioTechnology", serviceState);

      String voiceTechnology = "Unknown";
      if ((dataRadioTech == "18(IWLAN)" &&
              (voiceRadioTech == "20(NR_SA)" || voiceRadioTech == "14(LTE)")) ||
          voiceRadioTech == "18(IWLAN)") {
        voiceTechnology = "VoWiFi";
      } else if (voiceRadioTech == "20(NR_SA)") {
        voiceTechnology = "VoNR";
        if (imsRegistered == false) {
          voiceTechnology == "No Voice or VoLTE or CSFB";
        }
      } else if (voiceRadioTech == "14(LTE)") {
        voiceTechnology = "VoLTE";
        if (imsRegistered == false) {
          voiceTechnology == "No Voice or CSFB";
        }
      } else if (voiceRadioTech != "Unknown") {
        voiceTechnology = voiceRadioTech;
      }

      return {
        "bandwidths": bandwidths,
        "usingCa": usingCa,
        "lteAnchor": nrAvailable,
        "nrNsa": endcAvailable &&
            nrAvailable, // connected to 5G in NSA mode (EN-DC) requires NR available and EN-DC available
        "imsRegistered": imsRegistered,
        "carrierName": carrierName,
        "mvnoName": mvnoName,
        "voiceTechnology": voiceTechnology,
      };
    } on PlatformException catch (e) {
      // Log error or handle specific native exceptions
      print("Error fetching RRC status: ${e.message}");
      return Future.error("ERROR");
    }
  }
}

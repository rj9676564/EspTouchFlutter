import 'dart:async';

import 'package:flutter/services.dart';

class EsptouchConnectResult {
  const EsptouchConnectResult({
    required this.success,
    required this.cancel,
  });

  factory EsptouchConnectResult.fromMap(Map<String, dynamic>? map) {
    return EsptouchConnectResult(
      success: map?['success'] == true,
      cancel: map?['cancel'] == true,
    );
  }

  final bool success;
  final bool cancel;

  Map<String, bool> toMap() {
    return <String, bool>{
      'success': success,
      'cancel': cancel,
    };
  }
}

class EsptouchFlutter {
  static const MethodChannel _channel = MethodChannel('esptouch_flutter');

  static Future<EsptouchConnectResult> connectWifi(
    String mSsid,
    String mBssid,
    String pwd, {
    String devCount = "1",
    bool modelGroup = false,
  }) async {
    final Map<String, dynamic>? result =
        await _channel.invokeMapMethod<String, dynamic>('connectWifi', {
      'mSsid': mSsid,
      'mBssid': mBssid,
      'pwd': pwd,
      'devCount': devCount,
      'modelGroup': modelGroup,
    });
    return EsptouchConnectResult.fromMap(result);
  }

  static Future<Map<String, bool>> connectWifiMap(
    String mSsid,
    String mBssid,
    String pwd, {
    String devCount = "1",
    bool modelGroup = false,
  }) async {
    final EsptouchConnectResult result = await connectWifi(
      mSsid,
      mBssid,
      pwd,
      devCount: devCount,
      modelGroup: modelGroup,
    );
    return result.toMap();
  }

  static Future<bool> cancelConfig() async {
    final bool? version = await _channel.invokeMethod<bool>('cancelConnect');
    return version == true;
  }

  static Future<Map<String, dynamic>?> get wifiInfo async {
    final Map<String, dynamic>? version =
        await _channel.invokeMapMethod<String, dynamic>('getWifiInfo');
    return version;
  }
}

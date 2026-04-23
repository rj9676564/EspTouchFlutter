import 'package:flutter_esptouch/esptouch_flutter.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const MethodChannel channel = MethodChannel('esptouch_flutter');
  final binding = TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      MethodCall methodCall,
    ) async {
      return <String, dynamic>{'success': true, 'cancel': false};
    });
  });

  tearDown(() {
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('connectWifi', () async {
    final EsptouchConnectResult result = await EsptouchFlutter.connectWifi(
      "",
      "",
      "",
      devCount: "",
      modelGroup: true,
    );

    expect(result.success, isTrue);
    expect(result.cancel, isFalse);
    expect(result.toMap(), <String, bool>{'success': true, 'cancel': false});
  });

  test('connectWifiMap', () async {
    final Map<String, bool> result = await EsptouchFlutter.connectWifiMap(
      "",
      "",
      "",
      devCount: "",
      modelGroup: true,
    );

    expect(result, <String, bool>{'success': true, 'cancel': false});
  });
}

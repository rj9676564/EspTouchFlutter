#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint esptouch_flutter.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'flutter_esptouch'
  s.version          = '0.0.4'
  s.summary          = 'flutter_esptouch'
  s.description      = <<-DESC
esptouch_flutter
                       DESC
  s.homepage         = 'https://github.com/MrLaibin/EspTouchFlutter'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'none' => '568334413@qq.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.platform = :ios, '13.0'
  s.ios.deployment_target = '13.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
end

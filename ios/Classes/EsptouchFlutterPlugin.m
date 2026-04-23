#import "EsptouchFlutterPlugin.h"
#import "ESPTools.h"
#import "ESPTouchTask.h"
#import "ESPTouchResult.h"
#import "ESP_NetUtil.h"
#import "ESPTouchDelegate.h"
#import "ESPAES.h"
#import <CoreLocation/CoreLocation.h>
@implementation EsptouchFlutterPlugin{
    CLLocationManager *_locationManagerSystem;
}
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"esptouch_flutter"
            binaryMessenger:[registrar messenger]];
  EsptouchFlutterPlugin* instance = [[EsptouchFlutterPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
  
}
-(instancetype)init{
    self = [super init];
    if (self) {
        self._condition = [[NSCondition alloc]init];
        self.esptouchQueue = dispatch_queue_create("com.juhesaas.esptouch_flutter.connect", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}
- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if([@"getWifiInfo" isEqualToString:call.method]){
      _locationManagerSystem = [[CLLocationManager alloc]init];
      
      [_locationManagerSystem requestWhenInUseAuthorization];
      NSDictionary *wifiDic = [self fetchNetInfo];
      result(wifiDic);
  } else if([@"cancelConnect" isEqualToString:call.method]){
      if(self._esptouchTask!=nil){
          [self._esptouchTask interrupt];
      }
      result(@(YES));
  } else if([@"connectWifi" isEqualToString:call.method]){
      NSDictionary *dic = call.arguments;
      NSString* mSsid = dic[@"mSsid"];
      NSString* pwd = dic[@"pwd"];
      NSString* mBssid = dic[@"mBssid"];
      NSString* devCountStr = dic[@"devCount"];
      if(devCountStr==nil){
          devCountStr=@"1";
      }
      BOOL modeGroup = [dic[@"modelGroup"] boolValue];

      // 配网任务会同步等待 UDP 结果，放到后台串行队列避免阻塞 Flutter 主线程。
      dispatch_async(self.esptouchQueue, ^{
          NSArray* results = [self executeForResultsWithSsid:mSsid bssid:mBssid password:pwd taskCount:[devCountStr intValue] broadcast:modeGroup];
          ESPTouchResult *espResult = results.count > 0 ? results[0] : nil;
          NSDictionary *dic2 = @{
              @"success": @(espResult != nil && espResult.isSuc),
              @"cancel": @(espResult != nil && espResult.isCancelled)
          };
          dispatch_async(dispatch_get_main_queue(), ^{
              result(dic2);
          });
      });
  } else {
    result(FlutterMethodNotImplemented);
  }
}

- (NSDictionary *)fetchNetInfo
{
    NSMutableDictionary *wifiDic = [NSMutableDictionary dictionaryWithCapacity:0];
    wifiDic[@"mSsid"] = ESPTools.getCurrentWiFiSsid;
    wifiDic[@"mBssid"] = ESPTools.getCurrentBSSID;
    return wifiDic;
}

#pragma mark - the example of how to use executeForResults
- (NSArray *) executeForResultsWithSsid:(NSString *)apSsid bssid:(NSString *)apBssid password:(NSString *)apPwd taskCount:(int)taskCount broadcast:(BOOL)broadcast
{
    [self._condition lock];
    self._esptouchTask = [[ESPTouchTask alloc]initWithApSsid:apSsid andApBssid:apBssid andApPwd:apPwd];
    // set delegate
    [self._esptouchTask setEsptouchDelegate:self];
    [self._esptouchTask setPackageBroadcast:broadcast];
    [self._condition unlock];
    NSArray * esptouchResults = [self._esptouchTask executeForResults:taskCount];
    NSLog(@"ESPViewController executeForResult() result is: %@",esptouchResults);
    return esptouchResults;
}
-(void) onEsptouchResultAddedWithResult: (ESPTouchResult *) result
{
    NSLog(@"EspTouchDelegateImpl onEsptouchResultAddedWithResult bssid: %@", result.bssid);
}
@end

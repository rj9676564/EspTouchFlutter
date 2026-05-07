#import "ESPAES.h"
#import "ESPTools.h"
#import "ESPTouchDelegate.h"
#import "ESPTouchResult.h"
#import "ESPTouchTask.h"
#import "ESP_NetUtil.h"
#import <Flutter/Flutter.h>
@interface EsptouchFlutterPlugin : NSObject <FlutterPlugin, ESPTouchDelegate>
@property(nonatomic, strong) NSDictionary *netInfo;
@property(nonatomic, strong) NSCondition *_condition;
@property(nonatomic, strong) dispatch_queue_t esptouchQueue;
// to cancel ESPTouchTask when
@property(atomic, strong) ESPTouchTask *_esptouchTask;
@end

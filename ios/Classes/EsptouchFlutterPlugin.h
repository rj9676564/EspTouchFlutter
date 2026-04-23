#import <Flutter/Flutter.h>
#import "ESPTools.h"
#import "ESPTouchTask.h"
#import "ESPTouchResult.h"
#import "ESP_NetUtil.h"
#import "ESPTouchDelegate.h"
#import "ESPAES.h"
@interface EsptouchFlutterPlugin : NSObject<FlutterPlugin,ESPTouchDelegate>
@property (nonatomic, strong)NSDictionary *netInfo;
@property (nonatomic, strong) NSCondition *_condition;
@property (nonatomic, strong) dispatch_queue_t esptouchQueue;
// to cancel ESPTouchTask when
@property (atomic, strong) ESPTouchTask *_esptouchTask;
@end

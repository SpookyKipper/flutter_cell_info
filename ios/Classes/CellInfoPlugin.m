#import "CellInfoPlugin.h"
#import "NetworkManager.h"

@implementation CellInfoPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"cell_info"
            binaryMessenger:[registrar messenger]];
  CellInfoPlugin* instance = [[CellInfoPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  } 
  else if ([@"cell_info" isEqualToString:call.method]) {
       [[NetworkManager sharedManager] requestDataWithCompletion:^(NSDictionary *data) {
            result(data);
        }];
  } 
  else if ([@"sim_info" isEqualToString:call.method]) {
        // [[NetworkManager sharedManager] simsInfoWithCompletion:^(NSDictionary *data) {
        //     result(data);
        // }];
  }
  else {
    result(FlutterMethodNotImplemented);
  }
}

@end

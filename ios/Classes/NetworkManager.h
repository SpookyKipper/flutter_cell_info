#import <Foundation/Foundation.h>
#import <CoreTelephony/CTTelephonyNetworkInfo.h>
#import <CoreTelephony/CTCarrier.h>

@interface NetworkManager : NSObject

+ (instancetype)sharedManager;
- (void)requestDataWithCompletion:(void (^)(NSDictionary *))completion;
- (void)simsInfoWithCompletion:(void (^)(NSDictionary *))completion;

@end

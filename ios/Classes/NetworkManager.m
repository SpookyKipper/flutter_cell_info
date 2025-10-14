#import "NetworkManager.h"

@interface NetworkManager ()
@property (nonatomic, strong) CTTelephonyNetworkInfo *networkInfo;
@end

@implementation NetworkManager

+ (instancetype)sharedManager {
    static NetworkManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[NetworkManager alloc] init];
        sharedInstance.networkInfo = [[CTTelephonyNetworkInfo alloc] init];
    });
    return sharedInstance;
}

- (void)requestDataWithCompletion:(void (^)(NSDictionary *))completion {
    NSDictionary<NSString *, NSString *> *radioAccessTechnologyDict = self.networkInfo.serviceCurrentRadioAccessTechnology;
    
    NSMutableDictionary *data = [NSMutableDictionary dictionary];
    
    for (NSString *key in radioAccessTechnologyDict) {
        NSString *technology = radioAccessTechnologyDict[key];
        if (technology) {
            [data setObject:technology forKey:key];
        }
    }
    
    if (completion) {
        completion([data copy]);
    }
}

- (void)simsInfoWithCompletion:(void (^)(NSDictionary *))completion {
    NSMutableDictionary *simInfoDict = [NSMutableDictionary dictionary];
    NSDictionary<NSString *, CTCarrier *> *carrierDict = self.networkInfo.serviceSubscriberCellularProviders;
    
    for (NSString *key in carrierDict) {
        CTCarrier *carrier = carrierDict[key];
        if (carrier) {
            NSDictionary *simInfo = @{
                @"carrierName": carrier.carrierName ?: @"Unknown",
                @"mcc": carrier.mobileCountryCode ?: @"0",
                @"mnc": carrier.mobileNetworkCode ?: @"0"
            };
            [simInfoDict setObject:simInfo forKey:key];
        }
    }
    
    if (completion) {
        completion([simInfoDict copy]);
    }
}

@end

// SIMInfo.h
#import <Foundation/Foundation.h>

@interface SIMInfo : NSObject

@property (nonatomic, strong) NSString *carrierName;
@property (nonatomic, strong) NSString *displayName;
@property (nonatomic, assign) NSInteger mcc;
@property (nonatomic, assign) NSInteger mnc;
@property (nonatomic, strong) NSString *number;

@end
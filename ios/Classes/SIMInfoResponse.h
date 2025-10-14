// SIMInfoResponse.h
#import <Foundation/Foundation.h>
#import "SIMInfo.h"

@interface SIMInfoResponse : NSObject

@property (nonatomic, strong) NSMutableArray<SIMInfo *> *simInfoList;

@end
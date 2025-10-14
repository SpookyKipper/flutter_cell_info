// CellType.h
#import <Foundation/Foundation.h>
#import "CellData.h"

@interface CellType : NSObject

@property (nonatomic, strong) NSString *type;
@property (nonatomic, strong) CellData *cellData;

@end
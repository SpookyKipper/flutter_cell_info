// CellsResponse.h
#import <Foundation/Foundation.h>
#import "CellType.h"

@interface CellsResponse : NSObject

@property (nonatomic, strong) NSMutableArray<CellType *> *primaryCellList;
@property (nonatomic, strong) NSMutableArray<CellType *> *neighboringCellList;
@property (nonatomic, strong) NSMutableArray<CellData *> *cellDataList;

@end
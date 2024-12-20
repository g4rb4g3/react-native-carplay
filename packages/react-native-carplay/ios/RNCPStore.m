#import "RNCPStore.h"

@implementation RNCPStore {
    NSMutableDictionary* _templatesStore;
    NSMutableDictionary* _navigationSessionsStore;
    NSMutableDictionary* _tripsStore;
}

-(instancetype)init {
    if (self = [super init]) {
        _templatesStore = [[NSMutableDictionary alloc] init];
        _navigationSessionsStore = [[NSMutableDictionary alloc] init];
        _tripsStore = [[NSMutableDictionary alloc] init];
        self.cluster = [[NSMutableDictionary alloc] init];
    }

    return self;
}

+ (RNCPStore*) sharedManager {
    static RNCPStore *shared = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        shared = [[self alloc] init];
    });
    return shared;
}

- (CPTemplate*) findTemplateById:(NSString*)templateId {
    return [_templatesStore objectForKey:templateId];
}

- (NSString*) setTemplate:(NSString*)templateId template:(CPTemplate*)carPlayTemplate {
    [_templatesStore setObject:carPlayTemplate forKey:templateId];
    return templateId;
}

- (CPTrip*) findTripById:(NSString*)tripId {
    return [_tripsStore objectForKey:tripId];
}

- (NSString*) setTrip:(NSString*)tripId trip:(CPTrip*)trip {
    [_tripsStore setObject:trip forKey:tripId];
    return tripId;
}

- (CPNavigationSession*) findNavigationSessionById:(NSString*)navigationSessionId {
    return [_navigationSessionsStore objectForKey:navigationSessionId];
}

- (NSString*) setNavigationSession:(NSString*)navigationSessionId navigationSession:(CPNavigationSession*)navigationSession {
    [_navigationSessionsStore setObject:navigationSession forKey:navigationSessionId];
    return navigationSessionId;
}

- (NSArray*) getTemplateIds {
    return [_templatesStore allKeys];
}

@end

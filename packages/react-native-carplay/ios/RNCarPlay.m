#import "RNCarPlay.h"
#import <React/RCTConvert.h>
#import <React/RCTRootView.h>
#import "react_native_carplay/react_native_carplay-Swift.h"

@implementation RNCarPlay
{
    bool hasListeners;
    NSMutableDictionary<NSNumber *, CPNavigationAlert *> *navigationAlertWrappers;
    NSDate *lastShowTripsTime;
    NSString *currentSearchText;
}

@synthesize searchResultBlock;
@synthesize selectedResultBlock;
@synthesize isNowPlayingActive;

- (instancetype)init {
    self = [super init];
    if (self) {
        navigationAlertWrappers = [[NSMutableDictionary alloc] init];
    }
    return self;
}

- (void)startObserving {
    hasListeners = YES;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                               selector:@selector(handleNotification:)
                                                 name:RNCarPlayUtils.RNCarPlaySendEventNotification
                                                 object:nil];
}

- (void)stopObserving {
    hasListeners = NO;
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)handleNotification:(NSNotification *)notification {
  NSString *name = notification.userInfo[@"name"];
  id body = notification.userInfo[@"body"];

  if (name && body) {
    [self sendEventWithName:name body:body];
  } else if (name) {
    [self sendEventWithName:name body:@{}];
  }
}

+ (NSDictionary *) getConnectedWindowInformation: (CPWindow *) window {
    return @{
        @"width": @(window.bounds.size.width),
        @"height": @(window.bounds.size.height),
        @"scale": @(window.screen.scale)
    };
}

+ (void) connectWithInterfaceController:(CPInterfaceController*)interfaceController window:(CPWindow*)window {
    RNCPStore * store = [RNCPStore sharedManager];
    if (store.app == nil) {
        store.app = [[RNCarPlayApp alloc] init];
    }
    [store.app connectSceneWithInterfaceController:interfaceController window:window];
}

+ (void) disconnect {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayApp* app = store.app;
    [app disconnect];
}

+ (void) stateChanged:(BOOL)isVisible {
    RNCPStore *store = [RNCPStore sharedManager];
    [store setVisibility:isVisible forScene:RNCarPlayConstants.SceneIdCarPlayApp];
    [RNCarPlayUtils sendRNCarPlayEventWithName:RNCarPlayConstants.EventStateDidChange body:@{@"isVisible": @(isVisible)}];
}

RCT_EXPORT_MODULE();

+ (id)allocWithZone:(NSZone *)zone {
    static RNCarPlay *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [super allocWithZone:zone];
    });
    return sharedInstance;
}

- (NSArray<NSString *> *)supportedEvents
{
    return @[
        @"didConnect",
        @"didDisconnect",
        RNCarPlayConstants.EventStateDidChange,
        @"didPressMenuItem",
        @"appearanceDidChange",
        @"safeAreaInsetsDidChange",
        // interface
        @"barButtonPressed",
        @"backButtonPressed",
        @"didAppear",
        @"didDisappear",
        @"willAppear",
        @"willDisappear",
        @"buttonPressed",
        @"poppedToRoot",
        // grid
        @"gridButtonPressed",
        // information
        @"actionButtonPressed",
        // list
        @"didSelectListItem",
        @"didSelectListItemRowImage",
        // search
        @"updatedSearchText",
        @"searchButtonPressed",
        @"selectedResult",
        // tabbar
        @"didSelectTemplate",
        // nowplaying
        @"upNextButtonPressed",
        @"albumArtistButtonPressed",
        // poi
        @"didSelectPointOfInterest",
        // map
        @"mapButtonPressed",
        @"didUpdatePanGestureWithTranslation",
        @"didEndPanGestureWithVelocity",
        @"panBeganWithDirection",
        @"panEndedWithDirection",
        @"panWithDirection",
        @"didBeginPanGesture",
        @"didDismissPanningInterface",
        @"willDismissPanningInterface",
        @"didShowPanningInterface",
        @"didDismissNavigationAlert",
        @"willDismissNavigationAlert",
        @"didShowNavigationAlert",
        @"willShowNavigationAlert",
        @"didCancelNavigation",
        @"alertActionPressed",
        @"selectedPreviewForTrip",
        @"startedTrip",
        //dashboard
        @"dashboardDidConnect",
        @"dashboardDidDisconnect",
        @"dashboardButtonPressed",
        RNCarPlayConstants.EventDashboardStateDidChange,
        //cluster
        @"clusterControllerDidConnect",
        @"clusterWindowDidConnect",
        @"clusterDidDisconnect",
        @"clusterDidChangeCompassSetting",
        @"clusterDidChangeSpeedLimitSetting",
        @"clusterDidZoomIn",
        @"clusterDidZoomOut",
        @"clusterContentStyleDidChange",
        RNCarPlayConstants.EventClusterStateDidChange
    ];
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}


-(UIImage *)imageWithTint:(UIImage *)image andTintColor:(UIColor *)tintColor {
    UIImage *imageNew = [image imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
    UIImageView *imageView = [[UIImageView alloc] initWithImage:imageNew];
    imageView.tintColor = tintColor;
    UIGraphicsBeginImageContextWithOptions(imageView.bounds.size, NO, 0.0);
    [imageView.layer renderInContext:UIGraphicsGetCurrentContext()];
    UIImage *tintedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return tintedImage;
}

-(UIImage*)dynamicImageWithNormalImage:(UIImage*)normalImage darkImage:(UIImage*)darkImage {
  RNCPStore *store = [RNCPStore sharedManager];
    if (normalImage == nil || darkImage == nil) {
        return normalImage ? : darkImage;
    }
    if (@available(iOS 14.0, *)) {
      UIImageAsset* imageAsset = darkImage.imageAsset;

        // darkImage
        UITraitCollection* darkImageTraitCollection = [UITraitCollection traitCollectionWithTraitsFromCollections:
        @[[UITraitCollection traitCollectionWithUserInterfaceStyle:UIUserInterfaceStyleDark],
          [UITraitCollection traitCollectionWithDisplayScale:normalImage.scale]]];
        [imageAsset registerImage:normalImage withTraitCollection:darkImageTraitCollection];

        RNCarPlayApp* app = store.app;
        return [imageAsset imageWithTraitCollection: app.interfaceController.carTraitCollection];
    }
    else {
        return normalImage;
   }
}

- (UIImage *)imageWithSize:(UIImage *)image convertToSize:(CGSize)size {
    UIGraphicsImageRendererFormat *renderFormat = [UIGraphicsImageRendererFormat defaultFormat];
    renderFormat.opaque = NO;
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size format:renderFormat];
    
    UIImage *resizedImage = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    }];
    return resizedImage;
}

- (void)updateItemImageWithURL:(CPListItem *)item imgUrl:(NSString *)imgUrlString {
    NSURL *imgUrl = [NSURL URLWithString:imgUrlString];
    
    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithURL:imgUrl completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        if (data) {
            UIImage *image = [UIImage imageWithData:data];
            dispatch_async(dispatch_get_main_queue(), ^{
                [item setImage:image];
            });
        } else {
            NSLog(@"Failed to load image from URL: %@", imgUrl);
        }
    }];
    [task resume];
}

- (void)updateListRowItemImageWithURL:(CPListImageRowItem *)item imgUrl:(NSString *)imgUrlString index:(int)index {
    NSURL *imgUrl = [NSURL URLWithString:imgUrlString];
    
    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithURL:imgUrl completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        if (data) {
            UIImage *image = [UIImage imageWithData:data];
            
            dispatch_async(dispatch_get_main_queue(), ^{
                NSMutableArray* newImages = [item.gridImages mutableCopy];
                
                @try {
                    newImages[index] = image;
                }
                @catch (NSException *exception) {
                    // Best effort updating the array
                    NSLog(@"Failed to update images array of CPListImageRowItem");
                }
                
                [item updateImages:newImages];
            });
        } else {
            NSLog(@"Failed to load image for CPListImageRowItem from URL: %@", imgUrl);
        }
    }];
    [task resume];
}

- (void)handleBackButtonPress:(NSString *)templateId {
    RNCPStore *store = [RNCPStore sharedManager];
    
    if (self->hasListeners) {
        [self sendEventWithName:@"backButtonPressed" body:@{@"templateId":templateId}];
    }
    
    if (templateId != store.rootTemplateId) {
        [self popTemplate:true];
    }
}

RCT_EXPORT_METHOD(checkForConnection) {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayApp* app = store.app;
    if (app.isConnected && hasListeners) {
        [self sendEventWithName:@"didConnect" body:[app getConnectedWindowInformation]];
    }
}

RCT_EXPORT_METHOD(createTemplate:(NSString *)templateId config:(NSDictionary*)config callback:(id)callback) {
    // Get the shared instance of the RNCPStore class
    RNCPStore *store = [RNCPStore sharedManager];

    // Extract values from the 'config' dictionary
    NSString *type = [RCTConvert NSString:config[@"type"]];
    NSString *title = [RCTConvert NSString:config[@"title"]];
    NSArray *leadingNavigationBarButtons = [self parseBarButtons:[RCTConvert NSArray:config[@"leadingNavigationBarButtons"]] templateId:templateId];
    NSArray *trailingNavigationBarButtons = [self parseBarButtons:[RCTConvert NSArray:config[@"trailingNavigationBarButtons"]] templateId:templateId];
    
    // Create a new CPTemplate object
    CPTemplate *carPlayTemplate = [[CPTemplate alloc] init];
    
    CPBarButton *backButton = nil;
    if (![RCTConvert BOOL:config[@"backButtonHidden"]]) {
        NSString *backButtonTitle = [RCTConvert NSString:config[@"backButtonTitle"]];
        if (@available(iOS 14.0, *)) {
            backButton = [[CPBarButton alloc] initWithTitle:backButtonTitle handler:^(CPBarButton * _Nonnull barButton) {
                [self handleBackButtonPress:templateId];
            }];
        } else {
            backButton = [[CPBarButton alloc] initWithType:CPBarButtonTypeText handler:^(CPBarButton * _Nonnull barButton) {
                [self handleBackButtonPress:templateId];
            }];
            backButton.title = backButtonTitle;
        }
    }
    
    if ([type isEqualToString:@"search"]) {
        CPSearchTemplate *searchTemplate = [[CPSearchTemplate alloc] init];
        searchTemplate.delegate = self;
        self->currentSearchText = nil;
        carPlayTemplate = searchTemplate;
    }
    else if ([type isEqualToString:@"grid"]) {
        NSArray *buttons = [self parseGridButtons:[RCTConvert NSArray:config[@"buttons"]] templateId:templateId];
        CPGridTemplate *gridTemplate = [[CPGridTemplate alloc] initWithTitle:title gridButtons:buttons];
        [gridTemplate setLeadingNavigationBarButtons:leadingNavigationBarButtons];
        [gridTemplate setTrailingNavigationBarButtons:trailingNavigationBarButtons];
        [gridTemplate setBackButton:backButton];
        carPlayTemplate = gridTemplate;
    }
    else if ([type isEqualToString:@"list"]) {
        NSArray *sections = [self parseSections:[RCTConvert NSArray:config[@"sections"]] templateId:templateId];
        CPListTemplate *listTemplate;
        if (@available(iOS 15.0, *)) {
            if ([config objectForKey:@"assistant"]) {
                NSDictionary *assistant = [config objectForKey:@"assistant"];
                BOOL _enabled = [assistant valueForKey:@"enabled"];
                if (_enabled) {
                    CPAssistantCellConfiguration *conf = [[CPAssistantCellConfiguration alloc] initWithPosition:[RCTConvert CPAssistantCellPosition:[config valueForKey:@"position"]] visibility:[RCTConvert CPAssistantCellVisibility:[config valueForKey:@"visibility"]] assistantAction:[RCTConvert CPAssistantCellActionType:[config valueForKey:@"visibility"]]];
                    listTemplate = [[CPListTemplate alloc] initWithTitle:title sections:sections assistantCellConfiguration:conf];
                }
            }
        }
        if (listTemplate == nil) {
            // Fallback on earlier versions
            listTemplate = [[CPListTemplate alloc] initWithTitle:title sections:sections];
        }
        [listTemplate setLeadingNavigationBarButtons:leadingNavigationBarButtons];
        [listTemplate setTrailingNavigationBarButtons:trailingNavigationBarButtons];
        [listTemplate setBackButton:backButton];
        if (config[@"emptyViewTitleVariants"]) {
            if (@available(iOS 14.0, *)) {
                listTemplate.emptyViewTitleVariants = [RCTConvert NSArray:config[@"emptyViewTitleVariants"]];
            }
        }
        if (config[@"emptyViewSubtitleVariants"]) {
            if (@available(iOS 14.0, *)) {
                listTemplate.emptyViewSubtitleVariants = [RCTConvert NSArray:config[@"emptyViewSubtitleVariants"]];
            }
        }
        listTemplate.delegate = self;
        carPlayTemplate = listTemplate;
    }
    else if ([type isEqualToString:@"map"]) {
        CPMapTemplate *mapTemplate = [[CPMapTemplate alloc] init];

        [self applyConfigForMapTemplate:mapTemplate templateId:templateId config:config];
        [mapTemplate setLeadingNavigationBarButtons:leadingNavigationBarButtons];
        [mapTemplate setTrailingNavigationBarButtons:trailingNavigationBarButtons];
        [mapTemplate setBackButton:backButton];
        [mapTemplate setUserInfo:@{ @"templateId": templateId }];
        mapTemplate.mapDelegate = self;

        carPlayTemplate = mapTemplate;
    } else if ([type isEqualToString:@"voicecontrol"]) {
        CPVoiceControlTemplate *voiceTemplate = [[CPVoiceControlTemplate alloc] initWithVoiceControlStates: [self parseVoiceControlStates:config[@"voiceControlStates"]]];
        carPlayTemplate = voiceTemplate;
    } else if ([type isEqualToString:@"nowplaying"]) {
        CPNowPlayingTemplate *nowPlayingTemplate = [CPNowPlayingTemplate sharedTemplate];
        [nowPlayingTemplate setAlbumArtistButtonEnabled:[RCTConvert BOOL:config[@"albumArtistButtonEnabled"]]];
        [nowPlayingTemplate setUpNextTitle:[RCTConvert NSString:config[@"upNextButtonTitle"]]];
        [nowPlayingTemplate setUpNextButtonEnabled:[RCTConvert BOOL:config[@"upNextButtonEnabled"]]];
        NSMutableArray<CPNowPlayingButton *> *buttons = [NSMutableArray new];
        NSArray<NSDictionary*> *_buttons = [RCTConvert NSDictionaryArray:config[@"buttons"]];
        
        NSDictionary *buttonTypeMapping = @{
            @"shuffle": CPNowPlayingShuffleButton.class,
            @"add-to-library": CPNowPlayingAddToLibraryButton.class,
            @"more": CPNowPlayingMoreButton.class,
            @"playback": CPNowPlayingPlaybackRateButton.class,
            @"repeat": CPNowPlayingRepeatButton.class,
            @"image": CPNowPlayingImageButton.class
        };
        
        for (NSDictionary *_button in _buttons) {
            NSString *buttonType = [RCTConvert NSString:_button[@"type"]];
            NSDictionary *body = @{@"templateId":templateId, @"id": _button[@"id"] };
            Class buttonClass = buttonTypeMapping[buttonType];
            if (buttonClass) {
                CPNowPlayingButton *button;
                
                if ([buttonType isEqualToString:@"image"]) {
                    UIImage *_image = [RCTConvert UIImage:[_button objectForKey:@"image"]];
                    button = [[CPNowPlayingImageButton alloc] initWithImage:_image handler:^(__kindof CPNowPlayingImageButton * _Nonnull) {
                        if (self->hasListeners) {
                            [self sendEventWithName:@"buttonPressed" body:body];
                        }
                    }];
                } else {
                    button = [[buttonClass alloc] initWithHandler:^(__kindof CPNowPlayingButton * _Nonnull) {
                        if (self->hasListeners) {
                            [self sendEventWithName:@"buttonPressed" body:body];
                        }
                    }];
                }
                
                [buttons addObject:button];
            }
        }
        [nowPlayingTemplate updateNowPlayingButtons:buttons];
        carPlayTemplate = nowPlayingTemplate;
    } else if ([type isEqualToString:@"tabbar"]) {
        CPTabBarTemplate *tabBarTemplate = [[CPTabBarTemplate alloc] initWithTemplates:[self parseTemplatesFrom:config]];
        tabBarTemplate.delegate = self;
        carPlayTemplate = tabBarTemplate;
    } else if ([type isEqualToString:@"contact"]) {
        NSString *nm = [RCTConvert NSString:config[@"name"]];
        UIImage *img = [RCTConvert UIImage:config[@"image"]];
        CPContact *contact = [[CPContact alloc] initWithName:nm image:img];
        [contact setSubtitle:config[@"subtitle"]];
        [contact setActions:[self parseButtons:config[@"actions"] templateId:templateId]];
        CPContactTemplate *contactTemplate = [[CPContactTemplate alloc] initWithContact:contact];
        [contactTemplate setBackButton:backButton];
        carPlayTemplate = contactTemplate;
    } else if ([type isEqualToString:@"actionsheet"]) {
        NSString *title = [RCTConvert NSString:config[@"title"]];
        NSString *message = [RCTConvert NSString:config[@"message"]];
        NSMutableArray<CPAlertAction *> *actions = [NSMutableArray new];
        NSArray<NSDictionary*> *_actions = [RCTConvert NSDictionaryArray:config[@"actions"]];
        for (NSDictionary *_action in _actions) {
            CPAlertAction *action = [[CPAlertAction alloc] initWithTitle:[RCTConvert NSString:_action[@"title"]] style:[RCTConvert CPAlertActionStyle:_action[@"style"]] handler:^(CPAlertAction *a) {
                if (self->hasListeners) {
                    [self sendEventWithName:@"actionButtonPressed" body:@{@"templateId":templateId, @"id": _action[@"id"] }];
                }
            }];
            [actions addObject:action];
        }
        CPActionSheetTemplate *actionSheetTemplate = [[CPActionSheetTemplate alloc] initWithTitle:title message:message actions:actions];
        carPlayTemplate = actionSheetTemplate;
    } else if ([type isEqualToString:@"alert"]) {
        NSMutableArray<CPAlertAction *> *actions = [NSMutableArray new];
        NSArray<NSDictionary*> *_actions = [RCTConvert NSDictionaryArray:config[@"actions"]];
        for (NSDictionary *_action in _actions) {
            CPAlertAction *action = [[CPAlertAction alloc] initWithTitle:[RCTConvert NSString:_action[@"title"]] style:[RCTConvert CPAlertActionStyle:_action[@"style"]] handler:^(CPAlertAction *a) {
                if (self->hasListeners) {
                    [self sendEventWithName:@"actionButtonPressed" body:@{@"templateId":templateId, @"id": _action[@"id"] }];
                }
            }];
            [actions addObject:action];
        }
        NSArray<NSString*>* titleVariants = [RCTConvert NSArray:config[@"titleVariants"]];
        CPAlertTemplate *alertTemplate = [[CPAlertTemplate alloc] initWithTitleVariants:titleVariants actions:actions];
        carPlayTemplate = alertTemplate;
    } else if ([type isEqualToString:@"poi"]) {
        NSString *title = [RCTConvert NSString:config[@"title"]];
        NSMutableArray<__kindof CPPointOfInterest *> * items = [NSMutableArray new];
        NSUInteger selectedIndex = 0;

        NSArray<NSDictionary*> *_items = [RCTConvert NSDictionaryArray:config[@"items"]];
        for (NSDictionary *_item in _items) {
            CPPointOfInterest *poi = [RCTConvert CPPointOfInterest:_item];
            [poi setUserInfo:_item];
            [items addObject:poi];
        }

        CPPointOfInterestTemplate *poiTemplate = [[CPPointOfInterestTemplate alloc] initWithTitle:title pointsOfInterest:items selectedIndex:selectedIndex];
        [poiTemplate setBackButton:backButton];
        poiTemplate.pointOfInterestDelegate = self;
        carPlayTemplate = poiTemplate;
    } else if ([type isEqualToString:@"information"]) {
        NSString *title = [RCTConvert NSString:config[@"title"]];
        CPInformationTemplateLayout layout = [RCTConvert BOOL:config[@"leading"]] ? CPInformationTemplateLayoutLeading : CPInformationTemplateLayoutTwoColumn;
        NSMutableArray<__kindof CPInformationItem *> * items = [NSMutableArray new];
        NSMutableArray<__kindof CPTextButton *> * actions = [NSMutableArray new];

        NSArray<NSDictionary*> *_items = [RCTConvert NSDictionaryArray:config[@"items"]];
        for (NSDictionary *_item in _items) {
            [items addObject:[[CPInformationItem alloc] initWithTitle:_item[@"title"] detail:_item[@"detail"]]];
        }

        NSArray<NSDictionary*> *_actions = [RCTConvert NSDictionaryArray:config[@"actions"]];
        for (NSDictionary *_action in _actions) {
            CPTextButton *action = [[CPTextButton alloc] initWithTitle:_action[@"title"] textStyle:CPTextButtonStyleNormal handler:^(__kindof CPTextButton * _Nonnull contactButton) {
                if (self->hasListeners) {
                    [self sendEventWithName:@"actionButtonPressed" body:@{@"templateId":templateId, @"id": _action[@"id"] }];
                }
            }];
            [actions addObject:action];
        }

        CPInformationTemplate *informationTemplate = [[CPInformationTemplate alloc] initWithTitle:title layout:layout items:items actions:actions];
        [informationTemplate setTrailingNavigationBarButtons:trailingNavigationBarButtons];
        [informationTemplate setBackButton:backButton];
        carPlayTemplate = informationTemplate;
    }

    if (config[@"tabSystemItem"]) {
        carPlayTemplate.tabSystemItem = [RCTConvert NSInteger:config[@"tabSystemItem"]];
    }
    if (config[@"tabSystemImageName"]) {
        carPlayTemplate.tabImage = [UIImage systemImageNamed:[RCTConvert NSString:config[@"tabSystemImageName"]]];
    }
    if (config[@"tabImage"]) {
        carPlayTemplate.tabImage = [RCTConvert UIImage:config[@"tabImage"]];
    }
    if (config[@"tabTitle"]) {
        carPlayTemplate.tabTitle = [RCTConvert NSString:config[@"tabTitle"]];
    }

    [carPlayTemplate setUserInfo:@{ @"templateId": templateId }];
    [store setTemplate:templateId template:carPlayTemplate];
}

RCT_EXPORT_METHOD(createTrip:(NSString*)tripId config:(NSDictionary*)config) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTrip *trip = [self parseTrip:config];
    NSMutableDictionary *userInfo = trip.userInfo;
    if (!userInfo) {
        userInfo = [[NSMutableDictionary alloc] init];
        trip.userInfo = userInfo;
    }

    [userInfo setValue:tripId forKey:@"id"];
    [store setTrip:tripId trip:trip];
}

RCT_EXPORT_METHOD(updateTravelEstimatesForTrip:(NSString*)templateId tripId:(NSString*)tripId travelEstimates:(NSDictionary*)travelEstimates timeRemainingColor:(NSUInteger*)timeRemainingColor) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        CPTrip *trip = [[RNCPStore sharedManager] findTripById:tripId];
        if (trip) {
            CPTravelEstimates *estimates = [self parseTravelEstimates:travelEstimates];
            [mapTemplate updateTravelEstimates:estimates forTrip:trip withTimeRemainingColor:(CPTimeRemainingColor) timeRemainingColor];
        }
    }
}

RCT_REMAP_METHOD(startNavigationSession,
                 templateId:(NSString *)templateId
                 tripId:(NSString *)tripId
                 startNavigationSessionWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        CPTrip *trip = [[RNCPStore sharedManager] findTripById:tripId];
        if (trip) {
            CPNavigationSession *navigationSession = [[RNCPStore sharedManager] getNavigationSession];
            if (navigationSession) {
                [navigationSession cancelTrip];
                [[RNCPStore sharedManager] setNavigationSession:nil];
            }
            
            navigationSession = [mapTemplate startNavigationSessionForTrip:trip];
            [store setNavigationSession:navigationSession];
            resolve(nil);
        } else {
            reject(@"trip_not_found", @"Trip not found in store", nil);
        }
    } else {
        reject(@"template_not_found", @"Template not found in store", nil);
    }
}

RCT_EXPORT_METHOD(updateManeuvers:(NSArray*)maneuvers) {
    CPNavigationSession* navigationSession = [[RNCPStore sharedManager] getNavigationSession];
    if (navigationSession) {
        NSMutableArray<CPManeuver*>* upcomingManeuvers = [NSMutableArray array];
        if (@available(iOS 18.0, *)) {
            NSMutableArray<CPLaneGuidance*>* upcomingLaneGuidances = [NSMutableArray array];
            for (NSDictionary *maneuver in maneuvers) {
                CPManeuver *parsedManeuver = [self parseManeuver:maneuver];
                CPLaneGuidance *parsedLaneGuidance = [self parseLaneGuidance:maneuver];
                if (parsedLaneGuidance != nil) {
                    parsedManeuver.linkedLaneGuidance = parsedLaneGuidance;
                    [upcomingLaneGuidances addObject:parsedLaneGuidance];
                }
                
                [upcomingManeuvers addObject:parsedManeuver];
            }
            [navigationSession addLaneGuidances:upcomingLaneGuidances];
            [navigationSession setCurrentLaneGuidance:upcomingLaneGuidances.lastObject];
        } else {
            for (NSDictionary *maneuver in maneuvers) {
                [upcomingManeuvers addObject:[self parseManeuver:maneuver]];
            }
        }
        
        if (@available(iOS 17.4, *)) {
            // disgusting workaround to prevent crashes on iOS < 17.4 even though this should be supported since iOS 12 according to Apple docs
            [navigationSession addManeuvers:upcomingManeuvers];
        }

        [navigationSession setUpcomingManeuvers:upcomingManeuvers];
    }
}

RCT_EXPORT_METHOD(updateTravelEstimatesNavigationSession:(NSUInteger)maneuverIndex travelEstimates:(NSDictionary*)travelEstimates) {
    CPNavigationSession* navigationSession = [[RNCPStore sharedManager] getNavigationSession];
    if (navigationSession) {
        CPManeuver *maneuver = [[navigationSession upcomingManeuvers] objectAtIndex:maneuverIndex];
        if (maneuver) {
            [navigationSession updateTravelEstimates:[self parseTravelEstimates:travelEstimates] forManeuver:maneuver];
        }
    }
}

RCT_EXPORT_METHOD(pauseNavigationSession:(NSUInteger*)reason description:(NSString*)description resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    CPNavigationSession* navigationSession = [[RNCPStore sharedManager] getNavigationSession];
    if (navigationSession) {
        [navigationSession pauseTripForReason:(CPTripPauseReason) reason description:description];
        resolve(nil);
    } else {
        reject(@"no_session", @"Could not pause. No session found.", nil);
    }
}

RCT_EXPORT_METHOD(cancelNavigationSession:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    CPNavigationSession* navigationSession = [[RNCPStore sharedManager] getNavigationSession];
    if (navigationSession) {
        [navigationSession cancelTrip];
        [[RNCPStore sharedManager] setNavigationSession:nil];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            resolve(nil);
        });
    } else {
        reject(@"no_session", @"Could not cancel. No session found.", nil);
    }
}

RCT_EXPORT_METHOD(finishNavigationSession:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    CPNavigationSession* navigationSession = [[RNCPStore sharedManager] getNavigationSession];
    if (navigationSession) {
        [navigationSession finishTrip];
        [[RNCPStore sharedManager] setNavigationSession:nil];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            resolve(nil);
        });
    } else {
        reject(@"no_session", @"Could not finish. No session found.", nil);
    }
}

RCT_EXPORT_METHOD(setRootTemplate:(NSString *)templateId animated:(BOOL)animated) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    RNCarPlayApp* app = store.app;

    store.rootTemplateId = templateId;

    if (template) {
        [app.interfaceController setRootTemplate:template animated:animated completion:^(BOOL done, NSError * _Nullable err) {
            NSLog(@"error %@", err);
            // noop
        }];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(pushTemplate:(NSString *)templateId animated:(BOOL)animated) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    RNCarPlayApp* app = store.app;
    if (template) {
        [app.interfaceController pushTemplate:template animated:animated completion:^(BOOL done, NSError * _Nullable err) {
            NSLog(@"error %@", err);
            // noop
        }];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(popToTemplate:(NSString *)templateId animated:(BOOL)animated) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    RNCarPlayApp* app = store.app;
    if (template) {
        [app.interfaceController popToTemplate:template animated:animated completion:^(BOOL done, NSError * _Nullable err) {
            NSLog(@"error %@", err);
            // noop
        }];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(popToRootTemplate:(BOOL)animated) {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayApp* app = store.app;
    [app.interfaceController popToRootTemplateAnimated:animated completion:^(BOOL done, NSError * _Nullable err) {
        NSLog(@"error %@", err);
        if (done) {
            for (NSString *templateId in store.getTemplateIds) {
                if (templateId == store.rootTemplateId) {
                    continue;
                }
                [self sendEventWithName:@"poppedToRoot" body:@{ @"templateId": templateId }];
            }
        }
    }];
}

RCT_EXPORT_METHOD(popTemplate:(BOOL)animated) {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayApp* app = store.app;
    [app.interfaceController popTemplateAnimated:animated completion:^(BOOL done, NSError * _Nullable err) {
        NSLog(@"error %@", err);
        // noop
    }];
}

RCT_EXPORT_METHOD(presentTemplate:(NSString *)templateId animated:(BOOL)animated) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    RNCarPlayApp* app = store.app;
    if (template) {
        [app.interfaceController presentTemplate:template animated:animated completion:^(BOOL done, NSError * _Nullable err) {
            NSLog(@"error %@", err);
            // noop
        }];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(dismissTemplate:(BOOL)animated) {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayApp* app = store.app;
    [app.interfaceController dismissTemplateAnimated:animated];
}

RCT_EXPORT_METHOD(updateListTemplate:(NSString*)templateId config:(NSDictionary*)config) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template && [template isKindOfClass:[CPListTemplate class]]) {
        CPListTemplate *listTemplate = (CPListTemplate *)template;
        if (config[@"leadingNavigationBarButtons"]) {
            NSArray *leadingNavigationBarButtons = [self parseBarButtons:[RCTConvert NSArray:config[@"leadingNavigationBarButtons"]] templateId:templateId];
            [listTemplate setLeadingNavigationBarButtons:leadingNavigationBarButtons];
        }
        if (config[@"trailingNavigationBarButtons"]) {
            NSArray *trailingNavigationBarButtons = [self parseBarButtons:[RCTConvert NSArray:config[@"trailingNavigationBarButtons"]] templateId:templateId];
            [listTemplate setTrailingNavigationBarButtons:trailingNavigationBarButtons];
        }
        if (config[@"emptyViewTitleVariants"]) {
            listTemplate.emptyViewTitleVariants = [RCTConvert NSArray:config[@"emptyViewTitleVariants"]];
        }
        if (config[@"emptyViewSubtitleVariants"]) {
            NSLog(@"%@", [RCTConvert NSArray:config[@"emptyViewSubtitleVariants"]]);
            listTemplate.emptyViewSubtitleVariants = [RCTConvert NSArray:config[@"emptyViewSubtitleVariants"]];
        }
    }
}

RCT_EXPORT_METHOD(updateTabBarTemplates:(NSString *)templateId templates:(NSDictionary*)config) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPTabBarTemplate *tabBarTemplate = (CPTabBarTemplate*) template;
        [tabBarTemplate updateTemplates:[self parseTemplatesFrom:config]];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}


RCT_EXPORT_METHOD(updateListTemplateSections:(NSString *)templateId sections:(NSArray*)sections) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPListTemplate *listTemplate = (CPListTemplate*) template;
        [listTemplate updateSections:[self parseSections:sections templateId:templateId]];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(updateListTemplateItem:(NSString *)templateId config:(NSDictionary*)config) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPListTemplate *listTemplate = (CPListTemplate*) template;
        NSInteger sectionIndex = [RCTConvert NSInteger:config[@"sectionIndex"]];
        if (sectionIndex >= listTemplate.sections.count) {
            NSLog(@"Failed to update item at section %d, sections size is %d", index, listTemplate.sections.count);
            return;
        }
        CPListSection *section = listTemplate.sections[sectionIndex];
        NSInteger index = [RCTConvert NSInteger:config[@"itemIndex"]];
        if (index >= section.items.count) {
            NSLog(@"Failed to update item at index %d, section size is %d", index, section.items.count);
            return;
        }
        CPListItem *item = (CPListItem *)section.items[index];
        if (config[@"imgUrl"]) {
            NSString *imgUrlString = [RCTConvert NSString:config[@"imgUrl"]];
            [self updateItemImageWithURL:item imgUrl:imgUrlString];
        }
        if (config[@"image"]) {
            [item setImage:[RCTConvert UIImage:config[@"image"]]];
        }
        if (config[@"text"]) {
            [item setText:[RCTConvert NSString:config[@"text"]]];
        }
        if (config[@"detailText"]) {
            [item setDetailText:[RCTConvert NSString:config[@"detailText"]]];
        }
        if (config[@"isPlaying"]) {
            [item setPlaying:[RCTConvert BOOL:config[@"isPlaying"]]];
        }
        if (@available(iOS 14.0, *) && config[@"playbackProgress"]) {
            [item setPlaybackProgress:[RCTConvert CGFloat:config[@"playbackProgress"]]];
        }
        if (@available(iOS 14.0, *) && config[@"accessoryImage"]) {
            [item setAccessoryImage:[RCTConvert UIImage:config[@"accessoryImage"]]];
        }
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(updateInformationTemplateItems:(NSString *)templateId items:(NSArray*)items) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPInformationTemplate *informationTemplate = (CPInformationTemplate*) template;
        informationTemplate.items = [self parseInformationItems:items];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(updateInformationTemplateActions:(NSString *)templateId items:(NSArray*)actions) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPInformationTemplate *informationTemplate = (CPInformationTemplate*) template;
        informationTemplate.actions = [self parseInformationActions:actions templateId:templateId];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(getMaximumListItemCount:(NSString *)templateId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPListTemplate *listTemplate = (CPListTemplate*) template;
        resolve(@(CPListTemplate.maximumItemCount));
    } else {
        NSLog(@"Failed to find template %@", template);
        reject(@"template_not_found", @"Template not found in store", nil);
    }
}

RCT_EXPORT_METHOD(getMaximumListItemImageSize:(NSString *)templateId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPListTemplate *listTemplate = (CPListTemplate*) template;
        NSDictionary *sizeDict = @{
            @"width": @(CPListItem.maximumImageSize.width),
            @"height": @(CPListItem.maximumImageSize.height)
        };
        resolve(sizeDict);
    } else {
        NSLog(@"Failed to find template %@", template);
        reject(@"template_not_found", @"Template not found in store", nil);
    }
}

RCT_EXPORT_METHOD(getMaximumListSectionCount:(NSString *)templateId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPListTemplate *listTemplate = (CPListTemplate*) template;
        resolve(@(CPListTemplate.maximumSectionCount));
    } else {
        NSLog(@"Failed to find template %@", template);
        reject(@"template_not_found", @"Template not found in store", nil);
    }
}

RCT_EXPORT_METHOD(getMaximumNumberOfGridImages:(NSString *)templateId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPListTemplate *listTemplate = (CPListTemplate*) template;
        resolve(@(CPMaximumNumberOfGridImages));
    } else {
        NSLog(@"Failed to find template %@", template);
        reject(@"template_not_found", @"Template not found in store", nil);
    }
}

RCT_EXPORT_METHOD(getMaximumListImageRowItemImageSize:(NSString *)templateId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    CPTemplate *template = [store findTemplateById:templateId];
    if (template) {
        CPListTemplate *listTemplate = (CPListTemplate*) template;
        NSDictionary *sizeDict = @{
            @"width": @(CPListImageRowItem.maximumImageSize.width),
            @"height": @(CPListImageRowItem.maximumImageSize.height)
        };
        resolve(sizeDict);
    } else {
        NSLog(@"Failed to find template %@", template);
        reject(@"template_not_found", @"Template not found in store", nil);
    }
}

RCT_EXPORT_METHOD(updateMapTemplateConfig:(NSString *)templateId config:(NSDictionary*)config) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [self applyConfigForMapTemplate:mapTemplate templateId:templateId config:config];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(showPanningInterface:(NSString *)templateId animated:(BOOL)animated) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [mapTemplate showPanningInterfaceAnimated:animated];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(dismissPanningInterface:(NSString *)templateId animated:(BOOL)animated) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [mapTemplate dismissPanningInterfaceAnimated:animated];
    } else {
        NSLog(@"Failed to find template %@", template);
    }
}

RCT_EXPORT_METHOD(enableNowPlaying:(BOOL)enable) {
    if (enable && !isNowPlayingActive) {
        [CPNowPlayingTemplate.sharedTemplate addObserver:self];
    } else if (!enable && isNowPlayingActive) {
        [CPNowPlayingTemplate.sharedTemplate removeObserver:self];
    }
}

RCT_EXPORT_METHOD(hideTripPreviews:(NSString*)templateId) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [mapTemplate hideTripPreviews];
    }
}

RCT_EXPORT_METHOD(showTripPreviews:(NSString*)templateId tripIds:(NSArray*)tripIds tripConfiguration:(NSDictionary*)tripConfiguration) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    NSMutableArray *trips = [[NSMutableArray alloc] init];

    for (NSString *tripId in tripIds) {
        CPTrip *trip = [[RNCPStore sharedManager] findTripById:tripId];
        if (trip) {
            [trips addObject:trip];
        }
    }

    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [mapTemplate showTripPreviews:trips textConfiguration:[self parseTripPreviewTextConfiguration:tripConfiguration]];
        lastShowTripsTime = [NSDate date];
    }
}

RCT_EXPORT_METHOD(showTripPreview:(NSString*)templateId tripIds:(NSArray*)tripIds selectedTripId:(NSString*)selectedTripId tripConfiguration:(NSDictionary*)tripConfiguration) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    NSMutableArray *trips = [[NSMutableArray alloc] init];

    for (NSString *tripId in tripIds) {
        CPTrip *trip = [[RNCPStore sharedManager] findTripById:tripId];
        if (trip) {
            [trips addObject:trip];
        }
    }
    
    CPTrip *selectedTrip = [[RNCPStore sharedManager] findTripById:selectedTripId];

    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [mapTemplate showTripPreviews:trips selectedTrip:selectedTrip textConfiguration:[self parseTripPreviewTextConfiguration:tripConfiguration]];
        lastShowTripsTime = [NSDate date];
    }
}

RCT_EXPORT_METHOD(showRouteChoicesPreviewForTrip:(NSString*)templateId tripId:(NSString*)tripId tripConfiguration:(NSDictionary*)tripConfiguration) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    CPTrip *trip = [[RNCPStore sharedManager] findTripById:tripId];

    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [mapTemplate showRouteChoicesPreviewForTrip:trip textConfiguration:[self parseTripPreviewTextConfiguration:tripConfiguration]];
    }
}

RCT_EXPORT_METHOD(presentNavigationAlert:(NSString*)templateId json:(NSDictionary*)json animated:(BOOL)animated) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        NSNumber *navigationAlertId = [json objectForKey:@"navigationAlertId"];
        CPNavigationAlert *navigationAlert = navigationAlertWrappers[navigationAlertId];
        if (navigationAlert) {
            NSArray<NSString *> * titleVariants = [RCTConvert NSStringArray:json[@"titleVariants"]];
            NSArray<NSString *> * subtitleVariants = [RCTConvert NSStringArray:json[@"subtitleVariants"]];
            
            [navigationAlert updateTitleVariants:titleVariants subtitleVariants:subtitleVariants];
            return;
        }
        
        [mapTemplate presentNavigationAlert:[self parseNavigationAlert:json templateId:templateId] animated:animated];
    }
}

RCT_EXPORT_METHOD(dismissNavigationAlert:(NSString*)templateId animated:(BOOL)animated resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        [mapTemplate dismissNavigationAlertAnimated:animated completion:^(BOOL completion) {
            if (completion) {
                resolve(@YES);
            } else {
                resolve(@NO);
            }
        }];
    } else {
        reject(@"template_not_found", @"Template not found", nil);
    }
}

RCT_EXPORT_METHOD(activateVoiceControlState:(NSString*)templateId identifier:(NSString*)identifier) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPVoiceControlTemplate *voiceTemplate = (CPVoiceControlTemplate*) template;
        [voiceTemplate activateVoiceControlStateWithIdentifier:identifier];
    }
}

RCT_EXPORT_METHOD(reactToUpdatedSearchText:(NSString *)templateId templateId:(NSArray *)items) {
    NSArray *sectionsItems = [self parseListItems:items startIndex:0 templateId:templateId];

    if (self.searchResultBlock) {
        self.searchResultBlock(sectionsItems);
        self.searchResultBlock = nil;
    }
}

RCT_EXPORT_METHOD(reactToSelectedResult:(BOOL)status) {
    if (self.selectedResultBlock) {
        self.selectedResultBlock();
        self.selectedResultBlock = nil;
    }
}

RCT_EXPORT_METHOD(updateMapTemplateMapButtons:(NSString*) templateId mapButtons:(NSArray*) mapButtonConfig) {
    CPTemplate *template = [[RNCPStore sharedManager] findTemplateById:templateId];
    if (template) {
        CPMapTemplate *mapTemplate = (CPMapTemplate*) template;
        NSArray *mapButtons = [RCTConvert NSArray:mapButtonConfig];
        NSMutableArray *result = [NSMutableArray array];
        for (NSDictionary *mapButton in mapButtons) {
            NSString *_id = [mapButton objectForKey:@"id"];
            [result addObject:[RCTConvert CPMapButton:mapButton withHandler:^(CPMapButton * _Nonnull mapButton) {
                [self sendTemplateEventWithName:mapTemplate name:@"mapButtonPressed" json:@{ @"id": _id }];
            }]];
        }
        [mapTemplate setMapButtons:result];
    }
}

RCT_EXPORT_METHOD(getTopTemplate: (RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayApp* app = store.app;
    CPTemplate *topTemplate = app.interfaceController.topTemplate;
    if (topTemplate == nil || topTemplate.userInfo == nil || topTemplate.userInfo[@"templateId"] == nil) {
        resolve([NSNull null]);
    } else {
        resolve(topTemplate.userInfo[@"templateId"]);
    }
}

RCT_EXPORT_METHOD(getRootTemplate: (RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RNCPStore *store = [RNCPStore sharedManager];
    if (!store.rootTemplateId) {
        resolve([NSNull null]);
        return;
    }
    resolve(store.rootTemplateId);
}

# pragma parsers

- (void) applyConfigForMapTemplate:(CPMapTemplate*)mapTemplate templateId:(NSString*)templateId config:(NSDictionary*)config {
    RNCPStore *store = [RNCPStore sharedManager];

    if ([config objectForKey:@"guidanceBackgroundColor"]) {
        [mapTemplate setGuidanceBackgroundColor:[RCTConvert UIColor:config[@"guidanceBackgroundColor"]]];
    }
    else {
      [mapTemplate setGuidanceBackgroundColor:UIColor.systemGray5Color];
    }
    
    if ([config objectForKey:@"tripEstimateStyle"]) {
        [mapTemplate setTripEstimateStyle:[RCTConvert CPTripEstimateStyle:config[@"tripEstimateStyle"]]];
    }
    else {
      [mapTemplate setTripEstimateStyle:CPTripEstimateStyleDark];
    }

    if ([config objectForKey:@"leadingNavigationBarButtons"]){
        NSArray *leadingNavigationBarButtons = [self parseBarButtons:[RCTConvert NSArray:config[@"leadingNavigationBarButtons"]] templateId:templateId];
        [mapTemplate setLeadingNavigationBarButtons:leadingNavigationBarButtons];
    }
  
    if ([config objectForKey:@"trailingNavigationBarButtons"]){
        NSArray *trailingNavigationBarButtons = [self parseBarButtons:[RCTConvert NSArray:config[@"trailingNavigationBarButtons"]] templateId:templateId];
        [mapTemplate setTrailingNavigationBarButtons:trailingNavigationBarButtons];
    }

    if ([config objectForKey:@"mapButtons"]) {
        NSArray *mapButtons = [RCTConvert NSArray:config[@"mapButtons"]];
        NSMutableArray *result = [NSMutableArray array];
        for (NSDictionary *mapButton in mapButtons) {
            NSString *_id = [mapButton objectForKey:@"id"];
            [result addObject:[RCTConvert CPMapButton:mapButton withHandler:^(CPMapButton * _Nonnull mapButton) {
                [self sendTemplateEventWithName:mapTemplate name:@"mapButtonPressed" json:@{ @"id": _id }];
            }]];
        }
        [mapTemplate setMapButtons:result];
    }

    if ([config objectForKey:@"automaticallyHidesNavigationBar"]) {
        [mapTemplate setAutomaticallyHidesNavigationBar:[RCTConvert BOOL:config[@"automaticallyHidesNavigationBar"]]];
    }

    if ([config objectForKey:@"hidesButtonsWithNavigationBar"]) {
        [mapTemplate setHidesButtonsWithNavigationBar:[RCTConvert BOOL:config[@"hidesButtonsWithNavigationBar"]]];
    }

    if ([config objectForKey:@"render"]) {
        if (store.app == nil) {
            store.app = [[RNCarPlayApp alloc] init];
        }
        [store.app connectModuleWithBridge:self.bridge moduleName:templateId];
    }
}

- (NSArray<__kindof CPTemplate*>*) parseTemplatesFrom:(NSDictionary*)config {
    RNCPStore *store = [RNCPStore sharedManager];
    NSMutableArray<__kindof CPTemplate*> *templates = [NSMutableArray new];
    NSArray<NSDictionary*> *tpls = [RCTConvert NSDictionaryArray:config[@"templates"]];
    for (NSDictionary *tpl in tpls) {
        CPTemplate *templ = [store findTemplateById:tpl[@"id"]];
        // @todo UITabSystemItem
        [templates addObject:templ];
    }
    return templates;
}

- (NSArray<CPButton*>*) parseButtons:(NSArray*)buttons templateId:(NSString *)templateId {
    NSMutableArray *result = [NSMutableArray array];
    for (NSDictionary *button in buttons) {
        CPButton *_button;
        NSString *_id = [button objectForKey:@"id"];
        NSString *type = [button objectForKey:@"type"];
        if ([type isEqualToString:@"call"]) {
            _button = [[CPContactCallButton alloc] initWithHandler:^(__kindof CPButton * _Nonnull contactButton) {
                if (self->hasListeners) {
                    [self sendEventWithName:@"buttonPressed" body:@{@"id": _id, @"templateId":templateId}];
                }
            }];
        } else if ([type isEqualToString:@"message"]) {
            _button = [[CPContactMessageButton alloc] initWithPhoneOrEmail:[button objectForKey:@"phoneOrEmail"]];
        } else if ([type isEqualToString:@"directions"]) {
            _button = [[CPContactDirectionsButton alloc] initWithHandler:^(__kindof CPButton * _Nonnull contactButton) {
                if (self->hasListeners) {
                    [self sendEventWithName:@"buttonPressed" body:@{@"id": _id, @"templateId":templateId}];
                }
            }];
        }

        BOOL _disabled = [button objectForKey:@"disabled"];
        [_button setEnabled:!_disabled];

        NSString *_title = [button objectForKey:@"title"];
        [_button setTitle:_title];

        [result addObject:_button];
    }
    return result;
}

- (NSArray<CPBarButton*>*) parseBarButtons:(NSArray*)barButtons templateId:(NSString *)templateId {
    NSMutableArray *result = [NSMutableArray array];
    for (NSDictionary *barButton in barButtons) {
        CPBarButtonType _type;
        NSString *_id = [barButton objectForKey:@"id"];
        NSString *type = [barButton objectForKey:@"type"];
        if (type && [type isEqualToString:@"image"]) {
            _type = CPBarButtonTypeImage;
        } else {
            _type = CPBarButtonTypeText;
        }
        CPBarButton *_barButton = [[CPBarButton alloc] initWithType:_type handler:^(CPBarButton * _Nonnull barButton) {
            if (self->hasListeners) {
                [self sendEventWithName:@"barButtonPressed" body:@{@"id": _id, @"templateId":templateId}];
            }
        }];
        BOOL _disabled = [[barButton objectForKey:@"disabled"] isEqualToNumber:[NSNumber numberWithInt:1]];
        [_barButton setEnabled:!_disabled];

        if (_type == CPBarButtonTypeText) {
            NSString *_title = [barButton objectForKey:@"title"];
            [_barButton setTitle:_title];
        } else if (_type == CPBarButtonTypeImage) {
            UIImage *_image = [RCTConvert UIImage:[barButton objectForKey:@"image"]];
            [_barButton setImage:_image];
        }
        [result addObject:_barButton];
    }
    return result;
}

- (NSArray<CPListSection*>*)parseSections:(NSArray*)sections templateId:(NSString *)templateId {
    NSMutableArray *result = [NSMutableArray array];
    int index = 0;
    for (NSDictionary *section in sections) {
        NSArray *items = [section objectForKey:@"items"];
        NSString *_sectionIndexTitle = [section objectForKey:@"sectionIndexTitle"];
        NSString *_header = [section objectForKey:@"header"];
        NSArray *_items = [self parseListItems:items startIndex:index templateId:templateId];
        CPListSection *_section = [[CPListSection alloc] initWithItems:_items header:_header sectionIndexTitle:_sectionIndexTitle];
        [result addObject:_section];
        int count = (int) [items count];
        index = index + count;
    }
    return result;
}

- (NSArray<CPSelectableListItem>*)parseListItems:(NSArray*)items startIndex:(int)startIndex templateId:(NSString *)templateId {
    NSMutableArray *_items = [NSMutableArray array];
    int listIndex = startIndex;
    for (NSDictionary *item in items) {
        BOOL _showsDisclosureIndicator = [[item objectForKey:@"showsDisclosureIndicator"] isEqualToNumber:[NSNumber numberWithInt:1]];
        NSString *_detailText = [item objectForKey:@"detailText"];
        NSString *_text = [item objectForKey:@"text"];
        NSString *_id = [item objectForKey:@"id"];
        NSObject *_imageObj = [item objectForKey:@"image"];
        
        NSMutableDictionary *userInfo = [NSMutableDictionary dictionary];
        userInfo[@"index"] = @(listIndex);
        if (_id != nil) {
            userInfo[@"id"] = _id;
        }
        
        NSArray *_imageItems = [item objectForKey:@"images"];
        NSArray *_imageUrls = [item objectForKey:@"imgUrls"];
        
        if (_imageItems == nil && _imageUrls == nil) {
            UIImage *_image = [RCTConvert UIImage:_imageObj];
            CPListItem *_item;
            if (@available(iOS 14.0, *)) {
                CPListItemAccessoryType accessoryType = _showsDisclosureIndicator ? CPListItemAccessoryTypeDisclosureIndicator : CPListItemAccessoryTypeNone;
                _item = [[CPListItem alloc] initWithText:_text detailText:_detailText image:_image accessoryImage:nil accessoryType:accessoryType];
            } else {
                _item = [[CPListItem alloc] initWithText:_text detailText:_detailText image:_image showsDisclosureIndicator:_showsDisclosureIndicator];
            }
            if ([item objectForKey:@"isPlaying"]) {
                [_item setPlaying:[RCTConvert BOOL:[item objectForKey:@"isPlaying"]]];
            }
            if (item[@"imgUrl"]) {
                NSString *imgUrlString = [RCTConvert NSString:item[@"imgUrl"]];
                [self updateItemImageWithURL:_item imgUrl:imgUrlString];
            }
            
            [_item setUserInfo:userInfo];
            [_items addObject:_item];
        } else {
            // parse images
            NSMutableArray * _images = [NSMutableArray array];
            
            if (_imageItems != nil) {
                NSArray* slicedArray = [_imageItems subarrayWithRange:NSMakeRange(0, MIN(CPMaximumNumberOfGridImages, _imageItems.count))];

                for (NSObject *imageObj in slicedArray){
                    UIImage *_image = [RCTConvert UIImage:imageObj];
                    [_images addObject:_image];
                }
            }
            if (@available(iOS 14.0, *)) {
                
                CPListImageRowItem *_item;
                if (_images.count > 0)
                {
                    _item = [[CPListImageRowItem alloc] initWithText:_text images:_images];
                }
                else
                {
                    // Show only as much images as allowed.
                    NSArray* _slicedArray = [_imageUrls subarrayWithRange:NSMakeRange(0, MIN(CPMaximumNumberOfGridImages, _imageUrls.count))];//
                    
                    // create array with empty UI images, that will be replaced later
                    NSMutableArray *_imagesArray = [NSMutableArray arrayWithCapacity:_slicedArray.count];
                    for (NSUInteger i = 0; i < _slicedArray.count; i++) {
                        [_imagesArray addObject:[[UIImage alloc] init]];
                    }
                    _item = [[CPListImageRowItem alloc] initWithText:_text images:_imagesArray];
                    
                    
                    int _index = 0;
                    for (NSString* imgUrl in _slicedArray) {
                        [self updateListRowItemImageWithURL:_item imgUrl:imgUrl index:_index];
                        _index++;
                    }
                }
                
                [_item setListImageRowHandler:^(CPListImageRowItem * _Nonnull item, NSInteger index, dispatch_block_t  _Nonnull completionBlock) {
                    // Find the current template
                    RNCPStore *store = [RNCPStore sharedManager];
                    CPTemplate *template = [store findTemplateById:templateId];
                    if (template) {
                        [self sendTemplateEventWithName:template name:@"didSelectListItemRowImage" json:@{ @"index": @(listIndex), @"imageIndex": @(index)}];
                    }
                }];
                
                [_item setUserInfo:userInfo];
                [_items addObject:_item];
            }
            
        }
        listIndex = listIndex + 1;
    }
    return _items;
}


- (NSArray<CPInformationItem*>*)parseInformationItems:(NSArray*)items {
    NSMutableArray *_items = [NSMutableArray array];
    for (NSDictionary *item in items) {
        [_items addObject:[[CPInformationItem alloc] initWithTitle:item[@"title"] detail:item[@"detail"]]];
    }
    
    return _items;
}

- (NSArray<CPTextButton*>*)parseInformationActions:(NSArray*)actions templateId:(NSString *)templateId {
    NSMutableArray *_actions = [NSMutableArray array];
    for (NSDictionary *action in actions) {
        CPTextButton *_action = [[CPTextButton alloc] initWithTitle:action[@"title"] textStyle:CPTextButtonStyleNormal handler:^(__kindof CPTextButton * _Nonnull contactButton) {
            if (self->hasListeners) {
                [self sendEventWithName:@"actionButtonPressed" body:@{@"templateId":templateId, @"id": action[@"id"] }];
            }
        }];
        [_actions addObject:_action];
    }
    
    return _actions;
}

- (NSArray<CPGridButton*>*) parseGridButtons:(NSArray*)buttons templateId:(NSString*)templateId {
    NSMutableArray *result = [NSMutableArray array];
    int index = 0;
    for (NSDictionary *button in buttons) {
        NSString *_id = [button objectForKey:@"id"];
        NSArray<NSString*> *_titleVariants = [button objectForKey:@"titleVariants"];
        UIImage *_image = [RCTConvert UIImage:[button objectForKey:@"image"]];
        CPGridButton *_button = [[CPGridButton alloc] initWithTitleVariants:_titleVariants image:_image handler:^(CPGridButton * _Nonnull barButton) {
            if (self->hasListeners) {
                [self sendEventWithName:@"gridButtonPressed" body:@{@"id": _id, @"templateId":templateId, @"index": @(index) }];
            }
        }];
        BOOL _disabled = [button objectForKey:@"disabled"];
        [_button setEnabled:!_disabled];
        [result addObject:_button];
        index = index + 1;
    }
    return result;
}

- (CPTravelEstimates*)parseTravelEstimates: (NSDictionary*)json {
    NSString *units = [RCTConvert NSString:json[@"distanceUnits"]];
    double value = [RCTConvert double:json[@"distanceRemaining"]];

    NSUnit *unit = [NSUnitLength kilometers];
    if (units && [units isEqualToString: @"meters"]) {
        unit = [NSUnitLength meters];
    }
    else if (units && [units isEqualToString: @"miles"]) {
        unit = [NSUnitLength miles];
    }
    else if (units && [units isEqualToString: @"feet"]) {
        unit = [NSUnitLength feet];
    }
    else if (units && [units isEqualToString: @"yards"]) {
        unit = [NSUnitLength yards];
    }

    double time = [RCTConvert double:json[@"timeRemaining"]];
    if (value < 0 && time >= 0) {
        if (time >= 3600) {
            unit = [NSUnitDuration hours];
            value = lroundf(time / 3600);
        }
        else if (time < 60) {
            unit = [NSUnitDuration seconds];
            value = time;
        }
        else {
            unit = [NSUnitDuration minutes];
            value = lroundf(time / 60);
        }
    }

    NSMeasurement *distance = [[NSMeasurement alloc] initWithDoubleValue:value unit:unit];
    return [[CPTravelEstimates alloc] initWithDistanceRemaining:distance timeRemaining:time];
}

- (CPLaneGuidance*)parseLaneGuidance:(NSDictionary*)json  API_AVAILABLE(ios(18.0)){
    if ([json objectForKey:@"lanes"]) {
        CPLaneGuidance* laneGuidance = [[CPLaneGuidance alloc] init];

        if ([json objectForKey:@"instructionVariants"]) {
            [laneGuidance setInstructionVariants:[RCTConvert NSStringArray:json[@"instructionVariants"]]];
        }
        
        NSMutableArray<CPLane *> *lanes = [NSMutableArray array];
        NSArray<NSDictionary *> *lanesJSON = [RCTConvert NSArray:json[@"lanes"]];
        for (NSDictionary *laneJSON in lanesJSON) {
            NSMutableArray<NSMeasurement<NSUnitAngle *> *> *angles = [NSMutableArray array];
            if ([laneJSON objectForKey:@"angles"]) {
                NSArray<NSNumber *> *angleValues = [RCTConvert NSArray:laneJSON[@"angles"]];
                for (NSNumber *angle in angleValues) {
                    [angles addObject:[[NSMeasurement alloc] initWithDoubleValue:angle.doubleValue unit:NSUnitAngle.degrees]];
                }
            }

            NSMeasurement<NSUnitAngle *> *highlightedAngle = nil;
            if ([laneJSON objectForKey:@"highlightedAngle"]) {
                double highlightedAngleValue = [RCTConvert double:laneJSON[@"highlightedAngle"]];
                highlightedAngle = [[NSMeasurement alloc] initWithDoubleValue:highlightedAngleValue unit:NSUnitAngle.degrees];
            }

            CPLane *lane = [[CPLane alloc] initWithAngles:angles highlightedAngle:highlightedAngle isPreferred:NO];

            if ([laneJSON objectForKey:@"status"]) {
                NSString *status = [RCTConvert NSString:laneJSON[@"status"]];
                if ([status isEqualToString:@"preferred"]) {
                    [lane setStatus:CPLaneStatusPreferred];
                } else if ([status isEqualToString:@"good"]) {
                    [lane setStatus:CPLaneStatusGood];
                } else if ([status isEqualToString:@"notgood"]) {
                    [lane setStatus:CPLaneStatusNotGood];
                }
            }
            [lanes addObject:lane];
        }
        
        [laneGuidance setLanes:lanes];
        
        return laneGuidance;
    }
    
    return nil;
}

- (CPManeuver*)parseManeuver:(NSDictionary*)json {
    CPManeuver* maneuver = [[CPManeuver alloc] init];

    if ([json objectForKey:@"junctionImage"]) {
        UIImage *junctionImage = [RCTConvert UIImage:json[@"junctionImage"]];
        [maneuver setJunctionImage: junctionImage];
    }

    if ([json objectForKey:@"initialTravelEstimates"]) {
        CPTravelEstimates* travelEstimates = [self parseTravelEstimates:json[@"initialTravelEstimates"]];
        [maneuver setInitialTravelEstimates:travelEstimates];
    }

    if ([json objectForKey:@"symbolImage"]) {
        UIImage *symbolImage = [RCTConvert UIImage:json[@"symbolImage"]];

        if ([json objectForKey:@"symbolImageSize"]) {
            NSDictionary *size = [RCTConvert NSDictionary:json[@"symbolImageSize"]];
            double width = [RCTConvert double:size[@"width"]];
            double height = [RCTConvert double:size[@"height"]];
            symbolImage = [self imageWithSize:symbolImage convertToSize:CGSizeMake(width, height)];
        }
        
        if ([json objectForKey:@"tintSymbolImage"]) {
            UIColor *tintColor = [RCTConvert UIColor:json[@"tintSymbolImage"]];
            UIImage *darkImage = symbolImage;
            UIImage *lightImage = [self imageWithTint:symbolImage andTintColor:tintColor];
            symbolImage = [self dynamicImageWithNormalImage:lightImage darkImage:darkImage];
        }

        [maneuver setSymbolImage:symbolImage];
    }

    if ([json objectForKey:@"instructionVariants"]) {
        [maneuver setInstructionVariants:[RCTConvert NSStringArray:json[@"instructionVariants"]]];
    }

    if ([json objectForKey:@"attributedInstructionVariants"]) {
        // Create an array to hold attributed instruction variants
        NSMutableArray<NSAttributedString *> *attributedInstructions = [NSMutableArray array];
        
        // Process each item in the array from JSON
        NSArray *attributedInstructionVariants = [json objectForKey:@"attributedInstructionVariants"];
        for (NSDictionary *attributedInstructionVariant in attributedInstructionVariants) {
            NSString *text = [attributedInstructionVariant objectForKey:@"text"] ?: @"";
            NSMutableAttributedString *instruction = [[NSMutableAttributedString alloc] initWithString:text];

            NSArray *images = [attributedInstructionVariant objectForKey:@"images"];
            for (NSDictionary *image in images) {
                // If there's an image to attach
                if ([image objectForKey:@"image"]) {
                    UIImage *uiImage = [RCTConvert UIImage:image[@"image"]];
                    if (uiImage) {
                        NSInteger imagePosition = NSNotFound;
                        if ([image objectForKey:@"imagePosition"]) {
                            imagePosition = [image[@"imagePosition"] integerValue];
                        }
                        
                        NSTextAttachment *attachment = [NSTextAttachment textAttachmentWithImage:uiImage];
                        NSAttributedString *container = [NSAttributedString attributedStringWithAttachment:attachment];
                        
                        if (imagePosition != NSNotFound) {
                            [instruction insertAttributedString:container atIndex:imagePosition];
                        } else {
                            [instruction appendAttributedString:container];
                        }
                    }
                }
            }

            [attributedInstructions addObject:instruction];
        }
        
        [maneuver setAttributedInstructionVariants:attributedInstructions];
    }
    
    if (@available(iOS 17.4, *)) {
        if ([json objectForKey:@"maneuverType"]) {
            [maneuver setManeuverType:[RCTConvert int:json[@"maneuverType"]]];
        }
        if ([json objectForKey:@"junctionType"]) {
            [maneuver setJunctionType:[RCTConvert int:json[@"junctionType"]]];
        }
        if ([json objectForKey:@"trafficSide"]) {
            [maneuver setTrafficSide:[RCTConvert int:json[@"trafficSide"]]];
        }
        if ([json objectForKey:@"junctionExitAngle"]) {
            int junctionExitAngle = [RCTConvert int:json[@"junctionExitAngle"]];
            [maneuver setJunctionExitAngle:[[NSMeasurement alloc] initWithDoubleValue:junctionExitAngle unit:NSUnitAngle.degrees]];
        }
        NSArray<NSNumber *> *junctionElementAngles = [json objectForKey:@"junctionElementAngles"];
        if (junctionElementAngles) {
            NSMutableSet<NSMeasurement<NSUnitAngle *> *> *junctionElementAnglesMeasurements = [[NSMutableSet alloc] init];
            for (NSNumber *angle in junctionElementAngles) {
                NSMeasurement<NSUnitAngle *> *measurement = [[NSMeasurement alloc] initWithDoubleValue:angle.doubleValue unit:NSUnitAngle.degrees];
                [junctionElementAnglesMeasurements addObject:measurement];
            }
            [maneuver setJunctionElementAngles:junctionElementAnglesMeasurements];
        }
        
        NSArray<NSString *> *roadFollowingManeuverVariants = [RCTConvert NSArray:[json objectForKey:@"roadFollowingManeuverVariants"]];
        if (roadFollowingManeuverVariants.count > 0) {
            [maneuver setRoadFollowingManeuverVariants:roadFollowingManeuverVariants];
        }
        
        NSString *highwayExitLabel = [RCTConvert NSString:[json objectForKey:@"highwayExitLabel"]];
        if (highwayExitLabel != nil) {
            [maneuver setHighwayExitLabel:highwayExitLabel];
        }
    }

    return maneuver;
}

- (CPTripPreviewTextConfiguration*)parseTripPreviewTextConfiguration:(NSDictionary*)json {
    return [[CPTripPreviewTextConfiguration alloc] initWithStartButtonTitle:[RCTConvert NSString:json[@"startButtonTitle"]] additionalRoutesButtonTitle:[RCTConvert NSString:json[@"additionalRoutesButtonTitle"]] overviewButtonTitle:[RCTConvert NSString:json[@"overviewButtonTitle"]]];
}

- (CPTrip*)parseTrip:(NSDictionary*)config {
    if ([config objectForKey:@"config"]) {
        config = [config objectForKey:@"config"];
    }
    MKMapItem *origin = [RCTConvert MKMapItem:config[@"origin"]];
    MKMapItem *destination = [RCTConvert MKMapItem:config[@"destination"]];
    NSMutableArray *routeChoices = [NSMutableArray array];
    if ([config objectForKey:@"routeChoices"]) {
        NSInteger index = 0;
        for (NSDictionary *routeChoice in [RCTConvert NSArray:config[@"routeChoices"]]) {
            CPRouteChoice *cpRouteChoice = [RCTConvert CPRouteChoice:routeChoice];
            NSMutableDictionary *userInfo = cpRouteChoice.userInfo;
            if (!userInfo) {
                userInfo = [[NSMutableDictionary alloc] init];
                cpRouteChoice.userInfo = userInfo;
            }
            [userInfo setValue:[NSNumber numberWithInteger:index] forKey:@"index"];
            [routeChoices addObject:cpRouteChoice];
            index++;
        }
    }
    return [[CPTrip alloc] initWithOrigin:origin destination:destination routeChoices:routeChoices];
}

- (CPNavigationAlert*)parseNavigationAlert:(NSDictionary*)json templateId:(NSString*)templateId {
    CPImageSet *imageSet;
    if ([json objectForKey:@"lightImage"] && [json objectForKey:@"darkImage"]) {
        imageSet = [[CPImageSet alloc] initWithLightContentImage:[RCTConvert UIImage:json[@"lightImage"]] darkContentImage:[RCTConvert UIImage:json[@"darkImage"]]];
    }
    NSNumber *navigationAlertId = [json objectForKey:@"navigationAlertId"];
    
    CPAlertAction *secondaryAction = [json objectForKey:@"secondaryAction"] ? [self parseAlertAction:json[@"secondaryAction"] body:@{ @"templateId": templateId, @"secondary": @(YES), @"navigationAlertId": navigationAlertId }] : nil;

    CPNavigationAlert* alert = [[CPNavigationAlert alloc] initWithTitleVariants:[RCTConvert NSStringArray:json[@"titleVariants"]] subtitleVariants:[RCTConvert NSStringArray:json[@"subtitleVariants"]] imageSet:imageSet primaryAction:[self parseAlertAction:json[@"primaryAction"] body:@{ @"templateId": templateId, @"primary": @(YES), @"navigationAlertId": navigationAlertId }] secondaryAction:secondaryAction duration:[RCTConvert double:json[@"duration"]]];
    
    
    navigationAlertWrappers[navigationAlertId] = alert;

    return alert;
}

- (CPAlertAction*)parseAlertAction:(NSDictionary*)json body:(NSDictionary*)body {
    return [[CPAlertAction alloc] initWithTitle:[RCTConvert NSString:json[@"title"]] style:(CPAlertActionStyle) [RCTConvert NSUInteger:json[@"style"]] handler:^(CPAlertAction * _Nonnull action) {
        if (self->hasListeners) {
            [self sendEventWithName:@"alertActionPressed" body:body];
        }
    }];
}

- (NSArray<CPVoiceControlState*>*)parseVoiceControlStates:(NSArray<NSDictionary*>*)items {
    NSMutableArray<CPVoiceControlState*>* res = [NSMutableArray array];
    for (NSDictionary *item in items) {
        [res addObject:[self parseVoiceControlState:item]];
    }
    return res;
}

- (CPVoiceControlState*)parseVoiceControlState:(NSDictionary*)json {
    return [[CPVoiceControlState alloc] initWithIdentifier:[RCTConvert NSString:json[@"identifier"]] titleVariants:[RCTConvert NSStringArray:json[@"titleVariants"]] image:[RCTConvert UIImage:json[@"image"]] repeats:[RCTConvert BOOL:json[@"repeats"]]];
}

- (NSString*)panDirectionToString:(CPPanDirection)panDirection {
    switch (panDirection) {
        case CPPanDirectionUp: return @"up";
        case CPPanDirectionRight: return @"right";
        case CPPanDirectionDown: return @"down";
        case CPPanDirectionLeft: return @"left";
        case CPPanDirectionNone: return @"none";
    }
}

- (NSDictionary*)navigationAlertToJson:(CPNavigationAlert*)navigationAlert dismissalContext:(CPNavigationAlertDismissalContext)dismissalContext {
    NSString *dismissalCtx = @"none";
    
    switch (dismissalContext) {
        case CPNavigationAlertDismissalContextTimeout:
            dismissalCtx = @"timeout";
            break;
        case CPNavigationAlertDismissalContextSystemDismissed:
            dismissalCtx = @"system";
            break;
        case CPNavigationAlertDismissalContextUserDismissed:
            dismissalCtx = @"user";
            break;
    }
    
    NSMutableDictionary* userInfo = [[self navigationAlertToJson:navigationAlert] mutableCopy];

    userInfo[@"reason"] = dismissalCtx;
    
    return userInfo;
}
- (NSDictionary*)navigationAlertToJson:(CPNavigationAlert*)navigationAlert {
    __block NSDictionary* userInfo = nil;
    [navigationAlertWrappers enumerateKeysAndObjectsUsingBlock:^(NSNumber * _Nonnull navigationAlertId, CPNavigationAlert * _Nonnull alert, BOOL * _Nonnull stop) {
        if (alert == navigationAlert) {
            userInfo = @{ @"navigationAlertId": navigationAlertId };
            *stop = YES;
        }
    }];
    
    if (userInfo == nil) {
        return @{ @"todo": @(YES) };
    }
    
    return userInfo;
}

- (void)sendTemplateEventWithName:(CPTemplate *)template name:(NSString*)name {
    [self sendTemplateEventWithName:template name:name json:@{}];
}

- (void)sendTemplateEventWithName:(CPTemplate *)template name:(NSString*)name json:(NSDictionary*)json {
    NSMutableDictionary *body = [[NSMutableDictionary alloc] initWithDictionary:json];
    NSDictionary *userInfo = [template userInfo];
    [body setObject:[userInfo objectForKey:@"templateId"] forKey:@"templateId"];
    if (hasListeners) {
        [self sendEventWithName:name body:body];
    }
}


# pragma MapTemplate

- (void)mapTemplate:(CPMapTemplate *)mapTemplate selectedPreviewForTrip:(CPTrip *)trip usingRouteChoice:(CPRouteChoice *)routeChoice {
    NSTimeInterval timeSinceShow = [[NSDate date] timeIntervalSinceDate:lastShowTripsTime];
    
    if (timeSinceShow < 0.2) {
        // Called too soon after showing trips — likely automatic
        NSLog(@"Ignoring automatic trip selection (%.3f seconds after showTrips)", timeSinceShow);
        return;
    }
    
    NSDictionary *userInfo = trip.userInfo;
    NSString *tripId = [userInfo valueForKey:@"id"];

    NSDictionary *routeUserInfo = routeChoice.userInfo;
    NSString *routeIndex = [routeUserInfo valueForKey:@"index"];
    [self sendTemplateEventWithName:mapTemplate name:@"selectedPreviewForTrip" json:@{ @"tripId": tripId, @"routeIndex": routeIndex}];
}

- (void)mapTemplate:(CPMapTemplate *)mapTemplate startedTrip:(CPTrip *)trip usingRouteChoice:(CPRouteChoice *)routeChoice {
    NSDictionary *userInfo = trip.userInfo;
    NSString *tripId = [userInfo valueForKey:@"id"];

    NSDictionary *routeUserInfo = routeChoice.userInfo;
    NSString *routeIndex = [routeUserInfo valueForKey:@"index"];

    [self sendTemplateEventWithName:mapTemplate name:@"startedTrip" json:@{ @"tripId": tripId, @"routeIndex": routeIndex}];
}

- (void)mapTemplateDidCancelNavigation:(CPMapTemplate *)mapTemplate {
    CPNavigationSession* navigationSession = [[RNCPStore sharedManager] getNavigationSession];
    if (navigationSession) {
        [navigationSession cancelTrip];
        [[RNCPStore sharedManager] setNavigationSession:nil];
    }
    [self sendTemplateEventWithName:mapTemplate name:@"didCancelNavigation"];
}

//- (BOOL)mapTemplate:(CPMapTemplate *)mapTemplate shouldShowNotificationForManeuver:(CPManeuver *)maneuver {
//    // @todo
//}
//- (BOOL)mapTemplate:(CPMapTemplate *)mapTemplate shouldUpdateNotificationForManeuver:(CPManeuver *)maneuver withTravelEstimates:(CPTravelEstimates *)travelEstimates {
//    // @todo
//}
//- (BOOL)mapTemplate:(CPMapTemplate *)mapTemplate shouldShowNotificationForNavigationAlert:(CPNavigationAlert *)navigationAlert {
//    // @todo
//}

- (BOOL)mapTemplateShouldProvideNavigationMetadata:(CPMapTemplate *)mapTemplate {
    return [[[NSBundle mainBundle] objectForInfoDictionaryKey:@"RNCPMapTemplateShouldProvideNavigationMetadata"] boolValue];
}

- (void)mapTemplate:(CPMapTemplate *)mapTemplate willShowNavigationAlert:(CPNavigationAlert *)navigationAlert {
    [self sendTemplateEventWithName:mapTemplate name:@"willShowNavigationAlert" json:[self navigationAlertToJson:navigationAlert]];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate didShowNavigationAlert:(CPNavigationAlert *)navigationAlert {
    [self sendTemplateEventWithName:mapTemplate name:@"didShowNavigationAlert" json:[self navigationAlertToJson:navigationAlert]];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate willDismissNavigationAlert:(CPNavigationAlert *)navigationAlert dismissalContext:(CPNavigationAlertDismissalContext)dismissalContext {
    [self sendTemplateEventWithName:mapTemplate name:@"willDismissNavigationAlert" json:[self navigationAlertToJson:navigationAlert dismissalContext:dismissalContext]];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate didDismissNavigationAlert:(CPNavigationAlert *)navigationAlert dismissalContext:(CPNavigationAlertDismissalContext)dismissalContext {
    [self sendTemplateEventWithName:mapTemplate name:@"didDismissNavigationAlert" json:[self navigationAlertToJson:navigationAlert dismissalContext:dismissalContext]];
    
    for (NSNumber *navigationAlertId in [navigationAlertWrappers allKeys]) {
        CPNavigationAlert *alert = navigationAlertWrappers[navigationAlertId];
        if (alert == navigationAlert) {
            [navigationAlertWrappers removeObjectForKey:navigationAlertId];
            break;
        }
    }
}

- (void)mapTemplateDidShowPanningInterface:(CPMapTemplate *)mapTemplate {
    [self sendTemplateEventWithName:mapTemplate name:@"didShowPanningInterface"];
}
- (void)mapTemplateWillDismissPanningInterface:(CPMapTemplate *)mapTemplate {
    [self sendTemplateEventWithName:mapTemplate name:@"willDismissPanningInterface"];
}
- (void)mapTemplateDidDismissPanningInterface:(CPMapTemplate *)mapTemplate {
    [self sendTemplateEventWithName:mapTemplate name:@"didDismissPanningInterface"];
}
- (void)mapTemplateDidBeginPanGesture:(CPMapTemplate *)mapTemplate {
    [self sendTemplateEventWithName:mapTemplate name:@"didBeginPanGesture"];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate panWithDirection:(CPPanDirection)direction {
    [self sendTemplateEventWithName:mapTemplate name:@"panWithDirection" json:@{ @"direction": [self panDirectionToString:direction] }];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate panBeganWithDirection:(CPPanDirection)direction {
    [self sendTemplateEventWithName:mapTemplate name:@"panBeganWithDirection" json:@{ @"direction": [self panDirectionToString:direction] }];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate panEndedWithDirection:(CPPanDirection)direction {
    [self sendTemplateEventWithName:mapTemplate name:@"panEndedWithDirection" json:@{ @"direction": [self panDirectionToString:direction] }];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate didEndPanGestureWithVelocity:(CGPoint)velocity {
    [self sendTemplateEventWithName:mapTemplate name:@"didEndPanGestureWithVelocity" json:@{ @"velocity": @{ @"x": @(velocity.x), @"y": @(velocity.y) }}];
}
- (void)mapTemplate:(CPMapTemplate *)mapTemplate didUpdatePanGestureWithTranslation:(CGPoint)translation velocity:(CGPoint)velocity {
    [self sendTemplateEventWithName:mapTemplate name:@"didUpdatePanGestureWithTranslation" json:@{ @"translation": @{ @"x": @(translation.x), @"y": @(translation.y) }, @"velocity": @{ @"x": @(velocity.x), @"y": @(velocity.y) }}];
}

- (CPManeuverDisplayStyle)mapTemplate:(CPMapTemplate *)mapTemplate displayStyleForManeuver:(CPManeuver *)maneuver {
    if(maneuver.instructionVariants.count == 0) {
        return CPManeuverDisplayStyleSymbolOnly;
    } else {
        return CPManeuverDisplayStyleDefault;
    }
}

# pragma SearchTemplate

- (void)searchTemplate:(CPSearchTemplate *)searchTemplate selectedResult:(CPListItem *)item completionHandler:(void (^)(void))completionHandler {
    NSNumber* index = [item.userInfo objectForKey:@"index"];
    NSString* _id = [item.userInfo objectForKey:@"id"];
    
    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    json[@"index"] = index;
    if (_id != nil) {
        json[@"id"] = _id;
    }
    
    [self sendTemplateEventWithName:searchTemplate name:@"selectedResult" json:json];
    self.selectedResultBlock = completionHandler;
}

- (void)searchTemplateSearchButtonPressed:(CPSearchTemplate *)searchTemplate {
    [self sendTemplateEventWithName:searchTemplate name:@"searchButtonPressed" json:@{ @"searchText": self->currentSearchText ?: @"" }];
}

- (void)searchTemplate:(CPSearchTemplate *)searchTemplate updatedSearchText:(NSString *)searchText completionHandler:(void (^)(NSArray<CPListItem *> * _Nonnull))completionHandler {
    self->currentSearchText = searchText;
    [self sendTemplateEventWithName:searchTemplate name:@"updatedSearchText" json:@{ @"searchText": searchText }];
    self.searchResultBlock = completionHandler;
}

# pragma ListTemplate

- (void)listTemplate:(CPListTemplate *)listTemplate didSelectListItem:(CPListItem *)item completionHandler:(void (^)(void))completionHandler {
    NSNumber* index = [item.userInfo objectForKey:@"index"];
    NSString* _id = [item.userInfo objectForKey:@"id"];
    
    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    json[@"index"] = index;
    if (_id != nil) {
        json[@"id"] = _id;
    }
    
    [self sendTemplateEventWithName:listTemplate name:@"didSelectListItem" json:json];
    self.selectedResultBlock = completionHandler;
}

# pragma TabBarTemplate
- (void)tabBarTemplate:(CPTabBarTemplate *)tabBarTemplate didSelectTemplate:(__kindof CPTemplate *)selectedTemplate {
    NSString* selectedTemplateId = [[selectedTemplate userInfo] objectForKey:@"templateId"];
    [self sendTemplateEventWithName:tabBarTemplate name:@"didSelectTemplate" json:@{@"selectedTemplateId":selectedTemplateId}];
}

# pragma PointOfInterest
-(void)pointOfInterestTemplate:(CPPointOfInterestTemplate *)pointOfInterestTemplate didChangeMapRegion:(MKCoordinateRegion)region {
    // noop
}

-(void)pointOfInterestTemplate:(CPPointOfInterestTemplate *)pointOfInterestTemplate didSelectPointOfInterest:(CPPointOfInterest *)pointOfInterest {
    [self sendTemplateEventWithName:pointOfInterestTemplate name:@"didSelectPointOfInterest" json:[pointOfInterest userInfo]];
}

# pragma NowPlaying

- (void)nowPlayingTemplateUpNextButtonTapped:(CPNowPlayingTemplate *)nowPlayingTemplate {
    [self sendTemplateEventWithName:nowPlayingTemplate name:@"upNextButtonPressed"];
}

- (void)nowPlayingTemplateAlbumArtistButtonTapped:(CPNowPlayingTemplate *)nowPlayingTemplate {
    [self sendTemplateEventWithName:nowPlayingTemplate name:@"albumArtistButtonPressed"];
}


# pragma Dashboard

+ (void) connectWithDashboardController:(CPDashboardController*)dashboardController window:(UIWindow*)window {
    RNCPStore *store = [RNCPStore sharedManager];
    if (store.dashboard == nil) {
        store.dashboard = [[RNCarPlayDashboard alloc] init];
    }
    [store.dashboard connectSceneWithDashboardController:dashboardController window:window];
}

+ (void) disconnectFromDashbaordController {
    RNCPStore *store = [RNCPStore sharedManager];
    [store.dashboard disconnect];
}

+ (void) dashboardStateChanged:(BOOL)isVisible {
    RNCPStore *store = [RNCPStore sharedManager];
    [store setVisibility:isVisible forScene:RNCarPlayConstants.SceneIdCarPlayDashbaord];
    [RNCarPlayUtils sendRNCarPlayEventWithName:RNCarPlayConstants.EventDashboardStateDidChange body:@{@"isVisible": @(isVisible)}];
}

RCT_EXPORT_METHOD(createDashboard:(NSString *)dashboardId config:(NSDictionary*)config) {
    RNCPStore *store = [RNCPStore sharedManager];
    if (store.dashboard == nil) {
        store.dashboard = [[RNCarPlayDashboard alloc] init];
    }
    [store.dashboard connectModuleWithBridge:self.bridge moduleName:dashboardId buttonConfig:config];
}

RCT_EXPORT_METHOD(checkForDashboardConnection) {
    RNCPStore *store = [RNCPStore sharedManager];
    if (((RNCarPlayDashboard *)store.dashboard).isConnected && hasListeners) {
        [self sendEventWithName:@"dashboardDidConnect" body:[store.dashboard getConnectedWindowInformation]];
    }
}

RCT_EXPORT_METHOD(updateDashboardShortcutButtons:(NSDictionary*)config) {
    RNCPStore *store = [RNCPStore sharedManager];
    [store.dashboard updateDashboardButtonsWithConfig:config];
}

# pragma Cluster

+ (void) connectWithInstrumentClusterController:(CPInstrumentClusterController *)instrumentClusterController contentStyle:(UIUserInterfaceStyle)contentStyle clusterId:(NSString *)clusterId API_AVAILABLE(ios(15.4)) {
    RNCPStore *store = [RNCPStore sharedManager];
    if ([store.cluster objectForKey:clusterId] == nil) {
        store.cluster[clusterId] = [[RNCarPlayCluster alloc] init];
    }
    RNCarPlayCluster *cluster = [store.cluster objectForKey:clusterId];
    [cluster connectWithInstrumentClusterController:instrumentClusterController contentStyle:contentStyle clusterId:clusterId];
}

+ (void) clusterContentStyleDidChange:(UIUserInterfaceStyle)contentStyle clusterId:(NSString *)clusterId API_AVAILABLE(ios(15.4)) {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayCluster *cluster = [store.cluster objectForKey:clusterId];
    cluster.contentStyle = contentStyle;
    [RNCarPlayUtils sendRNCarPlayEventWithName:@"clusterContentStyleDidChange" body:@{@"id": clusterId, @"contentStyle": @(contentStyle)}];
}

+ (void) disconnectFromInstrumentClusterController:(NSString *)clusterId API_AVAILABLE(ios(15.4)) {
    RNCPStore *store = [RNCPStore sharedManager];
    RNCarPlayCluster *cluster = [store.cluster objectForKey:clusterId];
    [cluster disconnect];
    [store.cluster removeObjectForKey:clusterId];
}

+ (void) clusterStateChanged:(NSString *)clusterId isVisible:(BOOL)isVisible {
    RNCPStore *store = [RNCPStore sharedManager];
    [store setVisibility:isVisible forScene:clusterId];
    [RNCarPlayUtils sendRNCarPlayEventWithName:RNCarPlayConstants.EventClusterStateDidChange body:@{@"id": clusterId, @"isVisible": @(isVisible)}];
}

RCT_EXPORT_METHOD(initCluster:(NSString *)clusterId config:(NSDictionary *)config) {
    RNCPStore *store = [RNCPStore sharedManager];
    [store.cluster[clusterId] connectWithBridge:self.bridge config:config];
}

RCT_EXPORT_METHOD(checkForClusterConnection:(NSString *)clusterId) {
    if (@available(iOS 15.4, *)) {
        RNCPStore *store = [RNCPStore sharedManager];
        RNCarPlayCluster *cluster = [store.cluster objectForKey:clusterId];
        if (cluster != nil && cluster.isConnected && hasListeners) {
            [self sendEventWithName:@"clusterWindowDidConnect" body:[cluster getConnectedWindowInformation]];
        }
    }
}

@end

/******************************************************************************
 *  ALPS, The Acoustic Location Processing System.
 *  ALPS.h
 *  Copyright (C) 2015, WiSE Lab, Carnegie Mellon University
 *  All rights reserved.
 *
 *  This software is the property of Carnegie Mellon University. Source may
 *  be modified, but this license does not allow distribution.  Binaries built
 *  for research purposes may be freely distributed, but must acknowledge
 *  Carnegie Mellon University.  No other use or distribution can be made
 *  without the written permission of the authors and Carnegie Mellon
 *  University.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  Contributing Author(s):
 *  Patrick Lazik
 *
 *******************************************************************************/

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import "SRWebSocket.h"

//! Project version number for ALPS.
FOUNDATION_EXPORT double ALPSVersionNumber;

//! Project version string for ALPS.
FOUNDATION_EXPORT const unsigned char ALPSVersionString[];

#define ALPS_OK 0
#define ALPS_ERROR -1
#define BUFFER_LENGTH 96256
#define TDMA_SLOTS 8

#define DEFAULT_ALPS_THRESHOLD_DIVISOR 5.0f
#define DEFAULT_ALPS_THRESHOLD_MULTIPLIER 30.0f
#define DEFAULT_ALPS_THRESHOLD 0.15f

#define DEFAULT_ALPS_WEBSOCKET @"ws://alpssolver.duckdns.org:30005" // Default websocket address
#define WEBSOCKET_RECONNECT_TIMEOUT 3 // Websocket reconnect timeout in seconds
#define LOCAITON_REPEAT_TIME 0.5f //seconds

@protocol ALPSDelegate <NSObject>
- (void)ALPSDidReceiveData:(NSDictionary*) data;
@end

@interface ALPS : NSObject<SRWebSocketDelegate>

@property(readonly) BOOL running;
@property(nonatomic) float thresholdDivisor;
@property(nonatomic) float thresholdMultiplier;
@property(nonatomic) float threshold;
@property (nonatomic, weak) id <ALPSDelegate> delegate;
@property (copy) void (^callback)(NSDictionary*);

- (id) init;
- (id) initWithCallback:(void (^)(NSDictionary*))callback;
- (void) runDemo;
- (void)stopDemo;
- (void) checkStatus:(OSStatus)status :(NSString*)errMsg;
- (void)start:(NSError*) error;
- (void)stop:(NSError*) error;
- (int) writeWav:(NSString*) wavName;
- (NSData*) toaAndRSSIToJSON:(NSArray*) toaData :(NSArray*) rssiData;
- (NSString*) toaToTDOAString:(NSArray*) toaData;
- (NSDictionary*) jsonPositionToDictionary:(NSString*) position;

@end





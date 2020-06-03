//
//  PassportRecognizerDelegate.h
//  MicroBlinkIDPluginTest
//
//  Created by Pedro Remedios on 01/06/2020.
//

#ifndef PassportRecognizerDelegate_h
#define PassportRecognizerDelegate_h

#import <MicroBlink/MicroBlink.h>
#import <Cordova/CDVPlugin.h>

@interface PassportRecognizerDelegate : NSObject <MBBlinkIdOverlayViewControllerDelegate>

@property (nonatomic, strong) CDVInvokedUrlCommand *command;
@property (nonatomic, weak) id <CDVCommandDelegate> commandDelegate;

- (id)initWithRecognizer:(MBPassportRecognizer *)recognizer viewController:(UIViewController *)vc;

@end

#endif /* PassportRecognizerDelegate_h */

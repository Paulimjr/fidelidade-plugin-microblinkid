//
//  MobileECTPlugin.m
//  OutSystems - Mobility Experts
//
//  Created by Vitor Oliveira on 14/010/15.
//
//

#import "BlinkIdPlugin.h"
#import "PassportRecognizerDelegate.h"

@interface BlinkIdPlugin() <MBBlinkIdCombinedRecognizerDelegate>

@property (nonatomic, strong) MBBlinkIdCombinedRecognizer *blinkIDCombinedRecognizer;
@property (nonatomic, strong) MBPassportRecognizer *passportRecognizer;
@property (nonatomic, strong) PassportRecognizerDelegate *passportRecognizerDelegate;
@property (nonatomic, strong) MBUsdlRecognizer *usdlRecognizer;

@property (nonatomic, strong) MBRecognizerCollection *recognizerCollection;

@property (nonatomic, strong) CDVInvokedUrlCommand *command;
@property (nonatomic, strong) NSString *faceImageBase64;

@property (nonatomic, assign) BOOL sdkInitialized;

@end

@implementation BlinkIdPlugin

- (void)initializeSdk:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult = nil;
    NSString *licenseKey = nil;
    
    self.sdkInitialized = NO;
    
    if (command.arguments.count != 1) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"A license key must be provided to use this plugin"];
    }
    
    if (!command.arguments[0][@"ios"]) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_JSON_EXCEPTION messageAsString:@"No iOS license key found in parameter"];
    }
    
    if (pluginResult == nil) {
        licenseKey = command.arguments[0][@"ios"];
        [[MBMicroblinkSDK sharedInstance] setLicenseKey:licenseKey];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        self.sdkInitialized = YES;
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) scanIdCard:(CDVInvokedUrlCommand*)command {
	CDVPluginResult* pluginResult;
    
    if (!self.sdkInitialized) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"Please initialize the SDK before using this action"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    self.command = command;
    
    self.faceImageBase64 = @"";

	MBBlinkIdOverlaySettings *blinkIDOverlaySettings = [MBBlinkIdOverlaySettings new];

    self.blinkIDCombinedRecognizer = [MBBlinkIdCombinedRecognizer new];
    self.blinkIDCombinedRecognizer.returnFullDocumentImage = YES;
    self.blinkIDCombinedRecognizer.returnFaceImage = YES;
    
    self.recognizerCollection = [[MBRecognizerCollection alloc] initWithRecognizers:@[self.blinkIDCombinedRecognizer]];

    MBBlinkIdOverlayViewController * overlayVC = [[MBBlinkIdOverlayViewController alloc] initWithSettings:blinkIDOverlaySettings recognizerCollection:self.recognizerCollection delegate:self];
    UIViewController<MBRecognizerRunnerViewController> *recognizerRunnerViewController = [MBViewControllerFactory recognizerRunnerViewControllerWithOverlayViewController: overlayVC];

    /** You can use other presentation methods as well */
    [self.viewController presentViewController:recognizerRunnerViewController animated:YES completion:nil];
}

- (void) scanPassport:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult;
    
    if (!self.sdkInitialized) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"Please initialize the SDK before using this action"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    self.command = command;
    
    self.faceImageBase64 = @"";
    
    self.passportRecognizer = [MBPassportRecognizer new];
    self.passportRecognizer.returnFaceImage = YES;
    self.passportRecognizer.returnFullDocumentImage = YES;
    
    self.recognizerCollection = [[MBRecognizerCollection alloc] initWithRecognizers:@[self.passportRecognizer]];
    
    self.passportRecognizerDelegate = [[PassportRecognizerDelegate alloc] initWithRecognizer:self.passportRecognizer viewController:self.viewController];
    self.passportRecognizerDelegate.command = command;
    self.passportRecognizerDelegate.commandDelegate = self.commandDelegate;
    
    MBBlinkIdOverlaySettings *blinkIDOverlaySettings = [MBBlinkIdOverlaySettings new];
    
    MBBlinkIdOverlayViewController * overlayVC = [[MBBlinkIdOverlayViewController alloc] initWithSettings:blinkIDOverlaySettings recognizerCollection:self.recognizerCollection delegate:self.passportRecognizerDelegate];
    UIViewController<MBRecognizerRunnerViewController> *recognizerRunnerViewController = [MBViewControllerFactory recognizerRunnerViewControllerWithOverlayViewController: overlayVC];

    /** You can use other presentation methods as well */
    [self.viewController presentViewController:recognizerRunnerViewController animated:YES completion:nil];

}

- (void)blinkIdOverlayViewControllerDidFinishScanningFirstSide:(MBBlinkIdOverlayViewController *)blinkIdOverlayViewController {
    if (self.blinkIDCombinedRecognizer.result.faceImage != nil) {
        self.faceImageBase64 = [UIImagePNGRepresentation(self.blinkIDCombinedRecognizer.result.faceImage.image) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
    }
}

-(void)blinkIdOverlayViewControllerDidFinishScanning:(nonnull MBBlinkIdOverlayViewController *)blinkIdOverlayViewController state:(MBRecognizerResultState)state {
    CDVPluginResult *pluginResult = nil;
    
    NSString __block *frontImageBase64 = @"";
    NSString __block *backImageBase64 = @"";
    
    if (state == MBRecognizerResultStateValid) {
        [blinkIdOverlayViewController.recognizerRunnerViewController pauseScanning];
        
        MBMrzResult *results = self.blinkIDCombinedRecognizer.result.mrzResult;
        results = self.passportRecognizer.result.mrzResult;
        
//        NSLog(@"Back: %@", self.blinkIDCombinedRecognizer.result.fullDocumentBackImage.image);
//        NSLog(@"Front: %@", self.blinkIDCombinedRecognizer.result.fullDocumentFrontImage.image);
//        NSLog(@"Face: %@", self.blinkIDCombinedRecognizer.result.faceImage.image);
        
        if (self.blinkIDCombinedRecognizer.result.fullDocumentFrontImage != nil) {
            frontImageBase64 = [UIImagePNGRepresentation(self.blinkIDCombinedRecognizer.result.fullDocumentFrontImage.image) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
        }
        
        if (self.blinkIDCombinedRecognizer.result.fullDocumentBackImage != nil) {
            backImageBase64 = [UIImagePNGRepresentation(self.blinkIDCombinedRecognizer.result.fullDocumentBackImage.image) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
        }
        
        NSDictionary *IDCardData = [NSDictionary dictionaryWithObjectsAndKeys:  @(results.isParsed), @"isParsed",
                                                                                results.issuer, @"issuer",
                                                                                results.documentNumber, @"documentNumber",
                                                                                results.documentCode, @"documentCode",
                                                                                [self MBDateResultToOutSystems:results.dateOfExpiry], @"dateOfExpiry",
                                                                                results.primaryID, @"primaryId",
                                                                                results.secondaryID, @"secondaryId",
                                                                                [self MBDateResultToOutSystems:results.dateOfBirth], @"dataOfBirth",
                                                                                results.nationality, @"nationality",
                                                                                results.gender, @"sex",
                                                                                results.opt1, @"opt1",
                                                                                results.opt2, @"opt2",
                                                                                results.mrzText, @"mrzText",
                                                                                frontImageBase64, @"frontPhoto",
                                                                                backImageBase64, @"backPhoto",
                                                                                self.faceImageBase64, @"facePhoto",
                                                                                nil];
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:IDCardData];
        
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.viewController dismissViewControllerAnimated:YES completion:nil];
        });
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
}

- (void)blinkIdOverlayViewControllerDidTapClose:(nonnull MBBlinkIdOverlayViewController *)blinkIdOverlayViewController {
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
    });
}

- (void)documentOverlayViewControllerDidFinishScanning:(MBDocumentOverlayViewController *)documentOverlayViewController state:(MBRecognizerResultState)state {
    [documentOverlayViewController.recognizerRunnerViewController pauseScanning];
	dispatch_async(dispatch_get_main_queue(), ^{
		CDVPluginResult* pluginResult;
		
		if (state == MBRecognizerResultStateValid) {
			MBMrzResult *results = self.blinkIDCombinedRecognizer.result.mrzResult;
			[self.viewController dismissViewControllerAnimated:YES completion:nil];
			NSDictionary *IDCardData = [NSDictionary dictionaryWithObjectsAndKeys:
										@(results.isParsed), @"isParsed",
										results.issuer, @"issuer",
										results.documentNumber, @"documentNumber",
										results.documentCode, @"documentCode",
										[self MBDateResultToOutSystems:results.dateOfExpiry], @"dateOfExpiry",
										results.primaryID, @"primaryId",
										results.secondaryID, @"secondaryId",
										[self MBDateResultToOutSystems:results.dateOfBirth], @"dataOfBirth",
										results.nationality, @"nationality",
										results.gender, @"sex",
										results.opt1, @"opt1",
										results.opt2, @"opt2",
										results.mrzText, @"mrzText",
										nil];
			NSLog(@"%@", self.blinkIDCombinedRecognizer.result);
            NSLog(@"%@", self.blinkIDCombinedRecognizer.result.fullDocumentBackImage);
            NSLog(@"%@", self.blinkIDCombinedRecognizer.result.fullDocumentFrontImage);
            NSLog(@"%@", self.blinkIDCombinedRecognizer.result.faceImage);
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:IDCardData];
		} else {
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
		}
		
		[self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
	});
}

- (NSString *)MBDateResultToOutSystems:(MBDateResult *)dateResult {
	NSNumberFormatter *numberFormatter = [[NSNumberFormatter alloc] init];
	numberFormatter.paddingPosition = NSNumberFormatterPadBeforeSuffix;
	numberFormatter.paddingCharacter = @"0";
	numberFormatter.minimumIntegerDigits = 2;
	
	NSString *month = [numberFormatter stringFromNumber:@(dateResult.month)];
	NSString *day = [numberFormatter stringFromNumber:@(dateResult.day)];
	
	return [NSString stringWithFormat:@"%ld-%@-%@", (long)dateResult.year, month, day];
}

- (void)documentOverlayViewControllerDidTapClose:(nonnull MBDocumentOverlayViewController *)documentOverlayViewController {
	[self.viewController dismissViewControllerAnimated:YES completion:nil];
}

//- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
//    [self.viewController dismissViewControllerAnimated:YES completion:nil];
//}

@end

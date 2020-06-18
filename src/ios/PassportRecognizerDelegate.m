//
//  PassportRecognizerDelegate.m
//  MicroBlinkIDPluginTest
//
//  Created by Pedro Remedios on 01/06/2020.
//

#import <Foundation/Foundation.h>

#import "PassportRecognizerDelegate.h"

@interface PassportRecognizerDelegate ()

@property (nonatomic, strong) MBPassportRecognizer *recognizer;
@property (nonatomic, strong) UIViewController *viewController;

@property (nonatomic, strong) NSString *faceImageBase64;
@property (nonatomic, strong) NSString *fullDocumentImageBase64;

@end

@implementation PassportRecognizerDelegate

- (id)initWithRecognizer:(MBPassportRecognizer *)recognizer viewController:(UIViewController *)vc {
    if (!(self = [super init])) {
        return nil;
    }
    
    self.recognizer = recognizer;
    self.viewController = vc;
    
    return self;
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

- (void)blinkIdOverlayViewControllerDidFinishScanning:(nonnull MBBlinkIdOverlayViewController *)blinkIdOverlayViewController state:(MBRecognizerResultState)state {
    
    [blinkIdOverlayViewController.recognizerRunnerViewController pauseScanning];
    
//    NSLog(@"%@", self.recognizer.result.mrzResult);
    
    if (self.recognizer.result.faceImage != nil) {
//        NSLog(@"Passport face image: %@", [UIImagePNGRepresentation(self.recognizer.result.faceImage.image) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength]);
        self.faceImageBase64 = [UIImagePNGRepresentation(self.recognizer.result.faceImage.image) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
    }
    
    if (self.recognizer.result.fullDocumentImage != nil) {
//        NSLog(@"Passport full image: %@", [UIImagePNGRepresentation(self.recognizer.result.fullDocumentImage.image) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength]);
        self.fullDocumentImageBase64 = [UIImagePNGRepresentation(self.recognizer.result.fullDocumentImage.image) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
    }
    
    MBMrzResult *results = self.recognizer.result.mrzResult;
    
    NSDictionary *passportData = [NSDictionary dictionaryWithObjectsAndKeys:  @(results.isParsed), @"isParsed",
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
    self.fullDocumentImageBase64, @"frontPhoto",
    self.faceImageBase64, @"facePhoto",
    nil];

    NSError * err;
    NSData * jsonData = [NSJSONSerialization  dataWithJSONObject:passportData options:0 error:&err];
    NSString * passportDataString = [[NSString alloc] initWithData:jsonData   encoding:NSUTF8StringEncoding];
    
    CDVPluginResult __block *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:passportDataString];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
    });
}

- (void)blinkIdOverlayViewControllerDidTapClose:(nonnull MBBlinkIdOverlayViewController *)blinkIdOverlayViewController {
    [self.viewController dismissViewControllerAnimated:YES completion:nil];
}

@end

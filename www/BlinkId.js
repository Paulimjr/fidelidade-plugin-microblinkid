/** 
    Innovagency - Team Mobile
    Paulo Cesar & Pedro Remedios
*/
function BlinkId() {
}

/**
 * Initialize the SDK
 */
exports.initializeSdk = function (successCallback, errorCallback, licenseKey) {
    cordova.exec(successCallback, errorCallback, "BlinkIdPlugin", "initializeSdk", [licenseKey]);
};

/**
 * Scan ID Card
 */
exports.scanIdCard = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "BlinkIdPlugin", "scanIdCard");
};

/**
 * Scan passport
 */
exports.scanPassport = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "BlinkIdPlugin", "scanPassport");
};
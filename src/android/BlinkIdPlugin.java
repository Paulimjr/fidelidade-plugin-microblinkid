package com.fidelidade.blinkid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.diogolopes.outsystemscloud.docscan.R;
import com.microblink.MicroblinkSDK;
import com.microblink.entities.recognizers.Recognizer;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdCombinedRecognizer;
import com.microblink.entities.recognizers.blinkid.generic.classinfo.ClassInfo;
import com.microblink.entities.recognizers.blinkid.imageoptions.FaceImageOptions;
import com.microblink.entities.recognizers.blinkid.imageoptions.FullDocumentImageOptions;
import com.microblink.entities.recognizers.blinkid.imageresult.CombinedFullDocumentImageResult;
import com.microblink.entities.recognizers.blinkid.imageresult.FaceImageResult;
import com.microblink.entities.recognizers.blinkid.imageresult.FullDocumentImageResult;
import com.microblink.entities.recognizers.blinkid.mrtd.MrtdDocumentType;
import com.microblink.entities.recognizers.blinkid.mrtd.MrtdRecognizer;
import com.microblink.entities.recognizers.blinkid.mrtd.MrzResult;
import com.microblink.entities.recognizers.blinkid.passport.PassportRecognizer;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.image.Image;
import com.microblink.intent.IntentDataTransferMode;
import com.microblink.results.date.DateResult;
import com.microblink.uisettings.ActivityRunner;
import com.microblink.uisettings.BlinkIdUISettings;
import com.microblink.uisettings.DocumentUISettings;
import com.microblink.uisettings.UISettings;
import com.microblink.uisettings.options.BeepSoundUIOptions;
import com.microblink.uisettings.options.OcrResultDisplayMode;
import com.microblink.uisettings.options.OcrResultDisplayUIOptions;
import com.microblink.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by pcamilo on 27/05/2020
 */
public class BlinkIdPlugin extends CordovaPlugin {

    private PassportRecognizer mRecognizerPassport;
    private BlinkIdCombinedRecognizer mRecognizerDocumentId;
    private RecognizerBundle mRecognizerBundle;
    public static final int MY_REQUEST_CODE = 123;

    /* Cordova Plugin - Action to use this plugin Cards */
    private static final String ACTION_INITIALIZE_SDK = "initializeSdk";
    private static final String ACTION_SCAN_ID_CARD = "scanIdCard";
    private static final String ACTION_SCAN_PASSPORT = "scanPassport";
    private CallbackContext callbackContext;
    private MrtdRecognizer mMRTDRecognizer;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        switch (action) {
            case ACTION_INITIALIZE_SDK: {
                initializeSdk(args);
                break;
            }
            case ACTION_SCAN_ID_CARD: {
                scanDocumentId();
                break;
            }
            case ACTION_SCAN_PASSPORT: {
                scanPassport();
                break;
            }

        }
        return true;
    }

    private void initializeSdk(JSONArray args) throws JSONException {
        if (args != null) {
            /* Get the license key from cordova */
            String licenceKeY = args.getString(0);

            if (licenceKeY == null) {
                callbackContext.error("Is mandatory a license key to use the this plugin");
            } else {
                MicroblinkSDK.setLicenseKey(licenceKeY, this.cordova.getContext());
                MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.PERSISTED_OPTIMISED);
                callbackContext.success();
            }
        }
    }

    /**
     * Scan document id
     */
    private void scanDocumentId() {
        mRecognizerDocumentId = new BlinkIdCombinedRecognizer();
        mRecognizerDocumentId.setReturnFullDocumentImage(true);
        mRecognizerDocumentId.setReturnFaceImage(true);

        mRecognizerBundle = new RecognizerBundle(mRecognizerDocumentId);
        startScanning();
    }

    /**
     * Scan passport
     */
    public void scanPassport() {
        mRecognizerPassport = new PassportRecognizer();
        mRecognizerPassport.setReturnFaceImage(true);
        mRecognizerPassport.setReturnFullDocumentImage(true);

        ImageSettings.enableAllImages(mRecognizerPassport);
        scanAction(new DocumentUISettings(prepareRecognizerBundle(mRecognizerPassport)));
    }

    private RecognizerBundle prepareRecognizerBundle(@NonNull Recognizer<?>... recognizers ) {
        this.mRecognizerBundle = new RecognizerBundle(recognizers);
        return this.mRecognizerBundle;
    }

    private void scanAction(@NonNull UISettings activitySettings) {
        scanAction(activitySettings, null);
    }

    private void scanAction(@NonNull UISettings activitySettings, @Nullable Intent helpIntent) {
        setupActivitySettings(activitySettings, helpIntent);
        this.cordova.setActivityResultCallback(this);
        ActivityRunner.startActivityForResult(this.cordova.getActivity(), MY_REQUEST_CODE, activitySettings);
    }

    private void setupActivitySettings(@NonNull UISettings settings, @Nullable Intent helpIntent) {
        if (settings instanceof OcrResultDisplayUIOptions) {
            ((OcrResultDisplayUIOptions) settings).setOcrResultDisplayMode(OcrResultDisplayMode.ANIMATED_DOTS);
        }
    }

    /**
     * Scan scanning to use in recognizer bundle
     */
    private void startScanning() {
        BlinkIdUISettings settings = new BlinkIdUISettings(mRecognizerBundle);
        this.cordova.setActivityResultCallback(this);
        ActivityRunner.startActivityForResult(this.cordova.getActivity(), MY_REQUEST_CODE, settings);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                mRecognizerBundle.loadFromIntent(data);

                if (mRecognizerPassport != null) {
                    // Result Passport
                    PassportRecognizer.Result passportResult = mRecognizerPassport.getResult();
                    if (passportResult.getResultState() == Recognizer.Result.State.Valid) {
                        resultPassport(mRecognizerPassport.getResult(), passportResult);
                    } else {
                        Toast.makeText(cordova.getContext(), "Invalid document!", Toast.LENGTH_SHORT).show();
                    }
                }

                if (mRecognizerDocumentId != null) {
                    // Result Document ID
                    BlinkIdCombinedRecognizer.Result documentIdResult = mRecognizerDocumentId.getResult();
                    if (documentIdResult.getResultState() == Recognizer.Result.State.Valid) {
                        resultDocumentId(mRecognizerDocumentId.getResult(), documentIdResult);
                    } else {
                        Toast.makeText(cordova.getContext(), "Invalid document!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            Toast.makeText(cordova.getContext(), "Scanner cancelled!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Result Passport Identification
     */
    private void resultPassport(Recognizer.Result result, PassportRecognizer.Result mrzResult) {
        JSONObject jsonObject = new JSONObject();

        try {
            this.extractMRZResult(jsonObject, mrzResult.getMrzResult());

            if (result != null) {
                this.setFrontImage(jsonObject, result);

                if(result instanceof FullDocumentImageResult) {
                    FullDocumentImageResult imageResult = (FullDocumentImageResult) result;

                    if (imageResult.getFullDocumentImage() != null) {
                        jsonObject.put("fullDocumentImage", convertToBitmap(imageResult.getFullDocumentImage()));
                    }
                }
            }

            this.callbackContext.success(jsonObject.toString());

        } catch (JSONException e) {
            Log.e("MicroBlink", e.getMessage());
            this.callbackContext.error(e.getMessage());
        }

        resetRecognizers();
    }

    /**
     * Result Document Identification
     */
    private void resultDocumentId(Recognizer.Result result, BlinkIdCombinedRecognizer.Result mrzResult) {

        JSONObject jsonObject = new JSONObject();
        try {
            this.extractDocumentId(jsonObject, mrzResult);

            if (result != null) {
                this.setFrontImage(jsonObject, result);

                if(result instanceof CombinedFullDocumentImageResult) {
                    CombinedFullDocumentImageResult imageResult = (CombinedFullDocumentImageResult) result;

                    if (imageResult.getFullDocumentFrontImage() != null) {
                        jsonObject.put("imageFront", convertToBitmap(imageResult.getFullDocumentFrontImage()));
                    }

                    if (imageResult.getFullDocumentBackImage() != null) {
                        jsonObject.put("imageBack", convertToBitmap(imageResult.getFullDocumentBackImage()));
                    }
                }
            }

            this.callbackContext.success(jsonObject.toString());

        } catch (JSONException e) {
            Log.e("MicroBlink", e.getMessage());
            this.callbackContext.error(e.getMessage());
        }

        resetRecognizers();
    }

    private void resetRecognizers() {
        this.mRecognizerDocumentId = null;
        this.mRecognizerPassport = null;
    }

    /**
     * Get the front image
     *
     * @param jsonObject the jsonObject
     * @param result the result
     * @throws JSONException
     */
    private void setFrontImage(JSONObject jsonObject, Recognizer.Result result) throws JSONException {
        // Face image
        if(result instanceof FaceImageResult) {
            Image imageFace = ((FaceImageResult) result).getFaceImage();
            if (imageFace  != null) {
                jsonObject.put("faceImage", convertToBitmap(imageFace));
            }
        }
    }


    /**
     * Extract document ID
     * @param jsonObject the jsonObject
     * @param blinkId the mrzResult
     * @throws JSONException
     */
    private void extractDocumentId(JSONObject jsonObject, BlinkIdCombinedRecognizer.Result blinkId) throws JSONException {
        jsonObject.put("firstName", blinkId.getFirstName());
        jsonObject.put("lastName", blinkId.getLastName());
        jsonObject.put("sex", blinkId.getSex());
        jsonObject.put("documentNumber", blinkId.getDocumentNumber());
        jsonObject.put("dateOfExpiry", dateFormatted(blinkId.getDateOfExpiry()));
        jsonObject.put("dateOfExpiryPermanent", blinkId.isDateOfExpiryPermanent());
        jsonObject.put("address", blinkId.getAddress());
        jsonObject.put("additionalAddressInformation", blinkId.getAdditionalAddressInformation());
        jsonObject.put("dateOfBirth", dateFormatted(blinkId.getDateOfBirth()));
        jsonObject.put("placeOfBirth", blinkId.getPlaceOfBirth());
        jsonObject.put("nationality", blinkId.getNationality());
        jsonObject.put("race", blinkId.getRace());
        jsonObject.put("religion", blinkId.getReligion());
        jsonObject.put("maritalStatus", blinkId.getMaritalStatus());
        jsonObject.put("residentialStatus", blinkId.getResidentialStatus());
        jsonObject.put("employer", blinkId.getEmployer());
        jsonObject.put("personalNumber", blinkId.getPersonalIdNumber());
        jsonObject.put("documentAdditionalNumber", blinkId.getDocumentAdditionalNumber());
        jsonObject.put("issuingAuthority", blinkId.getIssuingAuthority());
        jsonObject.put("conditions", blinkId.getConditions());

        ClassInfo classInfo = blinkId.getClassInfo();
        jsonObject.put("country", classInfo.getCountry().name());
        jsonObject.put("region", classInfo.getRegion().name());
        jsonObject.put("type", classInfo.getType().name());

        if (blinkId.getMrzResult().isMrzParsed()) {
           this.extractMRZResult(jsonObject, blinkId.getMrzResult());
        }
    }

    /**
     * Extract MRZ Result
     *
     * @param jsonObject the jsonObject
     * @param mrzResult the mrzResult
     * @throws JSONException
     */
    protected void extractMRZResult(JSONObject jsonObject, MrzResult mrzResult) throws JSONException {
        jsonObject.put("documentType", mrzResult.getDocumentType());
        jsonObject.put("mrzVerified", mrzResult.isMrzVerified());
        jsonObject.put("isParsed", mrzResult.isMrzParsed());
        jsonObject.put("issuer", mrzResult.getIssuer());
        jsonObject.put("documentNumber", mrzResult.getDocumentNumber());
        jsonObject.put("documentCode", mrzResult.getDocumentCode());
        jsonObject.put("dateOfExpiry", dateFormatted(mrzResult.getDateOfExpiry()));
        jsonObject.put("primaryId", mrzResult.getPrimaryId());
        jsonObject.put("secondaryId", mrzResult.getSecondaryId());
        jsonObject.put("dateOfBirth", dateFormatted(mrzResult.getDateOfBirth()));
        jsonObject.put("nationality", mrzResult.getNationalityName());
        jsonObject.put("nationalityCode", mrzResult.getSanitizedNationality());
        jsonObject.put("sex", mrzResult.getGender());
        jsonObject.put("opt1", mrzResult.getOpt1());
        jsonObject.put("opt2", mrzResult.getOpt2());
        jsonObject.put("mrzText", mrzResult.getMrzText());

        int age = mrzResult.getAge();
        if (age != -1) {
            jsonObject.put("age", age);
        }

        jsonObject.put("sanitizedDocumentNumber",  mrzResult.getSanitizedDocumentNumber());
        jsonObject.put("sanitizedOpt1", mrzResult.getSanitizedOpt1());
    }


    /**
     * Convert bitmap to base64 string
     *
     * @param image the image
     * @return the base64 string
     */
    private String convertToBitmap(Image image) {
        Bitmap imageBitmap =  buildImage(image);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * Get the date from DateResult
     *
     * @param value the date unformatted
     * @return the date correct formatted
     */
    private String dateFormatted(DateResult value) {
        String valueFormatted = "";
        if (value != null && value.getDate() != null) {
            String[] val = value.getDate().toString().split("[.]");
            return val[2] + "-" + val[1] + "-" + val[0];
        }
        return valueFormatted;
    }

    /**
     * Build image to convert to Bitmap
     * @param value the image
     * @return the bitmap
     */
    private Bitmap buildImage(Image value) {
        Bitmap img = value.convertToBitmap();
        if ( img != null && value.getImageOrientation() != Orientation.ORIENTATION_UNKNOWN ) {

            boolean needTransform = false;

            // matrix for transforming the image
            Matrix matrix = new Matrix();
            int newWidth = img.getWidth();
            int newHeight = img.getHeight();

            if ( value.getImageOrientation() != Orientation.ORIENTATION_LANDSCAPE_RIGHT ) {
                needTransform = true;
                float pX = newWidth / 2.f;
                float pY = newWidth / 2.f;

                // rotate image and rescale image
                if (value.getImageOrientation() == Orientation.ORIENTATION_LANDSCAPE_LEFT) {
                    matrix.postRotate(180.f, pX, pY);
                } else {
                    if (value.getImageOrientation() == Orientation.ORIENTATION_PORTRAIT) {
                        matrix.postRotate(90.f, pX, pY);
                    } else if (value.getImageOrientation() == Orientation.ORIENTATION_PORTRAIT_UPSIDE) {
                        matrix.postRotate(270.f, pX, pY);
                    }
                }
            }

            // if image is too large, scale it down so it can be displayed in image view
            int maxDimension = Math.max(newWidth, newHeight);
            final int maxAllowedDimension = 1920;
            if (maxDimension > maxAllowedDimension) {
                needTransform = true;
                float scale = (float) maxAllowedDimension / maxDimension;
                matrix.postScale(scale, scale);
            }
            if (needTransform) {
                img = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, false);
            }
        }
        return img;
    }

    static class ImageSettings {

        public static Recognizer enableAllImages(Recognizer recognizer) {
            if(recognizer instanceof FullDocumentImageOptions) {
                FullDocumentImageOptions options = (FullDocumentImageOptions) recognizer;
                options.setReturnFullDocumentImage(true);
            }
            if(recognizer instanceof FaceImageOptions) {
                FaceImageOptions options = (FaceImageOptions) recognizer;
                options.setReturnFaceImage(true);
            }
            return recognizer;
        }
    }
}

package com.example.snapchatclone.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

@Deprecated
public class CameraOperationManager {

    private static final int NEW_SURFACE = 234;
    public static final int IMAGE_OUTPUT_SIZE = 0;
    public static final int CAMERA_CODE = 99;

    private static final String TAG = "CameraOperationManager";

    static final String BACK_CAMERA = "BACK_CAMERA";
    static final String FRONT_CAMERA = "FRONT_CAMERA";
    private final ImageReceiver mImageReceiver;

    private ArrayList<Surface> mCameraOutputSurfaceList;
    private CameraCharacteristics mCameraCharacteristics;
    /*private Range<Long> exposureTimeRanges;
    private Range<Integer> sensorSensitivityRanges;
    private long exposure_time = 20000000;//smaller exposure time means more images per second but also darker
    private int sensorSensitivity = 1500;*/
    private CaptureRequest mSingleCaptureRequest;

    public interface ImageReceiver{
        void OnReceiveImage(Image image);
    }

    public int getCameraOrientation() {
        int rotation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        switch(rotation){
            case 0:
                return Surface.ROTATION_0;
            case 90:
                return Surface.ROTATION_90;
            case 180:
                return Surface.ROTATION_180;
            case 270:
                return Surface.ROTATION_270;
        }
        return -1;
    }

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "Camera Opened!");
            mCameraDevice = camera;
            //no preview surface yet so wait
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
            Log.i(TAG,"Camera Closed!");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            switch (error){
                case ERROR_CAMERA_DEVICE:
                    Log.i(TAG,"ERROR_CAMERA_DEVICE!");
                    break;
                case ERROR_CAMERA_SERVICE:
                    Log.i(TAG,"ERROR_CAMERA_SERVICE!");
                    break;
                case ERROR_CAMERA_DISABLED:
                    Log.i(TAG,"ERROR_CAMERA_DISABLED!");
                    break;
                case ERROR_CAMERA_IN_USE:
                    Log.i(TAG,"ERROR_CAMERA_IN_USE!");
                    break;
                case ERROR_MAX_CAMERAS_IN_USE:
                    Log.i(TAG,"ERROR_MAX_CAMERAS_IN_USE!");
                    break;
            }
        }
    };
    private CameraCaptureSession.StateCallback mCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCameraCaptureSession = session;
            Log.i(TAG, "CaptureSession.onConfigured!");
            startCameraPreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //Log.i(TAG,"exp_time_used!!!!!=>"+result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
            //Log.i(TAG,"sensor_sensitivity!!!=>"+result.get(CaptureResult.SENSOR_SENSITIVITY));
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            System.out.println("onCaptureFailed!!!");
        }
    };
    private CameraCaptureSession.CaptureCallback mSingleCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            startCameraPreview();//restart camera preview
        }
    };

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    public void setImageOutputSize(Size size) {
        mHandler.sendMessage(mHandler.obtainMessage(IMAGE_OUTPUT_SIZE, size));
        //Log.i(TAG,"GIVEN SIZE => "+size.toString());
    }

    private class CameraHandler extends Handler{
        public CameraHandler(@NonNull Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == NEW_SURFACE){
                //handle getting a new surface
                Surface surface = (Surface)msg.obj;
                if(!mCameraOutputSurfaceList.contains(surface)) {
                    Log.i(TAG,"Adding surface...");
                    mCameraOutputSurfaceList.add(surface);
                    if(mCameraOutputSurfaceList.size() == 2){
                        try {
                            mCameraDevice.createCaptureSession(mCameraOutputSurfaceList, mCaptureSessionStateCallback, mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                            //sometimes onpause gets called and the camera is closed before configuration
                            //so we need to not crash here do what nullify and empty stuff or what?
                            //
                        }
                    }

                }
            }else if(msg.what == IMAGE_OUTPUT_SIZE){
                Size size = (Size)msg.obj;
                mImageReader = ImageReader.newInstance(size.getWidth(),
                        size.getHeight(),
                        ImageFormat.JPEG,
                        1);
                        //send image to camera fragment
                mImageReader.setOnImageAvailableListener( imageReader -> {
                    mImageReceiver.OnReceiveImage(imageReader.acquireLatestImage());
                },mHandler);
                addSurface(mImageReader.getSurface());
            }
            else {
                super.handleMessage(msg);
            }
        }
    }

    private ImageReader mImageReader;
    private HashMap<String, String> mCameraIdNameMap;
    private String mCurrentCameraID;
    private Size[] mImageOutSizes;

    CameraOperationManager(Context context,ImageReceiver mImageReceiver) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraIdNameMap = new HashMap<>();
        mCameraOutputSurfaceList = new ArrayList<>();
        this.mImageReceiver = mImageReceiver;
        getCameraId();
    }

    /*
     * Fill camera id name map with ids that have a front and back facing lens camera device
     * */
    private void getCameraId() {
        try {
            String[] ids = mCameraManager.getCameraIdList();
            for (int i = 0; i < ids.length; i++) {
                CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(ids[i]);
                int lens_face = cc.get(CameraCharacteristics.LENS_FACING);
                if (lens_face == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraIdNameMap.put(BACK_CAMERA, ids[i]);
                } else if (lens_face == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCameraIdNameMap.put(FRONT_CAMERA, ids[i]);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param cameraName Can only be "BACK_CAMERA" or "FRONT_CAMERA"
     */
    void setCamera(String cameraName) {

        mCurrentCameraID = mCameraIdNameMap.get(cameraName);
        try {
            if (mCurrentCameraID != null) {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraID);
            }
            //exposureTimeRanges = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            //sensorSensitivityRanges = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            /*if(exposureTimeRanges != null) {
                Log.i(TAG, "ExposureTimeRanges in ns => " + exposureTimeRanges.toString());
            }if(sensorSensitivityRanges != null) {
                Log.i(TAG, "SensorSensitivityRanges => " + sensorSensitivityRanges.toString());
            }*/
            //mImageOutSizes = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            //Log.i(TAG, "JPEG_SIZES => "+ Arrays.toString(mImageOutSizes));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Start thread for camera to use in background
     */
    void startCameraThread() {
        mHandlerThread = new HandlerThread("CameraOperationThread");
        mHandlerThread.start();
        mHandler = new CameraHandler(mHandlerThread.getLooper());
    }

    /**
     * Open camera that was set in setCamera(camName) call
     */
    void openCamera(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // if permission does not exist request the permission
            activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_CODE);
            return;
        }
        try {
            mCameraManager.openCamera(mCurrentCameraID, mCameraDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds an output surface to the list of camera output surfaces
     *
     * @param surface A surface the camera can use as an output
     */
    void addSurface(Surface surface) {
        //should use camera handler
        //guaranteed to be waiting
        mHandler.sendMessage(mHandler.obtainMessage(NEW_SURFACE,surface));
    }

    /**
     * Starts camera preview
     */
    private void startCameraPreview() {
        CaptureRequest.Builder builder;
        try {
            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mCameraOutputSurfaceList.get(0));

            //builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);

            //builder.set(CaptureRequest.SENSOR_SENSITIVITY,sensorSensitivityRanges.clamp(sensorSensitivity));
            //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,exposureTimeRanges.clamp(exposure_time));
            builder.set(CaptureRequest.CONTROL_AE_MODE,CameraMetadata.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
            mCameraCaptureSession.setRepeatingRequest(builder.build(), mCaptureCallback, mHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void takePicture() {
        //stop camera preview
        try {
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if(mSingleCaptureRequest == null) {
            try {
                CaptureRequest.Builder singleCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                singleCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                singleCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
                singleCaptureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
                //rotating from sensor to device orientation
                //
                int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.i("CameraOperationManager","SENSOR_ROTATION => "+sensorOrientation);
                singleCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION,sensorOrientation);
                //image will be rotated by this many degrees from natural orientation so if sensor matches
                singleCaptureBuilder.addTarget(mCameraOutputSurfaceList.get(1));//the surface to output images to

                mSingleCaptureRequest = singleCaptureBuilder.build();
                mCameraCaptureSession.capture(mSingleCaptureRequest, mSingleCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }else{
            try {
                mCameraCaptureSession.capture(mSingleCaptureRequest,mSingleCaptureCallback,mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    }



    /**
     * Close currently open camera
     */
    void closeCamera() {
        //close the open camera
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
        }
        if(!mCameraOutputSurfaceList.isEmpty()){
            mCameraOutputSurfaceList.clear();
        }
    }

    /**
     * Stop thread camera is operating on
     */
    void stopCameraThread() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    String getFormatName(int format){
        //if format can belong to imageformat or pixelformat
        Field[] fields = ImageFormat.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            try {
                if(field.getInt(field) == format){
                    return field.getName();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        fields = PixelFormat.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            try {
                if(field.getInt(field) == format){
                    return field.getName();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Size getApproximateSize(int width, int height) {
        //get an output size with the same width and height
        //if non available get one with the same aspect ratio
        int gcd = BigInteger.valueOf(width).gcd(BigInteger.valueOf(height)).intValue();
        Size aspect = new Size((width/gcd),(height/gcd));
        int aspect_w = aspect.getWidth();
        int aspect_h = aspect.getHeight();
        System.out.println("aspect =>"+aspect.toString());

        StreamConfigurationMap scm = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size closestSize = null;
        //int[] formats = scm.getOutputFormats();
        //stick to yuv 420 888
        Size[] sizes = scm.getOutputSizes(ImageFormat.YUV_420_888);
        for (int i = 0; i < sizes.length; i++) {
            //find one that has the same aspect or the closest in size that is smaller than the screen
            Size test = sizes[i];
            int testWidth = test.getWidth();
            int testHeight = test.getHeight();
            if(testWidth % aspect_w == 0 && testHeight % aspect_h == 0){
                //if same aspect then width mod aspect == 0 and height mod aspect == 0
                //example 1920 x 1080 and 2560 x 1440 are both 16:9
                //so 1920 mod 16 == 0 and so does 2560 mod 16
                //and 1080 mod 9 == 0 and 1440 mod 9 == 0
                if(closestSize == null){
                    closestSize = test;
                }else{
                    //there was another size we picked with the same aspect
                    //check if test is closer than the one we picked to actual size
                    if(Math.abs(width - testWidth) < Math.abs(width - closestSize.getWidth())){
                        closestSize = test;
                    }
                    //otherwise the already chosen is closer
                }
            }
        }
        //now if none are chosen pick the one that is closes in distance
        double distance = -1f;
        if(closestSize == null){
            for (int i = 0; i < sizes.length; i++) {
                //calculate the distance formula for each
                Size test = sizes[i];
                double test_distance = Math.sqrt(Math.pow(test.getWidth() - width,2f) + Math.pow(test.getHeight() - height,2f));
                if(distance == -1f){
                    closestSize = test;
                    distance = test_distance;
                }else if(test_distance < distance){
                        closestSize = test;
                        distance = test_distance;
                }
            }
        }

        return closestSize;
    }
}

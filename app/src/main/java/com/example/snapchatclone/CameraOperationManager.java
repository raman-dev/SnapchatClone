package com.example.snapchatclone;

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
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

class CameraOperationManager {

    static final int CAMERA_CODE = 99;
    private static final String TAG = "CameraOperationManager";

    static final String BACK_CAMERA = "BACK_CAMERA";
    static final String FRONT_CAMERA = "FRONT_CAMERA";
    private ArrayList<Surface> mCameraOutputSurfaceList;
    private CameraCharacteristics mCameraCharacteristics;
    private Range<Long> exposureTimeRanges;
    private Range<Integer> sensorSensitivityRanges;
    private long exposure_time = 20000000;//smaller exposure time means more images per second but also darker
    private int sensorSensitivity = 1500;

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
            //need to wait until at least one output surface is available
            if(mCameraOutputSurfaceList.isEmpty()){
                try {
                    Log.i(TAG,"Waiting for surface...");
                    mCameraOutputSurfaceList.add(blockingQ.take());
                    if(blockingQ.size() > 0){
                        blockingQ.drainTo(mCameraOutputSurfaceList);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                mCameraDevice.createCaptureSession(mCameraOutputSurfaceList, mCaptureSessionStateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
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

    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private ArrayBlockingQueue<Surface> blockingQ;

    private HashMap<String, String> mCameraIdNameMap;
    private String mCurrentCameraID;

    CameraOperationManager(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraIdNameMap = new HashMap<>();
        mCameraOutputSurfaceList = new ArrayList<>();
        blockingQ = new ArrayBlockingQueue<>(1);
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
            exposureTimeRanges = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            sensorSensitivityRanges = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            Log.i(TAG,"ExposureTimeRanges in ns => "+exposureTimeRanges.toString());
            Log.i(TAG,"SensorSensitivityRanges => "+sensorSensitivityRanges.toString());
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
        mHandler = new Handler(mHandlerThread.getLooper());
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
        Log.i(TAG,"Trying to add surface...");
        if(!blockingQ.contains(surface)) {
            Log.i(TAG,"Adding surface...");
            blockingQ.offer(surface);
        }
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
        int[] formats = scm.getOutputFormats();
        int difference_old = -1;
        for (int i = 0; i < formats.length; i++) {
            Size[] sizes = scm.getOutputSizes(formats[i]);
            //do a binary search instead
            //start from largest then go to half
            for (int i1 = 0; i1 < sizes.length; i1++) {
                Size s = sizes[i1];
                int sWidth = s.getWidth();
                if(sWidth/gcd == aspect_w && s.getHeight()/gcd == aspect_h){
                    //same aspect ratio
                    //if it has the same width or height then return this size;
                    if(sWidth == width){
                        return s;
                    }else{
                        //its a potentialSize
                        if(closestSize != null){
                            //s has the same aspect ratio
                            //s needs to be as close to width and height as possible
                            //calculate distance
                            int difference_new = Math.abs(sWidth-width);
                            if(difference_new < difference_old){
                                closestSize = s;
                                difference_old = Math.abs(closestSize.getWidth() - width);
                            }
                        }else{
                            closestSize = s;
                            difference_old = Math.abs(closestSize.getWidth() - width);
                        }
                    }
                }
            }
        }
        return closestSize;
    }
}

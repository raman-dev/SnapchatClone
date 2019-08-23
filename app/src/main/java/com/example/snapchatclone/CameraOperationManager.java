package com.example.snapchatclone;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.HashMap;

class CameraOperationManager {

    public static final int CAMERA_CODE = 99;
    private static final String TAG = "CameraOperationManager";

    public static final String BACK_CAMERA = "BACK_CAMERA";
    public static final String FRONT_CAMERA = "FRONT_CAMERA";

    public enum CAMERA_NAME {
        BACK_CAMERA, FRONT_CAMERA
    }

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "Camera Opened!");
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private HashMap<CAMERA_NAME, String> mCameraIdNameMap;
    private String mCurrentCameraID;

    public CameraOperationManager(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraIdNameMap = new HashMap<>();
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
                    mCameraIdNameMap.put(CAMERA_NAME.BACK_CAMERA, ids[i]);
                } else if (lens_face == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCameraIdNameMap.put(CAMERA_NAME.FRONT_CAMERA, ids[i]);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param cameraName Can only be "BACK_CAMERA" or "FRONT_CAMERA"
     */
    public void setCamera(String cameraName) {
        if (cameraName.equals(BACK_CAMERA)) {
            mCurrentCameraID = mCameraIdNameMap.get(CAMERA_NAME.BACK_CAMERA);
        } else if (cameraName.equals(FRONT_CAMERA)) {
            mCurrentCameraID = mCameraIdNameMap.get(CAMERA_NAME.FRONT_CAMERA);
        }
    }


    /**
     * Start thread for camera to use in background
     */
    public void startCameraThread(){
        mHandlerThread = new HandlerThread("CameraOperationThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    /**
     * Open camera that was set in setCamera(camName) call
     */
    public void openCamera(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // if permission does not exist request the permission
            activity.requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_CODE);
            return;
        }
        try {
            mCameraManager.openCamera(mCurrentCameraID, mCameraDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close currently open camera
     */
    public void closeCamera(){
        //close the open camera
        if(mCameraDevice != null){
            mCameraDevice.close();
        }
    }

    /**
     * Stop thread camera is operating on
     */
    public void stopCameraThread(){
        if(mHandler != null){
            mHandler.removeCallbacks(null);
            mHandler = null;
        }
        if(mHandlerThread != null){
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }
}

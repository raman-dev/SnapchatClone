package com.example.snapchatclone;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

class CameraOperationManager {

    static final int CAMERA_CODE = 99;
    private static final String TAG = "CameraOperationManager";

    static final String BACK_CAMERA = "BACK_CAMERA";
    static final String FRONT_CAMERA = "FRONT_CAMERA";
    private ArrayList<Surface> mCameraOutputSurfaceList;

    enum CAMERA_NAME {
        BACK_CAMERA, FRONT_CAMERA
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
            try {
                DisplayWaitLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                mCameraDevice.createCaptureSession(mCameraOutputSurfaceList, mCaptureSessionStateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

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
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };


    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Semaphore DisplayWaitLock;

    private HashMap<CAMERA_NAME, String> mCameraIdNameMap;
    private String mCurrentCameraID;

    CameraOperationManager(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraIdNameMap = new HashMap<>();
        mCameraOutputSurfaceList = new ArrayList<>();
        DisplayWaitLock = new Semaphore(1);
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
     * @param cameraName Can only be "BACK_CAMERA" or "FRONT_CAMERA"
     */
    void setCamera(String cameraName) {
        if (cameraName.equals(BACK_CAMERA)) {
            mCurrentCameraID = mCameraIdNameMap.get(CAMERA_NAME.BACK_CAMERA);
        } else if (cameraName.equals(FRONT_CAMERA)) {
            mCurrentCameraID = mCameraIdNameMap.get(CAMERA_NAME.FRONT_CAMERA);
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
        try {
            DisplayWaitLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        DisplayWaitLock.release();
        mCameraOutputSurfaceList.add(surface);
    }

    /**
     * Starts camera preview
     */
    private void startCameraPreview() {
        CaptureRequest.Builder builder;
        try {
            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mCameraOutputSurfaceList.get(0));

            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
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
    }

    /**
     * Stop thread camera is operating on
     */
    void stopCameraThread() {
        if (mHandler != null) {
            mHandler.removeCallbacks(null);
            mHandler = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }
}

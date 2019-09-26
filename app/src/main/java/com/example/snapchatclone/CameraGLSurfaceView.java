package com.example.snapchatclone;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class CameraGLSurfaceView extends GLSurfaceView {

    private CameraRenderer mCameraRenderer;
    private CameraOperationManager mCameraOperationManager;

    public CameraGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        mCameraOperationManager = new CameraOperationManager(context);
        mCameraOperationManager.setCamera(CameraOperationManager.BACK_CAMERA);
        mCameraRenderer = new CameraRenderer(context,mCameraOperationManager,this);
        setRenderer(mCameraRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void onStart(){
        mCameraOperationManager.startCameraThread();
    }

    public void OpenCamera(MainActivity activity,int rotation){
        mCameraOperationManager.openCamera(activity);
        mCameraRenderer.setSensorRotation(mCameraOperationManager.getCameraOrientation());
        mCameraRenderer.setDisplayRotation(rotation);
    }

    @Override
    public void onPause(){
        super.onPause();
        mCameraOperationManager.closeCamera();
        mCameraRenderer.onPause();
    }

    public void onStop(){
        mCameraOperationManager.stopCameraThread();
    }
}

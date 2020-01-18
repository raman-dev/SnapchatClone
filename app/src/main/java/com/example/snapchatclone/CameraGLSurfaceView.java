package com.example.snapchatclone;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class CameraGLSurfaceView extends GLSurfaceView {

    private CameraRenderer mCameraRenderer;

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
        mCameraRenderer = new CameraRenderer(context,this);

        setRenderer(mCameraRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setSensorRotation(int rotation){
        mCameraRenderer.setSensorRotation(rotation);
    }

    public void setDisplayRotation(int rotation){
        mCameraRenderer.setDisplayRotation(rotation);
    }

    @Override
    public void onPause(){
        super.onPause();
        mCameraRenderer.onPause();
    }

    public void setCameraOperationManager(CameraOperationManager cameraOperationManager) {
        mCameraRenderer.setCameraOperationManager(cameraOperationManager);
    }
}

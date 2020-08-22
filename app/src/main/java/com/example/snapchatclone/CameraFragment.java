package com.example.snapchatclone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment implements SurfaceHolder.Callback {

    private static final String TAG = "CameraFragment";
    private CameraGLSurfaceView mCameraGLSurfaceView;
    private CameraOperationManager mCameraOperationManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");


        mCameraOperationManager = new CameraOperationManager(getActivity());
        mCameraOperationManager.setCamera(CameraOperationManager.BACK_CAMERA);

        View view = inflater.inflate(R.layout.camera_layout, container, false);
        mCameraGLSurfaceView = view.findViewById(R.id.surfaceView);
        //mCameraGLSurfaceView.setZOrderOnTop(true);
        mCameraGLSurfaceView.setCameraOperationManager(mCameraOperationManager);
        //mCameraGLSurfaceView.getHolder().addCallback(this);
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CameraOperationManager.CAMERA_CODE && permissions[0].equals(Manifest.permission.CAMERA)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG,"CAMERA_PERMISSION GRANTED!");
                mCameraOperationManager.openCamera(getActivity());
            }
        } else {
            getActivity().finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mCameraOperationManager.startCameraThread();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraOperationManager.openCamera(getActivity());

        mCameraGLSurfaceView.setSensorRotation(mCameraOperationManager.getCameraOrientation());
        mCameraGLSurfaceView.setDisplayRotation(getActivity().getWindowManager().getDefaultDisplay().getRotation());

        mCameraGLSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("CameraFragment","onPause!");
        mCameraOperationManager.closeCamera();
        mCameraGLSurfaceView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mCameraOperationManager.stopCameraThread();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCameraOperationManager.addSurface(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}


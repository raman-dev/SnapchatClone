package com.example.snapchatclone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

public class CameraFragment extends Fragment implements TextureView.SurfaceTextureListener {

    private static final String TAG="CameraFragment";
    private CameraOperationManager mCameraOperationManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraOperationManager = new CameraOperationManager(getContext());
        mCameraOperationManager.setCamera(CameraOperationManager.BACK_CAMERA);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG,"onCreateView");
        View view = inflater.inflate(R.layout.camera_frag_layout,container,false);
        TextureView textureView = view.findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mCameraOperationManager.startCameraThread();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CameraOperationManager.CAMERA_CODE && permissions[0] == Manifest.permission.CAMERA){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                mCameraOperationManager.openCamera(getActivity());
            }
        }else{
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"onResume");
        mCameraOperationManager.openCamera(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG,"onPause");
        mCameraOperationManager.closeCamera();
    }

    @Override
    public void onStop() {
        super.onStop();
        mCameraOperationManager.stopCameraThread();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}

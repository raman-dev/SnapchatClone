package com.example.snapchatclone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CameraFragment extends Fragment{

    private static final String TAG = "CameraFragment";
    private CameraGLSurfaceView cameraGLSurfaceView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.camera_frag_layout, container, false);
        cameraGLSurfaceView = view.findViewById(R.id.cameraGLSurfaceView);
        cameraGLSurfaceView.setZOrderOnTop(true);
        return view;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CameraOperationManager.CAMERA_CODE && permissions[0].equals(Manifest.permission.CAMERA)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        } else {
            getActivity().finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        cameraGLSurfaceView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraGLSurfaceView.OpenCamera((MainActivity) getActivity(),getActivity().getWindowManager().getDefaultDisplay().getRotation());
        cameraGLSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraGLSurfaceView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        cameraGLSurfaceView.onStop();
    }
}


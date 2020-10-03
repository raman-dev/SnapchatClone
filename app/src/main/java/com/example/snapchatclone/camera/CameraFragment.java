package com.example.snapchatclone.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.example.snapchatclone.R;
import com.example.snapchatclone.SendSnapFragment;
import com.example.snapchatclone.SnapchatAPI;
import com.example.snapchatclone.UserConversationDataReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

@Deprecated
public class CameraFragment extends Fragment implements View.OnClickListener, CameraOperationManager.ImageReceiver {


    public static final int NEW_IMAGE = 1;

    private static final String TAG = "CameraFragment";

    private CameraGLSurfaceView mCameraGLSurfaceView;
    private CameraOperationManager mCameraOperationManager;
    public static final String SNAP_FILE_HOLDER_NAME = "snap_temp.jpg";

    private Handler mImageHandler;
    private UserConversationDataReceiver mReceiver;
    private SendSnapFragment.SendSnapListener mSendSnapListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");


        mCameraOperationManager = new CameraOperationManager(getActivity(),this);
        mCameraOperationManager.setCamera(CameraOperationManager.BACK_CAMERA);

        View view = inflater.inflate(R.layout.camera_layout, container, false);
        mCameraGLSurfaceView = view.findViewById(R.id.surfaceView);
        //mCameraGLSurfaceView.setZOrderOnTop(true);
        mCameraGLSurfaceView.setCameraOperationManager(mCameraOperationManager);
        //mCameraGLSurfaceView.getHolder().addCallback(this);
         view.findViewById(R.id.take_picture_button).setOnClickListener(this);
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

        mImageHandler = new ImageHandler(Looper.getMainLooper());
    }


    @Override
    public void onResume() {
        super.onResume();
        mCameraOperationManager.openCamera(getActivity());

        mCameraGLSurfaceView.setSensorRotation(mCameraOperationManager.getCameraOrientation());
        mCameraGLSurfaceView.setDisplayRotation(getActivity().getWindowManager().getDefaultDisplay().getRotation());
        mCameraGLSurfaceView.onResume();
        //need to instantiate imagereader with stuck
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
        mImageHandler.removeCallbacksAndMessages(null);
        mImageHandler = null;
    }

    @Override
    public void onClick(View view) {
        //after a picture is taken we need to create a new fragment called a send fragment
        //that had the image and a button to send that pulls up a bottom sheet dialog
        //to select people to send to
        mCameraOperationManager.takePicture();
    }

    @Override
    public void OnReceiveImage(Image image) {
        mImageHandler.sendMessage(mImageHandler.obtainMessage(NEW_IMAGE,image));
    }

    private class ImageHandler extends Handler{
        public ImageHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {

            if(msg.what == NEW_IMAGE){
                //write image to file then send file
                Image mImage = (Image)msg.obj;
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(new File(getActivity().getFilesDir(),SNAP_FILE_HOLDER_NAME));//should be a new file everytime
                    output.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mImage.close();
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //launch send snap fragment
                //ok so generate uuid here
                //then pass it down
                SendSnapFragment fragment = SendSnapFragment.newInstance(SNAP_FILE_HOLDER_NAME);
                fragment.setReceiver(mReceiver);
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_container,fragment)
                        .addToBackStack(null)
                        .commit();
                mImage.close();
            }
            else {
                super.handleMessage(msg);
            }
        }
    }

}


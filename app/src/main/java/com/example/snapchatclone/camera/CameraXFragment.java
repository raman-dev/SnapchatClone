package com.example.snapchatclone.camera;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.snapchatclone.MainAppFragment;
import com.example.snapchatclone.R;
import com.example.snapchatclone.SendSnapFragment;
import com.example.snapchatclone.SnapDisplayFragment;
import com.example.snapchatclone.UserConversationDataReceiver;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.snapchatclone.camera.CameraFragment.SNAP_FILE_HOLDER_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CameraXFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraXFragment extends Fragment implements View.OnClickListener, ImageCapture.OnImageSavedCallback {
    public static final String TAG = "CameraXFragment";
    private static final int IMAGE_SAVED_NEW = 0;
    private static final String DISPLAY_ROTATION = "DisplayRotation";
    private ExecutorService mExecutor;
    private ImageCapture.OutputFileOptions mOutputFileOptions;
    private int mDisplayRotation = -1;
    private UserConversationDataReceiver mReceiver;
    private PreviewView previewView;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    public CameraXFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *

     * @return A new instance of fragment CameraXFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CameraXFragment newInstance(int rotation) {
        CameraXFragment fragment = new CameraXFragment();
        Bundle args = new Bundle();
        args.putInt(DISPLAY_ROTATION,rotation);
        fragment.setArguments(args);
        return fragment;
    }


    private ImageCapture mImageCapture;
    private ImageSaveHandler mImageSaveHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.mDisplayRotation = getArguments().getInt(DISPLAY_ROTATION);
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /*
        *
        * Camera x takes 3 objects to use
        * 1. a use case object with some configurations
        * 2. listeners that determine what to do with capture data.
        * 3. and a lifecycle object
        * */
        mImageSaveHandler = new ImageSaveHandler(Looper.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        View view = inflater.inflate(R.layout.camera_x_layout, container, false);
        view.findViewById(R.id.captureImage_button).setOnClickListener(this);
        previewView = view.findViewById(R.id.cameraX_previewView);
        mOutputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(getActivity().getFilesDir(),SNAP_FILE_HOLDER_NAME))
                        .build();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();
                mImageCapture = new ImageCapture.Builder()
                        .setTargetRotation(mDisplayRotation)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider
                        .bindToLifecycle(
                                this,
                                cameraSelector,
                                preview,
                                mImageCapture);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //don't have to check this is safe to call multiple times
        mExecutor.shutdown();
        mExecutor = null;
    }

    @Override
    public void onClick(View view) {
        //take a picturte
        mImageCapture.takePicture(mOutputFileOptions,mExecutor ,this);
    }


    @Override
    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
        //when the image is saved hurray!
        mImageSaveHandler.sendMessage(mImageSaveHandler.obtainMessage(IMAGE_SAVED_NEW,outputFileResults));
    }

    @Override
    public void onError(@NonNull ImageCaptureException exception) {
        //exception.getImageCaptureError();
    }

    public void setReceiver(UserConversationDataReceiver mReceiver) {
        this.mReceiver = mReceiver;
    }

    private class ImageSaveHandler extends Handler{
        public ImageSaveHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == IMAGE_SAVED_NEW){
                //Toast.makeText(getContext(),"Saved File!",Toast.LENGTH_LONG).show();
                //create new fragment
                SendSnapFragment fragment = SendSnapFragment.newInstance(SNAP_FILE_HOLDER_NAME);
                fragment.setReceiver(mReceiver);
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_container,fragment)
                        .addToBackStack(null)
                        .commit();
            }else {
                super.handleMessage(msg);
            }
        }
    }
}
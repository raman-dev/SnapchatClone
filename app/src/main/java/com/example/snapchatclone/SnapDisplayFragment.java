package com.example.snapchatclone;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SnapDisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SnapDisplayFragment extends Fragment implements View.OnClickListener {


    private LinkedList<SnapWrapper> imageQueue;
    private ImageView mImageView;
    private FragmentRemovedListener mFragmentRemovedListener;
    private SnapViewedListener mSnapViewedListener;
    private Bitmap rotated = null;

    public SnapDisplayFragment() {
        // Required empty public constructor
    }

    public interface SnapViewedListener{
        void OnSnapViewed(String conversationId);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SnapDisplayFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SnapDisplayFragment newInstance() {
        SnapDisplayFragment fragment = new SnapDisplayFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        //set a transition that expands the imageview fragment and when this bitch is clicked go to the next file in the queu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.snap_display_layout, container, false);
        mImageView = view.findViewById(R.id.displaySnap_imageView);
        showImage();
        view.setOnClickListener(this);
        return view;
    }

    private void showImage() {
        try {
            SnapWrapper wrapper = imageQueue.removeLast();
            File file = wrapper.image_file;
            int rotation = 0;
            //get the rotation so the image is displayed in the orientation it was taken
            try {
                ExifInterface exifInterface = new ExifInterface(file);
                //
                rotation = exifInterface.getRotationDegrees();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap original = BitmapFactory.decodeStream(new FileInputStream(file));
            rotated = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            mImageView.setImageBitmap(rotated);
            file.delete();//remove from local directory
            SnapchatAPI.UpdateSnap(wrapper.snap);
            mSnapViewedListener.OnSnapViewed(wrapper.snap.getConversationId());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mFragmentRemovedListener = (FragmentRemovedListener) context;
        mSnapViewedListener = (SnapViewedListener)context;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rotated.recycle();
        rotated = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentRemovedListener.OnFragmentRemoved();
        mFragmentRemovedListener = null;
    }

    public void setImageQueue(LinkedList<SnapWrapper> imageQueue) {
        this.imageQueue = imageQueue;
    }

    @Override
    public void onClick(View view) {
        if (imageQueue.size() > 0) {
            showImage();
        } else {
            //remove fragment
            getParentFragmentManager().popBackStack();//remove self
        }
    }
}
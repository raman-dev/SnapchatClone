package com.example.snapchatclone;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.generated.model.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.example.snapchatclone.camera.CameraFragment.SNAP_FILE_HOLDER_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SendSnapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendSnapFragment extends Fragment implements View.OnClickListener, UserSendListFragmentBottomSheet.SendSnapListener {


    //private static final int FILE_SENT = 0;
    private static final int FILE_SEND_ERROR = 1;
    private static final int SNAP_SENT = 2;
    public String filename = null;
    private File image_file = null;
    private UserConversationDataReceiver mReceiver;

    private UserSendListFragmentBottomSheet bottomSheet;
    private Handler mSnapSendHandler;

    private class SnapSendHandler extends Handler{
        public SnapSendHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            if(msg.what == SNAP_SENT){
                Message snap = (Message) msg.obj;
                mSendSnapListener.OnSnapSent(snap);
            }
            else {
                super.handleMessage(msg);
            }
        }
    }

    public interface SendSnapListener{
        void OnSnapSent(Message snap);
    }

    private SendSnapListener mSendSnapListener;


    public SendSnapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SendSnapFragment.
     * @param snapFileHolderName
     */
    public static SendSnapFragment newInstance(String snapFileHolderName){//String param1, String param2) {
        SendSnapFragment fragment = new SendSnapFragment();
        Bundle args = new Bundle();
        args.putString("FILE_NAME",snapFileHolderName);
        fragment.setArguments(args);
        return fragment;
    }

    public void setReceiver(UserConversationDataReceiver mReceiver){
        this.mReceiver = mReceiver;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            filename = bundle.getString("FILE_NAME");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.send_snap_layout, container, false);
        view.findViewById(R.id.sendSnapSend_Button).setOnClickListener(this);
        view.findViewById(R.id.sendSnapCancel_Button).setOnClickListener(this);
        mSnapSendHandler = new SnapSendHandler(Looper.getMainLooper());
        image_file = new File(getActivity().getFilesDir(),SNAP_FILE_HOLDER_NAME);
        Bitmap mBitmap = null;
        try {
            mBitmap = BitmapFactory.decodeStream(new FileInputStream(image_file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int rotation = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(image_file);
            rotation = exifInterface.getRotationDegrees();
            //Log.("ExifInterface","ROTATION => "+rotation);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: ROTATE IMAGE
        ImageView imageView = view.findViewById(R.id.sendSnap_ImageView);
        //imageView.setRotation(90f);//correctly rotate
        Matrix matrix = new Matrix();
        //negative rotations are in ccw
        //positive rotation are in cw
        matrix.postRotate(rotation,0,0);
        //we need to find the difference
        imageView.setImageBitmap(Bitmap.createBitmap(mBitmap,0,0,mBitmap.getWidth(),mBitmap.getHeight(),matrix,true));
        return view;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.sendSnapSend_Button:
                //need to first write image to file and then send it
                //show modal bottom sheet
                //now we populate the modal bottom sheet with chat wrapper objects
                ///and when clicked we can send back to this shit where we have the file you know
                //show the bottom sheet dialog
                bottomSheet = UserSendListFragmentBottomSheet.newInstance();
                bottomSheet.setData(mReceiver.GetUserConversations(),this);
                bottomSheet.show(getParentFragmentManager(), "UserSendListFragmentBottomSheet");
                break;
            case R.id.sendSnapCancel_Button:
                //close the discard image and close fragment
                //when
                image_file.delete();
                image_file = null;
                getParentFragmentManager().popBackStack();//remove self
                //
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void SendSnap(ChatWrapper wrapper) {
        if(bottomSheet != null){
            bottomSheet.dismiss();
        }
        SnapchatAPI.SendSnap(mSnapSendHandler,SNAP_SENT,image_file, Amplify.Auth.getCurrentUser().getUserId(),wrapper.user.getUserId(),wrapper.conversationId);
        getParentFragmentManager().popBackStack();//close asap
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSendSnapListener = (SendSnapListener) context;
    }
}
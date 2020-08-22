package com.example.snapchatclone;

import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class ChatFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "ChatFragment";
    private BottomSheetBehavior mBottomSheet;

    public void setBottomSheet(BottomSheetBehavior mBottomSheet){
        this.mBottomSheet = mBottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.chat_layout, container, false);
        view.findViewById(R.id.profile).setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.profile:
                mBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
        }
    }
}

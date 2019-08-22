package com.example.snapchatclone;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FollowerFragment extends Fragment {

    private static final String TAG = "FollowerFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG,"onCreateView");
        return inflater.inflate(R.layout.follower_frag_layout,container,false);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG,"onPause");
    }
}

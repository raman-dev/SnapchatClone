package com.example.snapchatclone;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amplifyframework.core.Amplify;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

public class MainAppFragment extends Fragment implements MainActivity.BottomSheetBackPressListener, View.OnClickListener {

    public static final String TAG = "MainAppFragment";
    private static final int GET_USERNAME_RESULT = 0;
    private TabLayout tabLayout;
    private BottomSheetBehavior mBottomSheet;
    private TextView mProfileUsername;
    private AWSNetworkIOHandler mAWSNetworkIOHandler;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_layout, container, false);



        FragmentStatePagerAdapter adapter = new MyPagerAdapter(getFragmentManager());
        ViewPager pager = view.findViewById(R.id.viewPager);
        pager.setAdapter(adapter);
        tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(pager);
        //tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        /*
         * change icon color of tabs
         * cyan for chat
         * yellow for camera
         */
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //int tabIconColor = ContextCompat.getColor(context, R.color.tabSelectedIconColor);
                if (tab.getPosition() == 0) {
                    tab.getIcon().setColorFilter(Color.CYAN, PorterDuff.Mode.SRC_IN);
                } else {
                    tab.getIcon().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if(tab != null){
                    tab.getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mBottomSheet = BottomSheetBehavior.from(view.findViewById(R.id.bottomSheetContainer));
        Toolbar toolbar = view.findViewById(R.id.profileToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_down_arrow);
        toolbar.setNavigationOnClickListener(v -> mBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED));
        view.findViewById(R.id.signout_button).setOnClickListener(this);

        mProfileUsername = view.findViewById(R.id.profile_username);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAWSNetworkIOHandler = new AWSNetworkIOHandler(Looper.getMainLooper());
    }

    @Override
    public void onResume() {
        super.onResume();
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_chat_cyan);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_camera_yellow).select();

        mHandlerThread = new HandlerThread("AWSUserDataThread");
        mHandlerThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mHandler.post(() -> {
            /* run in background */
            AWSMobileClient awsMobileClient = (AWSMobileClient) Amplify.Auth.getPlugin("awsCognitoAuthPlugin").getEscapeHatch();
            try {
                String profile_username = awsMobileClient.getUserAttributes().get("preferred_username");
                Message msg = mAWSNetworkIOHandler.obtainMessage();
                msg.what = GET_USERNAME_RESULT;
                msg.obj = profile_username;
                mAWSNetworkIOHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
        mHandlerThread = null;
        mHandler = null;
    }

    @Override
    public boolean OnBackPressed() {
        //if bottom sheet is open the collapse and consume back pressed
        if(mBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED){
            mBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.signout_button:
                signOut();
                break;
        }
    }

    public void signOut() {
        Amplify.Auth.signOut(
                () -> {Log.i("AuthQuickstart", "Signed out successfully");
                        mBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);},
                error -> Log.e("AuthQuickstart", error.toString())
        );
    }


    private class AWSNetworkIOHandler extends Handler {
        public AWSNetworkIOHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            if(msg.what == GET_USERNAME_RESULT){
                String profile_username = (String)msg.obj;
                mProfileUsername.setText(profile_username);
            }
        }
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        private static final int NUM_PAGES = 2;

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    Log.i(TAG, "getChatFragment!");
                    ChatFragment fragment = new ChatFragment();
                    fragment.setBottomSheet(mBottomSheet);
                    return fragment;
                case 1:
                    Log.i(TAG, "getCameraFragment!");
                    return new CameraFragment();

            }
            return null;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}

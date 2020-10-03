package com.example.snapchatclone;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import com.example.snapchatclone.camera.CameraXFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class MainAppFragment extends Fragment implements
        FragmentRemovedListener,
        FriendRequestResponseListener,
        UserConversationDataReceiver,
        View.OnClickListener,
        ProfileInteractListener, SendSnapFragment.SendSnapListener, SnapDisplayFragment.SnapViewedListener {

    public static final String TAG = "MainAppFragment";
    private static final int NUM_PAGES = 2;
    private TabLayout tabLayout;

    private SendSnapFragment.SendSnapListener mSendSnapListener;
    private FragmentRemovedListener mFragmentRemovedListener;
    private FriendRequestResponseListener mFriendRequestResponseListener;
    private UserConversationDataReceiver mReceiver;

    private ProfileInteractListener mProfileInteractListener;

    private BottomSheetBehavior mProfileBottomSheet;
    private TextView mProfileUsername;
    private SnapDisplayFragment.SnapViewedListener mSnapViewedListener;

    public MainAppFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
       //Log.(TAG, "onAttach");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       //Log.(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.main_layout, container, false);

        ServerResponseHandler mServerResponseHandler = new ServerResponseHandler(Looper.getMainLooper());
        FragmentStatePagerAdapter adapter = new MyStatePagerAdapter(getParentFragmentManager());
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
                if (tab.getIcon() != null) {
                    if (tab.getPosition() == 0) {
                        tab.getIcon().setColorFilter(Color.CYAN, PorterDuff.Mode.SRC_IN);
                    } else {
                        tab.getIcon().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        //mFriendRequestMap = new HashMap<>();
        /**
         * profile as bottom sheet
         */
        mProfileBottomSheet = BottomSheetBehavior.from(view.findViewById(R.id.bottomSheetContainer));
        view.findViewById(R.id.friendRequestShow_button).setOnClickListener(this);
        Toolbar toolbar = view.findViewById(R.id.profileToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_down_arrow);
        toolbar.setNavigationOnClickListener(v ->
                mProfileBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED)
        );
        view.findViewById(R.id.signout_button).setOnClickListener(this);
        mProfileUsername = view.findViewById(R.id.profile_username);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
       //Log.(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_chat_cyan);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_camera_yellow);
       //Log.(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
       //Log.(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
       //Log.(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
       //Log.(TAG, "onDestroyView");
        mFragmentRemovedListener = null;
        mFriendRequestResponseListener = null;
        mProfileInteractListener = null;
        mReceiver = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       //Log.(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
       //Log.(TAG, "onDetach");
    }

    @Override
    public void OnFragmentRemoved() {
        //pass on remove event to chat list frag
        //signal chat list when fragments are removed
        //that are launched from within chatfragment
        mFragmentRemovedListener.OnFragmentRemoved();
    }

    @Override
    public void OnSendFriendRequestResponse(String requestSenderId, boolean accept) {
        mFriendRequestResponseListener.OnSendFriendRequestResponse(requestSenderId, accept);
    }

    @Override
    public ArrayList<ChatWrapper> GetUserConversations() {
        return mReceiver.GetUserConversations();
    }

    @Override
    public void OnSnapSent(com.amplifyframework.datastore.generated.model.Message snap) {
        mSendSnapListener.OnSnapSent(snap);
    }

    @Override
    public void OnSnapViewed(String conversationId) {
        mSnapViewedListener.OnSnapViewed(conversationId);
    }

    private class ServerResponseHandler extends Handler {
        public ServerResponseHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            /*if (msg.what == RECEIVED_FRIEND_REQUESTS) {
                //now what send the friend request list up to the mainappfragment
                //and update my
                PaginatedResult<FriendRequest> requests = (PaginatedResult<FriendRequest>) msg.obj;
                requests.getItems().forEach(friendRequest -> {
                    mFriendRequestMap.put(friendRequest.getAuthorId(), friendRequest);
                });
                //updateFriendRequestNotification(mFriendRequestMap.size());
            } else if (msg.what == RECEIVED_LIVE_FRIEND_REQUEST) {
                //do something with the friend request
               //Log.("AmplifySubscription", "Received friend request");
                //add to list of add friend notifications currently available
                FriendRequest request = (FriendRequest) msg.obj;
                mFriendRequestMap.put(request.getAuthorId(), request);
                //update the icon
                //updateFriendRequestNotification(mFriendRequestMap.size());
            } else {*/
            super.handleMessage(msg);
            //}
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.signout_button:
                //disable subscriptions
                SnapchatAPI.UnSubscribe();
                SnapchatAPI.SignOut();
                break;
            case R.id.friendRequestShow_button:
                mProfileInteractListener.ShowFriendRequests();
                break;
        }
    }

    @Override
    public void CloseProfile() {
    }

    @Override
    public void ShowFriendRequests() {

    }

    @Override
    public void ShowProfile() {
        //open the bottom sheet
        mProfileBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public boolean onBackPressProfile() {
        //if bottom sheet is open close it
        if (mProfileBottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mProfileBottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return true;
        }
        return false;
    }

    @Override
    public void SetUsername(String username) {
        mProfileUsername.setText(username);
    }

    private class MyStatePagerAdapter extends FragmentStatePagerAdapter {


        public MyStatePagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0) {

                ChatListFragment mChatListFragment = new ChatListFragment();
                mFragmentRemovedListener = mChatListFragment;
                mFriendRequestResponseListener = mChatListFragment;
                mReceiver = mChatListFragment;
                mProfileInteractListener = mChatListFragment;
                mSendSnapListener = mChatListFragment;
                mSnapViewedListener = mChatListFragment;
                mChatListFragment.setBottomSheet(mProfileBottomSheet);
                mChatListFragment.setProfileInteractListener(MainAppFragment.this);
                return mChatListFragment;
            } else {
                //CameraFragment mCameraFragment = new CameraFragment();
                //mCameraFragment.setReceiver(MainAppFragment.this);
                //return mCameraFragment;
                CameraXFragment fragment = CameraXFragment.newInstance(getActivity().getWindowManager().getDefaultDisplay().getRotation());
                fragment.setReceiver(MainAppFragment.this);
                return fragment;

            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }


}

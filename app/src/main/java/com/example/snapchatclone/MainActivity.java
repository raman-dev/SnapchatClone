package com.example.snapchatclone;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.amplifyframework.auth.AuthChannelEventName;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.datastore.generated.model.Message;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.SubscriptionToken;
import com.example.snapchatclone.auth.LoginActivity;
import com.example.snapchatclone.camera.CameraOperationManager;


public class MainActivity extends AppCompatActivity implements
        AddFriendBottomSheetDialog.SendFriendRequestListener,
        ProfileInteractListener,
        FriendRequestResponseListener,
        FragmentRemovedListener,
        SendSnapFragment.SendSnapListener,
        SnapDisplayFragment.SnapViewedListener{

    public static final int SIGNED_IN = 23;
    public static final int NOT_SIGNED_IN = 24;
    private static final String TAG = "MainActivity";

    private static final int FRIEND_REQUEST_SENT_SUCCESS = 0;
    private static final int FRIEND_REQUEST_SENT_FAILURE = 1;
    private static final int FRIEND_REQUEST_FAILED_TO_SEND = 2;
    private static final int FRIEND_RESPONSE_SENT = 3;
    private static final int LOGIN_REQUEST = 99;


    private SendSnapFragment.SendSnapListener mSendSnapListener;
    private FragmentRemovedListener mFragmentRemovedListener;
    private FriendRequestResponseListener mFriendRequestResponseListener;
    private ProfileInteractListener mProfileInteractListener;
    private SnapDisplayFragment.SnapViewedListener mSnapViewedListener;
    private ServerResponseHandler mServerResponseHandler;
    private FragmentManager fragmentManager;
    private boolean isLoggedIn = false;
    private SubscriptionToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Log.i(TAG, "onCreate!");
        mServerResponseHandler = new ServerResponseHandler(Looper.getMainLooper());
        fragmentManager = getSupportFragmentManager();
        //for explicit sign in and sign out events
        //check if user has camera permissions
        //if user has camera permission then continue to start the
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CameraOperationManager.CAMERA_CODE && permissions[0].equals(Manifest.permission.CAMERA)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runApp();
            }
        } else {
            finish();
        }
    }

    private void StartLoginActivity() {
        Intent loginIntent = new Intent();
        loginIntent.setAction(Intent.ACTION_VIEW);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//only 1 login activity allowed
        loginIntent.setClass(this, LoginActivity.class);
        startActivityForResult(loginIntent, LOGIN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Log.i(TAG, "onActivityResult");
        if (requestCode == LOGIN_REQUEST) {
            //
            if (resultCode == NOT_SIGNED_IN) {
                finish();//we need to close activity
                //since user pressed back
            } else {
                isLoggedIn = true;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.i(TAG, "onResume!");
        /*View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // if permission does not exist request the permission
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CameraOperationManager.CAMERA_CODE);
        } else {
            runApp();
        }

    }

    private void runApp() {
        //continue
        /**
         * AUTH STATE
         * if user is signed in and wifi is off
         * auth is non-null
         * if user logs out with wifi on || off
         * auth is now null
         */
        if (Amplify.Auth.getCurrentUser() != null || isLoggedIn) {
            //we can create here
            Amplify.Hub.subscribe(HubChannel.AUTH,
                    hubEvent -> {
                        if (hubEvent.getName().equals(InitializationStatus.SUCCEEDED.toString())) {
                            //Log.i("AuthQuickstart", "Auth successfully initialized");
                        } else if (hubEvent.getName().equals(InitializationStatus.FAILED.toString())) {
                            //Log.i("AuthQuickstart", "Auth failed to succeed");
                        } else {
                            switch (AuthChannelEventName.valueOf(hubEvent.getName())) {
                                case SIGNED_OUT:
                                    //Log.i("AuthQuickstart", "Auth just became signed out.");
                                    mProfileInteractListener.CloseProfile();
                                    isLoggedIn = false;
                                    StartLoginActivity();
                                    break;
                                case SESSION_EXPIRED:
                                    //Log.i("AuthQuickstart", "Auth session just expired.");
                                    isLoggedIn = false;
                                    StartLoginActivity();
                                    break;
                                default:
                                    //Log.w("AuthQuickstart", "Unhandled Auth Event: " + AuthChannelEventName.valueOf(hubEvent.getName()));
                                    break;
                            }
                        }
                    }
            );
            showMainFragment(fragmentManager);
            //Log.i("AmplifyAuth", "Auth.getCurrentUser is NON-NULL");
        } else {
            StartLoginActivity();
        }
    }

    private void showMainFragment(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(MainAppFragment.TAG) != null) {
            return;
        }
        //Log.i(TAG,"showMainFragment!");
        MainAppFragment fragment = new MainAppFragment();
        mFragmentRemovedListener = fragment;
        mFriendRequestResponseListener = fragment;
        mProfileInteractListener = fragment;
        mSendSnapListener = fragment;
        mSnapViewedListener = fragment;
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, MainAppFragment.TAG)
                .commit();
    }

    private void handleServerResponse(int resultCode, Object data) {
        mServerResponseHandler.sendMessage(mServerResponseHandler.obtainMessage(resultCode, data));
    }

    @Override
    public void onBackPressed() {
        //listener returns false if we didnt consume the back pressed event
        //now close the bottom sheet
        //so the main app fragment needs to what?
        //needs to implement profile interact listener and
        //if profile listener is null or profile is not expanded if
        if(mProfileInteractListener == null || !mProfileInteractListener.onBackPressProfile()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.i(TAG, "onPause!");
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Log.i(TAG, "onStop!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mServerResponseHandler.removeCallbacksAndMessages(null);
        mServerResponseHandler = null;
        //Log.i(TAG, "onDestroy!");
    }

    @Override
    public void OnFragmentRemoved() {
        mFragmentRemovedListener.OnFragmentRemoved();
    }

    @Override
    public void CloseProfile() {

    }
    @Override
    public void ShowFriendRequests() {

    }

    @Override
    public void ShowProfile() {
        mProfileInteractListener.ShowProfile();
    }

    @Override
    public boolean onBackPressProfile() {
        return false;
    }

    @Override
    public void SetUsername(String username) {
        //mProfileInteractListener.SetUsername(username);
    }

    @Override
    public void OnSnapSent(Message snap) {
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
        public void handleMessage(@NonNull android.os.Message msg) {
            switch (msg.what) {
                case FRIEND_REQUEST_SENT_SUCCESS:
                    Toast.makeText(getApplicationContext(), "Sent friend request!", Toast.LENGTH_LONG).show();
                    break;
                case FRIEND_REQUEST_SENT_FAILURE:
                    Toast.makeText(getApplicationContext(), "Failed to send friend request!", Toast.LENGTH_LONG).show();
                    break;
                case FRIEND_REQUEST_FAILED_TO_SEND:
                    Toast.makeText(getApplicationContext(), "Failed to send request!", Toast.LENGTH_LONG).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void SendFriendRequest(String recipientId, String authorUsername) {
        Amplify.API.mutate(SnapchatAPI.getCreateFriendRequest(recipientId, authorUsername),
                response -> {
                    //
                    if (response.hasData() && response.getData() != null) {
                        //
                        handleServerResponse(FRIEND_REQUEST_SENT_SUCCESS, response.getData());
                    } else {
                        //
                        handleServerResponse(FRIEND_REQUEST_SENT_FAILURE, response.getErrors());
                    }
                }, failure -> {
                    handleServerResponse(FRIEND_REQUEST_FAILED_TO_SEND, failure);
                });
    }

    @Override
    public void OnSendFriendRequestResponse(String requestSenderId, boolean accept) {
        //
        mFriendRequestResponseListener.OnSendFriendRequestResponse(requestSenderId, accept);
    }



}

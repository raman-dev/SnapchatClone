package com.example.snapchatclone;


import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.amplifyframework.auth.AuthChannelEventName;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.SubscriptionToken;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ClickableSpan mClickableSpan;
    private FragmentManager fragmentManager;
    private SubscriptionToken hubToken;
    private LoginFragment mLoginFragment;

    public interface BottomSheetBackPressListener {
        boolean OnBackPressed();
    }

    private BottomSheetBackPressListener mBottomSheetBackPressListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                //dismiss the signin dialog
                Log.i("MainActivity", "Clicked Create Account!");
                fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, new SignUpFragment())
                        .addToBackStack(null)
                        .commit();
            }
        };

        fragmentManager = getSupportFragmentManager();
        mLoginFragment = new LoginFragment();
        mLoginFragment.setClickableSpan(mClickableSpan,getResources().getString(R.string.create_account));
        //for explicit sign in and sign out events
        //


        Log.i(TAG, "onCreate!");
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );*/

        hubToken = Amplify.Hub.subscribe(HubChannel.AUTH,
                hubEvent -> {
                    if (hubEvent.getName().equals(InitializationStatus.SUCCEEDED.toString())) {
                        Log.i("AuthQuickstart", "Auth successfully initialized");
                    } else if (hubEvent.getName().equals(InitializationStatus.FAILED.toString())){
                        Log.i("AuthQuickstart", "Auth failed to succeed");
                    } else {
                        switch (AuthChannelEventName.valueOf(hubEvent.getName())) {
                            case SIGNED_IN:
                                Log.i("AuthQuickstart", "Auth just became signed in.");
                                //user clicked login and login happened
                                if(fragmentManager.findFragmentByTag("MainFragment") == null) {
                                    MainAppFragment fragment = new MainAppFragment();
                                    mBottomSheetBackPressListener = fragment;
                                    fragmentManager.beginTransaction()
                                            .replace(R.id.fragmentContainer, fragment,"MainFragment")
                                            .commit();
                                }
                                break;
                            case SIGNED_OUT:
                                Log.i("AuthQuickstart", "Auth just became signed out.");
                                fragmentManager.beginTransaction()
                                        .replace(R.id.fragmentContainer, mLoginFragment,"LoginFragment")
                                        .commit();
                                break;
                            case SESSION_EXPIRED:
                                Log.i("AuthQuickstart", "Auth session just expired.");
                                fragmentManager.beginTransaction()
                                        .replace(R.id.fragmentContainer, mLoginFragment,"LoginFragment")
                                        .commit();
                                break;
                            default:
                                Log.w("AuthQuickstart", "Unhandled Auth Event: " + AuthChannelEventName.valueOf(hubEvent.getName()));
                                break;
                        }
                    }
                }
        );

        if(Amplify.Auth.getCurrentUser() == null){
            //if we pause with signup activity
            if(fragmentManager.getBackStackEntryCount() == 0) {
                if(!mLoginFragment.isAdded()) {
                    fragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, mLoginFragment, "LoginFragment").commit();
                }
            }
        }else{
            MainAppFragment fragment = new MainAppFragment();
            mBottomSheetBackPressListener = fragment;
            fragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment,"MainFragment").commit();
        }
    }

    @Override
    public void onBackPressed() {
        //listener returns false if we didnt consume the back pressed event
        if(mBottomSheetBackPressListener == null || !mBottomSheetBackPressListener.OnBackPressed()){
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause!");
        Amplify.Hub.unsubscribe(hubToken);

    }

}

package com.example.snapchatclone;

import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity implements CustomSignInFragment.SignInDialogListener, CustomSignUpFragment.SignUpDialogListener {

    private static final String TAG = "MainActivity";
    private static final int NUM_PAGES = 2;
    private SpannableString mCreateAccountSpanString;//= new SpannableString("Create Account");
    private ClickableSpan mClickableSpan;
    private final String mLinkString = "Create Account";
    private CustomSignInFragment mSignInDialog;
    private CustomSignUpFragment mSignUpDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager pager = findViewById(R.id.pager);
        FragmentStatePagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        mCreateAccountSpanString = new SpannableString(mLinkString);
        Log.i(TAG, "onCreate!");
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        showSignInDialog();
        //check here if user is remembered
        //implement after
        //call sign up on first run
        //then call sign in after every run
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause!");
    }

    private void showSignInDialog() {
        if (mSignInDialog == null) {
            mSignInDialog = new CustomSignInFragment();
            mSignInDialog.setCancelable(false);

            mClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    //dismiss the signin dialog
                    Log.i("MainActivity","Clicked Create Account!");
                    mSignInDialog.dismiss();
                    showSignUpDialog();
                }
            };
            mCreateAccountSpanString.setSpan(mClickableSpan, 0, mLinkString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSignInDialog.setSpanText(mCreateAccountSpanString);
        }
        mSignInDialog.show(getSupportFragmentManager(), "SignInDialog");

    }

    private void showSignUpDialog() {
        if (mSignUpDialog == null) {
            mSignUpDialog = new CustomSignUpFragment();
        }
        mSignUpDialog.show(getSupportFragmentManager(), "SignUpDialog");
    }


    public void signIn(String username, String password) {
        Log.i("SignInDialog", "username,password => " + username + ", " + password);

        /*Amplify.Auth.signIn(
                username, //grab username from dialog
                password, //grab password from dialog
                result -> Log.i("AuthQuickstart", result.isSignInComplete() ? "Sign in succeeded" : "Sign in not complete"),
                error ->
                        //now what?
                        Log.e("AuthQuickstart", error.toString())
       );*/
    }

    public void signUp(String username, String email, String password, String confirm_password) {
        Log.i("SignInDialog", "username => " + username);
        Log.i("SignInDialog", "email => " + email);
        Log.i("SignInDialog", "password => " + password);
        Log.i("SignInDialog", "confirm_password => " + confirm_password);

        /*Amplify.Auth.signUp(
            "username",
            "Password123",
            AuthSignUpOptions.builder().userAttribute(AuthUserAttributeKey.email(), "ramandeepbhatti@live.ca").build(),
            result -> Log.i("AuthQuickStart", "Result: " + result.toString()),
            error -> Log.e("AuthQuickStart", "Sign up failed", error)
          );*/
    }

    @Override
    public void onDialogSignIn(DialogFragment dialog) {
        //sign in
        String username = ((EditText) dialog.getDialog().findViewById(R.id.username)).getText().toString();
        String password = ((EditText) dialog.getDialog().findViewById(R.id.password)).getText().toString();
        if (username.length() <= 0 || password.length() <= 0) {
            return;
        }
        dialog.dismiss();
        //username = "user123";
        //password = "password123";
        signIn(username, password);
    }

    @Override
    public void onDialogCancelSignIn(DialogFragment dialog) {
        dialog.dismiss();
        finish();//don't continue without signing in
    }

    @Override
    public void onDialogSignUp(DialogFragment dialog) {
        //need to grab more stuff here
        String username = ((EditText) dialog.getDialog().findViewById(R.id.signup_username)).getText().toString();
        String email = ((EditText) dialog.getDialog().findViewById(R.id.email_address)).getText().toString();
        String password = ((EditText) dialog.getDialog().findViewById(R.id.signup_password)).getText().toString();
        String confirm_password = ((EditText) dialog.getDialog().findViewById(R.id.confirm_password)).getText().toString();
        if (username.length() <= 0 || password.length() <= 0 || !password.equals(confirm_password)) {
            return;
        }

        dialog.dismiss();
        signUp(username, email, password, confirm_password);
    }

    @Override
    public void onDialogCancelSignUp(DialogFragment dialog) {
        //return to sign in page
        dialog.dismiss();
        showSignInDialog();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    Log.i(TAG, "getFollowerFragment!");
                    return new FollowerFragment();
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

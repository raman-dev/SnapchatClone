package com.example.snapchatclone.auth;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.core.Amplify;
import com.example.snapchatclone.R;
import com.google.android.material.textfield.TextInputLayout;

public class SignUpFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "SignUpFragment";
    private static final int USER_SIGN_UP_RESULT = 2;
    private static final int SIGN_UP_ERROR = 3;

    private TextView mSignUpLabel;

    private TextInputLayout mUsernameEditText;
    private TextInputLayout mPasswordEditText;
    private TextInputLayout mConfirmPasswordEditText;
    private TextInputLayout mEmailEditText;

    private Button mSignUpButton;

    private AmplifySignUpResultHandler mAmplifySignUpResultHandler;
    private FragmentRemoveListener mFragmentRemoveListener;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_left_fade_in));
        setExitTransition(inflater.inflateTransition(R.transition.slide_right_fade_out));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.signup_layout,container,false);

        Toolbar toolbar = view.findViewById(R.id.signup_toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            //Log.i(TAG,"go back to login");
            getParentFragmentManager()
                    .popBackStack();//remove myself from the backstack
        });

        mSignUpLabel = view.findViewById(R.id.signup_label);
        mUsernameEditText = view.findViewById(R.id.signup_username);
        mEmailEditText = view.findViewById(R.id.signup_email);
        mPasswordEditText = view.findViewById(R.id.signup_passwordInputLayout);
        mConfirmPasswordEditText = view.findViewById(R.id.signup_confirmPasswordInputLayout);

        mSignUpButton = view.findViewById(R.id.signup_button);
        mSignUpButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAmplifySignUpResultHandler = new AmplifySignUpResultHandler(Looper.myLooper());//always running on main ui thread
    }



    @Override
    public void onPause() {
        super.onPause();
        mAmplifySignUpResultHandler.removeCallbacksAndMessages(null);
        mAmplifySignUpResultHandler = null;
    }

    public String getUserName() {
        return mUsernameEditText.getEditText().getText().toString();
    }

    public String getEmail() {
        return mEmailEditText.getEditText().getText().toString();
    }

    public String getPassword() {
        String password = mPasswordEditText.getEditText().getText().toString();
        String confirmation = mConfirmPasswordEditText.getEditText().getText().toString();

        if(!password.equals(confirmation)){
            //throw new InputMismatchException();
            mConfirmPasswordEditText.setError("Passwords do not match!");
            return null;
        }
        return password;
    }

    public void setFragmentRemoveListener(FragmentRemoveListener mFragmentRemoveListener) {
        this.mFragmentRemoveListener = mFragmentRemoveListener;
    }

    public interface FragmentRemoveListener {
        void OnSignUpFragmentRemoved();
    }

    private class AmplifySignUpResultHandler extends Handler {
        public AmplifySignUpResultHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            if(msg.what == USER_SIGN_UP_RESULT){
                boolean isSignedUp = (boolean)msg.obj;
                if(isSignedUp){
                    Context context = getActivity().getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, "Account Created!", duration);
                    toast.show();
                    getActivity().getSupportFragmentManager().popBackStack();//return to the login fragment
                }
            }else if(msg.what == SIGN_UP_ERROR){
                String message = (String)msg.obj;
                Context context = getActivity().getApplicationContext();
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, message, duration);
                toast.show();
                showSigningUp(false);//sign up failed
            }
            else {
                super.handleMessage(msg);
            }
        }
    }

    public void signUp(String username, String email, String password) {
        //Log.i("SignUpDialog", "username => " + username);
        //Log.i("SignUpDialog", "email => " + email);
        //Log.i("SignUpDialog", "password => " + password);
        Amplify.Auth.signUp(
                email,
                password,
                AuthSignUpOptions.builder().userAttribute(AuthUserAttributeKey.preferredUsername(), username).build(),
                result -> {
                    //Log.i("AuthQuickStart", "Result: " + result.toString());
                    mAmplifySignUpResultHandler.sendMessage(mAmplifySignUpResultHandler.obtainMessage(USER_SIGN_UP_RESULT,result.isSignUpComplete()));
                },
                error -> {
                    Log.e("AuthQuickStart", "Sign up failed", error);
                    String message = error.getCause().getMessage().split("\\.")[0];
                    mAmplifySignUpResultHandler.sendMessage(mAmplifySignUpResultHandler.obtainMessage(SIGN_UP_ERROR,message));
                }
        );
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentRemoveListener.OnSignUpFragmentRemoved();
    }

    @Override
    public void onClick(View v) {
        String password = getPassword();
        if(password == null){
            return;
        }
        showSigningUp(true);
        signUp(getUserName(),getEmail(),password);
    }

    private void showSigningUp(boolean isSigningUp) {

        mSignUpButton.setEnabled(!isSigningUp);
        mUsernameEditText.setEnabled(!isSigningUp);
        mEmailEditText.setEnabled(!isSigningUp);
        mPasswordEditText.setEnabled(!isSigningUp);
        mConfirmPasswordEditText.setEnabled(!isSigningUp);

        if(isSigningUp){
            mSignUpButton.setTextColor(Color.GRAY);
            mSignUpLabel.setText("Signing Up...");
        }else{
            mSignUpButton.setTextColor(Color.BLACK);
            mSignUpLabel.setText("Sign Up");
        }
    }
}

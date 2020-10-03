package com.example.snapchatclone.auth;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amplifyframework.core.Amplify;
import com.example.snapchatclone.MainActivity;
import com.example.snapchatclone.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class LoginFragment extends Fragment implements
        View.OnClickListener,
        VerificationCodeDialog.VerificationDialogListener,
        SignUpFragment.FragmentRemoveListener
{

    private static final int USER_NOT_CONFIRMED = 0;
    private static final int UNHANDLED_AUTH_EVENT = 1;
    private static final int USER_CONFIRMED = 2;
    private static final int USER_CONFIRMATION_FAILED = 3;
    private static final int CONFIRMATION_RESENT = 4;



    public static final String LOGIN_TITLE = "Log In";
    public static final String LOGGING_IN_MESSAGE="Logging In...";

    private SpannableString mCreateAccountSpanString;
    private TextInputLayout mEmailEditText;//email is login username
    private TextInputLayout mPasswordEditText;
    private TextView mLoginLabel;
    private TextView mCreateAccountTextView;
    private ProgressBar mProgressBar;
    private CoordinatorLayout mCoordinatorLayout;
    private Button mLoginButton;

    private AmplifySignInResultHandler mAmplifySignInResultHandler;

    private static final String TAG = "LoginFragment";
    private boolean isLaunchingSignUp;
    private LoginCompleteListener mLoginListener;

    public void setClickableSpan(ClickableSpan mClickableSpan, String linkString) {
        mCreateAccountSpanString = new SpannableString(linkString);
        mCreateAccountSpanString.setSpan(mClickableSpan, 0, linkString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.fade_in));
        setExitTransition(inflater.inflateTransition(R.transition.fade_out));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_layout, container, false);

        //Log.i(TAG, "onCreateView!");

        mCoordinatorLayout = view.findViewById(R.id.login_coordinator);
        mEmailEditText = view.findViewById(R.id.login_email);
        mPasswordEditText = view.findViewById(R.id.login_password);
        mProgressBar = view.findViewById(R.id.login_progressBar);
        mLoginLabel = view.findViewById(R.id.login_label);


        setClickableSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                if (isLaunchingSignUp) {
                    return;
                }
                isLaunchingSignUp = true;
                SignUpFragment fragment = new SignUpFragment();
                fragment.setFragmentRemoveListener(LoginFragment.this);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.login2_container, fragment)
                        ///use add so fragment slides over without destroying login
                        //don't use add
                        //when using .add(parent_layout,fragment)
                        //the last fragment stays alive
                        //when using .replace(parent_layout,fragment)
                        .addToBackStack("SignUpFragment")
                        .commit();
            }
        },"Create Account");
        mCreateAccountTextView = view.findViewById(R.id.createAccount2_textView);
        mCreateAccountTextView.setText(mCreateAccountSpanString);
        mCreateAccountTextView.setMovementMethod(LinkMovementMethod.getInstance());

        mLoginButton = view.findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void ConfirmCode(String verificationCode) {

        Log.i(TAG,verificationCode);
        Amplify.Auth.confirmSignUp(
                getEmail(),
                verificationCode,
                result -> {
                        boolean isSignUpComplete = result.isSignUpComplete();
                        mAmplifySignInResultHandler.sendMessage(mAmplifySignInResultHandler.obtainMessage(USER_CONFIRMED,isSignUpComplete));
                        //Log.i("AmplifyAuth", isSignUpComplete ? "Confirm signUp succeeded" : "Confirm sign up not complete");
                    },
                error -> {
                    Log.e("AmplifyAuth", error.toString());
                    mAmplifySignInResultHandler.sendMessage(mAmplifySignInResultHandler.obtainMessage(USER_CONFIRMATION_FAILED,error.getCause().getMessage()));
                }
        );

    }

    @Override
    public void onResume() {
        super.onResume();
        mAmplifySignInResultHandler = new AmplifySignInResultHandler(Looper.getMainLooper(),this,mCoordinatorLayout);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAmplifySignInResultHandler.removeCallbacksAndMessages(null);
        mAmplifySignInResultHandler = null;
    }

    @Override
    public void CancelConfirmCode() {
        //reenable login inputs
        showLoggingIn(false,null);
    }

    @Override
    public void OnSignUpFragmentRemoved() {
        isLaunchingSignUp = false;
    }

    public interface LoginCompleteListener {
        void OnUserSignedIn();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mLoginListener = (LoginCompleteListener)context;
    }

    private class AmplifySignInResultHandler extends Handler {
        private LoginFragment fragment;
        private CoordinatorLayout snackbarView;
        private SpannableStringBuilder ssb;

        public AmplifySignInResultHandler(Looper mainLooper,LoginFragment fragment,CoordinatorLayout view) {
            super(mainLooper);
            this.fragment = fragment;
            snackbarView = view;
            String snackText = getResources().getString(R.string.verification_code_error);
            ssb = new SpannableStringBuilder().append(snackText);
            ssb.setSpan(new ForegroundColorSpan(Color.WHITE), 0, snackText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case USER_NOT_CONFIRMED:
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    VerificationCodeDialog dialog = VerificationCodeDialog.newInstance(getEmail());
                    dialog.setVerificationDialogListener(fragment);
                    dialog.show(fragment.getFragmentManager(),"VerificationCodeDialog");
                    break;
                case UNHANDLED_AUTH_EVENT:
                    showLoggingIn(false,(String)msg.obj);
                    break;
                case USER_CONFIRMED:
                    boolean signUpComplete = (boolean) msg.obj;
                    showLoggingIn(signUpComplete,null);
                    if(signUpComplete) {
                        signIn(getEmail(), getPassword());
                    }
                    break;
                case USER_CONFIRMATION_FAILED:
                    //needs to be dismissed
                    Snackbar snackbar = Snackbar.make(snackbarView,ssb,Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("RE-SEND",v -> resendConfirmation());
                    snackbar.show();
                    showLoggingIn(false,null);
                    break;
                case CONFIRMATION_RESENT:
                    showLoggingIn(false,null);
                    Toast.makeText(getActivity().getApplicationContext(),"Verification Code Sent",Toast.LENGTH_LONG).show();
                    break;
                case MainActivity.SIGNED_IN:
                    mLoginListener.OnUserSignedIn();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void resendConfirmation() {
        AWSMobileClient awsMobileClient = (AWSMobileClient) Amplify.Auth.getPlugin("awsCognitoAuthPlugin").getEscapeHatch();
        if (awsMobileClient != null) {
            awsMobileClient.resendSignUp(getEmail(), new Callback<SignUpResult>() {
                @Override
                public void onResult(SignUpResult result) {

                    mAmplifySignInResultHandler.sendEmptyMessage(CONFIRMATION_RESENT);
                    Log.i("AuthQuickStart","Verification Code Resent.");
                }

                @Override
                public void onError(Exception e) {
                    Log.e("AuthQuickStart", e.toString());
                }
            });
        }
    }

    public void signIn(String username, String password) {
        Log.i("SignInDialog", "username,password => " + username + ", " + password);
        Amplify.Auth.signIn(
                username, //grab username from dialog
                password, //grab password from dialog
                result ->// this event is handled in the mainactivity
                    {
                        Log.i("AmplifyAuth", result.isSignInComplete() ? "Sign in succeeded" : "Sign in not complete");
                        if(result.isSignInComplete()){
                            mAmplifySignInResultHandler.sendEmptyMessage(MainActivity.SIGNED_IN);
                        }
                        //we need to query for the user object that belongs to current user
                    },
                error -> {
                    //now what?

                    Log.e("AmplifyAuth", error.toString());

                    if(error.getCause() instanceof UserNotConfirmedException){
                        //launch confirmation dialog
                        //run on ui thread?
                        //do what now???
                        mAmplifySignInResultHandler.sendEmptyMessage(USER_NOT_CONFIRMED);
                    }else{
                        String error_message = error.getCause().getMessage().split("\\.")[0];
                        //pass message to main
                        mAmplifySignInResultHandler.sendMessage(mAmplifySignInResultHandler.obtainMessage(UNHANDLED_AUTH_EVENT,error_message));
                    }
                }
        );
    }

    public String getPassword() {
        return mPasswordEditText.getEditText().getText().toString();
    }

    public String getEmail() {
        return mEmailEditText.getEditText().getText().toString();
    }

    @Override
    public void onClick(View v) {

        String email = getEmail();
        String password = getPassword();
        //make sure non empty
        if(email.length() == 0 || password.length() == 0){
            mAmplifySignInResultHandler.sendMessage(mAmplifySignInResultHandler.obtainMessage(UNHANDLED_AUTH_EVENT,"Empty password/email field."));
            return;
        }

        showLoggingIn(true,null);
        signIn(email, password);
    }

    /**
     * If user is logging in successfully change ui to reflect this else
     * @param loggingIn Flag if user is logging in
     */
    private void showLoggingIn(boolean loggingIn, String message){
        //if logging in then disable login button
        //else enable if not logging in
        mLoginButton.setEnabled(!loggingIn);
        mLoginButton.setClickable(!loggingIn);
        mLoginButton.setFocusable(!loggingIn);

        mEmailEditText.setEnabled(!loggingIn);
        mPasswordEditText.setEnabled(!loggingIn);
        mCreateAccountTextView.setEnabled(!loggingIn);

        if(loggingIn) {
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mLoginLabel.setText(LOGGING_IN_MESSAGE);
            mLoginButton.setTextColor(Color.GRAY);
        }else{
            if(message != null) {
                Context context = getActivity().getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, message, duration);
                toast.show();
            }

            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mLoginLabel.setText(LOGIN_TITLE);
            mLoginButton.setTextColor(Color.BLACK);
        }

    }
}

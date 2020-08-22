package com.example.snapchatclone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.amplifyframework.core.Amplify;

public class VerificationCodeDialog extends DialogFragment {

    private String username;
    private EditText mVerificationCodeEditText;
    private VerificationDialogListener listener;

    public interface VerificationDialogListener {
        void ConfirmCode(String verificationCode);
        void CancelConfirmCode();
    }

    public static VerificationCodeDialog newInstance(String username) {
        VerificationCodeDialog fragment = new VerificationCodeDialog();
        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("login_username", username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        username = getArguments().getString("login_username");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.verification_code_layout,null);
        mVerificationCodeEditText = view.findViewById(R.id.verification_code);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.confirm, (dialog, id) -> {
                    // sign in the user ...
                    //try to confirm sign up
                    listener.ConfirmCode(mVerificationCodeEditText.getText().toString());
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    //LoginDialogFragment.this.getDialog().cancel();\

                    dialog.dismiss();//dismiss
                    //show toast
                    Toast.makeText(getActivity().getApplicationContext(),"Enter Verification Code to Sign In",Toast.LENGTH_LONG).show();
                    listener.CancelConfirmCode();
                });
        builder.setTitle("Enter Verification Code");
        return builder.create();
    }

    public void setVerificationDialogListener(VerificationDialogListener listener){
        this.listener = listener;
    }
}

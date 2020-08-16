package com.example.snapchatclone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class CustomSignUpFragment extends DialogFragment {

    public interface SignUpDialogListener {
        public void onDialogSignUp(DialogFragment dialog);
        public void onDialogCancelSignUp(DialogFragment dialog);
    }

    private SignUpDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Create the AlertDialog object and return it
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.signup_layout, null);
        Toolbar toolbar = (Toolbar)view.findViewById(R.id.signUpToolbar);
        toolbar.setTitle("Create Account");
        toolbar.setTitleTextColor(Color.WHITE);

        builder.setView(view)
                .setPositiveButton(R.string.create, (dialogInterface, i) -> {
                    //sign user in here
                    Log.i("SignUpFragment", "Sign user up!");
                    listener.onDialogSignUp(this);
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    //this.getDialog().cancel();
                    listener.onDialogCancelSignUp(this);
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (SignUpDialogListener)context;
    }
}

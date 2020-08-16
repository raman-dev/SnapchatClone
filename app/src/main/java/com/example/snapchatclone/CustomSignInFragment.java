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
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class CustomSignInFragment extends DialogFragment {

    private SpannableString mCreateAccountSpanString;

    public void setSpanText(SpannableString mCreateAccountSpanString) {
        this.mCreateAccountSpanString = mCreateAccountSpanString;
    }

    public interface SignInDialogListener {
        public void onDialogSignIn(DialogFragment dialog);
        public void onDialogCancelSignIn(DialogFragment dialog);
    }

    private SignInDialogListener mSignInClickListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Create the AlertDialog object and return it
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.signin_layout, null);

        Log.i("SignInFragment","onCreateDialog!");
        TextView textView = view.findViewById(R.id.createAccountLink);
        textView.setText(mCreateAccountSpanString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        Toolbar toolbar = (Toolbar)view.findViewById(R.id.my_toolbar);
        toolbar.setTitle("Login");
        toolbar.setTitleTextColor(Color.WHITE);

        builder.setView(view)
                .setPositiveButton(R.string.sign_in, (dialogInterface, i) -> {
                    //sign user in here
                    Log.i("SignInFragment", "Sign user in!");
                    mSignInClickListener.onDialogSignIn(this);
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    //this.getDialog().cancel();
                    mSignInClickListener.onDialogCancelSignIn(this);
                });

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        //cast context to listener since we are going to make mainactivity implement interface
        mSignInClickListener = (SignInDialogListener) context;

    }


}
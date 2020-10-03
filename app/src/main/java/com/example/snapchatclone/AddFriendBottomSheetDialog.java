package com.example.snapchatclone;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddFriendBottomSheetDialog extends BottomSheetDialogFragment implements View.OnClickListener {

    private TextView mUsernameTextview;
    private String userId;
    private String authorUsername;

    public interface SendFriendRequestListener {
        void SendFriendRequest(String recipientId,String authorUsername);
    }

    private SendFriendRequestListener mListener;

    public static AddFriendBottomSheetDialog newInstance(String authorUsername,String username,String userId) {
        AddFriendBottomSheetDialog fragment = new AddFriendBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString("USERNAME", username);
        args.putString("USER_ID", userId);
        args.putString("AUTHOR_USERNAME",authorUsername);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_friend_layout,container,false);
        view.findViewById(R.id.addFriend_button).setOnClickListener(this);
        String username = getArguments().getString("USERNAME");
        userId = getArguments().getString("USER_ID");
        authorUsername = getArguments().getString("AUTHOR_USERNAME");
        mUsernameTextview = view.findViewById(R.id.addFriend_username);
        mUsernameTextview.setText(username);
        return view;
    }

    @Override
    public void onClick(View view) {
        //send friend request
        //after the request close
        mListener.SendFriendRequest(userId,authorUsername);
        dismiss();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (SendFriendRequestListener)context;
    }
}

package com.example.snapchatclone;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     FriendRequestListDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class FriendRequestBottomSheet extends BottomSheetDialogFragment {

    //Customize parameter argument names
    private ArrayList<String> mDataSet;
    private ArrayList<String> mFriendRequestAuthorIds;

    private FriendRequestResponseListener mFriendRequestResponseListener;

    //Customize parameters
    public static FriendRequestBottomSheet newInstance(ArrayList<String> usernameList, ArrayList<String> userIds) {
        final FriendRequestBottomSheet fragment = new FriendRequestBottomSheet();
        final Bundle args = new Bundle();
        args.putStringArrayList("USERNAME_LIST",usernameList);
        args.putStringArrayList("FRIEND_REQUEST_AUTHOR_ID_LIST",userIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            Bundle args = getArguments();
            mDataSet = args.getStringArrayList("USERNAME_LIST");
            mFriendRequestAuthorIds = args.getStringArrayList("FRIEND_REQUEST_AUTHOR_ID_LIST");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.friend_request_list_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new FriendRequestAdapter());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mFriendRequestResponseListener = (FriendRequestResponseListener)context;
    }

    private class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

        public class FriendRequestViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView username;

            public FriendRequestViewHolder(@NonNull View itemView) {
                super(itemView);
                itemView.findViewById(R.id.friendRequestAccept_button).setOnClickListener(this);
                itemView.findViewById(R.id.friendRequestDecline_button).setOnClickListener(this);
                username = itemView.findViewById(R.id.friendRequestUsername_textView);
            }

            public void setData(String username) {
                this.username.setText(username);
            }

            @Override
            public void onClick(View view) {
                int id = view.getId();
                if(id == R.id.friendRequestAccept_button) {
                    mFriendRequestResponseListener.OnSendFriendRequestResponse(mFriendRequestAuthorIds.get(getAdapterPosition()), true);
                }else{
                    mFriendRequestResponseListener.OnSendFriendRequestResponse(mFriendRequestAuthorIds.get(getAdapterPosition()), false);
                }
                //now remove the selection
                mDataSet.remove(getAdapterPosition());
                notifyItemRemoved(getAdapterPosition());
            }
        }

        @NonNull
        @Override
        public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FriendRequestViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_item_layout,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
            holder.setData(mDataSet.get(position));
        }

        @Override
        public int getItemCount() {
            return mDataSet.size();
        }


    }

}
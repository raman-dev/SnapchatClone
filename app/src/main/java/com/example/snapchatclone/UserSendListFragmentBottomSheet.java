package com.example.snapchatclone;

import android.app.Dialog;
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
 *     UserSendListFragmentBottomSheet.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class UserSendListFragmentBottomSheet extends BottomSheetDialogFragment {


    private SendSnapListener mSnapListener;

    public interface SendSnapListener{
        void SendSnap(ChatWrapper wrapper);
    }

    //Customize parameters
    public static UserSendListFragmentBottomSheet newInstance() {
        final UserSendListFragmentBottomSheet fragment = new UserSendListFragmentBottomSheet();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private ArrayList<ChatWrapper> mDataSet;

    public void setData(ArrayList<ChatWrapper> mDataSet,SendSnapListener mSnapListener){
        this.mDataSet = mDataSet;
        this.mSnapListener = mSnapListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.MyBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_send_list_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new UserChatItemAdapter(mDataSet,mSnapListener));
    }

    private static class UserChatItemAdapter extends RecyclerView.Adapter<UserChatItemAdapter.UserViewHolder> {

        private final ArrayList<ChatWrapper> mDataSet;
        private final SendSnapListener mListener;


        private class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            final TextView username;
            private final  SendSnapListener listener;
            UserViewHolder(View itemView,SendSnapListener listener) {
                //Customize the item layout
                super(itemView);
                itemView.setOnClickListener(this);
                this.listener = listener;
                username = itemView.findViewById(R.id.sendSnapProfile_textView);
            }

            void setData(ChatWrapper wrapper){
                username.setText(wrapper.user.getUsername());
            }

            @Override
            public void onClick(View view) {
                listener.SendSnap(mDataSet.get(getAdapterPosition()));
            }
        }


        UserChatItemAdapter(ArrayList<ChatWrapper> mDataSet,SendSnapListener mListener) {
            this.mDataSet = mDataSet;
            this.mListener = mListener;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_send_item_layout,parent,false);
            //view.findViewById(R.id.sendItem_container).setClipToOutline(true);
            return new UserViewHolder(view,mListener);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            holder.setData(mDataSet.get(position));
        }

        @Override
        public int getItemCount() {
            return mDataSet.size();
        }

    }

}
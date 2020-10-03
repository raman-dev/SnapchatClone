package com.example.snapchatclone;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final ArrayList<MessageLite> mDataSet;
    private final String userId;

    public ChatMessageAdapter(ArrayList<MessageLite> mDataSet, String userId) {
        this.mDataSet = mDataSet;
        this.userId = userId;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView messageTextView;
        private View itemView;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.chat_messageTextView);
            this.itemView = itemView;
        }

        public void setData(MessageLite message) {
            //depending on who its from change gravity of the item
            //if the id matches signed in user then
            //
            if (!message.authorId.equals(userId)) {
                messageTextView.setGravity(Gravity.START);//if not a message from the user
                itemView.setBackgroundColor(Color.parseColor("#FFDDDDDD"));
            }
            String msgText = message.content;
            messageTextView.setText(msgText);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.setData(mDataSet.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


}

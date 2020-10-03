package com.example.snapchatclone;

import androidx.annotation.Nullable;

import com.amplifyframework.datastore.generated.model.User;

public class ChatWrapper {
    User user;//other user
    String conversationId;//conversation that signed in user and other user can modify


    public ChatWrapper(String conversationId, User user) {
        this.user = user;
        this.conversationId = conversationId;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj instanceof ChatWrapper){
            ChatWrapper other = (ChatWrapper)obj;
            return other.conversationId.equals(conversationId);
        }
        return false;
    }

}

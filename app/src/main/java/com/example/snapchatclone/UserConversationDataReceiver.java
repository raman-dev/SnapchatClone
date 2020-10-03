package com.example.snapchatclone;

import java.util.ArrayList;

public interface UserConversationDataReceiver {
    ArrayList<ChatWrapper> GetUserConversations();//a map from user to conversation
}

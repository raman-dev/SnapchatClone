package com.example.snapchatclone;

import com.amplifyframework.datastore.generated.model.FriendRequest;
import com.amplifyframework.datastore.generated.model.FriendRequestResponse;

public interface FriendRequestResponseListener {
    void OnSendFriendRequestResponse(String requestSenderId, boolean accept);
}

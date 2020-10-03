package com.example.snapchatclone;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.datastore.generated.model.DirectMessageChat;
import com.amplifyframework.datastore.generated.model.DirectMessageEditor;
import com.amplifyframework.datastore.generated.model.FriendRequest;
import com.amplifyframework.datastore.generated.model.FriendRequestResponse;
import com.amplifyframework.datastore.generated.model.Message;
import com.amplifyframework.datastore.generated.model.User;
import com.example.snapchatclone.auth.SignUpFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Grab conversation id's of all editor objects and then grab editors with same convo id but differrent userid
 */
public class ChatListFragment extends Fragment implements
        View.OnClickListener,
        ChatAdapter.OnConversationClickListener,
        ChatAdapter.OnSnapImageClickListener,
        FriendRequestResponseListener,
        FragmentRemovedListener,
        UserConversationDataReceiver,
        ProfileInteractListener,
        SendSnapFragment.SendSnapListener, SnapDisplayFragment.SnapViewedListener {


    private static final String TAG = "ChatListFragment";
    public static final int USER_QUERY_RESULT = 0;
    private static final int DIRECT_MESSAGE_CHAT_RESULT = 1;
    private static final int DIRECT_CHAT_EDITOR_RESULT = 2;
    private static final int DIRECT_MESSAGE_USER_CHAT_RESULT = 3;
    private static final int RECEIVED_FRIEND_REQUESTS = 4;
    private static final int RECEIVED_LIVE_FRIEND_REQUEST = 5;
    static final int RECEIVED_LIVE_MESSAGE = 6;
    private static final int USER_SIGNED_OUT = 7;
    private static final int FRIEND_REQUEST_RESPONSE_RECEIVED = 10;
    public static final int RECEIVED_USER_MESSAGE_LOCAL = 11;
    private static final int IMAGE_DOWNLOAD_RESULT = 13;
    private static final int RECEIVED_SNAPS = 14;
    static final int ACTION_RECEIVED = 15;
    static final int ACTION_SENT = 16;
    static final int ACTION_SENT_SNAP = 17;
    private static final int ACTION_RECEIVED_SNAP = 18;

    private RecyclerView mChatListRecyclerView;
    private ChatAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private User mUser;
    private ArrayList<ChatWrapper> mDirectChatDataSet;
    private HashMap<String, FriendRequest> mFriendRequestMap;//request by sender id
    private HashMap<String, ArrayList<Message>> mConversationMap;//conversation_id,message_list map
    private HashMap<String, LinkedList<SnapWrapper>> mSnapMap;

    private ServerResponseHandler mResponseHandler;
    private LiveResponseHandler mLiveResponseHandler;

    private BottomSheetBehavior mProfileBottomSheet;
    private TextView mFriendRequestNotificationTextView;
    private ReceiveMessageListener mReceiveMessageListener;
    private boolean isLaunchingNewFragment = false;
    private ProfileInteractListener mProfileInteractListener;
    private HashSet<String> mSnapViewedSet;

    @Override
    public ArrayList<ChatWrapper> GetUserConversations() {
        return mDirectChatDataSet;
    }

    @Override
    public void CloseProfile() {

    }

    @Override
    public void ShowFriendRequests() {
        if (mFriendRequestMap.size() > 0) {
            ArrayList<String> usernames = new ArrayList<>();
            ArrayList<String> userIds = new ArrayList<>();

            mFriendRequestMap.forEach((sender, request) -> {
                userIds.add(sender);
                usernames.add(request.getAuthorUsername());
            });
            FriendRequestBottomSheet
                    .newInstance(usernames, userIds)
                    .show(getParentFragmentManager(), "FriendRequestBottomSheetDialog");
        }
    }

    public void setBottomSheet(BottomSheetBehavior mProfileBottomSheet) {
        this.mProfileBottomSheet = mProfileBottomSheet;
    }

    public void ShowProfile() {
        mProfileBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public boolean onBackPressProfile() {
        return false;
    }

    @Override
    public void SetUsername(String username) {

    }

    public void setProfileInteractListener(ProfileInteractListener mProfileInteractListener) {
        this.mProfileInteractListener = mProfileInteractListener;
    }

    @Override
    public void OnSnapSent(Message snap) {
        //change the corresponding message
        mAdapter.setLastAction(snap.getConversationId(),ACTION_SENT_SNAP,snap.getCreatedAt());
        mAdapter.notifyDataSetChanged();
    }

    public void OnSnapReceived(SnapWrapper wrapper){
        String conversationId = wrapper.snap.getConversationId();
        mAdapter.setLastAction(conversationId,ACTION_RECEIVED_SNAP,wrapper.snap.getCreatedAt());
        //change the thumbnail to the latest image
        mAdapter.setThumbnail(getResources(),conversationId,wrapper.image_file);
        if(isVisible()){
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void OnSnapViewed(String conversationId) {
        mSnapViewedSet.add(conversationId);
        //we can then use this set to change thumbnails
    }

    /**
     * All fragments launched from this fragment implement this interface
     * so they can return the signal
     * use this to reset states and redraw chat items if need to
     */
    @Override
    public void OnFragmentRemoved() {
        isLaunchingNewFragment = false;
        mReceiveMessageListener = null;
        //redraw to reflect changes in state of conversation objects
        //thumbnails need to return to default two tone profile thumbnail
        //set of conversation ids that have had there image queue change
        if(mSnapViewedSet.size() > 0){
            mSnapViewedSet.forEach( conversationId ->{
                LinkedList<SnapWrapper> queue = mSnapMap.get(conversationId);
                //if there are still images in the queue then the thumbnail should now be the snap at the front of the queue
                if(queue.size() > 0){
                    //update thumbnail to the first image
                    //should be a priority queue sorted by time_stamp
                    mAdapter.setThumbnail(getResources(),conversationId,queue.getLast().image_file);
                }else{
                    //if the queue is now empty then change the thumbnail to the default profile thumbnail
                    mAdapter.setThumbnail(getResources(),conversationId,null);
                }
            });
        }
        mSnapViewedSet.clear();//we are now looking at the chat fragment
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Receive live messages from a user
     */
    public interface ReceiveMessageListener {
        void OnReceiveMessage(Message message);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.chat_list_layout, container, false);
        view.findViewById(R.id.profile).setOnClickListener(this);
        view.findViewById(R.id.search_imageView).setOnClickListener(this);

        mDirectChatDataSet = new ArrayList<>();
        mFriendRequestMap = new HashMap<>();//sender_id,friend_request map
        mConversationMap = new HashMap<>();
        mSnapMap = new HashMap<>();
        mSnapViewedSet = new HashSet<>();

        mResponseHandler = new ServerResponseHandler(Looper.getMainLooper());
        mLiveResponseHandler = new LiveResponseHandler(Looper.getMainLooper());
        mChatListRecyclerView = view.findViewById(R.id.userConversationRecyclerView);
        mLayoutManager = new LinearLayoutManager(inflater.getContext());
        mChatListRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ChatAdapter(mDirectChatDataSet, this, this);
        mChatListRecyclerView.setAdapter(mAdapter);

        mFriendRequestNotificationTextView = view.findViewById(R.id.friendRequestNotification_textView);
        //visibility initial is 0 until we fetch the friend requests and receive friend requests live
        mFriendRequestNotificationTextView.setVisibility(View.GONE);//when
        return view;
    }

    /**
     * Subscriptions handle live data
     * meaning that when User a sends a friend request to User b
     * while user b is within the application user b will receive the friend request
     * or the lifetime of the subscription. I am going to terminate connections when we logout or hit on pause
     */
    private void enableSubscriptions() {
        SnapchatAPI.SubscribeToFriendRequests(mLiveResponseHandler, RECEIVED_LIVE_FRIEND_REQUEST);
        SnapchatAPI.SubscribeToMessages(mLiveResponseHandler, RECEIVED_LIVE_MESSAGE);
        SnapchatAPI.SubscribeToFriendRequestResponse(mResponseHandler, FRIEND_REQUEST_RESPONSE_RECEIVED);//dont' user live response handler
    }

    @Override
    public void onResume() {
        super.onResume();
        //gets all friends and conversations as well
        if (mUser == null) {
            SnapchatAPI.QueryUserByUserId(mResponseHandler, USER_QUERY_RESULT);
            SnapchatAPI.QueryFriendRequests(mResponseHandler, RECEIVED_FRIEND_REQUESTS, null);
            enableSubscriptions();
        }
        //Log.i(TAG, "onResume");
    }

    @Override
    public void onStop() {
        super.onStop();
        SnapchatAPI.UnSubscribe();
        //Log.i(TAG, "onStop");
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.profile:
                //we clicked the toolbar inside the chat list fragment
                //and then we tell the main app fragment to show
                //mProfileInteractListener.ShowProfile();
                mProfileBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
            case R.id.search_imageView:
                //from right slide and fade in a search view where we can search by username
                //make the view unclickable after starting a transaction
                if (isLaunchingNewFragment) {
                    //if some fragment is already being created or launched don't create another one
                    return;
                }
                isLaunchingNewFragment = true;
                Fragment fragment = UserSearchFragment.newInstance(mUser.getUsername());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(UserSearchFragment.TAG)//add to back stack so when clicking back it will return to chat fragment
                        .commit();
                //show users
                break;
        }
    }

    @Override
    public void OnConversationClick(int position) {
        //do this check to stop multiple clicks on a conversation item
        //launch a chat fragment from here
        //given the position
        if (isLaunchingNewFragment) {
            //if some fragment is already being created or launched don't create another one
            return;
        }
        isLaunchingNewFragment = true;
        ChatWrapper wrapper = mDirectChatDataSet.get(position);
        ChatFragment fragment = ChatFragment.newInstance(
                wrapper.user.getUsername(),//other user username
                wrapper.user.getUserId(),//other user userid
                wrapper.conversationId,
                new ArrayList<>(mConversationMap.get(wrapper.conversationId)));
        mReceiveMessageListener = fragment;
        fragment.setLocalMessageHandler(mLiveResponseHandler);
        //when clicked we need to create and push to back stack
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.main_container, fragment)//so camera is paused
                //.add(R.id.main_container, fragment)
                .addToBackStack("ChatFragment")
                .commit();
    }

    @Override
    public void OnSnapImageClick(int position) {
        //this is a slide show that plays
        //i need to create another fragment hereT
        //that shows images from the queue when user taps on it
        //
        if (isLaunchingNewFragment) {
            //if some fragment is already being created or launched don't create another one
            return;
        }
        LinkedList<SnapWrapper> imageQueue = mSnapMap.get(mDirectChatDataSet.get(position).conversationId);
        //if there are no images to display return
        if (imageQueue == null || imageQueue.size() == 0) {
            return;
        }
        isLaunchingNewFragment = true;
        SnapDisplayFragment displayFragment = SnapDisplayFragment.newInstance();
        displayFragment.setImageQueue(mSnapMap.get(mDirectChatDataSet.get(position).conversationId));
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, displayFragment)
                .addToBackStack("SnapDisplayFragment")//add to back stack so when clicking back it will return to chat fragment
                .commit();
    }

    @Override
    public void OnSendFriendRequestResponse(String requestSenderId, boolean accept) {
        SnapchatAPI.SendFriendRequestResponse(mResponseHandler, FRIEND_REQUEST_RESPONSE_RECEIVED, mFriendRequestMap.get(requestSenderId), accept);
    }

    private class ServerResponseHandler extends Handler {
        public ServerResponseHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            switch (msg.what) {
                case FRIEND_REQUEST_RESPONSE_RECEIVED:
                    //gonna delete the friend request so remove it from the
                    //ui
                    //Log.i("ChatListFragment", "Removing friend request");
                    FriendRequestResponse response = (FriendRequestResponse) msg.obj;//if current user accepted the response
                    if (response.getAccepted()) {
                        //when a friend request response with accepted set to true is returned
                        //a conversation Id will come with it
                        //user the conversation id to query the chat object and the editors
                        //after this the normal process of creating dco, dce0 dce1
                        if (response.getRequestSenderId().equals(mUser.getUserId())) {
                            SnapchatAPI.DeleteFriendRequestResponse(response);
                        }
                        SnapchatAPI.QueryChatObject(
                                this,
                                DIRECT_MESSAGE_CHAT_RESULT,
                                response.getConversationId());
                        //response.getConversationId()
                        //continue with regular flow
                    }
                    mFriendRequestMap.remove(response.getRequestSenderId());//remove the request
                    updateFriendRequestNotification(mFriendRequestMap.size());
                    break;
                case USER_QUERY_RESULT:
                    //gets the signed in users' user object
                    mUser = (User) msg.obj;
                    mProfileInteractListener.SetUsername(mUser.getUsername());
                    //get all the user conversations
                    mUser.getConversations().forEach(editor -> {
                        //for each conversation the user is a part of
                        //grab the messages for that conversation
                        SnapchatAPI.QueryChatObject(
                                this,
                                DIRECT_MESSAGE_CHAT_RESULT,
                                editor.getConversationId());
                    });
                    //try and get the todos for
                    //now run a query for the message lists
                    break;
                case DIRECT_MESSAGE_CHAT_RESULT:
                    DirectMessageChat chat = (DirectMessageChat) msg.obj;
                    if (chat.getMessages() == null) {
                        mConversationMap.put(chat.getConversationId(), new ArrayList<>());
                        mSnapMap.put(chat.getConversationId(),new LinkedList<>());
                        mAdapter.setLastAction(chat.getConversationId(),-1,null);
                    } else {
                        ////Log.i(TAG,"asdefa");
                        
                        ArrayList<Message> chatMessages = new ArrayList<>();
                        ArrayList<Message> snaps = new ArrayList<>();
                        Message newest = null;
                        //sort into snaps and messages
                        for (Message message : chat.getMessages()) {
                            if(message.getIsSnap()){
                                if(message.getRecipientId().equals(mUser.getUserId()) && message.getUnread()) {
                                    snaps.add(message);
                                }
                            }else{
                                chatMessages.add(message);
                            }

                            if(newest == null){
                                newest = message;
                            }else{
                                if(newest.getCreatedAt().compareTo(message.getCreatedAt()) < 0){
                                    newest = message;
                                }
                            }
                        }
                        mConversationMap.put(chat.getConversationId(),chatMessages);
                        //download each snap
                        snaps.forEach( snap -> processSnapMessage(snap,mSnapMap.get(chat.getConversationId())));

                        if(newest != null) {

                            if (newest.getAuthorId().equals(mUser.getUserId())) {
                                if(newest.getIsSnap()) {
                                    mAdapter.setLastAction(chat.getConversationId(),ACTION_SENT_SNAP,newest.getCreatedAt());
                                }else {
                                    mAdapter.setLastAction(chat.getConversationId(), ACTION_SENT, newest.getCreatedAt());
                                }
                            }else{
                                if(newest.getIsSnap()) {
                                    mAdapter.setLastAction(chat.getConversationId(),ACTION_RECEIVED_SNAP,newest.getCreatedAt());
                                }else{
                                    mAdapter.setLastAction(chat.getConversationId(), ACTION_RECEIVED, newest.getCreatedAt());
                                }
                            }
                        }else{
                            mAdapter.setLastAction(chat.getConversationId(),-1,null);
                        }
                    }
                    //get the other direct message editor not the user currently signed in
                    SnapchatAPI.QueryChatEditors(
                            this,
                            DIRECT_CHAT_EDITOR_RESULT,
                            chat.getConversationId());
                    break;
                case DIRECT_CHAT_EDITOR_RESULT:
                    Iterable<DirectMessageEditor> editors = (Iterable<DirectMessageEditor>) msg.obj;
                    //editor.getUser();we gonna grab the user object and
                    //grab user connected to the editor
                    editors.forEach(editor -> {
                        if (!editor.getUserId().equals(mUser.getUserId())) {
                            //grab the user object so we can title the direct message chat
                            //this line is only for new editor objects so no messages exist yet
                            SnapchatAPI.QueryUserByConversationId(
                                    this,
                                    DIRECT_MESSAGE_USER_CHAT_RESULT,
                                    editor.getUserId(),
                                    editor.getConversationId());
                        }
                    });
                    break;
                case DIRECT_MESSAGE_USER_CHAT_RESULT:
                    ChatWrapper wrapper = (ChatWrapper) msg.obj;
                    //remove old conversation
                    if (mDirectChatDataSet.contains(wrapper)) {
                        mDirectChatDataSet.remove(wrapper);
                        mAdapter.notifyDataSetChanged();
                    }
                    mDirectChatDataSet.add(wrapper);
                    mAdapter.notifyItemInserted(mDirectChatDataSet.size() - 1);
                    break;
                case RECEIVED_FRIEND_REQUESTS:
                    //now what send the friend request list up to the mainappfragment
                    //and update my
                    PaginatedResult<FriendRequest> requests = (PaginatedResult<FriendRequest>) msg.obj;
                    requests.getItems().forEach(friendRequest -> {
                        mFriendRequestMap.put(friendRequest.getAuthorId(), friendRequest);
                    });
                    updateFriendRequestNotification(mFriendRequestMap.size());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Wrap snap object and create temp file for image download
     * call download image on snapchat api
     *
     * @param snap      The snap available notification received from server
     * @param snapQueue LIFO queue for snaps to be read from
     */
    private void processSnapMessage(Message snap, LinkedList<SnapWrapper> snapQueue) {
        LinkedList<SnapWrapper> queue = snapQueue;
        if (queue == null) {
            queue = new LinkedList<>();
            mSnapMap.put(snap.getConversationId(), queue);
        }
        SnapWrapper snapWrapper = new SnapWrapper(snap, new File(getContext().getFilesDir(), snap.getId() + ".jpg"));
        queue.addLast(snapWrapper);
        //how to add thumbnail
        //convo-queue map
        //map convo to adapter position
        //
        SnapchatAPI.DownloadImage(
                mLiveResponseHandler,
                IMAGE_DOWNLOAD_RESULT,
                snapWrapper);
    }

    private class LiveResponseHandler extends Handler {
        public LiveResponseHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            switch (msg.what) {
                case RECEIVED_LIVE_FRIEND_REQUEST:
                    //do something with the friend request
                    //Log.i("AmplifySubscription", "Received friend request");
                    //add to list of add friend notifications currently available
                    FriendRequest request = (FriendRequest) msg.obj;
                    mFriendRequestMap.put(request.getAuthorId(), request);
                    //update the icon
                    updateFriendRequestNotification(mFriendRequestMap.size());//mFriendRequestList.size());
                    break;
                case RECEIVED_LIVE_MESSAGE:
                    Message message = (com.amplifyframework.datastore.generated.model.Message) msg.obj;
                    //if the chat fragment is not currently visible
                    //then map it to the correct local chat list
                    if(!message.getIsSnap()) {
                        if (!mConversationMap.containsKey(message.getConversationId())) {
                            mConversationMap.put(message.getConversationId(), new ArrayList<>());
                        }
                        mConversationMap.get(message.getConversationId()).add(message);
                    }else{
                        processSnapMessage(message, mSnapMap.get(message.getConversationId()));
                    }
                    //if chat fragment is visible let the chat fragment update itself
                    //now depending on who sent the last message
                    //modify the chat object
                    //hold i am subscribed to messages for me
                    //so this will always be action_received
                    //
                    mAdapter.setLastAction(message.getConversationId(),ACTION_RECEIVED,message.getCreatedAt());
                    //maybe redraw
                    if (mReceiveMessageListener != null && !message.getIsSnap()) {
                        mReceiveMessageListener.OnReceiveMessage(message);
                    }else{
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                case RECEIVED_USER_MESSAGE_LOCAL:
                    Message message1 = (com.amplifyframework.datastore.generated.model.Message) msg.obj;
                    //if the chat fragment is not currently visible
                    //then map it to the correct local chat list
                    mConversationMap.get(message1.getConversationId()).add(message1);
                    if(!message1.getAuthorId().equals(mUser.getUserId())){
                        mAdapter.setLastAction(message1.getConversationId(),ACTION_RECEIVED,message1.getCreatedAt());
                    }else{
                        mAdapter.setLastAction(message1.getConversationId(),ACTION_SENT,message1.getCreatedAt());
                    }
                    break;
                case IMAGE_DOWNLOAD_RESULT:
                    //notify user of new snap
                    //notify user of new snap
                    //Log.i("ChatListFragment","RECEIVED SNAP!");
                    //change the thumbnail of
                    SnapWrapper wrapper = (SnapWrapper)msg.obj;
                    OnSnapReceived(wrapper);
                    //using the wrapper update the thumbnail image of the corresponding conversation object
                    //map wrapper.conversation -> adapter.position
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void updateFriendRequestNotification(int numFriendRequests) {
        int visibility = mFriendRequestNotificationTextView.getVisibility();
        if (numFriendRequests < 1) {
            if (visibility == View.VISIBLE) {
                mFriendRequestNotificationTextView.setVisibility(View.GONE);
            }
        } else {
            if (visibility == View.GONE) {
                mFriendRequestNotificationTextView.setVisibility(View.VISIBLE);
            }
            String friendRequestCountText = numFriendRequests + "";
            mFriendRequestNotificationTextView.setText(friendRequestCountText);
        }
    }
}

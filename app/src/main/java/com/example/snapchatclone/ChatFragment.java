package com.example.snapchatclone;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.transition.TransitionInflater;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.generated.model.Message;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment implements
        TextView.OnEditorActionListener,
        ChatListFragment.ReceiveMessageListener{
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private ChatMessageAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private EditText mChatEditText;

    private String authorId = null;
    private String conversationId = null;
    private String recipientId = null;
    //testing ids
    //place holder values we need to query and set these for each
    private String chatTitle;

    LinearLayout mChatContainer;

    private FragmentRemovedListener mFragmentRemovedListener;
    private Handler handler;

    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param conversationId the conversation id of the chat
     * @return A new instance of fragment ChatFragment.
     */
    public static ChatFragment newInstance(String chatTitle, String recipientId, String conversationId, ArrayList<Message> message_list) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        //split into 2 arrays since message is not serializable or extendable
        args.putString("CHAT_TITLE", chatTitle);
        args.putString("CONVERSATION_ID", conversationId);//conversationid
        args.putString("RECIPIENT_ID", recipientId);
        ArrayList<MessageLite> list = new ArrayList<>();
        message_list.forEach( message -> list.add(new MessageLite(message)));
        args.putParcelableArrayList("MESSAGE_LIST",list);
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView mRecyclerView;
    private ArrayList<MessageLite> mMessageList;

    public void setLocalMessageHandler(Handler handler){
        this.handler = handler;
    }

    //private Random random;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //getArguments().getString()
            Bundle bundle = getArguments();
            conversationId = bundle.getString("CONVERSATION_ID");
            chatTitle = bundle.getString("CHAT_TITLE");
            recipientId = bundle.getString("RECIPIENT_ID");
            mMessageList = bundle.getParcelableArrayList("MESSAGE_LIST");
            //create the message objects and then add it to the mdataset
        }
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_right));
        setExitTransition(inflater.inflateTransition(R.transition.slide_left));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.chat_layout, container, false);

        authorId = Amplify.Auth.getCurrentUser().getUserId();
        mAdapter = new ChatMessageAdapter(mMessageList, authorId);
        mRecyclerView = view.findViewById(R.id.chat_messageRecyclerView);
        mLayoutManager = new LinearLayoutManager(container.getContext());
        mLayoutManager.setStackFromEnd(true);//get recyclerview to show new items at the bottom
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        //sort the message list
        mMessageList.sort((m0, m1) -> m0.time_stamp.compareTo(m1.time_stamp));
        mChatEditText = view.findViewById(R.id.chat_textInputEditText);
        mChatEditText.setOnEditorActionListener(this);
        mChatContainer = view.findViewById(R.id.chat_container);
        ((TextView) view.findViewById(R.id.chat_title)).setText(chatTitle);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        //delay so we wait until transition is complete
        mChatEditText.requestFocus();
        if(mMessageList.size() > 0) {
            mRecyclerView.scrollToPosition(mMessageList.size() - 1);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mFragmentRemovedListener = (FragmentRemovedListener)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentRemovedListener.OnFragmentRemoved();
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendMessage(mChatEditText.getText().toString());
        }
        //clear text after sent is clicked
        mChatEditText.getText().clear();
        return false;
    }

    private void sendMessage(String content) {
        //send a message from here this should add the message to the recycler view but not
        //

        Message message = Message.builder()
                .authorId(authorId)
                .recipientId(recipientId)
                .conversationId(conversationId)
                .isSnap(false)
                .content(content)
                .build();
        mMessageList.add(new MessageLite(message));
        mAdapter.notifyItemInserted(mMessageList.size() - 1);
        mRecyclerView.scrollToPosition(mMessageList.size() - 1);
        //
        //ENABLE THIS TO SEND TO SERVER
        //need to give this a handle to the chat fragment handler
        SnapchatAPI.SendMessage(handler,ChatListFragment.RECEIVED_USER_MESSAGE_LOCAL,authorId,recipientId,conversationId,content);
    }

    /**
     * Receive messages in realtime
     * @param message Realtime message
     */
    @Override
    public void OnReceiveMessage(Message message) {
        mMessageList.add(new MessageLite(message));
        mAdapter.notifyItemInserted(mMessageList.size() - 1);
        mRecyclerView.scrollToPosition(mMessageList.size() - 1);
    }
}
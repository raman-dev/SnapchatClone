package com.example.snapchatclone;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.generated.model.DirectMessageEditor;
import com.amplifyframework.datastore.generated.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserSearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserSearchFragment extends Fragment implements SearchView.OnQueryTextListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    public static final String TAG = "UserSearchFragment";
    private static final int PAGE_LIMIT = 12;
    private static final long OPEN_KEYBOARD_DELAY = 80;
    private FragmentRemovedListener mFragmentRemovedListener;
    // TODO: Rename and change types of parameters

    public UserSearchFragment() {
        // Required empty public constructor
    }

    public static final int USER_SEARCH_RESULT = 0;

    private RecyclerView mSearchResultRecyclerView;
    private UserAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<User> mDataSet;
    private HashSet<String> mUserSet;//current users in set
    private UserSearchResultHandler mUserSearchResultHandler;
    private SearchView mSearchView;
    private String currentUserId = null;
    private String currentUsername = null;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment UserSearchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserSearchFragment newInstance(String currentUsername) {
        UserSearchFragment fragment = new UserSearchFragment();
        Bundle args = new Bundle();
        args.putString("CURRENT_USERNAME",currentUsername);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //get parameters for the fragment passed in here
            currentUsername = getArguments().getString("CURRENT_USERNAME");
        }
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_left));
        setExitTransition(inflater.inflateTransition(R.transition.slide_right));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.user_search_layout, container, false);
        mSearchView = view.findViewById(R.id.search_bar);
        mSearchView.setOnQueryTextListener(this);

        mSearchResultRecyclerView = view.findViewById(R.id.search_recyclerView);
        mDataSet = new ArrayList<>();
        mUserSet = new HashSet<>();
        mAdapter = new UserAdapter(mDataSet,getFragmentManager());
        mLayoutManager = new LinearLayoutManager(container.getContext());

        mSearchResultRecyclerView.setAdapter(mAdapter);
        mSearchResultRecyclerView.setLayoutManager(mLayoutManager);
        mSearchResultRecyclerView.addItemDecoration(new DividerItemDecoration(mSearchResultRecyclerView.getContext(),mLayoutManager.getOrientation()));

        mUserSearchResultHandler = new UserSearchResultHandler(Looper.getMainLooper());


        currentUserId = Amplify.Auth.getCurrentUser().getUserId();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        openKeyboard();
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

    private void openKeyboard(){
        //just run this 50ms later
        mSearchView.requestFocus();
    }

    @Override
    public boolean onQueryTextSubmit(String search_text) {
        //query_users(query,null);
        SnapchatAPI.QueryUsersByUsernameContains(
                mUserSearchResultHandler,
                USER_SEARCH_RESULT,
                currentUserId,
                search_text,
                PAGE_LIMIT,
                null);
        return true;
    }
    @Override
    public boolean onQueryTextChange(String username) {
        //search every time text
        //shits empry
        if(username.length() == 0) {
            mDataSet.clear();
            mUserSet.clear();
            mAdapter.notifyDataSetChanged();
        }//else{
            //to search while user is typing
            //don't do this might end up costing alot to me
            //query_users(username);
        //}
        return true;
    }

    /**
     * Handle async results from user query
     */
    private class UserSearchResultHandler extends Handler {
        public UserSearchResultHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == USER_SEARCH_RESULT) {
                if(msg.obj!=null){
                    //non empty result
                    PaginatedResult<User> result = (PaginatedResult<User>) msg.obj;
                    result.getItems().forEach(user ->{
                        if (!mUserSet.contains(user.getUserId())) {
                            mUserSet.add(user.getUserId());
                            List<DirectMessageEditor> convos = user.getConversations();
                            mDataSet.add(user);
                            mAdapter.notifyItemInserted(mDataSet.size() - 1);
                        }
                    });
                }
                return;
            }
            super.handleMessage(msg);
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> implements View.OnClickListener {
        ArrayList<User> mDataSet;
        FragmentManager fragmentManager;

        @Override
        public void onClick(View view) {
            //show add friend modal sheet
            //dialog.setUser(selectedUser);
            mSearchView.clearFocus();
            User selectedUser = mDataSet.get(Integer.parseInt((String)view.getTag()));
            AddFriendBottomSheetDialog dialog = AddFriendBottomSheetDialog.newInstance(
                    currentUsername,
                    selectedUser.getUsername(),
                    selectedUser.getUserId());
            dialog.show(fragmentManager,"AddFriendDialogShow");
        }

        private class UserViewHolder extends RecyclerView.ViewHolder {
            TextView username;
            View itemView;
            public UserViewHolder(@NonNull View itemView,View.OnClickListener listener) {
                super(itemView);
                this.itemView = itemView;
                this.itemView.setOnClickListener(listener);
                username = itemView.findViewById(R.id.item_username);
            }

            public void setData(User user,int position) {
                username.setText(user.getUsername());
                itemView.setTag(position+"");
            }
        }

        public UserAdapter(ArrayList<User> mDataSet, FragmentManager fragmentManager) {
            this.mDataSet = mDataSet;
            this.fragmentManager = fragmentManager;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_search_item_layout, parent, false);
            return new UserViewHolder(view,this);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            //maybe query before or query now?
            //now for each post in the post populate each holder
            holder.setData(mDataSet.get(position),position);
        }

        @Override
        public int getItemCount() {
            return mDataSet.size();
        }
    }
}
package com.example.snapchatclone;

import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.datastore.generated.model.Post;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class ChatFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "ChatFragment";
    private BottomSheetBehavior mBottomSheet;
    private RecyclerView mAppSyncRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<Post> mDataSet;


    public void setBottomSheet(BottomSheetBehavior mBottomSheet){
        this.mBottomSheet = mBottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.chat_layout, container, false);
        view.findViewById(R.id.profile).setOnClickListener(this);
        mAppSyncRecyclerView = view.findViewById(R.id.appsync_recyclerView);
        mLayoutManager = new LinearLayoutManager(inflater.getContext());
        mAppSyncRecyclerView.setLayoutManager(mLayoutManager);
        mDataSet = new ArrayList<>();
        mAdapter = new PostAdapter(mDataSet);
        mAppSyncRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.profile:
                mBottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
        }
    }

    private class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        ArrayList<Post> mDataSet;
        private class PostViewHolder extends RecyclerView.ViewHolder {
            TextView id;
            TextView title;
            TextView status;
            TextView rating;
            TextView content;

            public PostViewHolder(@NonNull View itemView) {
                super(itemView);

                id = itemView.findViewById(R.id.post_id);
                title = itemView.findViewById(R.id.post_title);
                status = itemView.findViewById(R.id.post_status);
                rating = itemView.findViewById(R.id.post_rating);
                content = itemView.findViewById(R.id.post_content);
            }

            public void setData(Post post){
                id.setText(post.getId());
                title.setText(post.getId());
                status.setText(""+post.getStatus());
                rating.setText(post.getRating());
                content.setText(post.getContent());
            }
        }

        public PostAdapter(ArrayList<Post> mDataSet){
            this.mDataSet = mDataSet;
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_item_layout,parent,false);
            PostViewHolder postViewHolder = new PostViewHolder(view);
            return postViewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            //maybe query before or query now?
            //now for each post in the post populate each holder
            for (int i = 0; i < mDataSet.size(); i++) {
                holder.setData(mDataSet.get(i));
            }
        }

        @Override
        public int getItemCount() {
            return mDataSet.size();
        }


    }


}

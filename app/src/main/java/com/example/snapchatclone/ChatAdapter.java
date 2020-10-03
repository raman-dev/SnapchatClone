package com.example.snapchatclone;

import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.UserViewHolder> {
    private static final long SECOND_MILLI = 1000; //1000ms in a second
    private static final long MIN_MILLI = SECOND_MILLI * 60;
    private static final long HOUR_MILLI = MIN_MILLI * 60;//60 mins
    private static final long DAY_MILLI = HOUR_MILLI * 24;//24 hours
    private static final long MONTH_MILLI = DAY_MILLI * 30;//30 days
    private static final long WEEK_MILLI = DAY_MILLI * 7;
    private static final String TAG = "ChatAdapter";

    private final OnSnapImageClickListener mSnapClickListener;
    private final HashMap<String, ThumbnailWrapper> mBitmapMap;
    ArrayList<ChatWrapper> mDataSet;
    OnConversationClickListener mListener;
    private HashMap<String, TimeStampActionWrapper> mLastActionMap;

    private class TimeStampActionWrapper {
        String time_stamp;
        int action;

        public TimeStampActionWrapper(String time_stamp, int action) {
            this.time_stamp = time_stamp;
            this.action = action;
        }
    }

    public void setThumbnail(Resources resources, String conversationId, File image_file) {
        //means thumbnail needs to be reset to default

        ThumbnailWrapper local = mBitmapMap.get(conversationId);
        //if we already have a thumbnail we can re-use it
        if (local != null) {
            //if the image sent in is null means we should reset to default thumbnail
            if (image_file == null) {
                local.updateThumbnail(resources);
            } else {
                local.updateThumbnail(resources, image_file);
            }
        } else {
            if (image_file == null) {
                mBitmapMap.put(conversationId, new ThumbnailWrapper(resources));
            } else {
                mBitmapMap.put(conversationId, new ThumbnailWrapper(resources, image_file));
            }
        }

    }

    public void setLastAction(String conversationId, int lastAction, String time_stamp) {
        this.mLastActionMap.put(conversationId, new TimeStampActionWrapper(time_stamp, lastAction));
    }

    public interface OnConversationClickListener {
        void OnConversationClick(int position);
    }

    public interface OnSnapImageClickListener {
        void OnSnapImageClick(int position);
    }

    class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final OnSnapImageClickListener snapListener;
        TextView username;
        ImageView profileView;
        TextView lastActionTextView;
        private final OnConversationClickListener listener;

        public UserViewHolder(@NonNull View itemView, OnConversationClickListener listener, OnSnapImageClickListener snapListener) {
            super(itemView);

            username = itemView.findViewById(R.id.chat_usernameTextView);
            this.listener = listener;
            this.snapListener = snapListener;
            itemView.findViewById(R.id.chatMessageContainer_constraintLayout).setOnClickListener(this);
            profileView = itemView.findViewById(R.id.chat_userProfileImageView);
            profileView.setOnClickListener(this);
            lastActionTextView = itemView.findViewById(R.id.chat_lastActionTextView);

        }

        public void setData(ChatWrapper wrapper) {
            username.setText(wrapper.user.getUsername());
            //if there is a thumbnail to show
            ThumbnailWrapper thumbnailWrapper = mBitmapMap.get(wrapper.conversationId);
            if (thumbnailWrapper != null) {
                if(thumbnailWrapper.isDefault) {
                    profileView.setImageDrawable(thumbnailWrapper.defaultThumbnail);
                }else {
                    profileView.setImageDrawable(thumbnailWrapper.roundedThumbnail);
                }
            }
            if (mLastActionMap.containsKey(wrapper.conversationId)) {
                //lastActionTextView.setText(wrapper.);
                String text = null;
                TimeStampActionWrapper lastAction = mLastActionMap.get(wrapper.conversationId);
                //there was never a last action
                if (lastAction.time_stamp == null) {
                    text = "Tap to chat.";//meaning no messages have ever been exchanged
                } else {
                    String time_elapsed = getTimeElapsed(lastAction.time_stamp);
                    if (lastAction.action == ChatListFragment.ACTION_SENT) {
                        text = "Sent - " + time_elapsed;
                    } else if (lastAction.action == ChatListFragment.ACTION_RECEIVED) {
                        text = "Received - " + time_elapsed;

                    } else if (lastAction.action == ChatListFragment.ACTION_SENT_SNAP) {
                        text = "Sent Snap - " + time_elapsed;
                    } else {
                        text = "Received Snap - " + time_elapsed;
                    }
                }
                lastActionTextView.setText(text);
            }
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.chat_userProfileImageView) {
                snapListener.OnSnapImageClick(getAdapterPosition());
            } else {
                listener.OnConversationClick(getAdapterPosition());
            }
        }
    }

    private String getTimeElapsed(String time_stamp) {
        //given time in iso8601 find difference between current time
        //and given time return in formatted string
        //find time elapsed
        long diff = Instant.now().toEpochMilli() - Instant.parse(time_stamp).toEpochMilli();
        //Log.i(TAG,"time_diff =>" +diff);
        //we'll say 30 days is a month
        if (diff % MONTH_MILLI == diff) {
            //less than a month elapsed
            if (diff % WEEK_MILLI == diff) {
                //less than a week elapsed
                if (diff % DAY_MILLI == diff) {
                    //less than day elapsed
                    if (diff % HOUR_MILLI == diff) {
                        //less than an hour elapsed
                        if (diff % MIN_MILLI == diff) {
                            //less than a minute don't care
                            //or less than a minute
                            return "less than a minute ago.";
                        } else {
                            return TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS) + "min";
                        }
                    } else {
                        return TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS) + "h";
                    }
                } else {
                    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + "d";
                }
            } else {
                return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 7 + "w";
            }
        } else {
            //at least 1 month elapsed
            return (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 30) + "mo";//months without decimal portion
        }

    }

    public ChatAdapter(ArrayList<ChatWrapper> dataSet, OnConversationClickListener listener, OnSnapImageClickListener snapListener) {
        mDataSet = dataSet;
        mListener = listener;
        mSnapClickListener = snapListener;
        mBitmapMap = new HashMap<>();
        mLastActionMap = new HashMap<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_user_item_layout, parent, false);
        return new UserViewHolder(view, mListener, mSnapClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        //maybe query before or query now?
        //now for each post in the post populate each holder
        holder.setData(mDataSet.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}

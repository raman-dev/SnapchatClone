package com.example.snapchatclone;

import android.os.Parcel;
import android.os.Parcelable;

import com.amplifyframework.datastore.generated.model.Message;

public class MessageLite implements Parcelable {
    String content;
    String authorId;
    String time_stamp;

    public MessageLite(Message message) {
        this.content = message.getContent();
        this.authorId = message.getAuthorId();
        this.time_stamp = message.getCreatedAt();
    }

    protected MessageLite(Parcel in) {
        content = in.readString();
        authorId = in.readString();
        time_stamp = in.readString();
    }

    public final Creator<MessageLite> CREATOR = new Creator<MessageLite>() {
        @Override
        public MessageLite createFromParcel(Parcel in) {
            return new MessageLite(in);
        }

        @Override
        public MessageLite[] newArray(int size) {
            return new MessageLite[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(content);
        parcel.writeString(authorId);
        parcel.writeString(time_stamp);
    }
}

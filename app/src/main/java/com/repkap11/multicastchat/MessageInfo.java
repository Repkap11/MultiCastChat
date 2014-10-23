package com.repkap11.multicastchat;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.charset.Charset;

public class MessageInfo implements Parcelable {
    public static final String KEY_MESSAGE_INFO = "KEY_MESSAGE_INFO";

    public String mUserName;
    public String mMessage;
    public String mTimeStamp;
    public String mSessionName;

    public MessageInfo(String userName, String message, String sessionName) {
        this(userName, message, sessionName, "" + System.currentTimeMillis());
    }

    private MessageInfo(String userName, String message, String sessionName, String timeStamp) {
        mUserName = userName;
        mMessage = message;
        mSessionName = sessionName;
        mTimeStamp = timeStamp;
    }
    public byte[]getBytesToTransmit(){
        return mMessage.getBytes(Charset.defaultCharset());
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUserName);
        dest.writeString(mMessage);
        dest.writeString(mSessionName);
        dest.writeString(mTimeStamp);
    }

    public static final Parcelable.Creator<MessageInfo> CREATOR =
            new Parcelable.Creator<MessageInfo>() {

                public MessageInfo createFromParcel(Parcel in) {
                    String userName = in.readString();
                    String message = in.readString();
                    String sessionName = in.readString();
                    String timeStamp = in.readString();
                    return new MessageInfo(userName, message, sessionName, timeStamp);
                }

                public MessageInfo[] newArray(int size) {
                    return new MessageInfo[size];
                }
            };
}

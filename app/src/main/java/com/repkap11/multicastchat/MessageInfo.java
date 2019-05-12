package com.repkap11.multicastchat;

import android.os.*;

import org.json.*;

import java.nio.charset.*;

public class MessageInfo implements Parcelable {
public static final String KEY_MESSAGE_INFO = "KEY_MESSAGE_INFO";

public String mUserName;
public String mMessage;
public long mTimeStamp;
public String mSessionName;
public int mFromLocalUser;

public MessageInfo(String userName, String message, String sessionName, boolean fromLocalUser) {
	this(userName, message, sessionName, System.currentTimeMillis(), fromLocalUser ? 1 : 0);
}

private MessageInfo(String userName, String message, String sessionName, long timeStamp, int fromLocalUser) {
	createInstance(userName, message, sessionName, timeStamp, fromLocalUser);
}

private void createInstance(String userName, String message, String sessionName, long timeStamp, int fromLocalUser) {
	mUserName = userName;
	mMessage = message == null ? "" : message;
	mSessionName = sessionName;
	mTimeStamp = timeStamp;
	mFromLocalUser = fromLocalUser;
}

public MessageInfo(byte[] data, int startingIndex, int length) {
	String rawData = new String(data, startingIndex, length);
	try {
		JSONObject jObject = new JSONObject(rawData);
		String userName = jObject.getString("mUserName");
		String message = jObject.getString("mMessage");
		String sessionName = jObject.getString("mSessionName");
		long timeStamp = jObject.getLong("mTimeStamp");
		createInstance(userName, message, sessionName, timeStamp, 0);
	} catch(Exception e) {
		createInstance("Unknown:", rawData, ActivityMain.SESSION_NAME, System.currentTimeMillis(), 0);
		//Fallback ot just sending the raw data as text...
	}
}

public byte[] getBytesToTransmit() {
	try {
		JSONObject jObject = new JSONObject();
		jObject.put("mUserName", mUserName);
		jObject.put("mMessage", mMessage);
		jObject.put("mSessionName", mSessionName);
		jObject.put("mTimeStamp", mTimeStamp);
		return jObject.toString().getBytes(Charset.defaultCharset());
	} catch(Exception e) {
		e.printStackTrace();
	}
	return null;
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
	dest.writeLong(mTimeStamp);
	dest.writeInt(mFromLocalUser);
}

public static final Parcelable.Creator<MessageInfo> CREATOR = new Parcelable.Creator<MessageInfo>() {

	public MessageInfo createFromParcel(Parcel in) {
		String userName = in.readString();
		String message = in.readString();
		String sessionName = in.readString();
		long timeStamp = in.readLong();
		int fromLocalUser = in.readInt();
		return new MessageInfo(userName, message, sessionName, timeStamp, fromLocalUser);
	}

	public MessageInfo[] newArray(int size) {
		return new MessageInfo[size];
	}
};
}

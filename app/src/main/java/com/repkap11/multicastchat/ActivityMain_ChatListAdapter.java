package com.repkap11.multicastchat;

import android.content.*;
import android.database.*;
import android.view.*;
import android.widget.*;

/**
 * Created by paul on 10/22/14.
 */
public class ActivityMain_ChatListAdapter extends CursorAdapter {
public ActivityMain_ChatListAdapter(Context context, String sessionName) {
	super(context, new MessageDatabaseHelper(context).getMessages(sessionName), 0);
}

@Override
public View newView(Context context, Cursor cursor, ViewGroup parent) {
	View child = View.inflate(context, R.layout.activity_main_chat_list_item, null);

	child.setTag(R.id.activity_main_chat_list_item_text_message, child.findViewById(R.id.activity_main_chat_list_item_text_message));
	child.setTag(R.id.activity_main_chat_list_item_text_sender_name, child.findViewById(R.id.activity_main_chat_list_item_text_sender_name));
	child.setTag(R.id.activity_main_chat_list_item_root, child.findViewById(R.id.activity_main_chat_list_item_root));
	return child;
}

@Override
public void bindView(View view, Context context, Cursor cursor) {
	((TextView) view.getTag(R.id.activity_main_chat_list_item_text_message)).setText(cursor.getString(cursor.getColumnIndex(MessageDatabaseContract.MessageEntry.COLUMN_NAME_MESSAGE)));
	((TextView) view.getTag(R.id.activity_main_chat_list_item_text_sender_name)).setText(cursor.getString(cursor.getColumnIndex(MessageDatabaseContract.MessageEntry.COLUMN_NAME_USER_NAME)));
	int fromLocalUser = cursor.getInt(cursor.getColumnIndex(MessageDatabaseContract.MessageEntry.COLUMN_NAME_FROM_LOCAL_USER));
	LinearLayout root = ((LinearLayout) view.getTag(R.id.activity_main_chat_list_item_root));

	if(fromLocalUser != 0) {
		root.setGravity(Gravity.LEFT);
	}else {
		root.setGravity(Gravity.RIGHT);
	}
}

}

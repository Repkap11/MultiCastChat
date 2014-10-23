package com.repkap11.multicastchat;

import android.content.Context;
import android.widget.SimpleCursorAdapter;

import com.repkap11.multicastchat.MessageDatabaseContract.MessageEntry;

/**
 * Created by paul on 10/22/14.
 */
public class ActivityMain_ChatListAdapter extends SimpleCursorAdapter {
    public ActivityMain_ChatListAdapter(Context context,String sessionName) {
        super(context,
                R.layout.activity_main_chat_list_item,
                new MessageDatabaseHelper(context).getMessages(sessionName),
                new String[]{MessageEntry.COLUMN_NAME_MESSAGE, MessageEntry.COLUMN_NAME_USER_NAME},
                new int[]{R.id.activity_main_chat_list_item_text_message, R.id.activity_main_chat_list_item_text_sender_name},
                0);
    }
}

package com.repkap11.multicastchat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.repkap11.multicastchat.MessageDatabaseContract.MessageEntry;

public class MessageDatabaseHelper {
    private MessageDatabaseOpenHelper mDbOpenHelper;

    public MessageDatabaseHelper(Context context) {
        mDbOpenHelper = new MessageDatabaseOpenHelper(context);
    }

    public void writeMessage(MessageInfo message) {// Gets the data repository in write mode
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();

// Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MessageEntry._ID,message.mTimeStamp);//TODO id
        values.put(MessageEntry.COLUMN_NAME_USER_NAME, message.mUserName);
        values.put(MessageEntry.COLUMN_NAME_CHAT_SESSION, message.mSessionName);
        values.put(MessageEntry.COLUMN_NAME_MESSAGE, message.mMessage);
        values.put(MessageEntry.COLUMN_NAME_TIMESTAMP, message.mTimeStamp);
		values.put(MessageEntry.COLUMN_NAME_FROM_LOCAL_USER, message.mFromLocalUser);

        // Insert the new row, returning the primary key value of the new row
        long newRowId;
        newRowId = db.insert(
                MessageEntry.TABLE_NAME,
                MessageEntry.COLUMN_NAME_NULLABLE,
                values);
    }

    public Cursor getMessages(String sessionName) {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                MessageEntry._ID,
                MessageEntry.COLUMN_NAME_USER_NAME,
                MessageEntry.COLUMN_NAME_MESSAGE,
                MessageEntry.COLUMN_NAME_TIMESTAMP,
				MessageEntry.COLUMN_NAME_FROM_LOCAL_USER
        };

        String selection = MessageEntry.COLUMN_NAME_CHAT_SESSION + " =?";
        String[] selectionArgs = new String[]{sessionName};
        // How you want the results sorted in the resulting Cursor
        String sortOrder = MessageEntry.COLUMN_NAME_TIMESTAMP + " ASC";

        Cursor c = db.query(
                MessageEntry.TABLE_NAME,  // The table to query
                projection,               // The columns to return
                selection,                // The columns for the WHERE clause
                selectionArgs,            // The values for the WHERE clause
                null,                     // don't group the rows
                null,                     // don't filter by row groups
                sortOrder                 // The sort order
        );
        return c;
    }
    public void deteleDatabase(){
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        mDbOpenHelper.deteleDatabase(db);
    }
}

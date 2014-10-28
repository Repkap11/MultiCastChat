package com.repkap11.multicastchat;

import android.content.*;
import android.database.sqlite.*;

import com.repkap11.multicastchat.MessageDatabaseContract.*;

public class MessageDatabaseOpenHelper extends SQLiteOpenHelper {
private static final String TEXT_TYPE = " TEXT";
private static final String INTEGER_TYPE = " INTEGER";
private static final String COMMA_SEP = ",";
private static final String SQL_CREATE_ENTRIES =
		"CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
				MessageEntry._ID + " INTEGER PRIMARY KEY," +
				MessageEntry.COLUMN_NAME_USER_NAME + TEXT_TYPE + COMMA_SEP +
				MessageEntry.COLUMN_NAME_MESSAGE + TEXT_TYPE + COMMA_SEP +
				MessageEntry.COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
				MessageEntry.COLUMN_NAME_FROM_LOCAL_USER + INTEGER_TYPE + COMMA_SEP +
				MessageEntry.COLUMN_NAME_CHAT_SESSION + TEXT_TYPE +

				" )";

private static final String SQL_DELETE_ENTRIES =
		"DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME;

// If you change the database schema, you must increment the database version.
public static final int DATABASE_VERSION = 1;
public static final String DATABASE_NAME = "Messages.db";

public MessageDatabaseOpenHelper(Context context) {
	super(context, DATABASE_NAME, null, DATABASE_VERSION);
}

public void onCreate(SQLiteDatabase db) {
	db.execSQL(SQL_CREATE_ENTRIES);
}

public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	// This database is only a cache for online data, so its upgrade policy is
	// to simply to discard the data and start over
	db.execSQL(SQL_DELETE_ENTRIES);
	onCreate(db);
}

public void deteleDatabase(SQLiteDatabase db) {
	//db.execSQL(SQL_DELETE_ENTRIES);
}

public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	onUpgrade(db, oldVersion, newVersion);
}
}
package com.repkap11.multicastchat;

import android.provider.BaseColumns;

/**
 * Created by paul on 10/22/14.
 */
public final class MessageDatabaseContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public MessageDatabaseContract() {
    }
    /* Inner class that defines the table contents */
    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "TABLE_NAME";
        public static final String COLUMN_NAME_CHAT_SESSION = "COLUMN_NAME_CHAT_SESSION";
        public static final String COLUMN_NAME_USER_NAME = "COLUMN_NAME_USER_NAME";
        public static final String COLUMN_NAME_MESSAGE = "COLUMN_NAME_MESSAGE";
        public static final String COLUMN_NAME_TIMESTAMP = "COLUMN_NAME_TIMESTAMP";
		public static final String COLUMN_NAME_FROM_LOCAL_USER = "COLUMN_NAME_FROM_LOCAL_USER";
        public static final String COLUMN_NAME_NULLABLE = "null";
    }
}

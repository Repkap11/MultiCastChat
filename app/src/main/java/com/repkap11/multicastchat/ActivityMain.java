package com.repkap11.multicastchat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;


public class ActivityMain extends Activity {

    private ListView mChatList;
    private Button mAddMessageButton;
    private ActivityMain_ChatListAdapter mChatListAdapter;
    public static final String SESSION_NAME = "testSession";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        mChatList = (ListView) findViewById(R.id.activity_main_chat_list);
        mChatListAdapter = new ActivityMain_ChatListAdapter(this, SESSION_NAME);

        startService(new Intent(this, MultiCastService.class));
        registerReceiver(new MessageReceiver(), new IntentFilter(MultiCastService.MESSAGE_RECEIVED));


        Button addMessageButton = (Button) findViewById(R.id.activity_main_chat_button_add_message);
        addMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageDatabaseHelper helper = new MessageDatabaseHelper(ActivityMain.this);
                MessageInfo message = new MessageInfo("paul", "Test Message2", SESSION_NAME);
                helper.writeMessage(message);
                mChatListAdapter.changeCursor(helper.getMessages(SESSION_NAME));

            }
        });
        Button deleteDatabaseButton = (Button) findViewById(R.id.activity_main_chat_button_delete_database);
        deleteDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MessageDatabaseHelper(ActivityMain.this).deteleDatabase();

            }
        });
        mChatList.setAdapter(mChatListAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MultiCastService.MESSAGE_RECEIVED)) {
                MessageInfo message = (MessageInfo) intent.getParcelableExtra(MultiCastService.MESSAGE_RECEIVED);
                MessageDatabaseHelper helper = new MessageDatabaseHelper(ActivityMain.this);
                helper.writeMessage(message);
                mChatListAdapter.changeCursor(helper.getMessages(SESSION_NAME));
            }
        }
    }
}

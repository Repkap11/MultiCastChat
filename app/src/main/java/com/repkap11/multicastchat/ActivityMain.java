package com.repkap11.multicastchat;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ActivityMain extends AppCompatActivity {

//echo -n "For the win" | socat - udp-datagram:192.168.0.255:56789,broadcast

    public static final int ACTIVITY_RESULT_PREFS_UPDATED = 1;
    private static final String TAG = ActivityMain.class.getSimpleName();
    private ListView mChatList;
    private EditText mTextBox;
    private Button mAddMessageButton;
    private ActivityMain_ChatListAdapter mChatListAdapter;
    private ServiceConnection mMultiCastServiceConnection;
    private MessageReceiver mMessageReciever = null;
    public static final String SESSION_NAME = "testSession";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        mChatList = (ListView) findViewById(R.id.activity_main_chat_list);
        mChatListAdapter = new ActivityMain_ChatListAdapter(this, SESSION_NAME);
        mChatList.setAdapter(mChatListAdapter);

        mTextBox = (EditText) findViewById(R.id.activity_main_text_box);

        mMessageReciever = new MessageReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, MultiCastService.class));
        } else {
            startService(new Intent(this, MultiCastService.class));
        }
        registerReceiver(mMessageReciever, new IntentFilter(MultiCastService.MESSAGE_RECEIVED));
        registerReceiver(mMessageReciever, new IntentFilter(MultiCastService.SERVICE_EXITING_EXIT_ACTIVITY));

        /*
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
        */

    }

    public void sendButtonOnClick(View v) {
        Log.e("paul", "Send button on click");
        MessageDatabaseHelper helper = new MessageDatabaseHelper(ActivityMain.this);
        MessageInfo message = new MessageInfo("Paul", mTextBox.getText().toString(), SESSION_NAME, true);
        mTextBox.setText("");
        helper.writeMessage(message);
        mChatListAdapter.changeCursor(helper.getMessages(SESSION_NAME));
        sendMessageToService(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMessageReciever != null) {
            unregisterReceiver(mMessageReciever);
        }
        if (mMultiCastServiceConnection != null) {
            unbindService(mMultiCastServiceConnection);
        }
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
            Intent prefIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(prefIntent, ACTIVITY_RESULT_PREFS_UPDATED);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case ACTIVITY_RESULT_PREFS_UPDATED:
                Log.e("paul", "onActivityResult called");
                Intent intent = new Intent(this, MultiCastService.class);
                stopService(intent);
                startService(intent);
                break;
        }
    }

    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MultiCastService.MESSAGE_RECEIVED)) {
                //MessageInfo message = (MessageInfo) intent.getParcelableExtra(MultiCastService.MESSAGE_RECEIVED);
                MessageDatabaseHelper helper = new MessageDatabaseHelper(ActivityMain.this);
                //helper.writeMessage(message);
                mChatListAdapter.changeCursor(helper.getMessages(SESSION_NAME));
            } else if (action.equals(MultiCastService.SERVICE_EXITING_EXIT_ACTIVITY)) {
                //Toast.makeText(ActivityMain.this, "Activity exit", Toast.LENGTH_SHORT).show();
                ActivityMain.this.finish();
            }
        }
    }

    private MultiCastService myServiceBinder;
    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            myServiceBinder = ((MultiCastService.MyBinder) binder).getService();
            Log.d("ServiceConnection", "connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("ServiceConnection", "disconnected");
            myServiceBinder = null;
        }
    };

    public void doBindService() {
        Intent intent = null;
        intent = new Intent(this, MultiCastService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        MessageDatabaseHelper helper = new MessageDatabaseHelper(ActivityMain.this);
        mChatListAdapter.changeCursor(helper.getMessages(SESSION_NAME));
        if (myServiceBinder == null) {
            doBindService();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        doUnBindService();
        super.onPause();
    }

    private void doUnBindService() {
        if (myServiceBinder != null) {
            unbindService(myConnection);
            myServiceBinder = null;
        }
    }

    private void doStopService() {
        stopService(new Intent(this, MultiCastService.class));
    }

    private void sendMessageToService(MessageInfo messageInfo) {
        Intent intent = new Intent(MultiCastService.TRANSMIT_MESSAGE);
        intent.putExtra(MultiCastService.TRANSMIT_MESSAGE, messageInfo);
        sendBroadcast(intent);
    }
}

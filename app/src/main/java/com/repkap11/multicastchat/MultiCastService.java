package com.repkap11.multicastchat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;

public class MultiCastService extends Service {
    public static final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
    public static final String SERVICE_EXITING_EXIT_ACTIVITY = "SERVICE_EXITING_EXIT_ACTIVITY";
    public static final String TRANSMIT_MESSAGE = "TRANSMIT_MESSAGE";
    private static final String REQUEST_EXIT = "REQUEST_EXIT";
    private final static int NOTIFICATION_MESSAGE_RECEIVED = 1;
    private static final String TAG = MultiCastService.class.getSimpleName();

    DatagramSocket socket = null;
    DatagramSocket sendSocket = null;
    Handler mHandler = new Handler();
    MessageReceiver mMessageReceiver = null;

    void startListenForUDPBroadcast() {
        new Thread(new Runnable() {
            public void run() {
                WifiManager wifi;
                wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiManager.MulticastLock ml = wifi.createMulticastLock("just some tag text");
                ml.acquire();
                byte[] buf = new byte[2200];
                try {
                    int port = 56789;
                    InetAddress broadcastIP = getBroadcastAddress();
                    Log.e(TAG, "run: address: " + broadcastIP.getHostAddress());

                    while (shouldRestartSocketListen) {
                        //socket.setSoTimeout(1000);
                        if (socket == null || socket.isClosed()) {
                            socket = new DatagramSocket(port, broadcastIP);
                            socket.setBroadcast(true);
                        }
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        message("Waiting for UDP broadcast");
                        socket.receive(packet);

                        //String senderIP = packet.getAddress().getHostAddress();
                        //message(senderIP);
                        //String message = new String(packet.getData(), 0, packet.getLength());
                        //message("Got UDB broadcast from " + senderIP + ", message: " + message);
                        broadcastIntentMessageRecieved(new MessageInfo(packet.getData(), 0, packet.getLength()));

                    }
                    //if (!shouldListenForUDPBroadcast) throw new ThreadDeath();
                } catch (Exception e) {
                    Log.e("Service", "Exception listening for brodcase");
                    message("no longer listening for UDP broadcasts because of error:" + e.getMessage());
                    e.printStackTrace();
                }
                ml.release();
            }
        }).start();
    }

    void sendMessage(final MessageInfo messageInfo) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    int port = 56789;
                    InetAddress broadcastIP = getBroadcastAddress();
                    if (sendSocket == null || sendSocket.isClosed()) {
                        DatagramChannel channel = DatagramChannel.open();
                        sendSocket = channel.socket();
                        //socket = new DatagramSocket(port, broadcastIP);
                        sendSocket.setBroadcast(true);
                    }
                    //WifiManager wifi = (WifiManager) MultiCastService.this.getSystemService(Context.WIFI_SERVICE);
                    //WifiManager.MulticastLock lock = wifi.createMulticastLock(MultiCastService.this.getPackageName());
                    //lock.acquire();
                    byte[] data = messageInfo.getBytesToTransmit();
                    //DatagramPacket packet = new DatagramPacket(data, data.length);
                    DatagramPacket packet = new DatagramPacket(data, 0, data.length, broadcastIP, port);
                    message("Sending UDP broadcast");
                    sendSocket.send(packet);
                    //lock.release();
                    message("Packet Sent");
                    //if (!shouldListenForUDPBroadcast) throw new ThreadDeath();
                } catch (Exception e) {
                    message("sending error:" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void message(final String message) {
        Log.e("Paul", message);
		/*
		mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MultiCastService.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void broadcastIntentMessageRecieved(MessageInfo message) {
        MessageDatabaseHelper helper = new MessageDatabaseHelper(this);
        helper.writeMessage(message);

        if (mIsBoundToActivity) {
            Intent intent = new Intent(MultiCastService.MESSAGE_RECEIVED);
            sendBroadcast(intent);
        } else {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = buildNotification(message);
            notificationManager.notify(NOTIFICATION_MESSAGE_RECEIVED, notification);
        }
    }

    private boolean shouldRestartSocketListen = true;

    void stopListen() {
        shouldRestartSocketListen = false;
        if (socket != null) {
            try {
                socket.close();
                Log.e("Mailbox", "Socket closed");
            } catch (Exception e) {
                Log.e("Mailbox", "Unable to close mailbox:" + e.getMessage());
            }
        }

    }

    PowerManager.WakeLock mWakeLock = null;
    WifiManager.MulticastLock mMultiCastLock = null;
    private static final String NOTIFICATION_CHANNEL_ID_NO_VIBE = "NOTIFICATION_CHANNEL_ID";
    private static final String NOTIFICATION_CHANNEL_ID_VIBE = "NOTIFICATION_CHANNEL_ID_VIBE";


    @Override
    public void onCreate() {
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ":MyWakeLock");
        mWakeLock.acquire();

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mMultiCastLock = wm.createMulticastLock(":MyMultiCastLock");
        mMultiCastLock.setReferenceCounted(true);
        mMultiCastLock.acquire();


        mMessageReceiver = new MessageReceiver();
        registerReceiver(mMessageReceiver, new IntentFilter(MultiCastService.TRANSMIT_MESSAGE));
        registerReceiver(mMessageReceiver, new IntentFilter(MultiCastService.REQUEST_EXIT));
        //Toast.makeText(this, "Service Created", Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        } else {
        }

        Intent intentMain = new Intent(this, ActivityMain.class);
        intentMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //RemoteViews remoteViewSmall = new RemoteViews(getPackageName(), R.layout.remote_notification);
        RemoteViews remoteViewLarge = new RemoteViews(getPackageName(), R.layout.remote_notification);
        PendingIntent pendingIntentClick = PendingIntent.getActivity(this, 0, intentMain, PendingIntent.FLAG_UPDATE_CURRENT);


        Intent intentExit = new Intent(MultiCastService.REQUEST_EXIT);
        PendingIntent pendingIntentExit = PendingIntent.getBroadcast(this, 0, intentExit, 0);
        remoteViewLarge.setOnClickPendingIntent(R.id.notification_button_exit, pendingIntentExit);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_NO_VIBE)
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setTicker(getString(R.string.notification_listening))
                .setContentTitle(getString(R.string.notification_listening))
                .setContentText(getString(R.string.notification_touch_to_open))
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(Notification.PRIORITY_MIN)
                //.setCustomContentView(remoteViewSmall)
                .setCustomBigContentView(remoteViewLarge);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }
        //builder.setSubText("setSubText");

        builder.setContentIntent(pendingIntentClick);
        //builder.setContent(remoteView);
        Notification notification = builder.build();
        //notification.setLatestEventInfo(this, "title", "text", pendingIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;

        startForeground(42, notification);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        {
            String channelName = "Listening in the Background";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID_NO_VIBE, channelName, NotificationManager.IMPORTANCE_MIN);
            chan.setLightColor(Color.BLUE);
            chan.enableVibration(true);
            chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            manager.createNotificationChannel(chan);
        }
        {
            String channelName = "Notifications for Message";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID_VIBE, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            chan.enableLights(false);
            chan.enableVibration(true);
            chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            manager.createNotificationChannel(chan);
        }
    }

    @Override
    public void onDestroy() {
        stopListen();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mMultiCastLock != null && mMultiCastLock.isHeld()) {
            mMultiCastLock.release();
        }
        unregisterReceiver(mMessageReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldRestartSocketListen = true;
        startListenForUDPBroadcast();
        return START_STICKY;
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager myWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
        if (myDhcpInfo == null) {
            Log.w(TAG, "Could not get broadcast address");
            return null;
        }
        byte[] quads2 = new byte[4];
        for (int k = 0; k < 4; k++)
            quads2[k] = (byte) ((myDhcpInfo.ipAddress >> k * 8) & 0xFF);
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByAddress(quads2));
        for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
            //short netPrefix = address.getNetworkPrefixLength();
            InetAddress adr = address.getBroadcast();
            if (adr != null) {
                return adr;
            }
        }
        Log.e(TAG, "getBroadcastAddress: Unable to find broadcast address");
        return null;
    }

    public boolean mIsBoundToActivity;

    @Override
    public boolean onUnbind(Intent intent) {
        mIsBoundToActivity = false;
        return true; // ensures onRebind is called
    }

    @Override
    public void onRebind(Intent intent) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_MESSAGE_RECEIVED);
        mIsBoundToActivity = true;
    }

    private Notification buildNotification(MessageInfo message) {

        Intent intent = new Intent(this, ActivityMain.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                NOTIFICATION_MESSAGE_RECEIVED,
                intent, PendingIntent.FLAG_ONE_SHOT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        Notification.Builder b;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID_VIBE);
        } else {
            b = new Notification.Builder(this);
        }

        b.setContentTitle(message.mUserName)
                .setContentText(message.mMessage)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setTicker(message.mUserName + ": " + message.mMessage)
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= 21) {
            b.setColor(getResources().getColor(R.color.colorPrimary));
        }
        Notification notification = b.build();

        notification.defaults |= Notification.DEFAULT_LIGHTS; // LED
        notification.defaults |= Notification.DEFAULT_VIBRATE; //Vibration
        //notification.defaults |= Notification.DEFAULT_SOUND; // Sound
        return notification;
    }

    private final IBinder mBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_MESSAGE_RECEIVED);
        mIsBoundToActivity = true;
        Log.d("service", "onBind");
        return mBinder;
    }

    public class MyBinder extends Binder {
        MultiCastService getService() {
            return MultiCastService.this;
        }
    }

    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MultiCastService.TRANSMIT_MESSAGE)) {
                MessageInfo messageInfo = intent.getParcelableExtra(MultiCastService.TRANSMIT_MESSAGE);
                Toast.makeText(MultiCastService.this, "onReceive", Toast.LENGTH_SHORT).show();
                sendMessage(messageInfo);
            } else if (action.equals(MultiCastService.REQUEST_EXIT)) {
                //Toast.makeText(MultiCastService.this, "Request exit", Toast.LENGTH_SHORT).show();
                Intent activityEndIntent = new Intent(MultiCastService.SERVICE_EXITING_EXIT_ACTIVITY);
                sendBroadcast(activityEndIntent);
                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                MultiCastService.this.sendBroadcast(it);
                MultiCastService.this.stopForeground(true);
                MultiCastService.this.stopSelf();
            }
        }
    }
}

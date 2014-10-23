package com.repkap11.multicastchat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public class MultiCastService extends Service {
    public static String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
    public static String TRANSMIT_MESSAGE = "TRANSMIT_MESSAGE";
    private final static int NOTIFICATION_MESSAGE_RECEIVED = 1;
    DatagramSocket socket = null;
    DatagramSocket sendSocket = null;
    Handler mHandler = new Handler();
    MessageReceiver mMessageReceiver = null;

    void startListenForUDPBroadcast() {
        new Thread(new Runnable() {
            public void run() {
                byte[] buf = new byte[2200];
                try {
                    String iaddress = "192.168.0.255";
                    int port = 56789;
                    InetAddress broadcastIP = InetAddress.getByName(iaddress);
                    while (shouldRestartSocketListen) {
                        //socket.setSoTimeout(1000);
                        if (socket == null || socket.isClosed()) {
                            socket = new DatagramSocket(port, broadcastIP);
                            socket.setBroadcast(true);
                        }
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        message("Waiting for UDP broadcast");
                        socket.receive(packet);

                        String senderIP = packet.getAddress().getHostAddress();
                        message(senderIP);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        message(senderIP);

                        message("Got UDB broadcast from " + senderIP + ", message: " + message);
                        broadcastIntentMessageRecieved(senderIP, message);
                    }
                    //if (!shouldListenForUDPBroadcast) throw new ThreadDeath();
                } catch (Exception e) {
                    message("no longer listening for UDP broadcasts because of error:" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void sendMessage(final MessageInfo messageInfo) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String iaddress = "192.168.0.255";
                    int port = 56789;
                    InetAddress broadcastIP = InetAddress.getByName(iaddress);
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
                    DatagramPacket packet = new DatagramPacket(data,0,data.length,broadcastIP,port);
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

    private void broadcastIntentMessageRecieved(String senderIP, String text) {
        MessageInfo message = new MessageInfo(senderIP, text, ActivityMain.SESSION_NAME);
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

    @Override
    public void onCreate() {
        mMessageReceiver = new MessageReceiver();
        registerReceiver(mMessageReceiver, new IntentFilter(MultiCastService.TRANSMIT_MESSAGE));
        Toast.makeText(this, "Service Created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        stopListen();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldRestartSocketListen = true;
        startListenForUDPBroadcast();
        Log.i("UDP", "Service started");
        return START_STICKY;
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

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), NOTIFICATION_MESSAGE_RECEIVED, new Intent(this, ActivityMain.class), PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder b = new Notification.Builder(this)
                .setContentTitle(message.mUserName)
                .setContentText(message.mMessage)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setTicker("Ticker String")
                .setContentIntent(contentIntent);
        return (b.build());
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
                Toast.makeText(MultiCastService.this,"onReceive",Toast.LENGTH_SHORT).show();
                sendMessage(messageInfo);
            }
        }
    }
}

package com.repkap11.multicastchat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MultiCastService extends Service {
    static String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
    private final static int NOTIFICATION_MESSAGE_RECEIVED = 1;
    DatagramSocket socket = null;
    Handler mHandler = new Handler();

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
                        broadcastIntent(senderIP, message);
                    }
                    //if (!shouldListenForUDPBroadcast) throw new ThreadDeath();
                } catch (IOException e) {
                    message("no longer listening for UDP broadcasts cause of error:" + e.getMessage());
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

    private void broadcastIntent(String senderIP, String text) {
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
            } catch (Exception e) {
                Log.e("Mailbox", "Unable to close mailbox:" + e.getMessage());
            }
        }
    }

    @Override
    public void onCreate() {
    }

    ;

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

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), NOTIFICATION_MESSAGE_RECEIVED, new Intent(this,ActivityMain.class), PendingIntent.FLAG_ONE_SHOT);
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
        Log.d("service","onBind");
        return mBinder;
    }

    public class MyBinder extends Binder {
        MultiCastService getService() {
            return MultiCastService.this;
        }
    }
}

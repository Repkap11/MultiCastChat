package com.repkap11.multicastchat;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;

public class MultiCastService extends Service {
    static String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
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
                    socket = null;
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
        Log.e("Paul",message);
        /*
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MultiCastService.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void broadcastIntent(String senderIP, String message) {
        Intent intent = new Intent(MultiCastService.MESSAGE_RECEIVED);
        intent.putExtra(MESSAGE_RECEIVED,new MessageInfo(senderIP,message,ActivityMain.SESSION_NAME));
        sendBroadcast(intent);
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildForegroundNotification(String filename) {
        Notification.Builder b = new Notification.Builder(this);
        b.setOngoing(true);
        b.setContentTitle(getString(R.string.notification_listening))
                .setContentText("Notification content text")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setTicker("Ticker String");

        return (b.build());
    }
}

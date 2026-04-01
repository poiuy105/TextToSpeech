package com.example.tcptospeech;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

public class TcpServerService extends Service implements TextToSpeech.OnInitListener {

    private static final String TAG = "TcpServerService";
    private static final int PORT = 7775;
    private static final String CHANNEL_ID = "TcpServerServiceChannel";

    private ServerSocket serverSocket;
    private boolean running = false;
    private TextToSpeech textToSpeech;
    private Thread serverThread;
    private static MainActivity mainActivity;

    public static void setMainActivity(MainActivity activity) {
        mainActivity = activity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startServer();
        return START_STICKY;
    }

    private void startServer() {
        running = true;
        serverThread = new Thread(new ServerRunnable());
        serverThread.start();
        startForeground(1, createNotification());
    }

    private void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket", e);
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (serverThread != null) {
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error joining server thread", e);
            }
        }
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TCP Server Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setContentTitle("TCP To Speech Server")
                .setContentText("Server running on port " + PORT)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed");
        }
    }

    private class ServerRunnable implements Runnable {
        @Override
        public void run() {
            while (running) {
                try {
                    serverSocket = new ServerSocket(PORT);
                    Log.d(TAG, "Server started, waiting for client...");

                    Socket clientSocket = serverSocket.accept();
                    Log.d(TAG, "Client accepted");

                    DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                    String message = in.readUTF();
                    Log.d(TAG, "Received message: " + message);

                    // Update UI with last message
                    if (mainActivity != null) {
                        mainActivity.updateLastMessage(message);
                    }

                    // Speak the message
                    if (textToSpeech != null && textToSpeech.isSpeaking()) {
                        textToSpeech.stop();
                    }
                    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);

                    // Close connections
                    in.close();
                    clientSocket.close();
                    serverSocket.close();

                } catch (IOException e) {
                    if (running) {
                        Log.e(TAG, "Server error", e);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

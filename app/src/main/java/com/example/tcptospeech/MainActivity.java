package com.example.tcptospeech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private Button startStopButton;
    private TextView serverStatusText;
    private TextView lastMessageText;
    private boolean serverRunning = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopButton = findViewById(R.id.start_stop_button);
        serverStatusText = findViewById(R.id.server_status);
        lastMessageText = findViewById(R.id.last_message);

        // Set this activity instance to the service
        TcpServerService.setMainActivity(this);

        checkPermissions();

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serverRunning) {
                    stopServer();
                } else {
                    startServer();
                }
            }
        });

        updateServerStatus(false);
        updateLastMessage("");
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.FOREGROUND_SERVICE}, PERMISSION_REQUEST_CODE);
        }
    }

    private void startServer() {
        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        serverRunning = true;
        updateServerStatus(true);
        startStopButton.setText(R.string.stop_server);
    }

    private void stopServer() {
        Intent serviceIntent = new Intent(this, TcpServerService.class);
        stopService(serviceIntent);
        serverRunning = false;
        updateServerStatus(false);
        startStopButton.setText(R.string.start_server);
    }

    private void updateServerStatus(boolean running) {
        String status = running ? getString(R.string.server_running) : getString(R.string.server_stopped);
        serverStatusText.setText(getString(R.string.server_status, status));
    }

    public void updateLastMessage(String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                lastMessageText.setText(getString(R.string.last_message, message));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (serverRunning) {
            stopServer();
        }
        super.onDestroy();
    }
}

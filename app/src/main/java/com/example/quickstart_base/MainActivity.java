package com.example.quickstart_base;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private AgoraManager agoraManager;
    // Fill the App ID of your project generated on Agora Console.
    private final String appId = "9d2498880e934632b38b0a68fa2f1622"; //""<Your app Id>";
    // Fill the temp token generated on Agora Console.
    private String channelName = "demo", token = "007eJxTYOCY0F1Un9O3TnBJRX1f/seLVv/0i+I41+2JsOQ5ofPtQ6ICg2WKkYmlhYWFQaqlsYmZsVGSsUWSQaKZRVqiUZqhmZFRqolNSkMgI0P8PzZmRgYIBPFZGFJSc/MZGAALwR3m"; //""<your access token>";
    FrameLayout flLocalVideo, flRemoteVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        agoraManager = new AgoraManager(this, appId);
        agoraManager.setVideoFrameLayouts(
                findViewById(R.id.local_video_view_container),
                findViewById(R.id.remote_video_view_container)
        );
        agoraManager.setListener(new AgoraManager.AgoraManagerListener() {
            @Override
            public void onMessageReceived(String message) {
                runOnUiThread(() ->
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    public void joinLeave(View view) {
        Button btnJoinLeave = findViewById(R.id.btnJoinLeave);

        if (!agoraManager.isJoined()) {
            agoraManager.joinChannel(channelName, token);
            btnJoinLeave.setText("Leave");
        } else {
            agoraManager.leaveChannel();
            btnJoinLeave.setText("Join");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        agoraManager.destroy();
    }
}
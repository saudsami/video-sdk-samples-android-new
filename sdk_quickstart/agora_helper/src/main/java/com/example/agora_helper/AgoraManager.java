package com.example.agora_helper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;

public class AgoraManager {

    protected final Context mContext;
    protected AgoraManagerListener mListener;
    protected RtcEngine agoraEngine;

    protected final String appId;
    protected String channelName;
    protected int localUid = 0, remoteUid = 0;
    protected boolean joined = false;
    protected FrameLayout localFrameLayout, remoteFrameLayout;
    protected final Activity activity;

    //SurfaceView to render local video in a Container.
    protected SurfaceView localSurfaceView;
    //SurfaceView to render Remote video in a Container.
    protected SurfaceView remoteSurfaceView;

    protected static final int PERMISSION_REQ_ID = 22;
    protected static final String[] REQUESTED_PERMISSIONS =
            {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            };

    public AgoraManager(Context context, String appId) {
        mContext = context;
        activity = (Activity) mContext;
        this.appId = appId;

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(activity, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }

        setupVideoSDKEngine();
    }

    public void setListener(AgoraManagerListener mListener) {
        this.mListener = mListener;
    }

    public void setVideoFrameLayouts(FrameLayout localFrameLayout, FrameLayout remoteFrameLayout) {
        this.localFrameLayout = localFrameLayout;
        this.remoteFrameLayout = remoteFrameLayout;
    }

    protected void setupLocalVideo() {
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        activity.runOnUiThread(() -> {
            localSurfaceView = new SurfaceView(mContext);
            localFrameLayout.addView(localSurfaceView);
            localSurfaceView.setVisibility(View.VISIBLE);
            // Call setupLocalVideo with a VideoCanvas having uid set to 0.
            agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        });
    }

    protected void setupRemoteVideo () {
        // Set up remote video
        activity.runOnUiThread(() -> {
            remoteSurfaceView = new SurfaceView(mContext);
            remoteSurfaceView.setZOrderMediaOverlay(true);
            remoteFrameLayout.addView(remoteSurfaceView);
            VideoCanvas videoCanvas = new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT,
                    Constants.VIDEO_MIRROR_MODE_ENABLED, remoteUid);
            agoraEngine.setupRemoteVideo(videoCanvas);
            // Display RemoteSurfaceView.
            remoteSurfaceView.setVisibility(View.VISIBLE);
        });
    }

    protected void setupVideoSDKEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = mContext;
            config.mAppId = appId;
            config.mEventHandler = getIRtcEngineEventHandler();
            agoraEngine = RtcEngine.create(config);
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine.enableVideo();
        } catch (Exception e) {
            sendMessage(e.toString());
        }
    }

    public boolean isJoined () {
        return joined;
    }

    public int joinChannel(String channelName, String token) {
        this.channelName = channelName;

        if (checkSelfPermission()) {
            ChannelMediaOptions options = new ChannelMediaOptions();
            // For a Video call, set the channel profile as COMMUNICATION.
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            // Display LocalSurfaceView.
            setupLocalVideo();
            // Start local preview.
            agoraEngine.startPreview();
            // Join the channel with a temp token.
            // You need to specify the user ID yourself, and ensure that it is unique in the channel.
            agoraEngine.joinChannel(token, channelName, localUid, options);
        } else {
            sendMessage("Permissions was not granted");
        }
        return 0;
    }

    public void leaveChannel() {
        if (!joined) {
            sendMessage("Join a channel first");
        } else {
            agoraEngine.leaveChannel();
            sendMessage("You left the channel");

            activity.runOnUiThread(() -> {
                // Stop remote video rendering.
                if (remoteSurfaceView != null)  remoteSurfaceView.setVisibility(View.GONE);
                // Stop local video rendering.
                if (localSurfaceView != null) localSurfaceView.setVisibility(View.GONE);
            });
            joined = false;
        }
    }

    public void destroy() {
        agoraEngine.stopPreview();
        agoraEngine.leaveChannel();
        RtcEngine.destroy();
        agoraEngine = null;
    }

    protected IRtcEngineEventHandler getIRtcEngineEventHandler() {

        return new IRtcEngineEventHandler() {
            @Override
            // Listen for the remote host joining the channel to get the uid of the host.
            public void onUserJoined(int uid, int elapsed) {
                sendMessage("Remote user joined " + uid);
                remoteUid = uid;

                // Set the remote video view
                setupRemoteVideo();
            }

            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                joined = true;
                sendMessage("Joined Channel " + channel);
                localUid = uid;
            }

            @Override
            public void onUserOffline(int uid, int reason) {
                sendMessage("Remote user offline " + uid + " " + reason);
                activity.runOnUiThread(() -> remoteSurfaceView.setVisibility(View.GONE));
            }
        };
    }



    protected boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(mContext, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }

    public interface AgoraManagerListener {
        void onMessageReceived(String message);
    }

    protected void sendMessage(String message) {
        mListener.onMessageReceived(message);
    }
}

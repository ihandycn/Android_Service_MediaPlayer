package com.training.MediaPlayer;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

public class MediaActivity extends Activity {
	// Tag that we will use for logging
    private static final String TAG = MediaActivity.class.getSimpleName();
    // flag to turn on/off debug messages from this class
    private static final boolean DEBUG = true;
    // Links to Java instances of our UI widgets in our Activity
    // Buttons
    private ImageButton playButton;
    private ImageButton pauseButton;
    private ImageButton stopButton;
    // Text view to display folder name
    private TextView folderName;
    // Song name founded in folder
    private TextView songName;
    // progress bar to show current position
    private ProgressBar songProgress;
    // flag to check is player in pause state
    private boolean isPaused;
    // interface to our service
    private IMediaPlayer player;
    // flag to specify is service bound or not
    private boolean isBound = false;
    // our broadcast receiver event handler
    private PlayerEventReceiver eventReceiver = new PlayerEventReceiver();
    // message IDs that we will send to Handler that is responsible for UI update
    private static final int MSG_DURATION = 0;
    private static final int MSG_POSITION = 1;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set main.xml as layout for our activity
        setContentView(R.layout.main);
        // get links to widgets. Use findViewById method to find exact widget by its id
        playButton = (ImageButton)findViewById(R.id.play_button);
        stopButton = (ImageButton)findViewById(R.id.stop_button);
        pauseButton = (ImageButton)findViewById(R.id.pause_button);
        folderName = (TextView)findViewById(R.id.folder_name);
        songName = (TextView)findViewById(R.id.song_name);
        songProgress = (ProgressBar)findViewById(R.id.song_progress);
        // Add the click listener for play button
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.d(TAG, "Play media");
                }
                // if service is bound
                if (isBound) {
                	// take the folder name value
                    String path = folderName.getText().toString();
                    // Create File object and direct it to folder name
                    File root = new File(path);
                    // take the first available file in directory
                    String fileFirst = root.list()[0];
                    // if paused, just resume
                    if (isPaused) {
                        try {
                            player.resume();
                        } catch (RemoteException e) {
                            Log.e(TAG, "Remote player service died");
                        }
                    } else {
                    	// otherwise, start playing and update song name in UI
                        try {
                            player.play(Uri.parse(path + "/" + fileFirst));
                            songName.setText(new File(fileFirst).getName());
                        } catch (RemoteException e) {
                            Log.e(TAG, "Remote player service died");
                        }
                    }
                }
            }
        });
        // Add onClickListener for pause button
        pauseButton.setOnClickListener(new View.OnClickListener() {
        	// just pause player if service is bound
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.d(TAG, "Pause media");
                }
                if (isBound) {
                    try {
                        player.pause();
                        isPaused = true;
                    } catch (RemoteException e) {
                        Log.e(TAG, "Remote player service died");
                    }
                }
            }
        });
        // add onClickListener to stop button
        stopButton.setOnClickListener(new View.OnClickListener() {
        	// stop player if service is bound
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.d(TAG, "Stop playing media");
                }
                if (isBound) {
                    try {
                        player.stop();
                        isPaused = false;
                    } catch (RemoteException e) {
                        Log.e(TAG, "Remote player service died");
                    }
                }
            }
        });
    }
    // Override onStart method in Activity to registry our broadcast receiver
    @Override
    protected void onStart() {
        super.onStart();
        // Create IntentFilter with action from our service
        IntentFilter filter = new IntentFilter(MediaService.PLAY_ACTION);
        // Register our class responsible for broadcast receiver events handling.
        registerReceiver(eventReceiver, filter);
        // Now bind to the service. To do so, create explicit Intent
        Intent serviceIntent = new Intent(this, MediaService.class);
        if (!isBound) {
        	// if we are not already bound, bind to the service
            bindService(serviceIntent, playerConnection, BIND_AUTO_CREATE);
        }
    }
    // override onStop method of Activity to unregister our broadcast receiver and unbind from service  
    @Override
    protected void onStop() {
        super.onStop();
        // unregister our broadcast receiver
        unregisterReceiver(eventReceiver);
        if (isBound) {
        	// unbind from our service
            unbindService(playerConnection);
        }
    }

    // Service connection implementation to handle events from bind/unbind state changes in service
    private ServiceConnection playerConnection = new ServiceConnection() {
    	// service is bound
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        	// get the interface to our service
            player = IMediaPlayer.Stub.asInterface(service);
            isBound = true;
        }
        // service is unbound
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            player = null;
        }
    };
    // implementation of our broadcast receiver handler
    private class PlayerEventReceiver extends BroadcastReceiver {
    	// broadcast event received
        @Override
        public void onReceive(Context context, Intent intent) {
        	// take the media file duration value from event, second parameter is default value
            int duration = intent.getIntExtra(MediaService.SET_DURATION, -1);
            if (duration > 0) {
            	// create message to be handled by our Handler that is responsible for UI update
                Message m = Message.obtain(handler, MSG_DURATION, duration, 0);
                // send message to the handler. Handler will handle it on UI thread
                handler.sendMessage(m);
            }
            // take the current position of playback
            int position = intent.getIntExtra(MediaService.SET_POSITION, -1);
            if (position >= 0 || position <= duration) {
            	// create message to be handled by our Handler that is responsible for UI update
                Message m = Message.obtain(handler, MSG_POSITION, position, 0);
                // send message to the handler. Handler will handle it on UI thread
                handler.sendMessage(m);
            }
        }
    }

    // Handler class implementation
    private Handler handler = new Handler() {
    	// message handle callback. It's invoked on UI thread
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	// handle the duration event and update progress bar accordingly
                case MSG_DURATION:
                    if (DEBUG) {
                        Log.i(TAG, "Duration: " + msg.arg1);
                    }
                    songProgress.setMax(msg.arg1);
                    break;
                 // handle the position event and update progress bar accordingly    
                case MSG_POSITION:
                    if (DEBUG) {
                        Log.i(TAG, "Position: " + msg.arg1);
                    }
                    songProgress.setProgress(msg.arg1);
                    break;
            }
        }
    };
}

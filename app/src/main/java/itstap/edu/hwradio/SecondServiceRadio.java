package itstap.edu.hwradio;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import de.greenrobot.event.EventBus;

import java.io.IOException;

public  class SecondServiceRadio extends Service implements  MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener{
    private NotificationManager notifManager;
    public static final String STATION_URI = "Station_uri";
    public static final String STATION_NAME = "Station_name";


    private static final String LOG_TAG = "PlaybackService";
    private final boolean L = true;
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";
    private String status;
    private AudioManager audioManager;
    private MediaControllerCompat.TransportControls transportControls;
    private String strAppName;
    private String strLiveBroadcast;
    private PlaybackStateCompat playbackState;
    private Binder binder = new ServiceBinder();
    private MediaSessionCompat mediaSession;
    private String streamUrl;
    private MediaNotificationManager notificationManager;
    private MediaPlayer mediaPlayer;

    public MediaSessionCompat getMediaSession(){

        return mediaSession;
    }

    public class ServiceBinder extends Binder {
        public SecondServiceRadio getService() {
            return SecondServiceRadio.this;
        }
    }
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
           if(mediaPlayer.isPlaying()){
               mediaPlayer.stop();
               mediaPlayer.release();
           }
        }
    };



    @Override
    public void onCreate() {
        super.onCreate();
        notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        notificationManager = new MediaNotificationManager(this);
        Log.d("create","Create service");
        // set the initial playback state
        playbackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build();

        // instantiate the media session
        mediaSession = new MediaSessionCompat(this, LOG_TAG);
       transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setCallback(mediaSessionCallback);
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(playbackState);
        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

    }

    final MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Uri uri = extras.getParcelable(STATION_URI);
            onPlayFromUri(uri, extras);
        }


        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {

            String name = extras.getString(STATION_NAME);

            try {
                switch (playbackState.getState()) {
                    case PlaybackStateCompat.STATE_NONE:
                    case PlaybackStateCompat.STATE_STOPPED:

                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(SecondServiceRadio.this, uri);
                        mediaPlayer.prepareAsync();
                        if(L) Log.i(LOG_TAG, "Buffering audio");
                        // set the playback state & set the audio's metadata
                        playbackState = new PlaybackStateCompat.Builder()
                                .setState(PlaybackStateCompat.STATE_BUFFERING, 0, 1.0f)
                                .build();
                        mediaSession.setPlaybackState(playbackState);
                        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
                                .build());
                        break;
                    default:
                        // stop
                        mediaPlayer.stop();
                        playbackState = new PlaybackStateCompat.Builder()
                                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                                .build();
                        mediaSession.setPlaybackState(playbackState);
                        updateNotification();
                        break;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error thrown during playback");
            }
        }


//        @Override
//        public void onPlay() {
//            if (playbackState.getState() == PlaybackStateCompat.STATE_STOPPED) {
//                if(L) Log.i(LOG_TAG, "Calling onPlay()");
//                mediaPlayer.start();
//                playbackState = new PlaybackStateCompat.Builder()
//                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
//                        .build();
//                mediaSession.setPlaybackState(playbackState);
//                updateNotification();
//            }
//        }


        @Override
        public void onStop() {
            int state = playbackState.getState();
            if(state == PlaybackStateCompat.STATE_PLAYING ||
                    state == PlaybackStateCompat.STATE_BUFFERING) {
                if(L) Log.i(LOG_TAG, "Calling onStop()");
                mediaPlayer.stop();
                playbackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                        .build();
                mediaSession.setPlaybackState(playbackState);
                updateNotification();


            }
        }
    };

    private void updateNotification() {

        Log.i(LOG_TAG, "Notification updated, if one existed");
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    public MediaSessionCompat.Token getMediaSessionToken() {
        return mediaSession.getSessionToken();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(mp!=null){
            mp.release();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(streamUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.prepareAsync();
        }
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();

    }

    public void play(String streamUrl){
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(streamUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.prepareAsync();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        streamUrl= intent.getStringExtra("url");
        String action = intent.getAction();
        Log.e("action",action);
        //        if(action.equals(PlaybackStatus.IDLE)){

        Thread tr=new Thread(new Runnable() {
            @Override
            public void run() {
                Intent in = new Intent(getBaseContext(), MainActivity.class);
                in.putExtra("status", PlaybackStatus.PLAYING);
                PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 1, in, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    pi.send(getApplicationContext(),1,in);
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                play(streamUrl);
                notificationManager.startNotify(PlaybackStatus.PLAYING);

            }
        });
        tr.start();
        //        }


        return START_NOT_STICKY;
    }
        @Override
        public void onDestroy() {
            super.onDestroy();
            if(L) Log.i(LOG_TAG, "Stopping playback service, releasing resources");
            mediaPlayer.release();
            mediaSession.release();
        }
}

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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import de.greenrobot.event.EventBus;

import java.io.IOException;

public  class SecondServiceRadio extends Service implements  MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener{
    public static final String STATION_URI = "Station_uri";
    public static final String STATION_NAME = "Station_name";


    private static final String LOG_TAG = "SecondServiceRadio";
    private final boolean L = true;
    private String status;
    private AudioManager audioManager;
    private MediaControllerCompat.TransportControls transportControls;

    private PlaybackStateCompat playbackState;
    private Binder binder = new ServiceBinder();
    private MediaSessionCompat mediaSession;
    private String streamUrl="";
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
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = new MediaNotificationManager(this);
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

    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        @Override
        public void onStop() {
            super.onStop();
            stop();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            resume();

        }
    };

    private void stop() {
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        notificationManager.cancelNotify();
    }

    private void pause() {
        mediaPlayer.pause();
        notificationManager.startNotify(PlaybackStatus.PAUSED);
    }
    public void resume() {


        if(streamUrl!= null){
            play(streamUrl);
        notificationManager.startNotify(PlaybackStatus.PLAYING);}
        else {
            play(MainActivity.STREAM);
            notificationManager.startNotify(PlaybackStatus.PLAYING);
        }
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
        String action = intent.getAction();
        if(TextUtils.isEmpty(action))
            return START_NOT_STICKY;
        Log.e("action_onStartCommand",action);
        streamUrl= intent.getStringExtra("url");
        if(streamUrl!=null)
        MainActivity.STREAM=streamUrl;
        switch (action){
            case PlaybackStatus.IDLE :
                Thread tr=new Thread(new Runnable() {
                    @Override
                    public void run() {
                       /* Intent in = new Intent(getBaseContext(), MainActivity.class);
                        in.putExtra("status", PlaybackStatus.PLAYING);*/
                        play(streamUrl);
                        notificationManager.startNotify(PlaybackStatus.PLAYING);

                    }
                });
                tr.start();

                break;
                case  PlaybackStatus.PAUSED:
                    pause();
                    
                break;
                 case  PlaybackStatus.STOPPED:
                     stop();
                break;
            case PlaybackStatus.PLAYING:
                    resume();

                break;

        }



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

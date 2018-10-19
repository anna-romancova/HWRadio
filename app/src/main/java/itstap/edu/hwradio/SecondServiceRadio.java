package itstap.edu.hwradio;


import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

public class SecondServiceRadio extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {
    private static final String LOG_TAG = "SecondServiceRadio";
    private final boolean L = true;
    private Thread tr;
    private String streamUrl = "";
    private MediaNotificationManager notificationManager;
    private MediaPlayer mediaPlayer;
    private String action;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = new MediaNotificationManager(this);
        Log.i(LOG_TAG, "Create service");

    }

    private void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if(tr.isAlive()){

            tr.interrupt();
        }
        notificationManager.cancelNotify();
    }

    private void pause() {

        mediaPlayer.pause();
        if(tr.isAlive()){

        tr.interrupt();
        }
        notificationManager.startNotify(PlaybackStatus.PAUSED);
    /*    Intent intentToActivity = new Intent(getBaseContext(),MainActivity.class)
                .setAction(PlaybackStatus.PAUSED)
                .setPackage(this.getPackageName());
        intentToActivity*/
    }

    public void resume() {

        if (streamUrl != null) {
            play(streamUrl);
            notificationManager.startNotify(PlaybackStatus.PLAYING);
        } else {
            play(MainActivity.STREAM);
            notificationManager.startNotify(PlaybackStatus.PLAYING);
        }
        if(tr.isAlive()){

            tr.interrupt();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp != null) {
            mp.release();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        try {
            tr.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mediaPlayer.isPlaying()) {
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

    public void play(String streamUrl) {
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
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
        notificationManager.startNotify(PlaybackStatus.PLAYING);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        action  = intent.getAction();
        if (TextUtils.isEmpty(action))
            return START_NOT_STICKY;
        Log.i(LOG_TAG, action);
        streamUrl = intent.getStringExtra("url");
        if (streamUrl != null) {
            MainActivity.STREAM = streamUrl;
        }
        tr = new Thread(new Runnable() {
            @Override
            public void run() {

                switch (action) {
                    case PlaybackStatus.IDLE:

                        play(streamUrl);
                        break;
                    case PlaybackStatus.PAUSED:
                        pause();

                        break;
                    case PlaybackStatus.STOPPED:

                        stop();

                        break;
                    case PlaybackStatus.PLAYING:

                        resume();

                        break;

                }
            }

        });


            tr.start();




        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Stopping playback service, releasing resources");
        mediaPlayer.release();
        if(tr.isAlive()){

            tr.interrupt();
        }
    }
}

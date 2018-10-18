package itstap.edu.hwradio;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.io.IOException;

public  class SecondServiceRadio extends Service implements  MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener{
    private NotificationManager notifManager;
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";
    private String status;
    private AudioManager audioManager;
    private String strAppName;
    private String strLiveBroadcast;
    private Binder binder = new ServiceBinder();
    private MediaSessionCompat mediaSession;
    private String streamUrl;
    private MediaNotificationManager notificationManager;
    private MediaPlayer mediaPlayer;

    public class ServiceBinder extends Binder {
        public SecondServiceRadio getService() {
            return SecondServiceRadio.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        notificationManager = new MediaNotificationManager(this);
        Log.d("create","Create service");

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


        return START_STICKY;
    }
}

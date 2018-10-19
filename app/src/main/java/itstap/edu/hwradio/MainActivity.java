package itstap.edu.hwradio;


import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import itstap.edu.hwradio.list.Shoutcast;
import itstap.edu.hwradio.list.ShoutcastHelper;
import itstap.edu.hwradio.list.ShoutcastListAdapter;


public class MainActivity extends Activity implements View.OnClickListener {

    public final static String DATA_STREAM ="http://37.59.14.77:8352/stream" ;
    public volatile   static String STREAM ="" ;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private ImageButton playTrigger;
    private  MediaPlayer mediaPlayer;
    private ListView listview;
    private RelativeLayout subPlayer;

    private TextView name;
    private boolean radioIsOpen;
    private String streamUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.INTERNET)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("promm", "Permission is granted");
            } else {
                Log.v("promm", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_NETWORK_STATE
                }, PERMISSION_REQUEST_CODE);

            }
        } else {
            Log.v("promm", "Permission is granted");
        }

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        playTrigger=findViewById(R.id.playTrigger);
        name=findViewById(R.id.name);
        subPlayer=findViewById(R.id.sub_player);
        listview=findViewById(R.id.listview);
        listview.setAdapter(new ShoutcastListAdapter(this, ShoutcastHelper.retrieveShoutcasts(this)));
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Shoutcast shoutcast = (Shoutcast) parent.getItemAtPosition(position);
                if(shoutcast == null){
                    return;
                }
                if(streamUrl!=null){
                    playTrigger.setImageResource(R.drawable.ic_play_arrow_black);
                    radioIsOpen=false;
                }
                name.setText(shoutcast.getName());
                subPlayer.setVisibility(View.VISIBLE);
                streamUrl = shoutcast.getUrl();
                Log.d("streamUrl",streamUrl);


            }
        });


        radioIsOpen=false;
        playTrigger.setImageResource(R.drawable.ic_play_arrow_black);
        playTrigger.setOnClickListener(this);
    }


    @Override
    protected void onDestroy() {
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        super.onDestroy();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            Log.d("this is creating", "shiva said right");
            if(action==PlaybackStatus.PAUSED) {
                playTrigger.setImageResource(R.drawable.ic_play_arrow_black);
            }
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.playTrigger:
                if (!radioIsOpen) {
                PendingIntent pi = createPendingResult(1, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
                    Intent intent = new Intent(getBaseContext(),SecondServiceRadio.class)
                        .setAction(PlaybackStatus.IDLE)
                        .putExtra("url", streamUrl)
                        .putExtra("name", name.getText().toString())
                        .putExtra("pi",pi)
                        .setPackage(this.getPackageName());
                int result = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
                if (result == PackageManager.PERMISSION_GRANTED) {
                    getApplicationContext().startService(intent);
                    Toast.makeText(this,"click play",Toast.LENGTH_SHORT).show();
                    playTrigger.setImageResource(R.drawable.ic_pause_black);
                    radioIsOpen=true;
                    Log.d("startService","startService");
                }



                }else {

                    PendingIntent pi = createPendingResult(2, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
                    Intent  intent = new Intent(getBaseContext(),SecondServiceRadio.class)
                            .setAction(PlaybackStatus.STOPPED)
                            .putExtra("pi",pi);
                    intent.setPackage(this.getPackageName());
                    int result = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        startService(intent);
                        Toast.makeText(this,"click stop",Toast.LENGTH_SHORT).show();
                        playTrigger.setImageResource(R.drawable.ic_play_arrow_black);
                        radioIsOpen=false;

                    }
                }

              /*  if (!radioIsOpen) {
                  try {
                       mediaPlayer = new MediaPlayer();
                       mediaPlayer.setDataSource(streamUrl);
                       mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                       mediaPlayer.setOnPreparedListener(this);
                       mediaPlayer.setOnCompletionListener(this);
                       mediaPlayer.setOnErrorListener(this);
                       mediaPlayer.prepareAsync();
                       radioIsOpen=true;
                       Toast.makeText(getBaseContext(), "Radio is playing...", Toast.LENGTH_LONG).show();
                       playTrigger.setImageResource(R.drawable.ic_pause_black);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }


                }else {
                    if(mediaPlayer.isPlaying()){
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        Toast.makeText(getBaseContext(), "Radio is closing...", Toast.LENGTH_LONG).show();
                        playTrigger.setImageResource(R.drawable.ic_play_arrow_black);
                    }
                }*/
                break;

        }

    }


}

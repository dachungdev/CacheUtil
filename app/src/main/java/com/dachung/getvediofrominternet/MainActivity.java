package com.dachung.getvediofrominternet;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import com.dachung.getvediofrominternet.CustomView.FullscreenView;
import com.dachung.getvediofrominternet.Util.FileLoader;
import com.dachung.getvediofrominternet.Util.MusicPlayer;
import com.dachung.getvediofrominternet.Util.VideoLoader;

public class MainActivity extends AppCompatActivity {

    private MusicPlayer musicPlayer;
    private FileLoader fileLoader;

    private final String CF_URL = "http://47.112.17.83:8080/music/ziran.mp3";
    private final String FANGYANG_URL = "http://47.112.17.83:8080/music/shaonv.mp3";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        FullscreenView fullscreenView = (FullscreenView) findViewById(R.id.fullscreen_videoview);
//
//        VideoLoader videoLoader = VideoLoader.build(getApplicationContext());
//        videoLoader.bindFile("http://47.112.17.83:8080/video/bgvideooo.mp4", fullscreenView);

        musicPlayer = MusicPlayer.build();
        fileLoader = FileLoader.build(getApplicationContext());

        Button cf_btn = (Button) findViewById(R.id.cf_btn);
        Button fangyang_btn = (Button) findViewById(R.id.fangyang_btn);

        cf_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileLoader.bindFile(CF_URL, musicPlayer);
            }
        });

        fangyang_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileLoader.bindFile(FANGYANG_URL, musicPlayer);
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        musicPlayer.close();
    }
}

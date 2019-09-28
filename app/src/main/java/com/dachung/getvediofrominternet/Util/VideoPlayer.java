package com.dachung.getvediofrominternet.Util;

import android.content.Context;
import android.util.AttributeSet;

import com.dachung.getvediofrominternet.CustomView.FullscreenView;

public class VideoPlayer extends FullscreenView implements PlayerImpl {

    public VideoPlayer(Context context) {
        super(context);
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void play(String path){
        // 若视频正在播放，则不打扰
        if(isPlaying()){
            return;
        }

        setVideoPath(path);
        start();
    }
}

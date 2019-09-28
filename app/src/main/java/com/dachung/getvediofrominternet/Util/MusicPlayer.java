package com.dachung.getvediofrominternet.Util;

import android.media.MediaPlayer;

import java.io.IOException;

public class MusicPlayer implements PlayerImpl {

    private static volatile MusicPlayer musicPlayer;

    private static volatile MediaPlayer mediaPlayer;

    private MusicPlayer(){
        this.mediaPlayer = new MediaPlayer();
    }

    public static MusicPlayer build(){
        if(musicPlayer == null){
            synchronized (MusicPlayer.class){
                if (musicPlayer == null){
                    musicPlayer = new MusicPlayer();
                }
            }
        }
        return musicPlayer;
    }

    @Override
    public void play(String path){
        if(mediaPlayer.isPlaying()){
            // 若音频正在播放，则停止
            mediaPlayer.reset();
        }
        try{
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void close(){
        mediaPlayer.release();
    }
}

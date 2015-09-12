package cn.nekocode.murmur.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cn.nekocode.murmur.MyApplication;
import cn.nekocode.murmur.media.Player;

/**
 * Created by nekocode on 2015/4/24 0024.
 */
public class PlayManager {
    private static PlayManager playManager;
    private Map<String, MediaPlayer> murmurPlayers;
    private Player songPlayer;
    private Context context;

    private String nowPlayingSong = null;

    public static PlayManager getInstant() {
        if (playManager == null) {
            playManager = new PlayManager();
        }

        return playManager;
    }

    private PlayManager() {
        context = MyApplication.get().getApplicationContext();

        songPlayer = new Player();
        murmurPlayers = new HashMap<>();
    }

    public static void onDestory() {
        playManager.songPlayer.stop();

        for(Map.Entry<String, MediaPlayer> mediaPlayer : playManager.murmurPlayers.entrySet()) {
            mediaPlayer.getValue().stop();
            mediaPlayer.getValue().release();
        }
        playManager.murmurPlayers.clear();

        playManager = null;
    }

    public void setPlayerListener(Player.PlayerListener listener) {
        songPlayer.setPlayerListener(listener);
    }

    public void addMurmurs(String names[]) {
        try {
            for (String name : names) {
                if (murmurPlayers.containsKey(name)) {
                    murmurPlayers.get(name).start();
                } else {
                    String murmurPath = FileManager.getAppRootPath() + "murmurs/" + name;

                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setDataSource(murmurPath);
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                        }
                    });

                    murmurPlayers.put(name, mediaPlayer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteMurmurs(String names[]) {
        for (String name : names) {
            if (murmurPlayers.containsKey(name)) {
                murmurPlayers.get(name).pause();
            }
        }
    }

    public void playSong(String url) {
        songPlayer.playUrl(url);
    }

    public void pauseSong() {
        songPlayer.pause();
    }
}

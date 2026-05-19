package com.example.vibemusicplayer.ui.viewmodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import java.io.IOException;

public class SongViewModel extends ViewModel {

    @SuppressLint("Range")
    public MediaPlayer getLocalMusicByNameAndArtist(Context context, String songName, String songArtist) {
        String filePath = null;
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = context.getContentResolver().query(
                musicUri,
                new String[]{MediaStore.Audio.Media.DATA},
                MediaStore.Audio.Media.TITLE + " = ? AND " + MediaStore.Audio.Media.ARTIST + " = ?",
                new String[]{songName, songArtist},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            cursor.close();
        }

        if (filePath != null) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(context, Uri.parse(filePath));
                mediaPlayer.prepare();
                return mediaPlayer;
            } catch (IOException e) {
                Log.e("MusicMetadataHelper", "Failed to get MP3 file", e);
                return null;
            }
        } else {
            Log.e("MusicMetadataHelper", "Failed to get MP3 file");
            return null;
        }
    }

    // 从服务器流式播放（MinIO URL）
    public MediaPlayer getRemoteMusic(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            return null;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();
            return mediaPlayer;
        } catch (IOException e) {
            Log.e("MusicMetadataHelper", "Failed to play remote MP3", e);
            return null;
        }
    }
}

package com.example.vibemusicplayer.ui.viewmodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vibemusicplayer.MyApplication;
import com.example.vibemusicplayer.ui.model.Song;
import com.example.vibemusicplayer.ui.util.MusicMetadataHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {
    public MutableLiveData<List<Song>> songsLiveData = new MutableLiveData<>();

    public void search(Context context, String keyword) {
        MyApplication app = (MyApplication) context.getApplicationContext();
        if (app.isLoggedIn()) {
            searchSongsFromServer(app, keyword);
            searchArtistsFromServer(app, keyword);
        } else {
            searchFromLocal(context, keyword);
        }
    }

    private void searchSongsFromServer(MyApplication app, String keyword) {
        RequestParams params = new RequestParams(app.getAllSongsUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());
        try {
            JSONObject body = new JSONObject();
            body.put("pageNum", 1);
            body.put("pageSize", 200);
            body.put("songName", keyword);
            params.setBodyContent(body.toString());
        } catch (Exception e) { e.printStackTrace(); }

        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                List<Song> songs = new ArrayList<>();
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.optJSONObject("data");
                        JSONArray items = data != null ? (data.optJSONArray("items") != null ? data.optJSONArray("items") : data.optJSONArray("records")) : null;
                        if (items != null) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.optJSONObject(i);
                                if (item != null) {
                                    String name = item.optString("songName", "");
                                    String artist = item.optString("artistName", "");
                                    String album = item.optString("album", "");
                                    String dur = item.optString("duration", "0");
                                    String cover = item.optString("coverUrl", null);
                                    long songId = item.optLong("songId", -1);
                                    String streamAudio = app.streamUrl + songId;
                                    try {
                                        double sec = Double.parseDouble(dur);
                                        long ms = (long) (sec * 1000);
                                        @SuppressLint("DefaultLocale")
                                        String formattedDuration = String.format("%d:%02d", (ms / 60000), (ms % 60000) / 1000);
                                        songs.add(new Song(songId, name, artist, album, formattedDuration, cover, streamAudio));
                                    } catch (NumberFormatException e) {
                                        songs.add(new Song(songId, name, artist, album, dur, cover, streamAudio));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                new Handler(Looper.getMainLooper()).post(() -> songsLiveData.postValue(songs));
            }
            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                new Handler(Looper.getMainLooper()).post(() -> songsLiveData.postValue(new ArrayList<>()));
            }
            @Override public void onCancelled(CancelledException cex) {}
            @Override public void onFinished() {}
        });
    }

    private void searchArtistsFromServer(MyApplication app, String keyword) {
        RequestParams params = new RequestParams(app.BASE_URL + "artist/getAllArtists");
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());
        try {
            JSONObject body = new JSONObject();
            body.put("pageNum", 1);
            body.put("pageSize", 50);
            body.put("artistName", keyword);
            params.setBodyContent(body.toString());
        } catch (Exception e) { e.printStackTrace(); }

        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            JSONArray items = data.optJSONArray("items");
                            if (items != null && items.length() > 0) {
                                // 将歌手搜索结果追加为带有 artist 标记的歌曲
                                List<Song> songs = new ArrayList<>();
                                for (int i = 0; i < items.length(); i++) {
                                    JSONObject item = items.optJSONObject(i);
                                    if (item != null) {
                                        String name = "歌手: " + item.optString("artistName", "");
                                        Song s = new Song(name, "", "", "", item.optString("avatar", ""));
                                        s.setSongId(-item.optLong("artistId", 0));
                                        s.setFromServer(true);
                                        songs.add(s);
                                    }
                                }
                                List<Song> current = songsLiveData.getValue();
                                if (current != null) {
                                    songs.addAll(0, current);
                                }
                                new Handler(Looper.getMainLooper()).post(() -> songsLiveData.postValue(songs));
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(Throwable ex, boolean b) {}
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void searchFromLocal(Context context, String keyword) {
        List<Song> songs = getMusicDataByKeyword(context, keyword);
        songsLiveData.postValue(songs);
    }

    public List<Song> getMusicDataByKeyword(Context context, String keyword) {
        List<Song> songs = new ArrayList<>();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(musicUri, null,
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.TITLE + " LIKE ?",
                new String[]{"%" + keyword + "%"}, MediaStore.Audio.Media.TITLE);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                long songId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                String filePath = MusicMetadataHelper.getMusicFilePath(context.getContentResolver(), songId);
                long milliseconds = Long.parseLong(duration);
                @SuppressLint("DefaultLocale")
                String formattedDuration = String.format("%d:%02d", (milliseconds / 60000), (milliseconds % 60000) / 1000);
                Uri albumArtUri = MusicMetadataHelper.getAlbumArtFromFile(context, filePath);
                songs.add(new Song(name, artist, album, formattedDuration, albumArtUri != null ? albumArtUri.toString() : null));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songs;
    }
}

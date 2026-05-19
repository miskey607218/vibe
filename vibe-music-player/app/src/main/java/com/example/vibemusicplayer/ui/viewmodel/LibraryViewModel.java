package com.example.vibemusicplayer.ui.viewmodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.lifecycle.LiveData;
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

public class LibraryViewModel extends ViewModel {
    private static final int PAGE_SIZE = 20;
    private final MutableLiveData<List<Song>> songsLiveData = new MutableLiveData<>();
    private final List<Song> allSongs = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    public LiveData<List<Song>> getSongsLiveData() {
        return songsLiveData;
    }

    // 异步加载所有数据（登录后用服务器，否则用本地）
    public void loadAllData(Context context) {
        MyApplication app = (MyApplication) context.getApplicationContext();
        if (app.isLoggedIn()) {
            loadFromServer(app);
        } else {
            loadFromLocal(context);
        }
    }

    // 从 Spring Boot 服务器加载
    private void loadFromServer(MyApplication app) {
        if (isLoading) return;
        isLoading = true;
        currentPage = 0;
        allSongs.clear();

        RequestParams params = new RequestParams(app.getAllSongsUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());

        try {
            JSONObject body = new JSONObject();
            body.put("pageNum", 1);
            body.put("pageSize", 200);
            params.setBodyContent(body.toString());
        } catch (Exception e) { e.printStackTrace(); }

        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.optJSONObject("data");
                        JSONArray items = null;
                        if (data != null) {
                            items = data.optJSONArray("items");
                            if (items == null) items = data.optJSONArray("records");
                        }
                        if (items != null) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.optJSONObject(i);
                                if (item != null) {
                                    String name = item.optString("songName", "");
                                    String artist = item.optString("artistName", "");
                                    String album = item.optString("album", "");
                                    String dur = item.optString("duration", "0");
                                    String cover = item.optString("coverUrl", null);
                                    String audio = item.optString("audioUrl", null);
                                    long songId = item.optLong("songId", -1);
                                    // 用代理流 URL 替换原始 MinIO URL
                                    String streamAudio = audio != null ? app.streamUrl + songId : null;

                                    try {
                                        double sec = Double.parseDouble(dur);
                                        long ms = (long) (sec * 1000);
                                        @SuppressLint("DefaultLocale")
                                        String formattedDuration = String.format("%d:%02d",
                                                (ms / 60000), (ms % 60000) / 1000);
                                        Song song = new Song(songId, name, artist, album, formattedDuration, cover, streamAudio);
                                        allSongs.add(song);
                                    } catch (NumberFormatException e) {
                                        Song song = new Song(songId, name, artist, album, dur, cover, streamAudio);
                                        allSongs.add(song);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                isLoading = false;
                hasMoreData = allSongs.size() > 0;
                new Handler(Looper.getMainLooper()).post(LibraryViewModel.this::loadNextPage);
            }
            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                isLoading = false;
                hasMoreData = false;
                new Handler(Looper.getMainLooper()).post(() -> songsLiveData.postValue(new ArrayList<>()));
            }
            @Override public void onCancelled(CancelledException cex) {}
            @Override public void onFinished() {}
        });
    }

    // 从本地 MediaStore 加载
    private void loadFromLocal(Context context) {
        if (isLoading) return;
        isLoading = true;
        currentPage = 0;
        allSongs.clear();

        new Thread(() -> {
            Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(
                    musicUri, null,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0",
                    null, MediaStore.Audio.Media.TITLE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    String duration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    long songId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    String filePath = MusicMetadataHelper.getMusicFilePath(context.getContentResolver(), songId);
                    long ms = Long.parseLong(duration);
                    @SuppressLint("DefaultLocale")
                    String formattedDuration = String.format("%d:%02d", (ms / 60000), (ms % 60000) / 1000);
                    Uri albumArtUri = MusicMetadataHelper.getAlbumArtFromFile(context, filePath);
                    Song song = new Song(name, artist, album, formattedDuration, albumArtUri != null ? albumArtUri.toString() : null);
                    allSongs.add(song);
                } while (cursor.moveToNext());
                cursor.close();
            }
            isLoading = false;
            hasMoreData = allSongs.size() > 0;
            new Handler(Looper.getMainLooper()).post(this::loadNextPage);
        }).start();
    }

    public void loadNextPage() {
        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allSongs.size());
        if (fromIndex < toIndex) {
            List<Song> currentList = new ArrayList<>(allSongs.subList(0, toIndex));
            songsLiveData.postValue(currentList);
            currentPage++;
            hasMoreData = toIndex < allSongs.size();
        } else {
            hasMoreData = false;
        }
    }

    public boolean hasMoreData() { return hasMoreData; }
}

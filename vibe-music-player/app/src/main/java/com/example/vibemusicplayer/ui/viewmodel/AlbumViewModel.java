package com.example.vibemusicplayer.ui.viewmodel;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vibemusicplayer.MyApplication;
import com.example.vibemusicplayer.ui.model.Album;
import com.example.vibemusicplayer.ui.util.MusicMetadataHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlbumViewModel extends ViewModel {
    private static final int PAGE_SIZE = 20;
    private final MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private final List<Album> allAlbums = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    public LiveData<List<Album>> getAlbumsLiveData() {
        return albumsLiveData;
    }

    public void loadAllData(Context context) {
        MyApplication app = (MyApplication) context.getApplicationContext();
        if (app.isLoggedIn()) {
            loadFromServer(app);
        } else {
            loadFromLocal(context);
        }
    }

    private void loadFromServer(MyApplication app) {
        if (isLoading) return;
        isLoading = true;
        currentPage = 0;
        allAlbums.clear();

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
                Map<String, Album> albumMap = new LinkedHashMap<>();
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.optJSONObject("data");
                        JSONArray items = data != null ? (data.optJSONArray("items") != null ? data.optJSONArray("items") : data.optJSONArray("records")) : null;
                        if (items != null) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.optJSONObject(i);
                                if (item != null) {
                                    String albumName = item.optString("album", "未知专辑");
                                    String cover = item.optString("coverUrl", null);
                                    Album existing = albumMap.get(albumName);
                                    if (existing == null) {
                                        Album album = new Album(albumName, "1", cover != null ? Uri.parse(cover) : null);
                                        albumMap.put(albumName, album);
                                    } else {
                                        int count = Integer.parseInt(existing.getCount()) + 1;
                                        albumMap.put(albumName, new Album(albumName, String.valueOf(count), cover != null ? Uri.parse(cover) : null));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                allAlbums.addAll(albumMap.values());
                isLoading = false;
                hasMoreData = !allAlbums.isEmpty();
                new Handler(Looper.getMainLooper()).post(AlbumViewModel.this::loadNextPage);
            }
            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                isLoading = false;
                hasMoreData = false;
                new Handler(Looper.getMainLooper()).post(() -> albumsLiveData.postValue(new ArrayList<>()));
            }
            @Override public void onCancelled(CancelledException cex) {}
            @Override public void onFinished() {}
        });
    }

    private void loadFromLocal(Context context) {
        if (isLoading) return;
        isLoading = true;
        currentPage = 0;
        allAlbums.clear();
        new Thread(() -> {
            Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(albumUri,
                    new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ALBUM_ART},
                    null, null, MediaStore.Audio.Albums.ALBUM + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    long songId = getFirstSongIdInAlbum(context, name);
                    String filePath = MusicMetadataHelper.getMusicFilePath(context.getContentResolver(), songId);
                    String count = String.valueOf(countSongsInAlbum(context, name));
                    Uri albumArtUri = MusicMetadataHelper.getAlbumArtFromFile(context, filePath);
                    allAlbums.add(new Album(name, count, albumArtUri));
                } while (cursor.moveToNext());
                cursor.close();
            }
            isLoading = false;
            hasMoreData = allAlbums.size() > 0;
            new Handler(Looper.getMainLooper()).post(this::loadNextPage);
        }).start();
    }

    public void loadNextPage() {
        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allAlbums.size());
        if (fromIndex < toIndex) {
            albumsLiveData.postValue(new ArrayList<>(allAlbums.subList(0, toIndex)));
            currentPage++;
            hasMoreData = toIndex < allAlbums.size();
        } else {
            hasMoreData = false;
        }
    }

    public boolean hasMoreData() { return hasMoreData; }

    public static int countSongsInAlbum(Context context, String albumName) {
        int songCount = 0;
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media.ALBUM + " = ?", new String[]{albumName}, null);
        if (cursor != null) { songCount = cursor.getCount(); cursor.close(); }
        return songCount;
    }

    public static long getFirstSongIdInAlbum(Context context, String albumName) {
        long songId = -1;
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media.ALBUM + " = ?", new String[]{albumName},
                MediaStore.Audio.Media.TRACK + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            songId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            cursor.close();
        }
        return songId;
    }
}

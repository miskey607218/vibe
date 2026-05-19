package com.example.vibemusicplayer.ui.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vibemusicplayer.MyApplication;
import com.example.vibemusicplayer.ui.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class FavoriteViewModel extends ViewModel {
    private static final int PAGE_SIZE = 20;
    private final MutableLiveData<List<Song>> favoriteLiveData = new MutableLiveData<>();
    private final List<Song> allFavorites = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    public LiveData<List<Song>> getFavoriteLiveData() {
        return favoriteLiveData;
    }

    // 兼容旧接口：支持 Context 参数（优先从服务器加载，登录后才能使用）
    public void loadAllData(Context context) {
        MyApplication app = (MyApplication) context.getApplicationContext();

        if (app.isLoggedIn()) {
            loadFromServer(app);
        } else {
            // 未登录时从本地 SQLite 加载
            loadFromLocal(context);
        }
    }

    // 无参版本（内部调用）
    public void loadAllData() {
        allFavorites.clear();
        currentPage = 0;
        hasMoreData = false;
        favoriteLiveData.postValue(new ArrayList<>());
    }

    // 从 Spring Boot 后端加载收藏
    private void loadFromServer(MyApplication app) {
        isLoading = true;
        currentPage = 0;
        allFavorites.clear();

        RequestParams params = new RequestParams(app.getFavoriteSongsUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());

        try {
            JSONObject body = new JSONObject();
            body.put("pageNum", 1);
            body.put("pageSize", 100);
            params.setBodyContent(body.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            JSONArray items = data.optJSONArray("items");
                            if (items == null) {
                                items = data.optJSONArray("records");
                            }
                            if (items != null) {
                                for (int i = 0; i < items.length(); i++) {
                                    JSONObject item = items.optJSONObject(i);
                                    if (item != null) {
                                        Song song = new Song(
                                                item.optString("songName", ""),
                                                item.optString("artistName", ""),
                                                item.optString("album", ""),
                                                item.optString("duration", ""),
                                                item.optString("coverUrl", null)
                                        );
                                        allFavorites.add(song);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isLoading = false;
                hasMoreData = false;
                new Handler(Looper.getMainLooper()).post(() -> favoriteLiveData.postValue(allFavorites));
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                isLoading = false;
                hasMoreData = false;
                new Handler(Looper.getMainLooper()).post(() -> favoriteLiveData.postValue(new ArrayList<>()));
            }

            @Override
            public void onCancelled(CancelledException cex) {
            }

            @Override
            public void onFinished() {
            }
        });
    }

    // 从本地 SQLite 加载（保留作为离线模式）
    private void loadFromLocal(Context context) {
        if (isLoading) return;
        isLoading = true;
        currentPage = 0;
        allFavorites.clear();

        new Thread(() -> {
            com.example.vibemusicplayer.ui.database.SongDatabaseHelper dbHelper =
                    new com.example.vibemusicplayer.ui.database.SongDatabaseHelper(context);
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            android.database.Cursor cursor = db.rawQuery("SELECT * FROM favorite_song", null);
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                    String album = cursor.getString(cursor.getColumnIndexOrThrow("album"));
                    String duration = cursor.getString(cursor.getColumnIndexOrThrow("duration"));
                    String uri = cursor.getString(cursor.getColumnIndexOrThrow("uri"));
                    String albumArtUri = (uri != null && !uri.isEmpty() && !"null".equals(uri)) ? uri : null;
                    Song song = new Song(name, artist, album, duration, albumArtUri);
                    allFavorites.add(song);
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
            isLoading = false;
            hasMoreData = allFavorites.size() > 0;
            new Handler(Looper.getMainLooper()).post(this::loadNextPage);
        }).start();
    }

    // 内存分页
    public void loadNextPage() {
        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allFavorites.size());
        if (fromIndex < toIndex) {
            List<Song> currentList = new ArrayList<>(allFavorites.subList(0, toIndex));
            favoriteLiveData.postValue(currentList);
            currentPage++;
            hasMoreData = toIndex < allFavorites.size();
        } else {
            hasMoreData = false;
        }
    }

    public boolean hasMoreData() {
        return hasMoreData;
    }
}

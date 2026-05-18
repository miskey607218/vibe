package com.example.vibemusicplayer.ui.viewmodel;

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
    private final MutableLiveData<List<Song>> favoriteLiveData = new MutableLiveData<>();
    private final List<Song> allFavorites = new ArrayList<>();

    public LiveData<List<Song>> getFavoriteLiveData() {
        return favoriteLiveData;
    }

    // 从 Spring Boot 后端加载收藏歌曲
    public void loadAllData() {
        allFavorites.clear();

        MyApplication app = (MyApplication) x.app();
        if (!app.isLoggedIn()) {
            favoriteLiveData.postValue(new ArrayList<>());
            return;
        }

        String url = app.getFavoriteSongsUrl;

        RequestParams params = new RequestParams(url);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());

        try {
            JSONObject body = new JSONObject();
            body.put("pageNum", 1);
            body.put("pageSize", 50);
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
                new Handler(Looper.getMainLooper()).post(() -> favoriteLiveData.postValue(allFavorites));
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
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
}

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
import com.example.vibemusicplayer.ui.model.Artist;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class ArtistViewModel extends ViewModel {
    private static final int PAGE_SIZE = 20;
    private final MutableLiveData<List<Artist>> artistsLiveData = new MutableLiveData<>();
    private final List<Artist> allArtists = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    public LiveData<List<Artist>> getArtistsLiveData() {
        return artistsLiveData;
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
        allArtists.clear();

        RequestParams params = new RequestParams(app.BASE_URL + "artist/getAllArtists");
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
                        JSONArray items = data != null ? (data.optJSONArray("items") != null ? data.optJSONArray("items") : data.optJSONArray("records")) : null;
                        if (items != null) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.optJSONObject(i);
                                if (item != null) {
                                    String name = item.optString("artistName", item.optString("name", ""));
                                    allArtists.add(new Artist(name, ""));
                                }
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                isLoading = false;
                hasMoreData = !allArtists.isEmpty();
                new Handler(Looper.getMainLooper()).post(ArtistViewModel.this::loadNextPage);
            }
            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                isLoading = false;
                hasMoreData = false;
                new Handler(Looper.getMainLooper()).post(() -> artistsLiveData.postValue(new ArrayList<>()));
            }
            @Override public void onCancelled(CancelledException cex) {}
            @Override public void onFinished() {}
        });
    }

    private void loadFromLocal(Context context) {
        if (isLoading) return;
        isLoading = true;
        currentPage = 0;
        allArtists.clear();
        new Thread(() -> {
            Uri artistUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(artistUri,
                    new String[]{MediaStore.Audio.Artists.ARTIST},
                    null, null, MediaStore.Audio.Artists.ARTIST + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String count = String.valueOf(countSongsInArtist(context, name));
                    allArtists.add(new Artist(name, count));
                } while (cursor.moveToNext());
                cursor.close();
            }
            isLoading = false;
            hasMoreData = allArtists.size() > 0;
            new Handler(Looper.getMainLooper()).post(this::loadNextPage);
        }).start();
    }

    public void loadNextPage() {
        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allArtists.size());
        if (fromIndex < toIndex) {
            artistsLiveData.postValue(new ArrayList<>(allArtists.subList(0, toIndex)));
            currentPage++;
            hasMoreData = toIndex < allArtists.size();
        } else {
            hasMoreData = false;
        }
    }

    public boolean hasMoreData() { return hasMoreData; }

    public static int countSongsInArtist(Context context, String artistName) {
        int songCount = 0;
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media.ARTIST + " = ?", new String[]{artistName}, null);
        if (cursor != null) { songCount = cursor.getCount(); cursor.close(); }
        return songCount;
    }
}

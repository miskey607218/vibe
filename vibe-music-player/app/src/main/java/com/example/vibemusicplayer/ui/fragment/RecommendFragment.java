package com.example.vibemusicplayer.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibemusicplayer.MyApplication;
import com.example.vibemusicplayer.R;
import com.example.vibemusicplayer.ui.adapter.LibraryAdapter;
import com.example.vibemusicplayer.ui.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class RecommendFragment extends Fragment {

    private RecyclerView songRecycler;
    private List<Song> recSongs = new ArrayList<>();
    private LibraryAdapter songAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommend, container, false);

        RecyclerView playlistRecycler = view.findViewById(R.id.rec_playlists);
        songRecycler = view.findViewById(R.id.rec_songs);

        songRecycler.setLayoutManager(new LinearLayoutManager(view.getContext()));
        songAdapter = new LibraryAdapter(recSongs);
        songRecycler.setAdapter(songAdapter);

        MyApplication app = (MyApplication) requireActivity().getApplication();
        if (app.isLoggedIn()) {
            loadRecommendedSongs(app);
            loadRecommendedPlaylists(app, playlistRecycler);
        } else {
            ((TextView) view.findViewById(R.id.rec_title)).setText("请先登录");
        }

        return view;
    }

    private void loadRecommendedSongs(MyApplication app) {
        RequestParams params = new RequestParams(app.getRecommendedSongsUrl);
        params.addHeader("Authorization", app.getAuthToken());
        x.http().get(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                List<Song> songs = new ArrayList<>();
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.optJSONObject(i);
                                if (item != null) {
                                    String name = item.optString("songName", "");
                                    String artist = item.optString("artistName", "");
                                    String album = item.optString("album", "");
                                    String dur = item.optString("duration", "0");
                                    String cover = item.optString("coverUrl", null);
                                    String audio = item.optString("audioUrl", null);
                                    long songId = item.optLong("songId", -1);
                                    String streamAudio = app.streamUrl + songId;
                                    Song s = new Song(songId, name, artist, album,
                                            formatDuration(dur), cover, streamAudio);
                                    songs.add(s);
                                }
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                new Handler(Looper.getMainLooper()).post(() -> {
                    recSongs.clear(); recSongs.addAll(songs);
                    songAdapter.updateData(recSongs);
                });
            }
            @Override public void onError(Throwable ex, boolean b) {}
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void loadRecommendedPlaylists(MyApplication app, RecyclerView recyclerView) {
        RequestParams params = new RequestParams(app.getRecommendedPlaylistsUrl);
        params.addHeader("Authorization", app.getAuthToken());
        x.http().get(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null && data.length() > 0) {
                            String msg = "为你推荐 " + data.length() + " 个歌单";
                            new Handler(Looper.getMainLooper()).post(() ->
                                ((TextView) requireView().findViewById(R.id.rec_label)).setText(msg));
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onError(Throwable ex, boolean b) {}
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private String formatDuration(String dur) {
        try {
            double sec = Double.parseDouble(dur);
            long ms = (long) (sec * 1000);
            return String.format("%d:%02d", (ms / 60000), (ms % 60000) / 1000);
        } catch (NumberFormatException e) { return dur; }
    }
}

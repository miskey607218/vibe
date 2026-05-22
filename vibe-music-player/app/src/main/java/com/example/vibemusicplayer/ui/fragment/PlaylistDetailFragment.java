package com.example.vibemusicplayer.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibemusicplayer.MyApplication;
import com.example.vibemusicplayer.R;
import com.example.vibemusicplayer.ui.adapter.AlbumDetailAdapter;
import com.example.vibemusicplayer.ui.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<Song> songs = new ArrayList<>();
    private long playlistId;
    private String playlistTitle;
    private AlbumDetailAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_detail, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_library);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        playlistId = getArguments().getLong("playlistId", -1);
        playlistTitle = getArguments().getString("title", "歌单");

        TextView txtTitle = view.findViewById(R.id.playlist_title);
        txtTitle.setText(playlistTitle);

        view.findViewById(R.id.btn_play_all).setOnClickListener(v -> {
            if (songs.isEmpty()) { Toast.makeText(getContext(), "歌单为空", Toast.LENGTH_SHORT).show(); return; }
            Bundle bundle = new Bundle();
            bundle.putSerializable("data", new ArrayList<>(songs));
            bundle.putInt("position", 0);
            NavController nav = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            nav.navigate(R.id.nav_song, bundle);
        });

        view.findViewById(R.id.btn_add_song).setOnClickListener(v -> showAddSongDialog());

        loadSongs();
        return view;
    }

    @Override public void onResume() { super.onResume(); loadSongs(); }

    private void loadSongs() {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        if (!app.isLoggedIn()) return;

        String url = app.getPlaylistDetailUrl + playlistId;
        RequestParams params = new RequestParams(url);
        params.addHeader("Authorization", app.getAuthToken());
        x.http().get(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                List<Song> list = new ArrayList<>();
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            JSONArray arr = data.optJSONArray("songs");
                            if (arr != null) {
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject item = arr.optJSONObject(i);
                                    if (item != null) {
                                        Song s = new Song(
                                            item.optLong("songId", -1),
                                            item.optString("songName", ""),
                                            item.optString("artistName", ""),
                                            item.optString("album", ""),
                                            formatDuration(item.optString("duration", "0")),
                                            item.optString("coverUrl", null),
                                            app.streamUrl + item.optLong("songId", -1)
                                        );
                                        list.add(s);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                new Handler(Looper.getMainLooper()).post(() -> {
                    songs.clear(); songs.addAll(list);
                    adapter = new AlbumDetailAdapter(playlistTitle, null, songs);
                    adapter.setOnLongClickListener((song, pos) -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("移除歌曲").setMessage("从歌单中移除「" + song.getName() + "」？")
                            .setPositiveButton("移除", (d,w) -> removeSongFromPlaylist(song.getSongId()))
                            .setNegativeButton("取消", null).show();
                    });
                    recyclerView.setAdapter(adapter);
                });
            }
            @Override public void onError(Throwable ex, boolean b) {}
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void showAddSongDialog() {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_song, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText searchInput = view.findViewById(R.id.add_song_search);
        RecyclerView searchResult = view.findViewById(R.id.add_song_result);
        searchResult.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<Song> searchSongs = new ArrayList<>();
        com.example.vibemusicplayer.ui.adapter.LibraryAdapter searchAdapter =
            new com.example.vibemusicplayer.ui.adapter.LibraryAdapter(searchSongs);
        searchResult.setAdapter(searchAdapter);

        view.findViewById(R.id.add_song_btn).setOnClickListener(v -> {
            String keyword = searchInput.getText().toString().trim();
            if (keyword.isEmpty()) return;
            RequestParams params = new RequestParams(app.getAllSongsUrl);
            params.setAsJsonContent(true);
            params.addHeader("Content-Type", "application/json");
            params.addHeader("Authorization", app.getAuthToken());
            try { params.setBodyContent(new JSONObject().put("pageNum",1).put("pageSize",50).put("songName",keyword).toString()); }
            catch (Exception e) { e.printStackTrace(); }
            x.http().post(params, new Callback.CommonCallback<String>() {
                @Override public void onSuccess(String result) {
                    List<Song> list = new ArrayList<>();
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.optInt("code")==0) {
                            JSONObject data = json.optJSONObject("data");
                            JSONArray arr = data != null ? (data.optJSONArray("items")!=null?data.optJSONArray("items"):data.optJSONArray("records")) : null;
                            if (arr != null) for (int i=0;i<arr.length();i++) {
                                JSONObject item = arr.optJSONObject(i);
                                if (item != null) {
                                    Song s = new Song(item.optLong("songId",-1), item.optString("songName",""),
                                        item.optString("artistName",""), item.optString("album",""),
                                        formatDuration(item.optString("duration","0")),
                                        item.optString("coverUrl",null), app.streamUrl + item.optLong("songId",-1));
                                    list.add(s);
                                }
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    new Handler(Looper.getMainLooper()).post(() -> {
                        searchSongs.clear(); searchSongs.addAll(list); searchAdapter.updateData(searchSongs);
                        // 点击搜索结果添加到歌单
                        searchAdapter.updateData(searchSongs);
                    });
                }
                @Override public void onError(Throwable ex, boolean b) {}
                @Override public void onCancelled(CancelledException e) {}
                @Override public void onFinished() {}
            });
        });

        // 长按搜索结果删除
        searchAdapter.setLongClickListener(song -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("添加到歌单").setMessage("将「" + song.getName() + "」添加到「" + playlistTitle + "」？")
                .setPositiveButton("添加", (d,w) -> addSongToPlaylist(song.getSongId()))
                .setNegativeButton("取消", null).show();
        });

        dialog.show();
    }

    private void addSongToPlaylist(long songId) {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        RequestParams params = new RequestParams(app.addSongToPlaylistUrl);
        params.addHeader("Authorization", app.getAuthToken());
        params.addBodyParameter("playlistId", String.valueOf(playlistId));
        params.addBodyParameter("songId", String.valueOf(songId));
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String r) { Toast.makeText(getContext(), "添加成功", Toast.LENGTH_SHORT).show(); loadSongs(); }
            @Override public void onError(Throwable ex, boolean b) { Toast.makeText(getContext(), "添加失败", Toast.LENGTH_SHORT).show(); }
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void removeSongFromPlaylist(long songId) {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        RequestParams params = new RequestParams(app.removeSongFromPlaylistUrl);
        params.addHeader("Authorization", app.getAuthToken());
        params.addBodyParameter("playlistId", String.valueOf(playlistId));
        params.addBodyParameter("songId", String.valueOf(songId));
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String r) { Toast.makeText(getContext(), "已移除", Toast.LENGTH_SHORT).show(); loadSongs(); }
            @Override public void onError(Throwable ex, boolean b) { Toast.makeText(getContext(), "移除失败", Toast.LENGTH_SHORT).show(); }
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private String formatDuration(String dur) {
        try { double sec = Double.parseDouble(dur); long ms = (long)(sec*1000); return String.format("%d:%02d",(ms/60000),(ms%60000)/1000); }
        catch (NumberFormatException e) { return dur; }
    }
}

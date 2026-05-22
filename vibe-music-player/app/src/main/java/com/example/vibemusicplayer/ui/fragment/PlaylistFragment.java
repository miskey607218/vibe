package com.example.vibemusicplayer.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibemusicplayer.MyApplication;
import com.example.vibemusicplayer.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment {

    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private List<PlaylistItem> playlists = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        recyclerView = view.findViewById(R.id.playlist_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new PlaylistAdapter(playlists, new OnPlaylistClickListener() {
            @Override public void onClick(PlaylistItem item) { onItemClick(item); }
            @Override public void onLongClick(PlaylistItem item) { onItemLongClick(item); }
        });
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.playlist_create).setOnClickListener(v -> showCreateDialog());
        return view;
    }

    @Override public void onResume() { super.onResume(); loadPlaylists(); }

    private void loadPlaylists() {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        if (!app.isLoggedIn()) return;
        RequestParams params = new RequestParams(app.getAllPlaylistsUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());
        try { JSONObject b = new JSONObject(); b.put("pageNum",1); b.put("pageSize",100); params.setBodyContent(b.toString()); }
        catch (Exception e) { e.printStackTrace(); }
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                List<PlaylistItem> items = new ArrayList<>();
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.optJSONObject("data");
                        JSONArray arr = data != null ? (data.optJSONArray("items") != null ? data.optJSONArray("items") : data.optJSONArray("records")) : null;
                        if (arr != null) for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item != null) items.add(new PlaylistItem(
                                item.optLong("playlistId", -1), item.optString("title", ""),
                                item.optString("coverUrl", item.optString("cover", null))));
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                new Handler(Looper.getMainLooper()).post(() -> { playlists.clear(); playlists.addAll(items); adapter.notifyDataSetChanged(); });
            }
            @Override public void onError(Throwable ex, boolean b) {}
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void onItemClick(PlaylistItem item) {
        Bundle b = new Bundle();
        b.putLong("playlistId", item.playlistId);
        b.putString("title", item.title);
        NavController nav = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        nav.navigate(R.id.nav_playlist_detail, b);
    }

    private void onItemLongClick(PlaylistItem item) {
        new AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setItems(new String[]{"编辑", "删除", "收藏"}, (d, which) -> {
                if (which == 0) showEditDialog(item);
                else if (which == 1) confirmDelete(item);
                else toggleCollect(item);
            }).show();
    }

    private void showCreateDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("歌单标题");
        new AlertDialog.Builder(requireContext()).setTitle("创建歌单").setView(input)
            .setPositiveButton("创建", (d,w) -> {
                String title = input.getText().toString().trim();
                if (TextUtils.isEmpty(title)) { Toast.makeText(getContext(), "标题不能为空", Toast.LENGTH_SHORT).show(); return; }
                createPlaylist(title);
            }).setNegativeButton("取消", null).show();
    }

    private void showEditDialog(PlaylistItem item) {
        EditText input = new EditText(requireContext());
        input.setText(item.title);
        new AlertDialog.Builder(requireContext()).setTitle("编辑歌单").setView(input)
            .setPositiveButton("保存", (d,w) -> {
                String title = input.getText().toString().trim();
                if (TextUtils.isEmpty(title)) { Toast.makeText(getContext(), "标题不能为空", Toast.LENGTH_SHORT).show(); return; }
                updatePlaylist(item.playlistId, title);
            }).setNegativeButton("取消", null).show();
    }

    private void confirmDelete(PlaylistItem item) {
        new AlertDialog.Builder(requireContext()).setTitle("确认删除").setMessage("删除「" + item.title + "」？")
            .setPositiveButton("删除", (d,w) -> deletePlaylist(item.playlistId))
            .setNegativeButton("取消", null).show();
    }

    private void toggleCollect(PlaylistItem item) {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        RequestParams params = new RequestParams(app.collectPlaylistUrl);
        params.addHeader("Authorization", app.getAuthToken());
        params.addBodyParameter("playlistId", String.valueOf(item.playlistId));
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String r) { Toast.makeText(getContext(), "已收藏", Toast.LENGTH_SHORT).show(); }
            @Override public void onError(Throwable ex, boolean b) { Toast.makeText(getContext(), "收藏失败", Toast.LENGTH_SHORT).show(); }
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void createPlaylist(String title) {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        RequestParams params = new RequestParams(app.createPlaylistUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());
        try { params.setBodyContent(new JSONObject().put("title", title).toString()); } catch (Exception e) {}
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String r) {
                Toast.makeText(getContext(), "创建成功", Toast.LENGTH_SHORT).show(); loadPlaylists();
            }
            @Override public void onError(Throwable ex, boolean b) {
                Toast.makeText(getContext(), "创建失败:" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void updatePlaylist(long id, String title) {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        RequestParams params = new RequestParams(app.updatePlaylistUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", app.getAuthToken());
        try { params.setBodyContent(new JSONObject().put("playlistId", id).put("title", title).toString()); } catch (Exception e) {}
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String r) { Toast.makeText(getContext(), "编辑成功", Toast.LENGTH_SHORT).show(); loadPlaylists(); }
            @Override public void onError(Throwable ex, boolean b) { Toast.makeText(getContext(), "编辑失败", Toast.LENGTH_SHORT).show(); }
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    private void deletePlaylist(long id) {
        MyApplication app = (MyApplication) requireActivity().getApplication();
        RequestParams params = new RequestParams(app.deletePlaylistUrl + id);
        params.addHeader("Authorization", app.getAuthToken());
        // xUtils 不支持 delete，使用 POST + method override
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String r) { Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show(); loadPlaylists(); }
            @Override public void onError(Throwable ex, boolean b) { Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show(); }
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }

    public static class PlaylistItem {
        public long playlistId; public String title, coverUrl;
        public PlaylistItem(long id, String t, String c) { playlistId=id; title=t; coverUrl=c; }
    }

    interface OnPlaylistClickListener { void onClick(PlaylistItem item); void onLongClick(PlaylistItem item); }

    static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {
        private List<PlaylistItem> data; private OnPlaylistClickListener listener;
        PlaylistAdapter(List<PlaylistItem> d, OnPlaylistClickListener l) { data=d; listener=l; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            PlaylistItem item = data.get(p);
            h.text1.setText(item.title);
            h.itemView.setOnClickListener(v -> listener.onClick(item));
            h.itemView.setOnLongClickListener(v -> { listener.onLongClick(item); return true; });
        }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder { android.widget.TextView text1; VH(View v) { super(v); text1 = v.findViewById(android.R.id.text1); } }
    }
}

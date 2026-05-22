package com.example.vibemusicplayer.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.vibemusicplayer.MyApplication;
import com.example.vibemusicplayer.R;
import com.example.vibemusicplayer.ui.database.SongDatabaseHelper;
import com.example.vibemusicplayer.ui.model.Song;
import com.example.vibemusicplayer.ui.viewmodel.SongViewModel;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.Random;

public class SongFragment extends Fragment {

    private enum PlayMode {
        LIST_LOOP, SINGLE_LOOP, RANDOM, SEQUENCE
    }

    private SongViewModel mViewModel;
    private android.media.MediaPlayer localMediaPlayer;
    private ExoPlayer exoPlayer;
    private String name, artist, album, duration, albumArt;
    private TextView textName, textArtist, textDuration, textProgress;
    private SeekBar seekBar;
    private ImageView modeToggle, playLast, playToggle, playNext, favoriteToggle;
    private Thread thread;
    private Handler seekHandler = new Handler(Looper.getMainLooper());
    private Runnable seekRunnable;
    private int time;
    private boolean flag = false;
    private ArrayList<Song> data;
    private int position;
    private PlayMode currentPlayMode = PlayMode.LIST_LOOP;
    private Random random = new Random();

    public static SongFragment newInstance() { return new SongFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song, container, false);
        data = (ArrayList<Song>) getArguments().getSerializable("data");
        position = getArguments().getInt("position");
        Song song = data.get(position);

        albumArt = song.getAlbumArtUriString();
        if (albumArt != null && !albumArt.isEmpty()) {
            ImageView img = view.findViewById(R.id.item_song_imageView);
            Glide.with(view.getContext()).load(song.isFromServer() ? albumArt : Uri.parse(albumArt))
                    .apply(RequestOptions.bitmapTransform(new CircleCrop())).error(R.drawable.nav_logo).into(img);
        } else {
            ((ImageView) view.findViewById(R.id.item_song_imageView)).setImageResource(R.drawable.nav_logo);
        }

        name = song.getName(); artist = song.getArtist(); album = song.getAlbum(); duration = song.getDuration();
        textName = view.findViewById(R.id.item_song_name); textArtist = view.findViewById(R.id.item_song_artist);
        textDuration = view.findViewById(R.id.text_view_duration); textProgress = view.findViewById(R.id.text_view_progress);
        textName.setText(name); textArtist.setText(artist); textDuration.setText(duration);
        seekBar = view.findViewById(R.id.seek_bar);
        modeToggle = view.findViewById(R.id.button_play_mode_toggle);
        playLast = view.findViewById(R.id.button_play_last);
        playToggle = view.findViewById(R.id.button_play_toggle);
        playNext = view.findViewById(R.id.button_play_next);
        favoriteToggle = view.findViewById(R.id.button_favorite_toggle);
        favoriteToggle.setImageResource(getSongByName(name) ? R.drawable.ic_favorite_yes : R.drawable.ic_favorite_no);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(SongViewModel.class);
        startPlayback();

        playToggle.setOnClickListener(v -> {
            if (isPlaying()) {
                time = getCurrentPosition();
                pause();
                playToggle.setImageResource(R.drawable.ic_play);
                flag = true;
            } else {
                resume();
                playToggle.setImageResource(R.drawable.ic_pause);
                flag = false;
                startSeekThread();
            }
        });

        playLast.setOnClickListener(v -> navigate(-1));
        playNext.setOnClickListener(v -> navigate(1));

        modeToggle.setOnClickListener(v -> {
            switch (currentPlayMode) {
                case LIST_LOOP: currentPlayMode = PlayMode.SINGLE_LOOP; modeToggle.setImageResource(R.drawable.ic_play_mode_single); Toast.makeText(getContext(), "单曲循环", Toast.LENGTH_SHORT).show(); break;
                case SINGLE_LOOP: currentPlayMode = PlayMode.RANDOM; modeToggle.setImageResource(R.drawable.ic_play_mode_shuffle); Toast.makeText(getContext(), "随机播放", Toast.LENGTH_SHORT).show(); break;
                case RANDOM: currentPlayMode = PlayMode.SEQUENCE; modeToggle.setImageResource(R.drawable.ic_play_mode_list); Toast.makeText(getContext(), "顺序播放", Toast.LENGTH_SHORT).show(); break;
                case SEQUENCE: currentPlayMode = PlayMode.LIST_LOOP; modeToggle.setImageResource(R.drawable.ic_play_mode_loop); Toast.makeText(getContext(), "列表循环", Toast.LENGTH_SHORT).show(); break;
            }
        });

        favoriteToggle.setOnClickListener(v -> {
            if (getSongByName(name)) { removeFromFavorites(name); favoriteToggle.setImageResource(R.drawable.ic_favorite_no); }
            else { insertToFavorites(name, artist, album, duration, albumArt); favoriteToggle.setImageResource(R.drawable.ic_favorite_yes); }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) { seekTo(progress); time = progress; }
                textProgress.setText(millisecondsToString(progress));
                if (isPlayerReady()) textDuration.setText(millisecondsToString(Math.max(0, getDuration() - progress)));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { if (isPlaying()) { time = getCurrentPosition(); pause(); } }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (exoPlayer != null) { exoPlayer.play(); }
                if (localMediaPlayer != null) { localMediaPlayer.start(); }
                playToggle.setImageResource(R.drawable.ic_pause);
                flag = false; startSeekThread();
            }
        });
    }

    private void startPlayback() {
        release();
        // 重置进度条到起点
        seekBar.setProgress(0);
        textProgress.setText("0:00");
        time = 0;
        Song s = data.get(position);
        name = s.getName(); artist = s.getArtist(); album = s.getAlbum(); duration = s.getDuration(); albumArt = s.getAlbumArtUriString();

        if (s.isFromServer() && s.getAudioUrl() != null) {
            // ExoPlayer for remote streaming
            Log.d("SongFragment", "ExoPlayer remote: " + s.getAudioUrl());
            exoPlayer = new ExoPlayer.Builder(requireContext()).build();
            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            ProgressiveMediaSource.Factory sourceFactory = new ProgressiveMediaSource.Factory(dataSourceFactory);
            exoPlayer.setMediaSource(sourceFactory.createMediaSource(MediaItem.fromUri(s.getAudioUrl())));
            exoPlayer.prepare();
            exoPlayer.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        seekBar.setMax((int) exoPlayer.getDuration());
                        exoPlayer.play();
                        flag = false; startSeekThread();
                    }
                }
                @Override public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                    Log.e("SongFragment", "ExoPlayer error: " + error.getErrorCodeName());
                    Toast.makeText(getContext(), "播放失败: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            localMediaPlayer = mViewModel.getLocalMusicByNameAndArtist(requireContext(), name, artist);
            if (localMediaPlayer != null) {
                seekBar.setMax(timeToMilliseconds(duration));
                localMediaPlayer.start();
                flag = false; startSeekThread();
            }
        }
    }

    private void navigate(int direction) {
        switch (currentPlayMode) {
            case LIST_LOOP: position = (position + direction + data.size()) % data.size(); playSongAt(position); break;
            case SEQUENCE:
                int next = position + direction;
                if (next >= 0 && next < data.size()) { position = next; playSongAt(position); }
                else { stop(); playToggle.setImageResource(R.drawable.ic_play); flag = true; }
                break;
            case SINGLE_LOOP: playSongAt(position); break;
            case RANDOM: position = random.nextInt(data.size()); playSongAt(position); break;
        }
    }

    private void playSongAt(int pos) {
        position = pos;
        release();
        seekBar.setProgress(0);
        textProgress.setText("0:00");
        time = 0;
        Song s = data.get(pos);
        name = s.getName(); artist = s.getArtist(); album = s.getAlbum(); duration = s.getDuration(); albumArt = s.getAlbumArtUriString();
        textName.setText(name); textArtist.setText(artist); textDuration.setText(duration);
        favoriteToggle.setImageResource(getSongByName(name) ? R.drawable.ic_favorite_yes : R.drawable.ic_favorite_no);

        if (albumArt != null && !albumArt.isEmpty()) {
            ImageView img = getView().findViewById(R.id.item_song_imageView);
            Glide.with(requireContext()).load(s.isFromServer() ? albumArt : Uri.parse(albumArt))
                    .apply(RequestOptions.bitmapTransform(new CircleCrop())).error(R.drawable.nav_logo).into(img);
        }

        if (s.isFromServer() && s.getAudioUrl() != null) {
            exoPlayer = new ExoPlayer.Builder(requireContext()).build();
            DefaultHttpDataSource.Factory df = new DefaultHttpDataSource.Factory();
            exoPlayer.setMediaSource(new ProgressiveMediaSource.Factory(df).createMediaSource(MediaItem.fromUri(s.getAudioUrl())));
            exoPlayer.prepare();
            exoPlayer.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        seekBar.setMax((int) exoPlayer.getDuration()); exoPlayer.play();
                        flag = false; startSeekThread();
                    }
                    if (state == Player.STATE_ENDED) handleCompletion();
                }
                @Override public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                    Toast.makeText(getContext(), "播放失败: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            localMediaPlayer = mViewModel.getLocalMusicByNameAndArtist(requireContext(), name, artist);
            if (localMediaPlayer != null) {
                seekBar.setMax(timeToMilliseconds(duration));
                localMediaPlayer.start();
                flag = false; startSeekThread();
                localMediaPlayer.setOnCompletionListener(mp -> handleCompletion());
            }
        }
    }

    private void handleCompletion() {
        switch (currentPlayMode) {
            case LIST_LOOP: position = (position + 1) % data.size(); playSongAt(position); break;
            case SINGLE_LOOP: playSongAt(position); break;
            case RANDOM: position = random.nextInt(data.size()); playSongAt(position); break;
            case SEQUENCE:
                if (position < data.size() - 1) { position++; playSongAt(position); }
                else { stop(); playToggle.setImageResource(R.drawable.ic_play); flag = true; }
                break;
        }
    }

    // ---- unified player control ----
    private boolean isPlaying() {
        if (exoPlayer != null) return exoPlayer.isPlaying();
        if (localMediaPlayer != null) return localMediaPlayer.isPlaying();
        return false;
    }
    private boolean isPlayerReady() { return exoPlayer != null || localMediaPlayer != null; }
    private int getCurrentPosition() { return exoPlayer != null ? (int) exoPlayer.getCurrentPosition() : (localMediaPlayer != null ? localMediaPlayer.getCurrentPosition() : 0); }
    private int getDuration() { return exoPlayer != null ? (int) exoPlayer.getDuration() : (localMediaPlayer != null ? localMediaPlayer.getDuration() : 0); }
    private void pause() { if (exoPlayer != null) exoPlayer.pause(); if (localMediaPlayer != null) localMediaPlayer.pause(); }
    private void resume() { if (exoPlayer != null) { exoPlayer.play(); exoPlayer.seekTo(time); } if (localMediaPlayer != null) { localMediaPlayer.start(); localMediaPlayer.seekTo(time); } }
    private void seekTo(int ms) { if (exoPlayer != null) exoPlayer.seekTo(ms); if (localMediaPlayer != null) localMediaPlayer.seekTo(ms); }
    private void stop() { if (exoPlayer != null) { exoPlayer.stop(); exoPlayer.release(); exoPlayer = null; } if (localMediaPlayer != null) { localMediaPlayer.stop(); localMediaPlayer.release(); localMediaPlayer = null; } }
    private void release() {
        flag = true;
        if (seekRunnable != null) seekHandler.removeCallbacks(seekRunnable);
        if (exoPlayer != null) { exoPlayer.stop(); exoPlayer.release(); exoPlayer = null; }
        if (localMediaPlayer != null) { localMediaPlayer.stop(); localMediaPlayer.release(); localMediaPlayer = null; }
    }

    @Override public void onPause() { super.onPause(); release(); }
    @Override public void onDestroy() { super.onDestroy(); }

    private void startSeekThread() {
        flag = false;
        if (seekRunnable != null) seekHandler.removeCallbacks(seekRunnable);
        seekRunnable = new Runnable() {
            @Override public void run() {
                if (flag) return;
                int pos = 0;
                if (exoPlayer != null) pos = (int) exoPlayer.getCurrentPosition();
                else if (localMediaPlayer != null) pos = localMediaPlayer.getCurrentPosition();
                seekBar.setProgress(pos);
                textProgress.setText(millisecondsToString(pos));
                if (pos > 0) textDuration.setText(millisecondsToString(Math.max(0, getDuration() - pos)));
                seekHandler.postDelayed(this, 1000);
            }
        };
        seekHandler.post(seekRunnable);
    }

    private int timeToMilliseconds(String t) { String[] p = t.split(":"); return (Integer.parseInt(p[0]) * 60 + Integer.parseInt(p.length > 1 ? p[1] : "0")) * 1000; }
    private String millisecondsToString(int ms) { return String.format("%02d:%02d", (ms / 60000) % 60, (ms / 1000) % 60); }

    private boolean isFavorited = false;
    @SuppressLint("Range")
    public boolean getSongByName(String name) {
        Song cs = data.get(position);
        MyApplication app = (MyApplication) requireActivity().getApplication();
        if (cs.isFromServer() && app.isLoggedIn()) return isFavorited;
        SongDatabaseHelper h = new SongDatabaseHelper(getContext());
        SQLiteDatabase db = h.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM favorite_song WHERE name = ?", new String[]{name});
        boolean f = c.moveToFirst();
        c.close(); db.close(); isFavorited = f;
        return f;
    }
    public void insertToFavorites(String n, String a, String al, String d, String art) {
        Song cs = data.get(position); MyApplication app = (MyApplication) requireActivity().getApplication();
        if (cs.isFromServer() && app.isLoggedIn()) {
            RequestParams p = new RequestParams(app.collectSongUrl);
            p.addHeader("Authorization", app.getAuthToken());
            p.addHeader("Content-Type", "application/x-www-form-urlencoded");
            p.addBodyParameter("songId", String.valueOf(cs.getSongId()));
            x.http().post(p, new Callback.CommonCallback<String>() {
                @Override public void onSuccess(String r) { isFavorited = true; }
                @Override public void onError(Throwable ex, boolean b) {}
                @Override public void onCancelled(CancelledException e) {}
                @Override public void onFinished() {}
            }); return;
        }
        SongDatabaseHelper h = new SongDatabaseHelper(getContext());
        SQLiteDatabase db = h.getWritableDatabase();
        ContentValues v = new ContentValues(); v.put("name", n); v.put("artist", a); v.put("album", al); v.put("duration", d); v.put("uri", art);
        if (db.insert("favorite_song", null, v) != -1) isFavorited = true;
        db.close();
    }
    public void removeFromFavorites(String n) {
        Song cs = data.get(position); MyApplication app = (MyApplication) requireActivity().getApplication();
        if (cs.isFromServer() && app.isLoggedIn()) {
            RequestParams p = new RequestParams(app.cancelCollectSongUrl);
            p.addHeader("Authorization", app.getAuthToken());
            p.addHeader("Content-Type", "application/x-www-form-urlencoded");
            p.addBodyParameter("songId", String.valueOf(cs.getSongId()));
            x.http().post(p, new Callback.CommonCallback<String>() {
                @Override public void onSuccess(String r) { isFavorited = false; }
                @Override public void onError(Throwable ex, boolean b) {}
                @Override public void onCancelled(CancelledException e) {}
                @Override public void onFinished() {}
            }); return;
        }
        SongDatabaseHelper h = new SongDatabaseHelper(getContext());
        SQLiteDatabase db = h.getWritableDatabase();
        db.delete("favorite_song", "name = ?", new String[]{n});
        isFavorited = false; db.close();
    }
}

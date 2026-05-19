package com.example.vibemusicplayer.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
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

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.Random;

public class SongFragment extends Fragment {

    // 播放模式枚举
    private enum PlayMode {
        LIST_LOOP,    // 列表循环
        SINGLE_LOOP,  // 单曲循环
        RANDOM,       // 随机播放
        SEQUENCE      // 顺序播放
    }

    private SongViewModel mViewModel;
    private MediaPlayer mediaPlayer;
    private String name, artist, album, duration, albumArt;
    private TextView textName, textArtist, textDuration, textProgress;
    private SeekBar seekBar;
    private ImageView modeToggle, playLast, playToggle, playNext, favoriteToggle;

    private Thread thread;
    private int time;   // 记录播放位置
    private boolean flag = false;
    private ArrayList<Song> data;
    private int position;
    private PlayMode currentPlayMode = PlayMode.LIST_LOOP; // 默认列表循环模式
    private Random random = new Random(); // 用于随机播放模式

    public static SongFragment newInstance() {
        return new SongFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song, container, false);

        data = (ArrayList<Song>) getArguments().getSerializable("data");
        position = getArguments().getInt("position");
        Song song = data.get(position);

        String albumArtUriString = song.getAlbumArtUriString();
        albumArt = albumArtUriString != null && !albumArtUriString.isEmpty() ? albumArtUriString : null;
        ImageView songImageView = view.findViewById(R.id.item_song_imageView);

        if (albumArt != null) {
            if (song.isFromServer()) {
                Glide.with(view.getContext())
                        .load(albumArt)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .error(R.drawable.nav_logo)
                        .into(songImageView);
            } else {
                Glide.with(view.getContext())
                        .load(Uri.parse(albumArt))
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .error(R.drawable.nav_logo)
                        .into(songImageView);
            }
        } else {
            songImageView.setImageResource(R.drawable.nav_logo);
        }

        name = song.getName();
        artist = song.getArtist();
        album = song.getAlbum();
        duration = song.getDuration();
        textName = view.findViewById(R.id.item_song_name);
        textArtist = view.findViewById(R.id.item_song_artist);
        textDuration = view.findViewById(R.id.text_view_duration);
        textName.setText(name);
        textArtist.setText(artist);
        textDuration.setText(duration);

        // 获取文本视图、拖动条和图像视图
        textProgress = view.findViewById(R.id.text_view_progress);
        seekBar = view.findViewById(R.id.seek_bar);
        modeToggle = view.findViewById(R.id.button_play_mode_toggle);
        playLast = view.findViewById(R.id.button_play_last);
        playToggle = view.findViewById(R.id.button_play_toggle);
        playNext = view.findViewById(R.id.button_play_next);
        favoriteToggle = view.findViewById(R.id.button_favorite_toggle);

        if (getSongByName(name)) {
            favoriteToggle.setImageResource(R.drawable.ic_favorite_yes);
        } else {
            favoriteToggle.setImageResource(R.drawable.ic_favorite_no);
        }

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(SongViewModel.class);

        Song currentSong = data.get(position);
        if (currentSong.isFromServer() && currentSong.getAudioUrl() != null) {
            // 服务端歌曲：异步准备后自动播放
            mediaPlayer = mViewModel.getRemoteMusic(currentSong.getAudioUrl());
            if (mediaPlayer != null) {
                mediaPlayer.setOnPreparedListener(mp -> {
                    seekBar.setMax(mp.getDuration());
                    mp.start();
                    flag = false;
                    if (thread == null || !thread.isAlive()) {
                        thread = new Thread(new SeekBarThread());
                        thread.start();
                    }
                });
            }
        } else {
            // 本地歌曲
            mediaPlayer = mViewModel.getLocalMusicByNameAndArtist(requireContext(), name, artist);
            if (mediaPlayer != null) {
                Log.d("MediaPlayerInfo", "MediaPlayer 对象已获取：" + mediaPlayer);
                seekBar.setMax(timeToMilliseconds(duration));
                mediaPlayer.start();
                flag = false;
                if (thread == null || !thread.isAlive()) {
                    thread = new Thread(new SeekBarThread());
                    thread.start();
                }
            } else {
                Log.d("MediaPlayerInfo", "未能获取到 MediaPlayer 对象");
            }
        }

        // 设置播放/暂停按钮的点击事件监听器
        playToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        time = mediaPlayer.getCurrentPosition(); // 获取当前播放位置
                        mediaPlayer.pause();
                        playToggle.setImageResource(R.drawable.ic_play); // 设置播放图标
                        // 停止更新进度条的线程
                        flag = true;
                    } else {
                        mediaPlayer.start();
                        mediaPlayer.seekTo(time); // 设置播放位置
                        playToggle.setImageResource(R.drawable.ic_pause); // 设置暂停图标
                        flag = false;
                        if (thread == null || !thread.isAlive()) {
                            thread = new Thread(new SeekBarThread()); // 启动线程
                            thread.start();
                        }
                    }
                }
            }
        });

        // 设置播放上一首按钮的点击事件监听器
        playLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentPlayMode) {
                    case LIST_LOOP:
                    case SEQUENCE:
                        // 列表循环和顺序播放：播放上一首，如果是第一首则播放最后一首
                        if (position > 0) {
                            position--;
                        } else {
                            position = data.size() - 1;
                        }
                        playSongAtPosition(position);
                        break;
                    case SINGLE_LOOP:
                        // 单曲循环：重新播放当前歌曲
                        playSongAtPosition(position);
                        break;
                    case RANDOM:
                        // 随机播放：随机选择一首歌曲
                        position = random.nextInt(data.size());
                        playSongAtPosition(position);
                        break;
                }
            }
        });

        // 设置播放下一首按钮的点击事件监听器
        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentPlayMode) {
                    case LIST_LOOP:
                        // 列表循环：播放下一首，如果是最后一首则播放第一首
                        if (position < data.size() - 1) {
                            position++;
                        } else {
                            position = 0;
                        }
                        playSongAtPosition(position);
                        break;
                    case SINGLE_LOOP:
                        // 单曲循环：重新播放当前歌曲
                        playSongAtPosition(position);
                        break;
                    case RANDOM:
                        // 随机播放：随机选择一首歌曲
                        position = random.nextInt(data.size());
                        playSongAtPosition(position);
                        break;
                    case SEQUENCE:
                        // 顺序播放：播放下一首，如果是最后一首则停止
                        if (position < data.size() - 1) {
                            position++;
                            playSongAtPosition(position);
                        } else {
                            // 已经是最后一首，停止播放
                            mediaPlayer.stop();
                            playToggle.setImageResource(R.drawable.ic_play);
                            flag = true;
                        }
                        break;
                }
            }
        });

        // 设置播放模式切换按钮的点击事件监听器
        modeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentPlayMode) {
                    case LIST_LOOP:
                        currentPlayMode = PlayMode.SINGLE_LOOP;
                        modeToggle.setImageResource(R.drawable.ic_play_mode_single);
                        Toast.makeText(getContext(), "单曲循环", Toast.LENGTH_SHORT).show();
                        break;
                    case SINGLE_LOOP:
                        currentPlayMode = PlayMode.RANDOM;
                        modeToggle.setImageResource(R.drawable.ic_play_mode_shuffle);
                        Toast.makeText(getContext(), "随机播放", Toast.LENGTH_SHORT).show();
                        break;
                    case RANDOM:
                        currentPlayMode = PlayMode.SEQUENCE;
                        modeToggle.setImageResource(R.drawable.ic_play_mode_list);
                        Toast.makeText(getContext(), "顺序播放", Toast.LENGTH_SHORT).show();
                        break;
                    case SEQUENCE:
                        currentPlayMode = PlayMode.LIST_LOOP;
                        modeToggle.setImageResource(R.drawable.ic_play_mode_loop);
                        Toast.makeText(getContext(), "列表循环", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        // 设置收藏/取消收藏按钮的点击事件监听器
        favoriteToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSongByName(name)) {
                    removeFromFavorites(name);
                    favoriteToggle.setImageResource(R.drawable.ic_favorite_no);
                } else {
                    insertToFavorites(name, artist, album, duration, albumArt);
                    favoriteToggle.setImageResource(R.drawable.ic_favorite_yes);
                }
            }
        });

        // 设置拖动条的变化监听器，用于更新音乐播放进度和显示已经播放的时间以及还未播放的时间
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress); // 用户拖动拖动条时更新播放进度
                    playToggle.setImageResource(R.drawable.ic_pause); // 设置暂停图标
                    flag = false; // 重新启动更新进度条的线程
                    if (thread == null || !thread.isAlive()) {
                        thread = new Thread(new SeekBarThread()); // 启动线程
                        thread.start();
                    }
                }
                // 更新已经播放的时间
                textProgress.setText(millisecondsToString(progress));
                // 更新还未播放的时间
                if (mediaPlayer != null) {
                    textDuration.setText(millisecondsToString(mediaPlayer.getDuration() - progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 拖动条开始被用户触摸时触发的方法，这里可以暂停音乐播放
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 拖动条停止被用户触摸时触发的方法，这里可以恢复音乐播放
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            }
        });
    }

    private void playSongAtPosition(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Song song = data.get(position);
        name = song.getName();
        artist = song.getArtist();
        album = song.getAlbum();
        duration = song.getDuration();
        albumArt = song.getAlbumArtUriString();

        textName.setText(name);
        textArtist.setText(artist);
        textDuration.setText(duration);

        if (getSongByName(name)) {
            favoriteToggle.setImageResource(R.drawable.ic_favorite_yes);
        } else {
            favoriteToggle.setImageResource(R.drawable.ic_favorite_no);
        }

        if (albumArt != null) {
            // 使用 Glide 加载图片
            Glide.with(requireContext())
                    .load(Uri.parse(albumArt))
                    .apply(RequestOptions.bitmapTransform(new CircleCrop())) // 应用圆形裁剪
                    .error(R.drawable.nav_logo) // 错误时的占位符
                    .into((ImageView) getView().findViewById(R.id.item_song_imageView)); // 将图片加载到 bannerImageView
        } else {
            // 如果 logo 为 null，则使用默认的占位符图像
            ((ImageView) getView().findViewById(R.id.item_song_imageView)).setImageResource(R.drawable.nav_logo);
        }

        mediaPlayer = mViewModel.getLocalMusicByNameAndArtist(requireContext(), name, artist);
        if (mediaPlayer != null) {
            // 设置播放完成监听器
            final int currentPosition = position; // 保存当前位置
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    switch (currentPlayMode) {
                        case LIST_LOOP:
                            // 列表循环：播放到最后一首后回到第一首
                            if (currentPosition < data.size() - 1) {
                                SongFragment.this.position = currentPosition + 1;
                            } else {
                                SongFragment.this.position = 0;
                            }
                            playSongAtPosition(SongFragment.this.position);
                            break;
                        case SINGLE_LOOP:
                            // 单曲循环：重复播放当前歌曲
                            playSongAtPosition(currentPosition);
                            break;
                        case RANDOM:
                            // 随机播放：随机选择一首歌曲
                            SongFragment.this.position = random.nextInt(data.size());
                            playSongAtPosition(SongFragment.this.position);
                            break;
                        case SEQUENCE:
                            // 顺序播放：播放到最后一首后停止
                            if (currentPosition < data.size() - 1) {
                                SongFragment.this.position = currentPosition + 1;
                                playSongAtPosition(SongFragment.this.position);
                            } else {
                                // 播放到最后一首，停止播放
                                mediaPlayer.stop();
                                playToggle.setImageResource(R.drawable.ic_play);
                                flag = true;
                            }
                            break;
                    }
                }
            });
            
            mediaPlayer.start();
            seekBar.setMax(timeToMilliseconds(duration));
            flag = false;
            if (thread == null || !thread.isAlive()) {
                thread = new Thread(new SeekBarThread());
                thread.start();
            }
        }
    }

    // 将格式化的时间字符串（00:00）转换为毫秒
    private int timeToMilliseconds(String time) {
        // 拆分时间字符串为小时、分钟和秒
        String[] parts = time.split(":");
        int hours = 0, minutes = 0, seconds = 0;
        if (parts.length == 2) {
            minutes = Integer.parseInt(parts[0]);
            seconds = Integer.parseInt(parts[1]);
        } else if (parts.length == 3) {
            hours = Integer.parseInt(parts[0]);
            minutes = Integer.parseInt(parts[1]);
            seconds = Integer.parseInt(parts[2]);
        }

        // 将小时、分钟和秒转换为毫秒数
        return hours * 3600000 + minutes * 60000 + seconds * 1000;
    }

    // 将毫秒转换为格式化的时间字符串（00:00）
    private String millisecondsToString(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 自定义的线程
    class SeekBarThread implements Runnable {
        @Override
        public void run() {
            while (!flag) {
                if (mediaPlayer != null) {
                    try {
                        if (mediaPlayer.isPlaying()) {
                            // 将SeekBar位置设置到当前播放位置
                            int currentPosition = mediaPlayer.getCurrentPosition();
                            seekBar.setProgress(currentPosition);
                            // 更新已经播放的时间
                            requireActivity().runOnUiThread(() -> textProgress.setText(millisecondsToString(currentPosition)));
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        break; // 遇到异常，退出循环
                    }
                }
                try {
                    // 每100毫秒更新一次位置
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break; // 遇到异常，退出循环
                }
            }
        }
    }

    private boolean isFavorited = false;

    // 检查歌曲是否已收藏（服务端优先）
    @SuppressLint("Range")
    public boolean getSongByName(String name) {
        Song currentSong = data.get(position);
        MyApplication app = (MyApplication) requireActivity().getApplication();

        // 服务端歌曲：通过 API 检查
        if (currentSong.isFromServer() && app.isLoggedIn()) {
            return isFavorited;
        }
        // 本地歌曲：查 SQLite
        SongDatabaseHelper dbHelper = new SongDatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM favorite_song WHERE name = ?", new String[]{name});
        boolean found = cursor.moveToFirst();
        cursor.close();
        db.close();
        isFavorited = found;
        return found;
    }

    // 收藏歌曲（服务端优先）
    public void insertToFavorites(String name, String artist, String album, String duration, String albumArt) {
        Song currentSong = data.get(position);
        MyApplication app = (MyApplication) requireActivity().getApplication();

        if (currentSong.isFromServer() && app.isLoggedIn()) {
            RequestParams params = new RequestParams(app.collectSongUrl);
            params.addHeader("Authorization", app.getAuthToken());
            params.addQueryStringParameter("songId", String.valueOf(currentSong.getSongId()));
            x.http().post(params, new Callback.CommonCallback<String>() {
                @Override public void onSuccess(String result) {
                    isFavorited = true;
                    Log.d("SongFragment", "服务器收藏成功");
                }
                @Override public void onError(Throwable ex, boolean isOnCallback) {
                    Log.e("SongFragment", "收藏失败: " + ex.getMessage());
                }
                @Override public void onCancelled(CancelledException cex) {}
                @Override public void onFinished() {}
            });
            return;
        }

        // 本地 SQLite
        SongDatabaseHelper dbHelper = new SongDatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("artist", artist);
        values.put("album", album);
        values.put("duration", duration);
        values.put("uri", albumArt);
        long newRowId = db.insert("favorite_song", null, values);
        if (newRowId != -1) {
            isFavorited = true;
            Log.d("SongFragment", "歌曲已成功收藏");
        }
        db.close();
    }

    // 取消收藏（服务端优先）
    public void removeFromFavorites(String name) {
        Song currentSong = data.get(position);
        MyApplication app = (MyApplication) requireActivity().getApplication();

        if (currentSong.isFromServer() && app.isLoggedIn()) {
            RequestParams params = new RequestParams(app.cancelCollectSongUrl);
            params.addHeader("Authorization", app.getAuthToken());
            params.addQueryStringParameter("songId", String.valueOf(currentSong.getSongId()));
            x.http().post(params, new Callback.CommonCallback<String>() {
                @Override public void onSuccess(String result) {
                    isFavorited = false;
                    Log.d("SongFragment", "服务器取消收藏成功");
                }
                @Override public void onError(Throwable ex, boolean isOnCallback) {
                    Log.e("SongFragment", "取消收藏失败: " + ex.getMessage());
                }
                @Override public void onCancelled(CancelledException cex) {}
                @Override public void onFinished() {}
            });
            return;
        }

        SongDatabaseHelper dbHelper = new SongDatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("favorite_song", "name = ?", new String[]{name});
        isFavorited = false;
        db.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        flag = true; // 停止播放进度更新线程的循环
        try {
            if (thread != null) {
                thread.join(); // 等待播放进度更新线程结束
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    // 销毁 Fragment 时停止播放进度更新线程
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
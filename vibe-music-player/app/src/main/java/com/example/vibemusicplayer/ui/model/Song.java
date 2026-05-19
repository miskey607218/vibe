package com.example.vibemusicplayer.ui.model;

import java.io.Serializable;

public class Song implements Serializable {
    private long songId;       // 服务器歌曲ID
    private String name;
    private String artist;
    private String album;
    private String duration;
    private String albumArtUri; // 封面图片的 URI 或 URL 字符串
    private String audioUrl;    // 服务器音频URL（MinIO）
    private boolean fromServer; // 是否来自服务器

    public Song(String name, String artist, String album, String duration, String albumArtUri) {
        this.songId = -1;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.albumArtUri = albumArtUri;
        this.audioUrl = null;
        this.fromServer = false;
    }

    // 服务器歌曲构造函数
    public Song(long songId, String name, String artist, String album, String duration,
                String coverUrl, String audioUrl) {
        this.songId = songId;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.albumArtUri = coverUrl;
        this.audioUrl = audioUrl;
        this.fromServer = true;
    }

    public long getSongId() { return songId; }
    public void setSongId(long id) { this.songId = id; }

    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getDuration() { return duration; }
    public String getAlbumArtUriString() { return albumArtUri; }
    public void setAlbumArtUri(String uri) { this.albumArtUri = uri; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String url) { this.audioUrl = url; }

    public boolean isFromServer() { return fromServer; }
    public void setFromServer(boolean b) { this.fromServer = b; }
}

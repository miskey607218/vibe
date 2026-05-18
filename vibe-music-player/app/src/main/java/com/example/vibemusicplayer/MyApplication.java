package com.example.vibemusicplayer;

import android.app.Application;

import org.xutils.x;

public class MyApplication extends Application {
    // Spring Boot 后端地址（与PC端共享数据库）
    public String BASE_URL = "http://192.168.1.114:8080/";

    // 认证接口
    public String loginUrl = BASE_URL + "auth/login";

    // 反馈接口
    public String feedbackUrl = BASE_URL + "feedback/addFeedback";

    // 收藏接口
    public String collectSongUrl = BASE_URL + "favorite/collectSong";
    public String cancelCollectSongUrl = BASE_URL + "favorite/cancelCollectSong";
    public String getFavoriteSongsUrl = BASE_URL + "favorite/getFavoriteSongs";

    // 歌曲接口
    public String getAllSongsUrl = BASE_URL + "song/getAllSongs";
    public String getRecommendedSongsUrl = BASE_URL + "song/getRecommendedSongs";

    // 歌单接口
    public String getAllPlaylistsUrl = BASE_URL + "playlist/getAllPlaylists";
    public String getRecommendedPlaylistsUrl = BASE_URL + "playlist/getRecommendedPlaylists";

    // 轮播图
    public String getBannerUrl = BASE_URL + "banner/getBannerList";

    // 当前登录 token
    private String authToken = null;

    @Override
    public void onCreate() {
        super.onCreate();
        x.Ext.init(this);
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public boolean isLoggedIn() {
        return authToken != null && !authToken.isEmpty();
    }

    public void logout() {
        this.authToken = null;
    }
}

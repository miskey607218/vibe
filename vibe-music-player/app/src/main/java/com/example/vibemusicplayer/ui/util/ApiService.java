package com.example.vibemusicplayer.ui.util;

import android.util.Log;

import com.example.vibemusicplayer.MyApplication;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

/**
 * 统一 API 服务层，封装与 Spring Boot 后端的 HTTP 通信
 */
public class ApiService {

    private static final String TAG = "ApiService";

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * 发起 POST JSON 请求
     */
    public static void post(String url, JSONObject body, ApiCallback<JSONObject> callback) {
        RequestParams params = new RequestParams(url);
        params.setAsJsonContent(true);
        params.setBodyContent(body.toString());
        params.addHeader("Content-Type", "application/json");

        addAuthToken(params);

        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    int code = json.optInt("code", -1);
                    if (code == 0) {
                        callback.onSuccess(json);
                    } else {
                        callback.onError(json.optString("message", "请求失败"));
                    }
                } catch (Exception e) {
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                callback.onError("网络错误: " + ex.getMessage());
            }

            @Override
            public void onCancelled(CancelledException cex) {
                callback.onError("请求已取消");
            }

            @Override
            public void onFinished() {
            }
        });
    }

    /**
     * 发起 GET 请求（带查询参数）
     */
    public static void get(String url, ApiCallback<JSONObject> callback) {
        RequestParams params = new RequestParams(url);
        addAuthToken(params);

        x.http().get(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    int code = json.optInt("code", -1);
                    if (code == 0) {
                        callback.onSuccess(json);
                    } else {
                        callback.onError(json.optString("message", "请求失败"));
                    }
                } catch (Exception e) {
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                callback.onError("网络错误: " + ex.getMessage());
            }

            @Override
            public void onCancelled(CancelledException cex) {
                callback.onError("请求已取消");
            }

            @Override
            public void onFinished() {
            }
        });
    }

    /**
     * 发起 POST 请求（带查询参数，用于收藏/取消收藏等）
     */
    public static void postWithParams(String url, JSONObject params, ApiCallback<JSONObject> callback) {
        RequestParams rp = new RequestParams(url);
        addAuthToken(rp);

        // 将参数以 URL 查询参数形式添加
        java.util.Iterator<String> keys = params.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            rp.addQueryStringParameter(key, params.optString(key));
        }

        x.http().post(rp, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    int code = json.optInt("code", -1);
                    if (code == 0) {
                        callback.onSuccess(json);
                    } else {
                        callback.onError(json.optString("message", "请求失败"));
                    }
                } catch (Exception e) {
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                callback.onError("网络错误: " + ex.getMessage());
            }

            @Override
            public void onCancelled(CancelledException cex) {
                callback.onError("请求已取消");
            }

            @Override
            public void onFinished() {
            }
        });
    }

    private static void addAuthToken(RequestParams params) {
        MyApplication app = (MyApplication) x.app();
        if (app.isLoggedIn()) {
            params.addHeader("Authorization", app.getAuthToken());
        }
    }
}

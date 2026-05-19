package com.example.vibemusicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

public class LoginActivity extends AppCompatActivity {

    private EditText accountInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        accountInput = findViewById(R.id.login_account);
        passwordInput = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        statusText = findViewById(R.id.login_status);

        // 默认填入测试账号
        accountInput.setText("admin_1");
        passwordInput.setText("123456abc");

        loginButton.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String account = accountInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(account)) {
            statusText.setText("请输入用户名或邮箱");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            statusText.setText("请输入密码");
            return;
        }

        loginButton.setEnabled(false);
        statusText.setText("登录中...");

        MyApplication app = (MyApplication) getApplication();
        RequestParams params = new RequestParams(app.loginUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");

        try {
            JSONObject body = new JSONObject();
            body.put("account", account);
            body.put("password", password);
            params.setBodyContent(body.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    int code = json.optInt("code", -1);
                    if (code == 0) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            String token = data.optString("token");
                            int userType = data.optInt("userType", 0);

                            app.setAuthToken(token);

                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("userType", userType);
                                startActivity(intent);
                                finish();
                            });
                        }
                    } else {
                        String msg = json.optString("message", "登录失败");
                        runOnUiThread(() -> {
                            statusText.setText(msg);
                            loginButton.setEnabled(true);
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        statusText.setText("解析失败: " + e.getMessage());
                        loginButton.setEnabled(true);
                    });
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                runOnUiThread(() -> {
                    statusText.setText("网络错误: " + ex.getMessage());
                    loginButton.setEnabled(true);
                });
            }

            @Override
            public void onCancelled(CancelledException cex) {
                runOnUiThread(() -> loginButton.setEnabled(true));
            }

            @Override
            public void onFinished() {
            }
        });
    }
}

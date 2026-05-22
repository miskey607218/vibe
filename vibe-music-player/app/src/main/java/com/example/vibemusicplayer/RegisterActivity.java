package com.example.vibemusicplayer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, codeInput, passwordInput;
    private Button sendCodeBtn, registerBtn;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameInput = findViewById(R.id.register_username);
        emailInput = findViewById(R.id.register_email);
        codeInput = findViewById(R.id.register_code);
        passwordInput = findViewById(R.id.register_password);
        sendCodeBtn = findViewById(R.id.register_send_code);
        registerBtn = findViewById(R.id.register_button);
        statusText = findViewById(R.id.register_status);

        sendCodeBtn.setOnClickListener(v -> sendCode());
        registerBtn.setOnClickListener(v -> doRegister());
    }

    private void sendCode() {
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            statusText.setText("请输入邮箱");
            return;
        }
        sendCodeBtn.setEnabled(false);
        MyApplication app = (MyApplication) getApplication();
        String url = app.sendCodeUrl + "?email=" + email;
        x.http().get(new RequestParams(url), new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                    sendCodeBtn.setEnabled(true);
                });
            }
            @Override public void onError(Throwable ex, boolean isOnCallback) {
                runOnUiThread(() -> {
                    statusText.setText("发送失败: " + ex.getMessage());
                    sendCodeBtn.setEnabled(true);
                });
            }
            @Override public void onCancelled(CancelledException cex) {}
            @Override public void onFinished() {}
        });
    }

    private void doRegister() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String code = codeInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(code) || TextUtils.isEmpty(password)) {
            statusText.setText("请填写所有字段");
            return;
        }
        registerBtn.setEnabled(false);
        statusText.setText("注册中...");

        MyApplication app = (MyApplication) getApplication();
        RequestParams params = new RequestParams(app.registerUrl);
        params.setAsJsonContent(true);
        params.addHeader("Content-Type", "application/json");
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("username", username);
            body.put("email", email);
            body.put("password", password);
            body.put("verificationCode", code);
            params.setBodyContent(body.toString());
        } catch (Exception e) { e.printStackTrace(); }

        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            @Override public void onError(Throwable ex, boolean isOnCallback) {
                runOnUiThread(() -> {
                    statusText.setText("注册失败: " + ex.getMessage());
                    registerBtn.setEnabled(true);
                });
            }
            @Override public void onCancelled(CancelledException cex) {}
            @Override public void onFinished() {}
        });
    }
}

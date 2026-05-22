package com.example.vibemusicplayer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameView, emailView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        usernameView = findViewById(R.id.profile_username);
        emailView = findViewById(R.id.profile_email);
        Button changePwd = findViewById(R.id.profile_change_password);
        Button logout = findViewById(R.id.profile_logout);
        Button delete = findViewById(R.id.profile_delete);

        MyApplication app = (MyApplication) getApplication();
        usernameView.setText("用户名: --");
        emailView.setText("邮箱: --");

        // 加载用户信息
        if (app.isLoggedIn()) {
            RequestParams params = new RequestParams(app.getUserInfoUrl);
            params.addHeader("Authorization", app.getAuthToken());
            x.http().get(params, new Callback.CommonCallback<String>() {
                @Override public void onSuccess(String result) {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.optInt("code") == 0) {
                            JSONObject data = json.optJSONObject("data");
                            if (data != null) {
                                String uname = data.optString("username", "");
                                String email = data.optString("email", "");
                                runOnUiThread(() -> {
                                    usernameView.setText("用户名: " + uname);
                                    emailView.setText("邮箱: " + email);
                                });
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onError(Throwable ex, boolean b) {}
                @Override public void onCancelled(CancelledException e) {}
                @Override public void onFinished() {}
            });
        }

        changePwd.setOnClickListener(v -> showPasswordDialog(app));
        logout.setOnClickListener(v -> {
            app.logout();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        delete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("确认注销")
                .setMessage("注销后所有数据将无法恢复，确定要注销吗？")
                .setPositiveButton("确认注销", (d, w) -> deleteAccount(app))
                .setNegativeButton("取消", null)
                .show();
        });
    }

    private void showPasswordDialog(MyApplication app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改密码");
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);
        EditText oldPwd = view.findViewById(R.id.dlg_old_password);
        EditText newPwd = view.findViewById(R.id.dlg_new_password);
        EditText repeatPwd = view.findViewById(R.id.dlg_repeat_password);
        builder.setPositiveButton("确认", null);
        builder.setNegativeButton("取消", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String op = oldPwd.getText().toString().trim();
                String np = newPwd.getText().toString().trim();
                String rp = repeatPwd.getText().toString().trim();
                if (op.isEmpty() || np.isEmpty() || rp.isEmpty()) {
                    Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!np.equals(rp)) {
                    Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                RequestParams params = new RequestParams(app.updatePasswordUrl);
                params.setAsJsonContent(true);
                params.addHeader("Content-Type", "application/json");
                params.addHeader("Authorization", app.getAuthToken());
                try {
                    JSONObject body = new JSONObject();
                    body.put("oldPassword", op);
                    body.put("newPassword", np);
                    body.put("repeatPassword", rp);
                    params.setBodyContent(body.toString());
                } catch (Exception e) { e.printStackTrace(); }
                x.http().post(params, new Callback.CommonCallback<String>() {
                    @Override public void onSuccess(String result) {
                        runOnUiThread(() -> {
                            Toast.makeText(ProfileActivity.this, "密码修改成功，请重新登录", Toast.LENGTH_SHORT).show();
                            app.logout();
                            Intent i = new Intent(ProfileActivity.this, LoginActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i); finish(); dialog.dismiss();
                        });
                    }
                    @Override public void onError(Throwable ex, boolean b) {
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "修改失败:" + ex.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onCancelled(CancelledException e) {}
                    @Override public void onFinished() {}
                });
            });
        });
        dialog.show();
    }

    private void deleteAccount(MyApplication app) {
        RequestParams params = new RequestParams(app.deleteAccountUrl);
        params.addHeader("Authorization", app.getAuthToken());
        x.http().post(params, new Callback.CommonCallback<String>() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "账号已注销", Toast.LENGTH_SHORT).show();
                    app.logout();
                    Intent i = new Intent(ProfileActivity.this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i); finish();
                });
            }
            @Override public void onError(Throwable ex, boolean b) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "注销失败:" + ex.getMessage(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onCancelled(CancelledException e) {}
            @Override public void onFinished() {}
        });
    }
}

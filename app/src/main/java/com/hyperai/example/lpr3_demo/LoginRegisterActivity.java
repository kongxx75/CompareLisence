package com.hyperai.example.lpr3_demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.hyperai.example.lpr3_demo.utils.UserManager;

public class LoginRegisterActivity extends AppCompatActivity {
    private EditText etUser, etPwd, etPwd2;
    private Button btnLogin, btnRegister, btnSwitch;
    private TextView tvTitle;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register);

        etUser = findViewById(R.id.et_user);
        etPwd = findViewById(R.id.et_pwd);
        etPwd2 = findViewById(R.id.et_pwd2);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnSwitch = findViewById(R.id.btn_switch);
        tvTitle = findViewById(R.id.tv_title);

        updateView();

        btnSwitch.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateView();
        });

        btnLogin.setOnClickListener(v -> {
            String u = etUser.getText().toString().trim();
            String p = etPwd.getText().toString();
            if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (UserManager.login(this, u, p)) {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
            }
        });

        btnRegister.setOnClickListener(v -> {
            String u = etUser.getText().toString().trim();
            String p = etPwd.getText().toString();
            String p2 = etPwd2.getText().toString();
            if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p) || TextUtils.isEmpty(p2)) {
                Toast.makeText(this, "请填写所有信息", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!p.equals(p2)) {
                Toast.makeText(this, "两次密码输入不一致", Toast.LENGTH_SHORT).show();
                return;
            }
            if (UserManager.register(this, u, p)) {
                Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                isLoginMode = true;
                updateView();
            } else {
                Toast.makeText(this, "用户名已存在", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateView() {
        if (isLoginMode) {
            tvTitle.setText("登录");
            etPwd2.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.GONE);
            btnSwitch.setText("没有账号？注册");
        } else {
            tvTitle.setText("注册");
            etPwd2.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            btnSwitch.setText("已有账号？登录");
        }
    }
}
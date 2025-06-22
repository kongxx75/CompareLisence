package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.hyperai.example.lpr3_demo.utils.UserManager;
import cn.leancloud.LCUser;

public class MineFragment extends Fragment {

    private Button btnLogin, btnImportDb, btnExportDb, btnSetting;
    private TextView tvLoginStatus;
    private Context mCtx;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mine, container, false);
        mCtx = getContext();

        tvLoginStatus = root.findViewById(R.id.tvLoginStatus);
        btnLogin = root.findViewById(R.id.btnLogin);
        btnImportDb = root.findViewById(R.id.btnImportDb);
        btnExportDb = root.findViewById(R.id.btnExportDb);
        btnSetting = root.findViewById(R.id.btnSetting);

        refreshLoginStatus();

        btnLogin.setOnClickListener(v -> {
            if (UserManager.isLoggedIn()) {
                new AlertDialog.Builder(mCtx)
                        .setTitle("退出登录")
                        .setMessage("确定要退出登录吗？")
                        .setPositiveButton("退出", (dialog, which) -> {
                            UserManager.logout();
                            Toast.makeText(mCtx, "已退出登录", Toast.LENGTH_SHORT).show();
                            refreshLoginStatus();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                startActivity(new Intent(mCtx, LoginRegisterActivity.class));
            }
        });

        btnImportDb.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).importDatabase();
            }
        });
        btnExportDb.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).exportDatabase();
            }
        });
        btnSetting.setOnClickListener(v -> {
            Toast.makeText(mCtx, "敬请期待", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLoginStatus();
    }

    private void refreshLoginStatus() {
        String user = UserManager.getLoginUser();
        if (user != null) {
            tvLoginStatus.setText("已登录用户: " + user);
            btnLogin.setText("退出登录");
        } else {
            tvLoginStatus.setText("未登录");
            btnLogin.setText("登录/注册");
        }
    }
}
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
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;

public class MineFragment extends Fragment {

    private Button btnLogin, btnLocalPlateManager, btnCloudPlateManager, btnSetting;
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
        btnLocalPlateManager = root.findViewById(R.id.btnLocalPlateManager);
        btnCloudPlateManager = root.findViewById(R.id.btnCloudPlateManager);
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
                Intent intent = new Intent(mCtx, LoginRegisterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                if (getActivity() != null) getActivity().overridePendingTransition(0, 0);
            }
        });

        btnLocalPlateManager.setOnClickListener(v -> {
            showLocalPlateManagerDialog();
        });
        btnCloudPlateManager.setOnClickListener(v -> {
            showCloudPlateManagerDialog();
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

    private void backupToCloud() {
        new Thread(() -> {
            PlateDatabase db = PlateDatabase.getInstance(mCtx.getApplicationContext());
            java.util.List<PlateEntity> plates = db.plateDao().getAllPlates();
            requireActivity().runOnUiThread(() -> Toast.makeText(mCtx, "正在上传...", Toast.LENGTH_SHORT).show());
            com.hyperai.example.lpr3_demo.utils.CloudPlateManager.uploadPlateList(mCtx, plates, new com.hyperai.example.lpr3_demo.utils.CloudPlateManager.Callback() {
                @Override
                public void onSuccess() {
                    requireActivity().runOnUiThread(() -> Toast.makeText(mCtx, "上传成功", Toast.LENGTH_SHORT).show());
                }
                @Override
                public void onFailed(String msg) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(mCtx, "上传失败: " + msg, Toast.LENGTH_SHORT).show());
                }
            });
        }).start();
    }

    private void importFromCloud() {
        com.hyperai.example.lpr3_demo.utils.CloudPlateManager.downloadPlateList(mCtx, new com.hyperai.example.lpr3_demo.utils.CloudPlateManager.CallbackWithData() {
            @Override
            public void onSuccess(java.util.List<PlateEntity> plates) {
                new Thread(() -> {
                    PlateDatabase db = PlateDatabase.getInstance(mCtx.getApplicationContext());
                    for (PlateEntity plate : plates) {
                        PlateEntity exist = db.plateDao().findPlateByCode(plate.getPlateCode());
                        if (exist == null) {
                            db.plateDao().insertPlate(plate);
                        }
                    }
                    requireActivity().runOnUiThread(() -> Toast.makeText(mCtx, "导入完成", Toast.LENGTH_SHORT).show());
                }).start();
            }
            @Override
            public void onFailed(String msg) {
                requireActivity().runOnUiThread(() -> Toast.makeText(mCtx, "导入失败: " + msg, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // 自定义本地车牌库管理弹窗
    private void showLocalPlateManagerDialog() {
        final Dialog dialog = new Dialog(mCtx);
        LinearLayout layout = new LinearLayout(mCtx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(Gravity.CENTER);
        String[] options = {"导入本地车牌备份", "备份车牌库到本地"};
        for (int i = 0; i < options.length; i++) {
            TextView tv = new TextView(mCtx);
            tv.setText(options[i]);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(18);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(0, 32, 0, 32);
            tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setBackgroundResource(R.drawable.bg_option_button);
            int index = i;
            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (index == 0) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).importDatabase();
                    }
                } else if (index == 1) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).exportDatabase();
                    }
                }
            });
            layout.addView(tv);
            if (i < options.length - 1) {
                View divider = new View(mCtx);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                params.setMargins(0, 16, 0, 16);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                layout.addView(divider);
            }
        }
        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        dialog.show();
    }

    // 自定义云端车牌库管理弹窗
    private void showCloudPlateManagerDialog() {
        final Dialog dialog = new Dialog(mCtx);
        LinearLayout layout = new LinearLayout(mCtx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(Gravity.CENTER);
        String[] options = {"备份车牌库到云端", "从云端导入车牌库"};
        for (int i = 0; i < options.length; i++) {
            TextView tv = new TextView(mCtx);
            tv.setText(options[i]);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(18);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(0, 32, 0, 32);
            tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setBackgroundResource(R.drawable.bg_option_button);
            int index = i;
            tv.setOnClickListener(v -> {
                dialog.dismiss();
                if (index == 0) {
                    backupToCloud();
                } else if (index == 1) {
                    importFromCloud();
                }
            });
            layout.addView(tv);
            if (i < options.length - 1) {
                View divider = new View(mCtx);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                params.setMargins(0, 16, 0, 16);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                layout.addView(divider);
            }
        }
        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        dialog.show();
    }
}
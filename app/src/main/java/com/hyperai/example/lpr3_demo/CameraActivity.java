package com.hyperai.example.lpr3_demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.Plate;
import com.hyperai.example.lpr3_demo.camera.CameraViewModel;
import com.hyperai.example.lpr3_demo.utils.BitmapUtils;
import com.hyperai.example.lpr3_demo.utils.PermissionUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 实时车牌识别，自动与本地库比对，显示录入时间和备注
 * 新增功能：
 * - 保存按钮可保存图片当前帧及车牌信息
 * - 识别命中本地库时震动提示
 * - 所有数据库/摄像头try-catch防止闪退
 */
public class CameraActivity extends AppCompatActivity {

    FrameLayout previewFl;
    CameraPreviews cameraPreview;
    TextView plateTv;
    ImageView image;
    TextView matchTipTv;
    FloatingActionButton fabSavePlate;

    // 用于保存最新识别到的车牌
    private Plate lastRecognizedPlate;
    // 用于保存最近一帧的Bitmap
    private Bitmap lastFrameBitmap;

    private CameraViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        matchTipTv = findViewById(R.id.match_tip_tv);
        fabSavePlate = findViewById(R.id.fabSavePlate);

        // 权限申请
        PermissionUtils.checkAndRequestPermissions(this);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        viewModel.getLastFrameBitmap().observe(this, bitmap -> lastFrameBitmap = bitmap);

        // 保存按钮点击事件
        fabSavePlate.setOnClickListener(v -> {
            if (lastRecognizedPlate == null) {
                Toast.makeText(this, "暂无可保存的车牌", Toast.LENGTH_SHORT).show();
                return;
            }
            showSavePlateDialog(lastRecognizedPlate, lastFrameBitmap);
        });
    }

    private void initCamera() {
        previewFl = findViewById(R.id.preview_fl);
        plateTv = findViewById(R.id.plate_tv);
        image = findViewById(R.id.image);
        cameraPreview = new CameraPreviews(this, viewModel); // 传递viewModel获取帧
        previewFl.addView(cameraPreview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraPreview == null) {
            initCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraPreview = null;
    }

    private void stopPreview() {
        previewFl.removeAllViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 提取纯车牌号，比如【蓝牌】京A66666 -> 京A66666
     */
    private String extractPlateCode(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("^[\\[【][^\\]】]+[\\]】]", "").trim();
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(Plate[] plates) {
        StringBuilder showText = new StringBuilder();
        for (Plate plate : plates) {
            String type = "未知车牌";
            if (plate.getType() != HyperLPR3.PLATE_TYPE_UNKNOWN) {
                type = HyperLPR3.PLATE_TYPE_MAPS[plate.getType()];
            }
            String pStr = "[" + type + "]" + plate.getCode() + "\n";
            showText.append(pStr);
        }
        plateTv.setText(showText);

        if (plates.length > 0) {
            Plate plate = plates[0];
            lastRecognizedPlate = plate;
            String type = "未知车牌";
            if (plate.getType() != HyperLPR3.PLATE_TYPE_UNKNOWN) {
                type = HyperLPR3.PLATE_TYPE_MAPS[plate.getType()];
            }
            String codeWithType = "[" + type + "]" + plate.getCode();
            String pureCode = extractPlateCode(codeWithType);
            checkPlateWithLocalDb(pureCode);
        } else {
            lastRecognizedPlate = null;
            matchTipTv.setText("");
        }
    }

    /**
     * 比对数据库，命中时震动
     */
    private void checkPlateWithLocalDb(String recognizedCode) {
        new Thread(() -> {
            PlateDao dao = PlateDatabase.getInstance(getApplicationContext()).plateDao();
            PlateEntity entity = null;
            try {
                entity = dao.findPlateByCode(recognizedCode);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "数据库异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                return;
            }
            PlateEntity finalEntity = entity;
            runOnUiThread(() -> {
                if (finalEntity != null) {
                    String formattedTime;
                    try {
                        long millis = Long.parseLong(finalEntity.getTimestamp());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        formattedTime = sdf.format(new Date(millis));
                    } catch (Exception e) {
                        formattedTime = finalEntity.getTimestamp();
                    }
                    matchTipTv.setText("车牌吻合\n车牌号：" + finalEntity.getPlateCode()
                            + "\n录入时间：" + formattedTime
                            + "\n备注：" + finalEntity.getPlateType());
                    matchTipTv.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    vibrateOnce(this);
                } else {
                    matchTipTv.setText("未在本地库中找到该车牌");
                    matchTipTv.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            });
        }).start();
    }

    /**
     * 震动一次
     */
    private void vibrateOnce(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(200);
            }
        } catch (Exception e) {
            // 忽略震动异常
        }
    }

    /**
     * 保存车牌弹窗，支持备注和显示时间，并保存当前帧图片
     */
    private void showSavePlateDialog(Plate plate, Bitmap frameBitmap) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_plate, null);
        EditText etRemark = dialogView.findViewById(R.id.etRemark);
        EditText etTime = dialogView.findViewById(R.id.etTime);

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        etTime.setText(now);
        etTime.setEnabled(false);

        new AlertDialog.Builder(this)
                .setTitle("保存车牌")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String remark = etRemark.getText().toString().trim();
                    String time = String.valueOf(System.currentTimeMillis());
                    String imagePath = "";

                    // 保存当前帧
                    if (frameBitmap != null) {
                        try {
                            File file = BitmapUtils.saveBitmapToAppDir(this, frameBitmap);
                            imagePath = file != null ? file.getAbsolutePath() : "";
                        } catch (Exception e) {
                            Toast.makeText(this, "图片保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    PlateEntity entity = new PlateEntity();
                    entity.setPlateCode(plate.getCode());
                    entity.setPlateType(remark);
                    entity.setTimestamp(time);
                    entity.setImagePath(imagePath);

                    new Thread(() -> {
                        try {
                            PlateDatabase.getInstance(getApplicationContext()).plateDao().insertPlate(entity);
                            runOnUiThread(() -> Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show());
                        } catch (Exception ex) {
                            runOnUiThread(() -> Toast.makeText(this, "数据库写入异常: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .show();
    }
}
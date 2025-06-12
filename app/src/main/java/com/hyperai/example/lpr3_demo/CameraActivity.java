package com.hyperai.example.lpr3_demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.Plate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 实时车牌识别，自动与本地库比对，显示录入时间和备注
 */
public class CameraActivity extends Activity {

    FrameLayout previewFl;
    CameraPreviews cameraPreview;
    TextView plateTv;
    ImageView image;
    TextView matchTipTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        matchTipTv = findViewById(R.id.match_tip_tv);
    }

    private void initCamera() {
        previewFl = findViewById(R.id.preview_fl);
        plateTv = findViewById(R.id.plate_tv);
        image = findViewById(R.id.image);
        cameraPreview = new CameraPreviews(this);
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
        String showText = "";
        for (Plate plate : plates) {
            String type = "未知车牌";
            if (plate.getType() != HyperLPR3.PLATE_TYPE_UNKNOWN) {
                type = HyperLPR3.PLATE_TYPE_MAPS[plate.getType()];
            }
            String pStr = "[" + type + "]" + plate.getCode() + "\n";
            showText += pStr;
        }
        plateTv.setText(showText);

        // 只匹配第一个车牌
        if (plates.length > 0) {
            Plate plate = plates[0];
            String type = "未知车牌";
            if (plate.getType() != HyperLPR3.PLATE_TYPE_UNKNOWN) {
                type = HyperLPR3.PLATE_TYPE_MAPS[plate.getType()];
            }
            String codeWithType = "[" + type + "]" + plate.getCode();
            String pureCode = extractPlateCode(codeWithType);
            checkPlateWithLocalDb(pureCode);
        } else {
            matchTipTv.setText("");
        }
    }

    /**
     * 比对数据库
     */
    private void checkPlateWithLocalDb(String recognizedCode) {
        new Thread(() -> {
            PlateDao dao = PlateDatabase.getInstance(getApplicationContext()).plateDao();
            PlateEntity entity = dao.findPlateByCode(recognizedCode);
            runOnUiThread(() -> {
                if (entity != null) {
                    // 格式化时间
                    String formattedTime;
                    try {
                        long millis = Long.parseLong(entity.getTimestamp());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        formattedTime = sdf.format(new Date(millis));
                    } catch (Exception e) {
                        formattedTime = entity.getTimestamp();
                    }
                    matchTipTv.setText("车牌吻合\n车牌号：" + entity.getPlateCode()
                            + "\n录入时间：" + formattedTime
                            + "\n备注：" + entity.getPlateType());
                    matchTipTv.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    matchTipTv.setText("未在本地库中找到该车牌");
                    matchTipTv.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            });
        }).start();
    }
}
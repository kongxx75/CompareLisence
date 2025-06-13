package com.hyperai.example.lpr3_demo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.HyperLPRParameter;
import com.hyperai.hyperlpr3.bean.Plate;
import com.hyperai.example.lpr3_demo.utils.PermissionUtils;

public class MainActivity extends AppCompatActivity {

    private Button cameraBtn;
    private Button albumBtn;
    private Button databaseBtn;
    private Button exportBtn;
    private Button importBtn;
    private Context mCtx;
    private static final int REQUEST_CAMERA_CODE = 1;
    private final String TAG = "HyperLPR-App";
    private ImageView imageView;
    private TextView mResult;

    private ActivityResultLauncher<Intent> pickPhotoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCtx = this;
        cameraBtn = findViewById(R.id.cameraBtn);
        albumBtn = findViewById(R.id.albumBtn);
        imageView = findViewById(R.id.imageView);
        mResult = findViewById(R.id.mResult);
        databaseBtn = findViewById(R.id.databaseBtn);

        exportBtn = findViewById(R.id.exportBtn);    // 新增
        importBtn = findViewById(R.id.importBtn);    // 新增

        PermissionUtils.checkAndRequestPermissions(this);

        // 初始化车牌识别算法参数
        HyperLPRParameter parameter = new HyperLPRParameter()
                .setDetLevel(HyperLPR3.DETECT_LEVEL_LOW)
                .setMaxNum(1)
                .setRecConfidenceThreshold(0.85f);
        HyperLPR3.getInstance().init(this, parameter);

        // 注册图片选择回调
        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            if (bitmap != null) {
                                processAndSavePlate(bitmap, uri.toString());
                            } else {
                                Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        cameraBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            MainActivity.this.startActivity(intent);
        });

        albumBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickPhotoLauncher.launch(intent);
        });

        databaseBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, PlateListActivity.class);
            MainActivity.this.startActivity(intent);
        });

        exportBtn.setOnClickListener(v -> com.hyperai.example.lpr3_demo.utils.FileUtils.exportDatabase(this));
        importBtn.setOnClickListener(v -> com.hyperai.example.lpr3_demo.utils.FileUtils.importDatabase(this));
    }

    private void processAndSavePlate(Bitmap bitmap, String imagePath) {
        imageView.setImageBitmap(bitmap);
        StringBuilder showText = new StringBuilder();
        Plate[] plates = HyperLPR3.getInstance().plateRecognition(bitmap, HyperLPR3.CAMERA_ROTATION_0, HyperLPR3.STREAM_BGRA);
        for (Plate plate : plates) {
            String type = "未知车牌";
            if (plate.getType() != HyperLPR3.PLATE_TYPE_UNKNOWN) {
                type = HyperLPR3.PLATE_TYPE_MAPS[plate.getType()];
            }
            String pStr = "[" + type + "]" + plate.getCode() + "\n";
            showText.append(pStr);
            saveToDatabase(plate, type, imagePath);
        }
        mResult.setText(showText.toString());
    }

    private void saveToDatabase(Plate plate, String plateType, String imagePath) {
        PlateEntity entity = new PlateEntity();
        entity.setPlateCode(plate.getCode());
        entity.setPlateType(plateType);
        entity.setTimestamp(String.valueOf(System.currentTimeMillis()));
        entity.setImagePath(imagePath);

        new Thread(() -> {
            try {
                PlateDatabase.getInstance(getApplicationContext()).plateDao().insertPlate(entity);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "写入数据库异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
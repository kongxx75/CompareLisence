package com.hyperai.example.lpr3_demo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.HyperLPRParameter;
import com.hyperai.hyperlpr3.bean.Plate;
import com.hyperai.example.lpr3_demo.utils.PermissionUtils;
import com.hyperai.example.lpr3_demo.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private Button cameraBtn;
    private Button albumBtn;
    private Button databaseBtn;
    private Button exportBtn;
    private Button importBtn;
    private Context mCtx;
    private static final int REQUEST_CAMERA_CODE = 1;
    private ImageView imageView;
    private TextView mResult;

    private static final int REQ_EXPORT_DB = 1001;
    private static final int REQ_IMPORT_DB = 1002;

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
        exportBtn = findViewById(R.id.exportBtn);
        importBtn = findViewById(R.id.importBtn);

        PermissionUtils.checkAndRequestPermissions(this);

        // 初始化参数
        HyperLPRParameter parameter = new HyperLPRParameter()
                .setDetLevel(HyperLPR3.DETECT_LEVEL_LOW)
                .setMaxNum(1)
                .setRecConfidenceThreshold(0.85f);
        HyperLPR3.getInstance().init(this, parameter);

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

        exportBtn.setOnClickListener(v -> exportDatabase());
        importBtn.setOnClickListener(v -> importDatabase());
    }

    // 识别车牌并保存到数据库
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

    // 保存车牌信息到数据库
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

    /*** ========== 导出数据库 ========== ***/
    private void exportDatabase() {
        // 1. 保证所有数据同步到磁盘
        runOnUiThread(() -> Toast.makeText(this, "准备导出，请稍候...", Toast.LENGTH_SHORT).show());
        try {
            PlateDatabase db = PlateDatabase.getInstance(getApplicationContext());
            // 强制WAL合并，确保所有数据写入主库
            db.getOpenHelper().getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)");
            db.close(); // 关闭数据库连接，释放文件锁
        } catch (Exception e) {
            // 忽略
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "plate_database");
        startActivityForResult(intent, REQ_EXPORT_DB);
    }

    /*** ========== 导入数据库 ========== ***/
    private void importDatabase() {
        // 1. 关闭数据库连接，释放文件锁
        try {
            PlateDatabase db = PlateDatabase.getInstance(getApplicationContext());
            db.close();
        } catch (Exception e) {
            // 忽略
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQ_IMPORT_DB);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        if (requestCode == REQ_EXPORT_DB) {
            try {
                File dbFile = getDatabasePath("plate_database");
                if (!dbFile.exists() || dbFile.length() == 0) {
                    Toast.makeText(this, "数据库文件不存在或为空，无法导出", Toast.LENGTH_LONG).show();
                    return;
                }
                InputStream in = new FileInputStream(dbFile);
                OutputStream out = getContentResolver().openOutputStream(data.getData());
                FileUtils.copyStream(in, out);
                // 验证导出文件
                if (validateDatabaseFile(dbFile.getAbsolutePath())) {
                    Toast.makeText(this, "导出成功: " + dbFile.length() + " 字节", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "警告：导出的数据库文件可能不完整", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQ_IMPORT_DB) {
            try {
                // 复制文件
                InputStream in = getContentResolver().openInputStream(data.getData());
                File dbFile = getDatabasePath("plate_database");
                OutputStream out = new FileOutputStream(dbFile, false);
                FileUtils.copyStream(in, out);

                // 验证导入文件
                if (!validateDatabaseFile(dbFile.getAbsolutePath())) {
                    Toast.makeText(this, "导入的数据库文件无效或不兼容", Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(this, "导入成功，加载中..", Toast.LENGTH_LONG).show();
                // 杀进程，防止Room使用旧缓存
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception e) {
                Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 验证数据库文件是否合法（表存在、版本兼容等）
     */
    private boolean validateDatabaseFile(String path) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
            // 检查表是否存在
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='plates'", null);
            if (cursor == null || !cursor.moveToFirst()) return false;
            // 检查至少有一条数据（可选）
            cursor = db.rawQuery("SELECT count(*) FROM plates", null);
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                if (count >= 0) return true;
            }
            // 检查数据库版本
            int version = db.getVersion();
            if (version != 1) return false; // 你的Room版本为1
            return true;
        } catch (SQLiteException e) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }
}
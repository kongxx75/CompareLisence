package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hyperai.example.lpr3_demo.utils.PermissionUtils;
import com.hyperai.example.lpr3_demo.utils.FileUtils;
import com.hyperai.example.lpr3_demo.utils.UserManager;
import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.HyperLPRParameter;
import com.hyperai.hyperlpr3.bean.Plate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_EXPORT_DB = 1001;
    private static final int REQ_IMPORT_DB = 1002;
    private BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionUtils.checkAndRequestPermissions(this);

        navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_plate_list) {
                fragment = new PlateListFragment();
            } else if (itemId == R.id.navigation_recognition) {
                // 启动实时识别
                startActivity(new android.content.Intent(this, CameraActivity.class));
                return true;
            } else if (itemId == R.id.navigation_message) {
                fragment = new MessageFragment();
            } else if (itemId == R.id.navigation_user) {
                fragment = new MineFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
                return true;
            }
            return false;
        });

        // 默认显示车牌库页面
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.container, new PlateListFragment())
            .commit();

        // 初始化HyperLPR参数（如有必要）
        HyperLPRParameter parameter = new HyperLPRParameter()
                .setDetLevel(HyperLPR3.DETECT_LEVEL_LOW)
                .setMaxNum(1)
                .setRecConfidenceThreshold(0.85f);
        HyperLPR3.getInstance().init(this, parameter);
    }

    // 保证为public, 供MineFragment调用
    public void exportDatabase() {
        runOnUiThread(() -> Toast.makeText(this, "准备导出，请稍候...", Toast.LENGTH_SHORT).show());
        try {
            PlateDatabase db = PlateDatabase.getInstance(getApplicationContext());
            db.getOpenHelper().getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)");
            db.close();
        } catch (Exception e) {
            // 忽略
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "plate_database");
        startActivityForResult(intent, REQ_EXPORT_DB);
    }

    public void importDatabase() {
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
                InputStream in = getContentResolver().openInputStream(data.getData());
                File dbFile = getDatabasePath("plate_database");
                OutputStream out = new FileOutputStream(dbFile, false);
                FileUtils.copyStream(in, out);
                if (!validateDatabaseFile(dbFile.getAbsolutePath())) {
                    Toast.makeText(this, "导入的数据库文件无效或不兼容", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(this, "导入成功，加载中..", Toast.LENGTH_LONG).show();
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception e) {
                Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean validateDatabaseFile(String path) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='plates'", null);
            if (cursor == null || !cursor.moveToFirst()) return false;
            cursor = db.rawQuery("SELECT count(*) FROM plates", null);
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                if (count >= 0) return true;
            }
            int version = db.getVersion();
            if (version != 1) return false;
            return true;
        } catch (SQLiteException e) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }
}
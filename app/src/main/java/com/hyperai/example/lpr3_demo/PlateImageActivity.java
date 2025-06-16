package com.hyperai.example.lpr3_demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.github.chrisbanes.photoview.PhotoView;
import java.io.File;

public class PlateImageActivity extends AppCompatActivity {
    private String plateCode;
    private String imagePath;
    private PhotoView photoView;
    private TextView textView;

    public static void start(Context context, String plateCode, String imagePath) {
        Intent intent = new Intent(context, PlateImageActivity.class);
        intent.putExtra("plate_code", plateCode);
        intent.putExtra("image_path", imagePath);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_image);

        photoView = findViewById(R.id.photoView);
        textView = findViewById(R.id.textView);

        plateCode = getIntent().getStringExtra("plate_code");
        imagePath = getIntent().getStringExtra("image_path");

        setTitle("车牌图片：" + plateCode);

        if (imagePath != null && !imagePath.isEmpty()) {
            File file = new File(imagePath);
            if (file.exists()) {
                photoView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                textView.setText("");
                // 点击图片用系统相册查看大图
                photoView.setOnClickListener(v -> openImageWithGallery(file));
                // 长按图片弹出删除
                photoView.setOnLongClickListener(v -> { showDeleteImageDialog(); return true; });
            } else {
                textView.setText("未找到相关图片");
                photoView.setImageResource(R.drawable.ic_image_not_found);
            }
        } else {
            textView.setText("未找到相关图片");
            photoView.setImageResource(R.drawable.ic_image_not_found);
        }
    }

    private void openImageWithGallery(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri, "image/*");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法用系统相册打开图片", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteImageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除图片")
                .setMessage("确定要删除该图片并解除与车牌关联吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteImage())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteImage() {
        if (imagePath == null || imagePath.isEmpty()) {
            Toast.makeText(this, "没有图片可删除", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(imagePath);
        boolean existed = file.exists();
        boolean deleted = false;
        if (existed) {
            deleted = file.delete();
            // 通知图库刷新
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }
        final boolean finalDeleted = deleted;
        new Thread(() -> {
            PlateDao dao = PlateDatabase.getInstance(getApplicationContext()).plateDao();
            PlateEntity entity = dao.findPlateByCode(plateCode);
            if (entity != null) {
                entity.setImagePath("");
                dao.updatePlate(entity);
            }
            runOnUiThread(() -> {
                if (finalDeleted) {
                    Toast.makeText(this, "图片已删除", Toast.LENGTH_SHORT).show();
                } else if (existed) {
                    Toast.makeText(this, "图片文件删除失败", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show();
                }
                finish(); // 返回车牌库
            });
        }).start();
    }
}
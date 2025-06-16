package com.hyperai.example.lpr3_demo;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class PlateImageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_image);

        ImageView imageView = findViewById(R.id.imageView);
        TextView textView = findViewById(R.id.textView);

        Intent intent = getIntent();
        String plateCode = intent.getStringExtra("plate_code");
        String imagePath = intent.getStringExtra("image_path");

        setTitle("车牌图片：" + plateCode);

        if (imagePath != null && !imagePath.isEmpty()) {
            File file = new File(imagePath);
            if (file.exists()) {
                imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                textView.setText("");
            } else {
                textView.setText("未找到相关图片");
            }
        } else {
            textView.setText("未找到相关图片");
        }
    }
}
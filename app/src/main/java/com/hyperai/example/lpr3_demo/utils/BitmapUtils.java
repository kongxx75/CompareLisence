package com.hyperai.example.lpr3_demo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class BitmapUtils {
    /**
     * NV21 byte[] 转 Bitmap
     */
    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height, int rotation) {
        try {
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 100, out);
            byte[] bytes = out.toByteArray();
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (rotation != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 保存Bitmap到应用内部目录
     */
    public static File saveBitmapToAppDir(Context ctx, Bitmap bitmap) throws Exception {
        File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "plate_frames");
        if (!dir.exists()) dir.mkdirs();
        String fileName = "frame_" + System.currentTimeMillis() + ".jpg";
        File file = new File(dir, fileName);
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();
        return file;
    }
}
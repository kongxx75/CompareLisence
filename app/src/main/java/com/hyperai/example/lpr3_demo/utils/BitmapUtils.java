package com.hyperai.example.lpr3_demo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    /**
     * 将NV21格式的数据转换为Bitmap，支持旋转
     */
    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height, int rotation) {
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        // 旋转
        if (rotation != 0 && bitmap != null) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    /**
     * 保存图片到自定义文件夹(如 /Pictures/PlateFrames/ )，并通知图库刷新
     * @param context 上下文
     * @param bitmap  要保存的图片
     * @return 保存成功的图片File
     * @throws IOException 写入异常
     */
    public static File saveBitmapToPlateDir(Context context, Bitmap bitmap) throws IOException {
        String dirName = "PlateFrames"; // 专用文件夹名
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File plateDir = new File(picturesDir, dirName);

        if (!plateDir.exists()) {
            plateDir.mkdirs();
        }

        String fileName = "plate_" + System.currentTimeMillis() + ".jpg";
        File file = new File(plateDir, fileName);
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();

        // 通知图库刷新
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);

        return file;
    }
}
package com.hyperai.example.lpr3_demo.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.*;

public class FileUtils {

    /** 一键导出数据库到 /Download/plate_database.db */
    public static void exportDatabase(Activity activity) {
        try {
            File dbFile = activity.getDatabasePath("plate_database");
            File destFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "plate_database.db");
            copyFile(dbFile, destFile);
            Toast.makeText(activity, "数据库导出至: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(activity, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 一键导入/覆盖数据库，默认从 /Download/plate_database.db 导入 */
    public static void importDatabase(Activity activity) {
        try {
            File destFile = activity.getDatabasePath("plate_database");
            File srcFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "plate_database.db");
            copyFile(srcFile, destFile);
            Toast.makeText(activity, "数据库已导入，重启App生效", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(activity, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        if (!src.exists()) throw new IOException("源文件不存在: " + src.getAbsolutePath());
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst, false);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
package com.hyperai.example.lpr3_demo.utils;

import java.io.*;

public class FileUtils {
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
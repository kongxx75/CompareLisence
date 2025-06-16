package com.hyperai.example.lpr3_demo.utils;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 微信推送工具类，调用 WxPusher API 发送文本消息
 */
public class WxPusherUtils {
    private static final String API_URL = "https://wxpusher.zjiecode.com/api/send/message";
    // 替换为你的AppToken和UID
    private static final String APP_TOKEN = "AT_UbTJdJ22BTm7NmP7BUixpHZudxsnXF4L";
    private static final String UID = "UID_gc5TomaeytEH5l6IcbKqqemlbeWB";

    /**
     * 发送文本消息到 WxPusher
     * @param content 推送内容
     * @param callback OkHttp 回调，处理结果
     */
    public static void sendMessage(String content, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        String json = "{"
                + "\"appToken\":\"" + APP_TOKEN + "\","
                + "\"content\":\"" + content + "\","
                + "\"summary\":\"车牌识别提醒\","
                + "\"contentType\":1,"
                + "\"uids\":[\"" + UID + "\"]"
                + "}";

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder().url(API_URL).post(body).build();
        client.newCall(request).enqueue(callback);
    }
}
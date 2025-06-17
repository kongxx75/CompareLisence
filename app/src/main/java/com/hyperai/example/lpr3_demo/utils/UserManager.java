package com.hyperai.example.lpr3_demo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {
    private static final String PREF_NAME = "users";
    private static final String KEY_LOGIN = "current_login";

    public static boolean register(Context ctx, String username, String password) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (sp.contains(username)) return false; // 已存在
        sp.edit().putString(username, password).apply();
        return true;
    }

    public static boolean login(Context ctx, String username, String password) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String pwd = sp.getString(username, null);
        if (pwd != null && pwd.equals(password)) {
            sp.edit().putString(KEY_LOGIN, username).apply();
            return true;
        }
        return false;
    }

    public static void logout(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_LOGIN).apply();
    }

    public static String getLoginUser(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_LOGIN, null);
    }

    public static boolean isLoggedIn(Context ctx) {
        return getLoginUser(ctx) != null;
    }
}
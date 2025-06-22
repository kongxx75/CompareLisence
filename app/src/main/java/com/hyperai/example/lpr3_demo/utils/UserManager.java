package com.hyperai.example.lpr3_demo.utils;

import cn.leancloud.LCUser;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import android.content.Context;

public class UserManager {
    // 注册（异步，回调方式）
    public static void register(String username, String password, Observer<LCUser> callback) {
        LCUser user = new LCUser();
        user.setUsername(username);
        user.setPassword(password);
        user.signUpInBackground()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(callback);
    }

    // 登录（异步，回调方式）
    public static void login(String username, String password, Observer<LCUser> callback) {
        LCUser.logIn(username, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(callback);
    }

    // 登出
    public static void logout() {
        LCUser.logOut();
    }

    // 获取当前登录用户的用户名
    public static String getLoginUser() {
        LCUser user = LCUser.getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    // 是否已登录
    public static boolean isLoggedIn() {
        return LCUser.getCurrentUser() != null;
    }
}
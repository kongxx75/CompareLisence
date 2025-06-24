package com.hyperai.example.lpr3_demo.utils;

import cn.leancloud.LCUser;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import android.content.Context;
import cn.leancloud.LCQuery;
import cn.leancloud.LCObject;
import java.util.List;

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

    // 根据用户名或昵称查找用户
    public static void searchUser(String keyword, io.reactivex.Observer<List<LCUser>> callback) {
        LCQuery<LCUser> query = new LCQuery<>("_User");
        query.whereContains("username", keyword);
        query.limit(10);
        query.findInBackground()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(callback);
    }

    // 添加好友（自定义 Friend 表，记录双方关系）
    public static void addFriend(LCUser targetUser, io.reactivex.Observer<LCObject> callback) {
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser == null) return;
        LCObject friend = new LCObject("Friend");
        friend.put("user", currentUser);
        friend.put("friend", targetUser);
        friend.saveInBackground()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(callback);
    }
}
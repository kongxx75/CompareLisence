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

    // 判断是否已是好友
    public static void isFriend(LCUser targetUser, io.reactivex.Observer<Boolean> callback) {
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser == null) {
            callback.onNext(false);
            callback.onComplete();
            return;
        }
        LCQuery<LCObject> query = new LCQuery<>("Friend");
        query.whereEqualTo("user", currentUser);
        query.whereEqualTo("friend", targetUser);
        query.countInBackground()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new io.reactivex.Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) { callback.onSubscribe(d); }
                @Override
                public void onNext(Integer count) { callback.onNext(count > 0); }
                @Override
                public void onError(Throwable e) { callback.onError(e); }
                @Override
                public void onComplete() { callback.onComplete(); }
            });
    }

    // 添加好友（不能重复添加）
    public static void addFriend(LCUser targetUser, io.reactivex.Observer<LCObject> callback) {
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser == null) return;
        isFriend(targetUser, new io.reactivex.Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) { callback.onSubscribe(d); }
            @Override
            public void onNext(Boolean isFriend) {
                if (isFriend) {
                    callback.onError(new Exception("该用户已是您的好友"));
                } else {
                    LCObject friend = new LCObject("Friend");
                    friend.put("user", currentUser);
                    friend.put("friend", targetUser);
                    friend.saveInBackground()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(callback);
                }
            }
            @Override
            public void onError(Throwable e) { callback.onError(e); }
            @Override
            public void onComplete() { callback.onComplete(); }
        });
    }

    // 获取当前用户的所有好友
    public static void getFriendList(io.reactivex.Observer<List<LCUser>> callback) {
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser == null) return;
        LCQuery<LCObject> query = new LCQuery<>("Friend");
        query.whereEqualTo("user", currentUser);
        query.include("friend");
        query.findInBackground()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new io.reactivex.Observer<List<LCObject>>() {
                @Override
                public void onSubscribe(Disposable d) { callback.onSubscribe(d); }
                @Override
                public void onNext(List<LCObject> objects) {
                    List<LCUser> friends = new java.util.ArrayList<>();
                    for (LCObject obj : objects) {
                        LCUser friend = (LCUser) obj.get("friend");
                        if (friend != null) friends.add(friend);
                    }
                    callback.onNext(friends);
                }
                @Override
                public void onError(Throwable e) { callback.onError(e); }
                @Override
                public void onComplete() { callback.onComplete(); }
            });
    }
}
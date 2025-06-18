package com.hyperai.example.lpr3_demo;

import android.content.Context;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlateRepository {
    private final PlateDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlateRepository(Context context) {
        dao = PlateDatabase.getInstance(context).plateDao();
    }

    public interface DataCallback<T> {
        void onResult(T data);
    }

    // 原有异步查询
    public void getPlatesAsync(String query, DataCallback<List<PlateEntity>> callback) {
        executor.execute(() -> {
            List<PlateEntity> list;
            try {
                if (query == null || query.trim().isEmpty()) {
                    list = dao.getAllPlates();
                } else {
                    list = dao.searchPlates("%" + query.trim() + "%");
                }
            } catch (Exception e) {
                list = null;
            }
            callback.onResult(list);
        });
    }

    // 分页异步查询
    public void getPlatesPagedAsync(String query, int limit, int offset, DataCallback<List<PlateEntity>> callback) {
        executor.execute(() -> {
            List<PlateEntity> list;
            try {
                if (query == null || query.trim().isEmpty()) {
                    list = dao.getAllPlatesPaged(limit, offset);
                } else {
                    list = dao.searchPlatesPaged("%" + query.trim() + "%", limit, offset);
                }
            } catch (Exception e) {
                list = null;
            }
            callback.onResult(list);
        });
    }
}
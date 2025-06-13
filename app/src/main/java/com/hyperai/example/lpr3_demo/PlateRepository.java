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
}
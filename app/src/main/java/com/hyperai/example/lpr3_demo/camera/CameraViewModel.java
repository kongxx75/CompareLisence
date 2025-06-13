package com.hyperai.example.lpr3_demo.camera;

import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CameraViewModel extends ViewModel {
    private final MutableLiveData<Bitmap> lastFrameBitmap = new MutableLiveData<>();

    public void setLastFrameBitmap(Bitmap bitmap) {
        lastFrameBitmap.postValue(bitmap);
    }

    public LiveData<Bitmap> getLastFrameBitmap() {
        return lastFrameBitmap;
    }
}
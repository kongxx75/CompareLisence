package com.hyperai.example.lpr3_demo.plate;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hyperai.example.lpr3_demo.PlateEntity;
import com.hyperai.example.lpr3_demo.PlateRepository;

import java.util.List;

public class PlateListViewModel extends ViewModel {
    private final MutableLiveData<List<PlateEntity>> plateList = new MutableLiveData<>();

    public void loadPlateList(PlateRepository repo, String query) {
        repo.getPlatesAsync(query, data -> plateList.postValue(data));
    }

    public LiveData<List<PlateEntity>> getPlateList() {
        return plateList;
    }
}
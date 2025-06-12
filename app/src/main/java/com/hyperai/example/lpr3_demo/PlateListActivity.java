package com.hyperai.example.lpr3_demo;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class PlateListActivity extends AppCompatActivity {

    private ListView listView;
    private PlateAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_list);

        listView = findViewById(R.id.plate_list);
        loadPlateData();
    }

    private void loadPlateData() {
        new Thread(() -> {
            List<PlateEntity> plates = PlateDatabase.getInstance(getApplicationContext()).plateDao().getAllPlates();
            runOnUiThread(() -> {
                if (plates.isEmpty()) {
                    Toast.makeText(this, "没有车牌记录", Toast.LENGTH_SHORT).show();
                } else {
                    adapter = new PlateAdapter(this, plates);
                    listView.setAdapter(adapter);
                }
            });
        }).start();
    }
}
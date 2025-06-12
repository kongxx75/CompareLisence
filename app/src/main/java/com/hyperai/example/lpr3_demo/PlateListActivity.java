package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class PlateListActivity extends AppCompatActivity {

    private ListView listView;
    private PlateAdapter adapter;
    private EditText searchEditText;
    private TextView emptyTextView;
    private List<PlateEntity> plates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_list);

        listView = findViewById(R.id.plate_list);
        searchEditText = findViewById(R.id.searchEditText);
        emptyTextView = findViewById(R.id.emptyTextView);

        loadPlateData(null);

        // 搜索功能
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadPlateData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 长按删除
        listView.setOnItemLongClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
            PlateEntity toDelete = plates.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("删除车牌")
                    .setMessage("确定要删除[" + toDelete.getPlateCode() + "]吗？")
                    .setPositiveButton("删除", (dialog, which) -> deletePlate(toDelete))
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    private void loadPlateData(String query) {
        new Thread(() -> {
            PlateDao dao = PlateDatabase.getInstance(getApplicationContext()).plateDao();
            if (query == null || query.trim().isEmpty()) {
                plates = dao.getAllPlates();
            } else {
                plates = dao.searchPlates("%" + query.trim() + "%");
            }
            runOnUiThread(() -> {
                if (plates == null || plates.isEmpty()) {
                    emptyTextView.setVisibility(android.view.View.VISIBLE);
                    listView.setAdapter(null);
                } else {
                    emptyTextView.setVisibility(android.view.View.GONE);
                    adapter = new PlateAdapter(this, plates);
                    listView.setAdapter(adapter);
                }
            });
        }).start();
    }

    private void deletePlate(PlateEntity plate) {
        new Thread(() -> {
            PlateDatabase.getInstance(getApplicationContext()).plateDao().deletePlate(plate);
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                loadPlateData(searchEditText.getText().toString());
            });
        }).start();
    }
}
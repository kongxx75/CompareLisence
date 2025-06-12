package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class PlateListActivity extends AppCompatActivity {

    private ListView listView;
    private PlateAdapter adapter;
    private EditText searchEditText;
    private TextView emptyTextView;
    private List<PlateEntity> plates;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_list);

        listView = findViewById(R.id.plate_list);
        searchEditText = findViewById(R.id.searchEditText);
        emptyTextView = findViewById(R.id.emptyTextView);
        fabAdd = findViewById(R.id.fab_add);

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

        // 添加车牌功能
        fabAdd.setOnClickListener(v -> showAddPlateDialog());
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

    // 新增：手动添加车牌对话框
    private void showAddPlateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("手动添加车牌");

        // 自定义dialog布局
        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_plate, null);
        final EditText editPlateCode = dialogView.findViewById(R.id.edit_plate_code);
        final EditText editPlateType = dialogView.findViewById(R.id.edit_plate_type);
        builder.setView(dialogView);

        builder.setPositiveButton("添加", (dialog, which) -> {
            String code = editPlateCode.getText().toString().trim().toUpperCase();
            String type = editPlateType.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(this, "车牌号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            PlateEntity entity = new PlateEntity();
            entity.setPlateCode(code);
            entity.setPlateType(TextUtils.isEmpty(type) ? "无备注" : type);
            entity.setTimestamp(String.valueOf(System.currentTimeMillis()));
            entity.setImagePath(""); // 手动添加没有图片
            new Thread(() -> {
                PlateDatabase.getInstance(getApplicationContext()).plateDao().insertPlate(entity);
                runOnUiThread(() -> {
                    Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                    loadPlateData(searchEditText.getText().toString());
                });
            }).start();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
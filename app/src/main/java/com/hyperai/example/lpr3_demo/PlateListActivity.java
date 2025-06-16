package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.content.Intent;
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

        // 搜索输入框提示
        searchEditText.setHint("输入查询信息");

        loadPlateData(null);

        // 搜索功能
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadPlateData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 长按弹出操作菜单
        listView.setOnItemLongClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
            PlateEntity selectedPlate = plates.get(position);

            String[] options = {"取消", "修改", "删除"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请选择操作");
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 取消
                        dialog.dismiss();
                        break;
                    case 1: // 修改
                        showEditPlateDialog(selectedPlate);
                        break;
                    case 2: // 删除
                        showDeleteConfirmDialog(selectedPlate);
                        break;
                }
            });
            builder.show();
            return true;
        });

        // 单击车牌项，进入图片展示页面
        listView.setOnItemClickListener((parent, view, position, id) -> {
            PlateEntity selectedPlate = plates.get(position);
            PlateImageActivity.start(this, selectedPlate.getPlateCode(), selectedPlate.getImagePath());
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
            entity.setImagePath("");
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

    // 新增：编辑车牌对话框
    private void showEditPlateDialog(PlateEntity plate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改车牌信息");

        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_plate, null);
        final EditText editPlateCode = dialogView.findViewById(R.id.edit_plate_code);
        final EditText editPlateType = dialogView.findViewById(R.id.edit_plate_type);

        // 设置原有内容
        editPlateCode.setText(plate.getPlateCode());
        editPlateType.setText(plate.getPlateType());

        builder.setView(dialogView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newCode = editPlateCode.getText().toString().trim().toUpperCase();
            String newType = editPlateType.getText().toString().trim();
            if (TextUtils.isEmpty(newCode)) {
                Toast.makeText(this, "车牌号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            plate.setPlateCode(newCode);
            plate.setPlateType(TextUtils.isEmpty(newType) ? "无备注" : newType);
            // 时间不能修改
            new Thread(() -> {
                PlateDatabase.getInstance(getApplicationContext()).plateDao().updatePlate(plate);
                runOnUiThread(() -> {
                    Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                    loadPlateData(searchEditText.getText().toString());
                });
            }).start();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 新增：删除确认对话框
    private void showDeleteConfirmDialog(PlateEntity plate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除 [" + plate.getPlateCode() + "] 这条记录吗？");
        builder.setPositiveButton("删除", (dialog, which) -> deletePlate(plate));
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
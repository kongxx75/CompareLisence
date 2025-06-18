package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.util.List;

public class PlateListFragment extends Fragment {
    private ListView listView;
    private PlateAdapter adapter;
    private EditText searchEditText;
    private TextView emptyTextView;
    private List<PlateEntity> plates;
    private FloatingActionButton fabAdd;

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                          @Nullable android.view.ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plate_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        listView = root.findViewById(R.id.plate_list);
        searchEditText = root.findViewById(R.id.searchEditText);
        emptyTextView = root.findViewById(R.id.emptyTextView);
        fabAdd = root.findViewById(R.id.fab_add);

        searchEditText.setHint("输入查询信息");

        loadPlateData(null);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadPlateData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            PlateEntity selectedPlate = plates.get(position);
            String[] options = {"取消", "修改", "删除"};
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("请选择操作");
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: dialog.dismiss(); break;
                    case 1: showEditPlateDialog(selectedPlate); break;
                    case 2: showDeleteConfirmDialog(selectedPlate); break;
                }
            });
            builder.show();
            return true;
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            PlateEntity selectedPlate = plates.get(position);
            String imagePath = selectedPlate.getImagePath();
            File imgFile = (imagePath != null && !imagePath.isEmpty()) ? new File(imagePath) : null;
            if (imgFile != null && imgFile.exists()) {
                PlateImageActivity.start(getContext(), selectedPlate.getPlateCode(), imagePath);
            } else {
                Toast.makeText(requireContext(), "无相关的图片", Toast.LENGTH_SHORT).show();
            }
        });

        fabAdd.setOnClickListener(v -> showAddPlateDialog());
    }

    private void loadPlateData(String query) {
        // 提前获取Context，避免子线程getContext()为null
        final Context appCtx = requireContext().getApplicationContext();
        final android.os.Handler mainHandler = new android.os.Handler(requireActivity().getMainLooper());
        new Thread(() -> {
            PlateDao dao = PlateDatabase.getInstance(appCtx).plateDao();
            List<PlateEntity> result;
            if (query == null || query.trim().isEmpty()) {
                result = dao.getAllPlates();
            } else {
                result = dao.searchPlates("%" + query.trim() + "%");
            }
            plates = result;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (plates == null || plates.isEmpty()) {
                    emptyTextView.setVisibility(android.view.View.VISIBLE);
                    listView.setAdapter(null);
                } else {
                    emptyTextView.setVisibility(android.view.View.GONE);
                    adapter = new PlateAdapter(appCtx, plates);
                    listView.setAdapter(adapter);
                }
            });
        }).start();
    }

    private void deletePlate(PlateEntity plate) {
        final Context appCtx = requireContext().getApplicationContext();
        final android.os.Handler mainHandler = new android.os.Handler(requireActivity().getMainLooper());
        new Thread(() -> {
            String imagePath = plate.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    imgFile.delete();
                    requireActivity().sendBroadcast(
                            new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(imgFile))
                    );
                }
            }
            PlateDatabase.getInstance(appCtx).plateDao().deletePlate(plate);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                loadPlateData(searchEditText.getText().toString());
            });
        }).start();
    }

    private void showAddPlateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("手动添加车牌");
        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_plate, null);
        final EditText editPlateCode = dialogView.findViewById(R.id.edit_plate_code);
        final EditText editPlateType = dialogView.findViewById(R.id.edit_plate_type);
        builder.setView(dialogView);

        builder.setPositiveButton("添加", (dialog, which) -> {
            String code = editPlateCode.getText().toString().trim().toUpperCase();
            String type = editPlateType.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(requireContext(), "车牌号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            PlateEntity entity = new PlateEntity();
            entity.setPlateCode(code);
            entity.setPlateType(TextUtils.isEmpty(type) ? "无备注" : type);
            entity.setTimestamp(String.valueOf(System.currentTimeMillis()));
            entity.setImagePath("");
            final Context appCtx = requireContext().getApplicationContext();
            final android.os.Handler mainHandler = new android.os.Handler(requireActivity().getMainLooper());
            new Thread(() -> {
                PlateDatabase.getInstance(appCtx).plateDao().insertPlate(entity);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show();
                    loadPlateData(searchEditText.getText().toString());
                });
            }).start();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showEditPlateDialog(PlateEntity plate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("修改车牌信息");
        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_plate, null);
        final EditText editPlateCode = dialogView.findViewById(R.id.edit_plate_code);
        final EditText editPlateType = dialogView.findViewById(R.id.edit_plate_type);

        editPlateCode.setText(plate.getPlateCode());
        editPlateType.setText(plate.getPlateType());

        builder.setView(dialogView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newCode = editPlateCode.getText().toString().trim().toUpperCase();
            String newType = editPlateType.getText().toString().trim();
            if (TextUtils.isEmpty(newCode)) {
                Toast.makeText(requireContext(), "车牌号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            plate.setPlateCode(newCode);
            plate.setPlateType(TextUtils.isEmpty(newType) ? "无备注" : newType);
            final Context appCtx = requireContext().getApplicationContext();
            final android.os.Handler mainHandler = new android.os.Handler(requireActivity().getMainLooper());
            new Thread(() -> {
                PlateDatabase.getInstance(appCtx).plateDao().updatePlate(plate);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "修改成功", Toast.LENGTH_SHORT).show();
                    loadPlateData(searchEditText.getText().toString());
                });
            }).start();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteConfirmDialog(PlateEntity plate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除 [" + plate.getPlateCode() + "] 这条记录吗？");
        builder.setPositiveButton("删除", (dialog, which) -> deletePlate(plate));
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
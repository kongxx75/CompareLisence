package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlateListFragment extends Fragment {
    private ListView listView;
    private PlateAdapter adapter;
    private EditText searchEditText;
    private TextView emptyTextView;
    private List<PlateEntity> plates = new ArrayList<>();
    private FloatingActionButton fabAdd;

    // 分页参数
    private static final int PAGE_SIZE = 30;
    private static final int LOAD_MORE_SIZE = 20;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentQuery = null;
    private int currentOffset = 0;
    private PlateRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plate_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        listView = root.findViewById(R.id.plate_list);
        searchEditText = root.findViewById(R.id.searchEditText);
        emptyTextView = root.findViewById(R.id.emptyTextView);
        fabAdd = root.findViewById(R.id.fab_add);

        searchEditText.setHint("输入查询信息");
        repository = new PlateRepository(requireContext().getApplicationContext());
        resetPaging();
        loadPlateData(currentQuery, true);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                resetPaging();
                loadPlateData(currentQuery, true);
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

        // 上拉加载更多
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isLoading && !isLastPage
                        && totalItemCount > 0
                        && (firstVisibleItem + visibleItemCount >= totalItemCount)
                        && totalItemCount >= PAGE_SIZE) {
                    loadPlateData(currentQuery, false);
                }
            }
        });
    }

    private void resetPaging() {
        plates.clear();
        currentOffset = 0;
        isLastPage = false;
        isLoading = false;
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadPlateData(String query, boolean isRefresh) {
        if (isLoading || isLastPage) return;
        isLoading = true;

        int pageSize = isRefresh ? PAGE_SIZE : LOAD_MORE_SIZE;
        int offset = isRefresh ? 0 : currentOffset;

        repository.getPlatesPagedAsync(query, pageSize, offset, list -> {
            // 切回主线程安全操作UI
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                if (isRefresh) plates.clear();
                if (list != null) plates.addAll(list);

                currentOffset = plates.size();
                isLastPage = (list == null || list.size() < pageSize);
                isLoading = false;

                if (adapter == null) {
                    adapter = new PlateAdapter(requireContext().getApplicationContext(), plates);
                    listView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
                if (plates.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    emptyTextView.setVisibility(View.GONE);
                }
            });
        });
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
                    resetPaging();
                    loadPlateData(currentQuery, true);
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
                    resetPaging();
                    loadPlateData(currentQuery, true);
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
                resetPaging();
                loadPlateData(currentQuery, true);
            });
        }).start();
    }
}
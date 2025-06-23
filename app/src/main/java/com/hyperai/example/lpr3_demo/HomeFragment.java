package com.hyperai.example.lpr3_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hyperai.example.lpr3_demo.utils.BitmapUtils;
import com.hyperai.example.lpr3_demo.utils.WxPusherUtils;
import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.Plate;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private Button cameraBtn, albumBtn;
    private ImageView imageView;
    private TextView mResult;
    private FloatingActionButton fabAddPlate, fabNotify;
    private ActivityResultLauncher<Intent> pickPhotoLauncher;

    private Plate lastRecognizedPlate = null;
    private Bitmap lastRecognizedBitmap = null;
    private String lastMatchedInfo = null;

    // 新增：保存当前图片文件路径，便于后续大图预览和删除
    private String currentImagePath = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        cameraBtn = root.findViewById(R.id.cameraBtn);
        albumBtn = root.findViewById(R.id.albumBtn);
        imageView = root.findViewById(R.id.imageView);
        mResult = root.findViewById(R.id.mResult);
        fabAddPlate = root.findViewById(R.id.fab_add_plate);
        fabNotify = root.findViewById(R.id.fab_notify);

        cameraBtn.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), CameraActivity.class);
            startActivity(intent);
            if (getActivity() != null) getActivity().overridePendingTransition(0, 0);
        });

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                            if (bitmap != null) {
                                // 保存当前图片路径
                                currentImagePath = getRealPathFromURI(uri);
                                processAndShowPlate(bitmap, currentImagePath);
                            } else {
                                Toast.makeText(getContext(), "图片加载失败", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "图片加载失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        albumBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickPhotoLauncher.launch(intent);
        });

        fabAddPlate.setOnClickListener(v -> {
            if (lastRecognizedPlate != null && lastRecognizedBitmap != null) {
                showSavePlateDialog(requireContext(), lastRecognizedPlate, lastRecognizedBitmap);
            }
        });

        fabNotify.setOnClickListener(v -> {
            if (lastRecognizedPlate != null) {
                showSendNotifyDialog(lastRecognizedPlate);
            }
        });

        fabAddPlate.setVisibility(View.GONE);
        fabNotify.setVisibility(View.GONE);

        // 新增：图片点击放大与长按删除
        imageView.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(currentImagePath)) {
                PlateImageActivity.start(requireContext(), lastRecognizedPlate != null ? lastRecognizedPlate.getCode() : "", currentImagePath);
                if (getActivity() != null) getActivity().overridePendingTransition(0, 0);
            } else if (lastRecognizedBitmap != null) {
                try {
                    File tmp = BitmapUtils.saveBitmapToPlateDir(requireContext(), lastRecognizedBitmap);
                    currentImagePath = tmp.getAbsolutePath();
                    PlateImageActivity.start(requireContext(), lastRecognizedPlate != null ? lastRecognizedPlate.getCode() : "", currentImagePath);
                    if (getActivity() != null) getActivity().overridePendingTransition(0, 0);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "无法打开大图: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "无图片可查看", Toast.LENGTH_SHORT).show();
            }
        });

        imageView.setOnLongClickListener(v -> {
            if (TextUtils.isEmpty(currentImagePath)) {
                Toast.makeText(requireContext(), "无图片可删除", Toast.LENGTH_SHORT).show();
                return true;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("删除图片")
                    .setMessage("确定要删除该图片吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        File file = new File(currentImagePath);
                        boolean existed = file.exists();
                        boolean deleted = false;
                        if (existed) {
                            deleted = file.delete();
                            requireActivity().sendBroadcast(
                                    new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file))
                            );
                        }
                        if (deleted) {
                            Toast.makeText(requireContext(), "图片已删除", Toast.LENGTH_SHORT).show();
                        } else if (existed) {
                            Toast.makeText(requireContext(), "无权限，请手动删除", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "图片文件不存在", Toast.LENGTH_SHORT).show();
                        }
                        // 清空当前图片信息
                        imageView.setImageResource(android.R.color.transparent);
                        currentImagePath = null;
                        lastRecognizedBitmap = null;
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        return root;
    }

    private void processAndShowPlate(Bitmap bitmap, String imagePath) {
        imageView.setImageBitmap(bitmap);
        StringBuilder showText = new StringBuilder();
        Plate[] plates = HyperLPR3.getInstance().plateRecognition(bitmap, HyperLPR3.CAMERA_ROTATION_0, HyperLPR3.STREAM_BGRA);

        lastRecognizedPlate = (plates.length > 0) ? plates[0] : null;
        lastRecognizedBitmap = (plates.length > 0) ? bitmap : null;
        lastMatchedInfo = null;

        if (plates.length == 0) {
            showText.append("未识别到车牌");
            mResult.setText(showText.toString());
            fabAddPlate.setVisibility(View.GONE);
            fabNotify.setVisibility(View.GONE);
            return;
        }

        Plate plate = plates[0];
        String type = "未知车牌";
        if (plate.getType() != HyperLPR3.PLATE_TYPE_UNKNOWN) {
            type = HyperLPR3.PLATE_TYPE_MAPS[plate.getType()];
        }
        String codeWithType = "[" + type + "]" + plate.getCode();
        showText.append(codeWithType).append("\n");

        checkPlateWithLocalDb(plate.getCode(), showText, plate, bitmap);
    }

    private void checkPlateWithLocalDb(String recognizedCode, StringBuilder showText, Plate plate, Bitmap bitmap) {
        new Thread(() -> {
            PlateDao dao = PlateDatabase.getInstance(requireContext().getApplicationContext()).plateDao();
            PlateEntity entity = null;
            try {
                entity = dao.findPlateByCode(recognizedCode);
            } catch (SQLiteException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "数据库异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            PlateEntity finalEntity = entity;
            requireActivity().runOnUiThread(() -> {
                if (finalEntity != null) {
                    String formattedTime;
                    try {
                        long millis = Long.parseLong(finalEntity.getTimestamp());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        formattedTime = sdf.format(new Date(millis));
                    } catch (Exception e) {
                        formattedTime = finalEntity.getTimestamp();
                    }
                    lastMatchedInfo = "本地已存在\n车牌号：" + finalEntity.getPlateCode()
                            + "\n备注：" + finalEntity.getPlateType()
                            + "\n录入时间：" + formattedTime;
                    mResult.setText(showText.toString() + lastMatchedInfo);
                    fabAddPlate.setVisibility(View.GONE);
                    fabNotify.setVisibility(View.VISIBLE);
                } else {
                    lastMatchedInfo = null;
                    mResult.setText(showText.toString() + "未在本地库中找到该车牌");
                    fabAddPlate.setVisibility(View.VISIBLE);
                    fabNotify.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    // 保存车牌弹窗，支持备注和显示时间，并保存图片到系统相册
    private void showSavePlateDialog(Context ctx, Plate plate, Bitmap frameBitmap) {
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_save_plate, null);
        EditText etRemark = dialogView.findViewById(R.id.etRemark);
        EditText etTime = dialogView.findViewById(R.id.etTime);

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        etTime.setText(now);
        etTime.setEnabled(false);

        new AlertDialog.Builder(ctx)
                .setTitle("保存车牌")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String remark = etRemark.getText().toString().trim();
                    String time = String.valueOf(System.currentTimeMillis());
                    String imagePath = "";

                    // 保存图片到本地
                    if (frameBitmap != null) {
                        try {
                            File file = BitmapUtils.saveBitmapToPlateDir(ctx, frameBitmap);
                            imagePath = (file != null) ? file.getAbsolutePath() : "";
                            currentImagePath = imagePath; // 更新当前图片路径
                        } catch (Exception e) {
                            Toast.makeText(ctx, "图片保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    PlateEntity entity = new PlateEntity();
                    entity.setPlateCode(plate.getCode());
                    entity.setPlateType(remark);
                    entity.setTimestamp(time);
                    entity.setImagePath(imagePath);

                    new Thread(() -> {
                        try {
                            PlateDatabase.getInstance(ctx.getApplicationContext()).plateDao().insertPlate(entity);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(ctx, "保存成功", Toast.LENGTH_SHORT).show();
                                fabAddPlate.setVisibility(View.GONE);
                                mResult.setText("已保存到本地库！");
                            });
                        } catch (Exception ex) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(ctx, "数据库写入异常: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .show();
    }

    // 发送通知弹窗
    private void showSendNotifyDialog(Plate plate) {
        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        StringBuilder content = new StringBuilder();
        content.append(timeStr)
                .append("，识别到车牌").append(plate.getCode());
        new AlertDialog.Builder(requireContext())
                .setTitle("是否发送通知？")
                .setMessage(content.toString())
                .setCancelable(false)
                .setPositiveButton("是", (dialog, which) -> {
                    WxPusherUtils.sendMessage(content.toString(), new Callback() {
                        @Override public void onFailure(Call call, java.io.IOException e) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "消息发送失败", Toast.LENGTH_SHORT).show());
                        }
                        @Override public void onResponse(Call call, Response response) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "消息已发送", Toast.LENGTH_SHORT).show());
                        }
                    });
                    dialog.dismiss();
                })
                .setNegativeButton("否", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // 获取图片真实路径（兼容多种相册返回类型）
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(contentUri, proj, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(idx);
            }
        } catch (Exception e) {
            // fallback
        }
        // 有些Android 10+直接返回content://uri
        return contentUri.toString();
    }
}
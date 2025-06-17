package com.hyperai.example.lpr3_demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.Plate;

public class HomeFragment extends Fragment {

    private Button cameraBtn, albumBtn;
    private ImageView imageView;
    private TextView mResult;
    private ActivityResultLauncher<Intent> pickPhotoLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        cameraBtn = root.findViewById(R.id.cameraBtn);
        albumBtn = root.findViewById(R.id.albumBtn);
        imageView = root.findViewById(R.id.imageView);
        mResult = root.findViewById(R.id.mResult);

        cameraBtn.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), CameraActivity.class);
            startActivity(intent);
        });

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                            if (bitmap != null) {
                                processAndShowPlate(bitmap, uri.toString());
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

        return root;
    }

    private void processAndShowPlate(Bitmap bitmap, String imagePath) {
        imageView.setImageBitmap(bitmap);
        StringBuilder showText = new StringBuilder();
        Plate[] plates = HyperLPR3.getInstance().plateRecognition(bitmap, HyperLPR3.CAMERA_ROTATION_0, HyperLPR3.STREAM_BGRA);
        for (Plate plate : plates) {
            String type = "未知车牌";
            if (plate.getType() != HyperLPR3.PLATE_TYPE_UNKNOWN) {
                type = HyperLPR3.PLATE_TYPE_MAPS[plate.getType()];
            }
            String pStr = "[" + type + "]" + plate.getCode() + "\n";
            showText.append(pStr);
            // 可调用数据库方法保存
        }
        mResult.setText(showText.toString());
    }
}
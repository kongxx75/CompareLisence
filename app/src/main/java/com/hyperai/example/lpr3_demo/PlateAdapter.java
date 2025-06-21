package com.hyperai.example.lpr3_demo;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlateAdapter extends BaseAdapter {

    private Context context;  // 这里要传Activity的Context
    private List<PlateEntity> plates;

    public PlateAdapter(Context context, List<PlateEntity> plates) {
        this.context = context;
        this.plates = plates;
    }

    @Override
    public int getCount() { return plates.size(); }

    @Override
    public Object getItem(int position) { return plates.get(position); }

    @Override
    public long getItemId(int position) { return plates.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_plate, parent, false);
            holder = new ViewHolder();
            holder.plateCode = convertView.findViewById(R.id.plate_code);
            holder.plateType = convertView.findViewById(R.id.plate_type);
            holder.timestamp = convertView.findViewById(R.id.timestamp);
            holder.imgPreview = convertView.findViewById(R.id.img_preview);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PlateEntity plate = plates.get(position);
        holder.plateCode.setText("车牌号: " + plate.getPlateCode());
        holder.plateType.setText("备注: " + plate.getPlateType());

        // 格式化时间戳为人类可读格式
        String timeStr = plate.getTimestamp();
        String formattedTime;
        try {
            long millis = Long.parseLong(timeStr);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            formattedTime = sdf.format(new Date(millis));
        } catch (Exception e) {
            formattedTime = timeStr; // fallback
        }
        holder.timestamp.setText("识别时间: " + formattedTime);

        // 判断图片是否真实存在
        String imagePath = plate.getImagePath();
        File imgFile = (imagePath != null && !imagePath.isEmpty()) ? new File(imagePath) : null;
        if (imgFile != null && imgFile.exists()) {
            holder.imgPreview.setVisibility(View.VISIBLE);
            // 这里用Activity context
            holder.imgPreview.setOnClickListener(v -> {
                if (context instanceof androidx.fragment.app.FragmentActivity) {
                    PlateImageDialogFragment dialog = PlateImageDialogFragment.newInstance(plate.getPlateCode(), plate.getImagePath());
                    dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "plate_image");
                }
            });
        } else {
            holder.imgPreview.setVisibility(View.GONE);
            holder.imgPreview.setOnClickListener(null);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView plateCode;
        TextView plateType;
        TextView timestamp;
        ImageView imgPreview;
    }
}
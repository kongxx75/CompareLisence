package com.hyperai.example.lpr3_demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlateAdapter extends BaseAdapter {

    private Context context;
    private List<PlateEntity> plates;

    public PlateAdapter(Context context, List<PlateEntity> plates) {
        this.context = context;
        this.plates = plates;
    }

    @Override
    public int getCount() {
        return plates.size();
    }

    @Override
    public Object getItem(int position) {
        return plates.get(position);
    }

    @Override
    public long getItemId(int position) {
        return plates.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_plate, parent, false);
        }

        TextView plateCode = convertView.findViewById(R.id.plate_code);
        TextView plateType = convertView.findViewById(R.id.plate_type);
        TextView timestamp = convertView.findViewById(R.id.timestamp);

        PlateEntity plate = plates.get(position);
        plateCode.setText("车牌号: " + plate.getPlateCode());
        plateType.setText("备注: " + plate.getPlateType());

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
        timestamp.setText("识别时间: " + formattedTime);

        return convertView;
    }
}
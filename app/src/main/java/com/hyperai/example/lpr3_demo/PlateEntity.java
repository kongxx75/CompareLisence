package com.hyperai.example.lpr3_demo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plates")
public class PlateEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String plateCode;
    private String plateType;
    private String timestamp;
    private String imagePath;

    // 必须的无参构造方法
    public PlateEntity() {}

    // Getter & Setter for id
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    // Getter & Setter for plateCode
    public String getPlateCode() {
        return plateCode;
    }
    public void setPlateCode(String plateCode) {
        this.plateCode = plateCode;
    }

    // Getter & Setter for plateType
    public String getPlateType() {
        return plateType;
    }
    public void setPlateType(String plateType) {
        this.plateType = plateType;
    }

    // Getter & Setter for timestamp
    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // Getter & Setter for imagePath
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
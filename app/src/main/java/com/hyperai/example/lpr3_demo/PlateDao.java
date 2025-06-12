package com.hyperai.example.lpr3_demo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PlateDao {
    @Insert
    void insertPlate(PlateEntity plate);
    @Update
    void updatePlate(PlateEntity plate);
    @Delete
    void deletePlate(PlateEntity plate);
    @Query("SELECT * FROM plates ORDER BY timestamp DESC")
    List<PlateEntity> getAllPlates();
    @Query("SELECT * FROM plates WHERE plateCode LIKE :query OR plateType LIKE :query OR timestamp LIKE :query ORDER BY timestamp DESC")
    List<PlateEntity> searchPlates(String query);
    @Query("SELECT * FROM plates WHERE plateCode = :plateCode LIMIT 1")
    PlateEntity findPlateByCode(String plateCode);


}
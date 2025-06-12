package com.hyperai.example.lpr3_demo;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PlateEntity.class}, version = 1, exportSchema = false)
public abstract class PlateDatabase extends RoomDatabase {
    private static volatile PlateDatabase INSTANCE;
    public abstract PlateDao plateDao();

    public static PlateDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PlateDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    PlateDatabase.class, "plate_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
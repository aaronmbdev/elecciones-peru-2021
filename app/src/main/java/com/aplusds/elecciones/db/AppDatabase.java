package com.aplusds.elecciones.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Ciudadano.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CiudadanoDAO ciudadanoDAO();
}

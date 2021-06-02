package com.aplusds.elecciones.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface CiudadanoDAO {

    @Query("SELECT * FROM Ciudadano WHERE DNI = :dni")
    Ciudadano find(String dni);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Ciudadano citizen);


}

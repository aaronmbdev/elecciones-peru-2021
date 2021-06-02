package com.aplusds.elecciones.db;

import android.content.Context;

import androidx.room.Room;

public class CiudadanoDAOImp {
    private static CiudadanoDAOImp ciudadanoDaoImp;
    private CiudadanoDAO mDAO;

    private CiudadanoDAOImp(Context context) {
        Context appContext = context.getApplicationContext();
        AppDatabase db = Room.databaseBuilder(appContext,AppDatabase.class,"padron").allowMainThreadQueries().build();
        mDAO = db.ciudadanoDAO();
    }

    public static CiudadanoDAOImp get(Context context) {
        if(ciudadanoDaoImp == null) ciudadanoDaoImp = new CiudadanoDAOImp(context);
        return ciudadanoDaoImp;
    }

    public Ciudadano find(String dni) {
        return mDAO.find(dni);
    }

    public void insert(Ciudadano citizen) {
        mDAO.insert(citizen);
    }

}

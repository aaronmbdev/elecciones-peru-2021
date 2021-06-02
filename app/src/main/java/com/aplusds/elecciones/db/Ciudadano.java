package com.aplusds.elecciones.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Ciudadano {
    @ColumnInfo(name = "NOMBBRES")
    private String nombre;
    @ColumnInfo(name = "APELLIDO_PATERNO")
    private String paterno;
    @ColumnInfo(name = "APELLIDO_MATERNO")
    private String materno;
    @ColumnInfo(name = "MESA")
    private int mesa;

    @PrimaryKey
    private int DNI;

    public Ciudadano() {

    }
    public String getNombreCompleto() {
        return nombre+" "+paterno+" "+materno;
    }
    public String getPaterno() {
        return paterno;
    }
    public String getMaterno() {
        return materno;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public void setPaterno(String paterno) {
        this.paterno = paterno;
    }
    public void setMaterno(String materno) {
        this.materno = materno;
    }
    public void setMesa(int mesa) {
        this.mesa = mesa;
    }
    public void setDNI(int DNI) {
        this.DNI = DNI;
    }

    public int getDNI() {
        return DNI;
    }
    public int getMesa() {
        return mesa;
    }
}

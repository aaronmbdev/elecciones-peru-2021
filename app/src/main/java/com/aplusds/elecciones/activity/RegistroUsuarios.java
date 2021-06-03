package com.aplusds.elecciones.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aplusds.elecciones.R;
import com.aplusds.elecciones.utils.NetworkCheck;
import com.aplusds.elecciones.utils.Tools;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistroUsuarios extends AppCompatActivity {

    private Button registro;
    private EditText dni,num;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuarios);
        initToolbar();
        dni = findViewById(R.id.usuario);
        num = findViewById(R.id.num);
        registro = findViewById(R.id.registrobtn2);
        mPrefs = getApplicationContext().getSharedPreferences("MAIN_PREF", Context.MODE_PRIVATE);


        registro.setOnClickListener(v -> {
            String user = dni.getText().toString();
            String numero = num.getText().toString();
            if(!user.equals("") && !numero.equals("")) {
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                DatabaseReference rf = db.getReference("/registro/"+numero+"/usuario");
                DatabaseReference rf2 = db.getReference("/registro/"+numero+"/asistencia");
                rf2.setValue(false);
                if(NetworkCheck.isConnect(getApplicationContext())) {
                    rf.setValue(user);
                    mPrefs.edit().putBoolean("usuarioRegistrado",true).apply();
                    mPrefs.edit().putInt("numtablet",Integer.parseInt(numero)).apply();
                    Intent intent = new Intent(getApplicationContext(), Main.class);
                    startActivity(intent);
                    finish();
                }
            } else {
                Toast.makeText(getApplicationContext(),"Debes llenar todos los campos para continuar",Toast.LENGTH_LONG).show();
            }
        });

    }
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Tools.setSystemBarColor(this, R.color.colorRojo);
    }
}
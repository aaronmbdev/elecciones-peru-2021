package com.aplusds.elecciones.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aplusds.elecciones.BackgroundTask;
import com.aplusds.elecciones.db.Ciudadano;
import com.aplusds.elecciones.db.CiudadanoDAOImp;
import com.aplusds.elecciones.R;
import com.aplusds.elecciones.utils.NetworkCheck;
import com.aplusds.elecciones.utils.Tools;
import com.crowdfire.cfalertdialog.CFAlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class Main extends AppCompatActivity {

    private EditText input;
    private ImageButton buscarbtn;
    private SharedPreferences mPrefs;
    public static final String TAG = "MyTag";
    RequestQueue requestQueue;
    private TextView cargando,ctexto,mesa,dni,nombre;
    private ImageView imagen;
    private ProgressBar bar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_grid_fab);
        initToolbar();
        /* SETUP VISUAL ELEMENTS */
        input = findViewById(R.id.dniinput);
        mesa = findViewById(R.id.mesa);
        dni = findViewById(R.id.dni);
        imagen = findViewById(R.id.imagen);
        nombre = findViewById(R.id.nombre);
        buscarbtn = findViewById(R.id.buscarbtn);
        /* SETUP DATABASE  */
        cargando = findViewById(R.id.cargando);
        ctexto = findViewById(R.id.ctexto);
        bar = findViewById(R.id.bar);

        buscarbtn.setOnClickListener(v -> {
            triggerSearch();
        });


        mPrefs = getApplicationContext().getSharedPreferences("MAIN_PREF", Context.MODE_PRIVATE);
        if(!mPrefs.getBoolean("dbLoaded",false)) {
            //Primer launch, cargar base de datos
            cargando.setText("Cargando...");
            ctexto.setText("Se está descargando la información desde un servidor seguro. La aplicación estará disponible tan pronto termine");
            buscarbtn.setEnabled(false);
            input.setEnabled(false);
            if(NetworkCheck.isConnect(getApplicationContext())) {
                getRemoteData();
            } else {
                CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this)
                        .setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT)
                        .setTitle("Error, no hay internet.")
                        .setMessage("Es la primera vez que se ejecuta esta app y es necesario tener conexión a internet para descargar por primera vez la base de datos.")
                        .setCancelable(false)
                        .addButton("Cerrar",-1,-1, CFAlertDialog.CFAlertActionStyle.NEGATIVE, CFAlertDialog.CFAlertActionAlignment.CENTER, (dialog,which) -> {
                            System.exit(0);
                        });
                builder.show();
            }
        } else {
            cargando.setText("Base de datos cargada");
            ctexto.setText("");
            bar.setVisibility(View.INVISIBLE);

        }


    }

    private void triggerSearch() {
        String dniInput = String.valueOf(input.getText());
        input.setText(null);
        buscarbtn.setEnabled(false);
        final Ciudadano[] aux = {null};
        new BackgroundTask(Main.this) {
            @Override
            public void doInBackground() {
                aux[0] = CiudadanoDAOImp.get(getApplicationContext()).find(dniInput);
            }
            @Override
            public void onPostExecute() {
                if(aux[0] != null) {
                    int mesaP = (aux[0].getMesa() - 82400) + 1;
                    String color = "";
                    if(mesaP >= 1 && mesaP <= 50) {
                        color = "AZUL";
                    } else if(mesaP >= 51 && mesaP <= 90) {
                        color = "VERDE";
                    } else {
                        color = "AMARILLO";
                    }
                    nombre.setText("Nombre: "+aux[0].getNombreCompleto());
                    dni.setText("DNI: "+String.valueOf(aux[0].getDNI()));
                    mesa.setText("Mesa: "+String.valueOf(mesaP)+" - "+color);
                    cargando.setText("Persona encontrada correctamente.");
                    imagen.setImageResource(R.drawable.check);
                } else {
                    nombre.setText("Nombre: -");
                    dni.setText("DNI: -");
                    mesa.setText("Mesa: -");
                    imagen.setImageResource(R.drawable.detener);
                    cargando.setText("No se ha encontrado ninguna persona con DNI "+dniInput+". Revise la dirección en la parte posterior de la tarjeta.");
                }
            }
        }.execute();
        buscarbtn.setEnabled(true);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Tools.setSystemBarColor(this, R.color.colorRojo);
    }


    public void getRemoteData() {
        new Thread(() -> {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
            String url = getResources().getString(R.string.ENDPOINT);
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                boolean cont = false;
                JSONArray arr = new JSONArray();
                try {
                    arr = new JSONArray(response);

                    cont = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(cont) {
                    JSONObject auxObject;
                    for(int i = 0; i<arr.length(); i++) {
                        try {
                            auxObject = arr.getJSONObject(i);
                            Ciudadano aux = new Ciudadano();
                            aux.setDNI(auxObject.getInt("DNI"));
                            aux.setMesa(auxObject.getInt("MESA"));
                            aux.setNombre(auxObject.getString("NOMBRES"));
                            aux.setPaterno(auxObject.getString("APELLIDO_PATERNO"));
                            aux.setMaterno(auxObject.getString("APELLIDO_MATERNO"));
                            CiudadanoDAOImp.get(getApplicationContext()).insert(aux);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                mPrefs.edit().putBoolean("dbLoaded",true).apply();
                runOnUiThread(() -> {
                    cargando.setText("Base de datos cargada");
                    ctexto.setText("Se han cargado los datos correctamente");
                    buscarbtn.setEnabled(true);
                    input.setEnabled(true);
                    bar.setVisibility(View.INVISIBLE);
                });

            }, error -> {
                CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this)
                        .setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT)
                        .setTitle("Error")
                        .setMessage("Ha habido un error de conexión con el servidor.")
                        .setCancelable(false)
                        .addButton("Cerrar",-1,-1, CFAlertDialog.CFAlertActionStyle.NEGATIVE, CFAlertDialog.CFAlertActionAlignment.CENTER, (dialog,which) -> {
                            System.exit(0);
                        });
                builder.show();
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    final HashMap<String,String> postParams = new HashMap<String,String>();
                    postParams.put("auth",getResources().getString(R.string.KEYACCESS));
                    return postParams;
                }
            };
            stringRequest.setTag(TAG);
            requestQueue.add(stringRequest);
        }).start();

    }


    @Override
    protected void onStop() {
        super.onStop();
        if(requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}

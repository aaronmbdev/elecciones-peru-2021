package com.aplusds.elecciones.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Main extends AppCompatActivity implements LocationListener {

    private EditText input;
    private ImageButton buscarbtn;
    private SharedPreferences mPrefs;
    public static final String TAG = "MyTag";
    RequestQueue requestQueue;
    private TextView cargando, ctexto, mesa, dni, nombre, debes;
    private ImageView imagen;
    private ProgressBar bar;
    private Button asistencia;
    private DatabaseReference asistenciaRF;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
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
        debes = findViewById(R.id.debes2);
        asistencia = findViewById(R.id.asistencia);
        asistencia.setVisibility(View.INVISIBLE);

        buscarbtn.setOnClickListener(v -> {
            triggerSearch();
        });

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] perms = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"};
            requestPermissions(perms, 1);
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);

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
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            int numero = mPrefs.getInt("numtablet",0);
            asistenciaRF = db.getReference("/registro/"+numero+"/asistencia");
            cargando.setText("Base de datos cargada");
            ctexto.setText("");
            bar.setVisibility(View.INVISIBLE);
            if(!mPrefs.getBoolean("usuarioRegistrado",false)) {
                Intent intent = new Intent(getApplicationContext(), RegistroUsuarios.class);
                startActivity(intent);
                finish();
            } else {
                asistencia.setVisibility(View.VISIBLE);
                asistencia.setOnClickListener(v -> {
                    if(NetworkCheck.isConnect(getApplicationContext())) {
                        asistenciaRF.setValue(true);
                        asistencia.setEnabled(false);
                        Toast.makeText(getApplicationContext(),"Se ha solicitado asistencia correctamente",Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"No hay conexión a internet",Toast.LENGTH_LONG).show();
                    }
                });


                DatabaseReference rf = db.getReference("/registro/"+numero+"/asistencia");
                ValueEventListener postListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean bool = snapshot.getValue(Boolean.class);
                        if(bool != null && !bool) {
                            asistencia.setEnabled(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };
                rf.addValueEventListener(postListener);


                debes.setText("Si has terminado de usar la tablet, recuerda entregarla a uno de los responsables");
            }

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
                    int mesaP = (aux[0].getMesa() - 82400);
                    String color = "";
                    if(mesaP >= 0 && mesaP <= 49) {
                        color = "AZUL";
                    } else if(mesaP >= 50 && mesaP <= 90) {
                        color = "VERDE";
                    } else {
                        color = "AMARILLO";
                    }
                    nombre.setText("Nombre: "+aux[0].getNombreCompleto());
                    dni.setText("DNI: "+ aux[0].getDNI());
                    mesa.setText("Mesa: "+ mesaP +" - "+color);
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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if(NetworkCheck.isConnect(getApplicationContext())) {
            mPrefs = getApplicationContext().getSharedPreferences("MAIN_PREF", Context.MODE_PRIVATE);
            if(mPrefs.getBoolean("usuarioRegistrado",false)) {
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                int numero = mPrefs.getInt("numtablet",0);
                DatabaseReference rf = db.getReference("/registro/"+numero+"/long");
                DatabaseReference rf2 = db.getReference("/registro/"+numero+"/lat");
                DatabaseReference rf3 = db.getReference("/registro/"+numero+"/lastSeen");
                rf.setValue(location.getLongitude());
                rf2.setValue(location.getLatitude());
                rf3.setValue(System.currentTimeMillis()/1000);
            }
        }
    }
}

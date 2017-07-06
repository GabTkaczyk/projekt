package com.example.gabi.projekt;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private Button B_start;
    private Button B_stop;
    private Button B_zapisz;
    private Button B_wczytaj;
    private Button B_usun_plik;
    private Button B_pokaz_poprzednie_wyniki;
    private TextView T_jaka_szybkosc;
    private TextView T_tytul_kierunek;
    private TextView T_wartosc_kierunek;
    private TextView T_liczba_krokow;
    private PowerManager.WakeLock wakeLock;
    private PowerManager powerManager;
    private Intent krokiIntent;
    private Intent szybkoscIntent;
    private SensorManager sensorManager;
    private String kierunek;
    Kroki mServiceKroki;
    Szybkosc mServiceSzybkosc;
    boolean mBoundKroki = false;
    boolean mBoundSzybkosc = false;
    private int statusKroki = 0;
    private float statusSzybkosc = 0;
    private static final String myFile = "zapisProjekt.txt";
    private String wczytaneWyniki;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        T_tytul_kierunek = (TextView)findViewById(R.id.ID_tytul_kierunek_drogi);
        T_tytul_kierunek.setText("Idziesz w kierunku");
        T_wartosc_kierunek = (TextView)findViewById(R.id.ID_kierunek);
        T_jaka_szybkosc = (TextView)findViewById(R.id.ID_jaka_szybkosc);
        T_liczba_krokow = (TextView)findViewById(R.id.ID_liczba_krokow);
        B_start = (Button)findViewById(R.id.ID_zaczynamy_impreze);
        B_stop = (Button)findViewById(R.id.ID_koniec);
        B_stop.setEnabled(false);
        B_zapisz = (Button)findViewById(R.id.ID_zapisz);
        B_zapisz.setEnabled(false);
        B_wczytaj = (Button)findViewById(R.id.ID_wczytaj);
        setDisableIfFileNotExists(B_wczytaj);
        B_usun_plik = (Button)findViewById(R.id.ID_usun_plik);
        setDisableIfFileNotExists(B_usun_plik);
        B_pokaz_poprzednie_wyniki = (Button)findViewById(R.id.ID_wczytaj);
        setDisableIfFileNotExists(B_pokaz_poprzednie_wyniki);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), sensorManager.SENSOR_DELAY_NORMAL, null);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
                "MyWakelockTag");
    }//onCreate

    @Override
    protected void onPause() {
        Log.d("onPause", "w mainie");
        super.onPause();
        wakeLock.release();
    }//End of onPause

    @Override
    protected void onResume() {
        Log.d("onResume", "w mainie");
        super.onResume();
        wakeLock.acquire();
    }//End of onResume

    //przycisk
    public void onClickZaczynamyImpreze(View view){
        Log.d("nacisniety", "onKlikKroki");
        T_liczba_krokow.setText("");
        T_jaka_szybkosc.setText("");
        B_stop.setEnabled(true);
        B_start.setEnabled(false);
        B_zapisz.setEnabled(false);

        krokiIntent = new Intent(this, Kroki.class);
        startService(krokiIntent);
        bindService(krokiIntent, mConnectionKroki, Context.BIND_AUTO_CREATE);

        szybkoscIntent = new Intent(this, Szybkosc.class);
        startService(szybkoscIntent);
        bindService(szybkoscIntent, mConnectionSzybkosc, Context.BIND_AUTO_CREATE);
    }
    //przycisk
    public void onClickStop(View view){
        Log.d("nacisniety", "stop");
        B_start.setEnabled(true);
        B_stop.setEnabled(false);
        B_zapisz.setEnabled(true);
        //super.onStop();
        if(mBoundKroki){
            statusKroki = mServiceKroki.getStatus();
            Log.d("odebrano", String.valueOf(statusKroki));
            T_liczba_krokow.setText("Liczba wykonanych krokow: " + String.valueOf(statusKroki));
        }
        if(mBoundKroki){
            unbindService(mConnectionKroki);
            mBoundKroki = false;
        }
        stopService(new Intent(this, Kroki.class));
        //==============
        if(mBoundSzybkosc){
            statusSzybkosc = mServiceSzybkosc.getStatus();
            Log.d("odebrano", String.valueOf(statusSzybkosc));
            T_jaka_szybkosc.setText("Szybkosc to: " + String.valueOf(statusSzybkosc));
        }
        if(mBoundSzybkosc){
            unbindService(mConnectionSzybkosc);
            mBoundSzybkosc = false;
        }
        stopService(new Intent(this, Szybkosc.class));
    }

    public void onClickZapisz(View view){
        B_zapisz.setEnabled(false);
        B_wczytaj.setEnabled(true);
        B_usun_plik.setEnabled(true);

        String wynik = "Liczba krokow: " + statusKroki + "/Srednia szybkosc: " + statusSzybkosc;

        Context context = getApplicationContext();
        File file = new File(context.getFilesDir(), myFile);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(myFile, Context.MODE_PRIVATE);
            outputStream.write(wynik.getBytes());
            outputStream.close();
            Log.d("zapisalo sie", "w pliku");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onClickPokazPoprzednieWyniki(View view){
        B_usun_plik.setEnabled(true);
        String line = "";
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = null;

        try {
            InputStream inputStream = openFileInput(myFile);
                if(inputStream != null){
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    bufferedReader = new BufferedReader(inputStreamReader);
                    wczytaneWyniki = "";
                    stringBuilder = new StringBuilder();
                }
            while ( (line = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
            }
            inputStream.close();
            wczytaneWyniki = stringBuilder.toString();

        } catch ( IOException ioe ) {
            ioe.printStackTrace ( ) ;
        }
        Dialog dialog = createPlainAlertDialog("wyniki", wczytaneWyniki);
        dialog.show();
        Log.d("dsds", String .valueOf(wczytaneWyniki));

    }

    public void onClickUsunPlik(View view){
        Context context = getApplicationContext();
        File file = new File(context.getFilesDir(), myFile);
        if( file.exists() ){
            file.delete();
        }
        setDisableIfFileNotExists(B_pokaz_poprzednie_wyniki);
        B_usun_plik.setEnabled(false);
    }

    @Override
    public void onSensorChanged (SensorEvent event){
        kierunek = "";
        if(event.values[0] > 355 || event.values[0] < 5)//0
        {
            kierunek = "Polnoc (N)";
        }else if(event.values[0] < 85 && event.values[0] >= 5 )//<90
        {
            kierunek = "Polnocny-wschod (NE)";
        }else if(event.values[0] >= 85 && event.values[0] < 95 )//==90
        {
            kierunek = "Wschod (E)";
        }else if(event.values[0] < 175 && event.values[0] >= 95 )//<180
        {
            kierunek = "Polodniowy-Wschod (SE)";
        }else if(event.values[0] >= 175 && event.values[0] < 185 )//==180
        {
            kierunek = "Polodnie (S)";
        }else if(event.values[0] >= 185 && event.values[0] < 265 )//<270
        {
            kierunek = "Polodniowy-zachod (SW)";
        }else if(event.values[0] >= 265  && event.values[0] < 275 )//==270
        {
            kierunek = "Zachod (W)";
        }else if(event.values[0] >= 275 && event.values[0] <= 355 )//<360
        {
            kierunek = "Polnocny-zachod (NW)";
        }
        T_wartosc_kierunek.setText(kierunek);

    }//onSensorChanged
    @Override
    public void onAccuracyChanged (Sensor sensor, int accuracy){
    }

    private ServiceConnection mConnectionKroki =  new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName className, IBinder service){
            Kroki.LocalBinder binder = (Kroki.LocalBinder) service;
            mServiceKroki = binder.getService();
            mBoundKroki = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0){
            mBoundKroki = false;
        }
    };

    private ServiceConnection mConnectionSzybkosc = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName className, IBinder service){
            Szybkosc.LocalBinder binder = (Szybkosc.LocalBinder) service;
            mServiceSzybkosc = binder.getService();
            mBoundSzybkosc = true;
        }
        @Override
            public void onServiceDisconnected(ComponentName arg0){
                mBoundSzybkosc = false;
        }
    };

    public void setDisableIfFileNotExists(Button b){
        Context context = getApplicationContext();
        File file = new File(context.getFilesDir(), myFile);
        if(!file.exists()){
            b.setEnabled(false);
        }
    }

    private Dialog createPlainAlertDialog(String title, String message){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        return dialogBuilder.create();
    }



}//klasa


//Toast.makeText(this, "loooool", Toast.LENGTH_LONG).show();
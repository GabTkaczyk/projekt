package com.example.gabi.projekt;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

public class Kroki extends Service implements SensorEventListener {

    protected SensorManager manager;
    protected Sensor sensor;
    protected float licznikKrokow = 0;
    private final IBinder mBinder = new LocalBinder();

        @Override
        public void onCreate() {
            Log.d("jest", "w onCreate kroki");
            // Start up the thread running the service.  Note that we create a
            // separate thread because the service normally runs in the process's
            // main thread, which we don't want to block.  We also make it
            // background priority so CPU-intensive work will not disrupt our UI.
            HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();

            manager = (SensorManager)getSystemService(SENSOR_SERVICE);
            sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if( sensor != null ){
                manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), manager.SENSOR_DELAY_NORMAL, null );
                Log.d("jea", "zarejestrowano czujnik krokow");
            } else{
                Toast.makeText(this, "nie ma czujnika krokow", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d("JEST W", "ON START COMMAND KROKI");
            // If we get killed, after returning from here, restart
            return START_STICKY;
        }

    @Override
    public void onDestroy(){
        Log.d("koniec krokow", "onDestroy");
        super.onDestroy();
        stopSelf();
    }

    public class LocalBinder extends Binder{
        Kroki getService(){
            return Kroki.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {//service
        return mBinder;
    }

    public int getStatus(){
        ///tu unregister
        manager.unregisterListener(this);
        int licznik = (int)licznikKrokow;
        return licznik;
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        licznikKrokow ++;
        Log.d("czujnik", "LCZNIK KROKORW++  ------   " + String.valueOf(licznikKrokow));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
    }

}

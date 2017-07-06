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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Szybkosc extends Service implements SensorEventListener {

    protected SensorManager manager;
    protected Sensor sensor;
    protected float [] gravity = new float[2];
    protected float [] linear_acceleration = new float [2];
    protected long lastTime = 0;
    protected long lastX = 0;
    protected long lastZ = 0;
    protected long lastFast = 0;
    protected float currentFast = 0;
    protected float currentFastA = 0;
    protected float currentFastB = 0;
    protected final float alpha = 0.8f;
    private final IBinder mBinder = new LocalBinder();
    protected List values = Collections.synchronizedList(new ArrayList());


    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if( sensor != null ){
            manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), manager.SENSOR_DELAY_NORMAL, null );
            Log.d("jea", "ZAREJESTROWANO CZUJNIK ACC");
        } else{
            Toast.makeText(this, "nie ma czujnika krokow", Toast.LENGTH_LONG).show();
        }
    }//onCreate

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("JEST W", "ON START COMMAND");
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.d("koniec", "szybkosci");
        super.onDestroy();
        stopSelf();
    }

    public class LocalBinder extends Binder {
        Szybkosc getService(){
            return Szybkosc.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public float getStatus() {
        manager.unregisterListener(this);

        float srednia = 0;
        for( int i = 0; i<values.size(); i++ ){
            Log.d("wyszlo: -", String.valueOf(values.get(i)));
            srednia += Float.parseFloat(String.valueOf(values.get(i)));
        }
        srednia = srednia/values.size();
        return srednia;
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        Log.d("SZYBKO IDZIEMY", "PFFFFFUUUUUOOOOOAAAAAAAAAAAAAAAAAAAA!!!!!!!!!!");
        if( lastTime>0 )
        {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            //gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] + gravity[0];
            linear_acceleration[1] = event.values[2] + gravity[1];
            //linear_acceleration[2] = event.values[2] + gravity[1];

            currentFastA = 0 - linear_acceleration[0];
            currentFastB = 0 - linear_acceleration[1];
            if( currentFastA<0 ){
                currentFastA = currentFastA * (-1);
            }
            if( currentFastB<0 ){
                currentFastB = currentFastB * (-1);
            }
            currentFast = currentFastA + currentFastB;

            Log.d("lol wynik: ", String.valueOf(currentFast));
            values.add(currentFast);
        }
        lastTime = event.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
    }
}//class

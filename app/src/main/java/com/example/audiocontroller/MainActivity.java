package com.example.audiocontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor sensorLight;
    Sensor sensorAcc;

    private boolean prevMovingState;
    private boolean moving;
    private boolean prevLightState = true;
    private boolean underLight = true;

    private int hitCount = 0;
    private float hitSum = 0;


    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAccel = 0f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        sensorManager= (SensorManager) getSystemService(Service.SENSOR_SERVICE);
        sensorLight=sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorAcc=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        createBroadcast();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,sensorLight,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,sensorAcc,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
        {
            int SAMPLE_SIZE = 5;
            float THRESHOLD = 0.2f;

            float[] accelValue = sensorEvent.values;

            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(accelValue[0]*accelValue[0] + accelValue[1]*accelValue[1] + accelValue[2]*accelValue[2]);

            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (hitCount <= SAMPLE_SIZE) {
                hitCount++;
                hitSum += Math.abs(mAccel);
            }
            else
            {
                moving = (float) (hitSum / SAMPLE_SIZE) > THRESHOLD;

                hitCount = 0;
                hitSum = 0;

                if(moving != prevMovingState) // Detect change in movement
                {
                    prevMovingState = moving;
                    createBroadcast();
                }
            }
        }
        else if(sensorEvent.sensor.getType()==Sensor.TYPE_LIGHT)
        {
            float lux =  sensorEvent.values[0];
            underLight = lux > 10;
            if(underLight != prevLightState)
            {
                prevLightState = underLight;
                createBroadcast();
            }
        }
    }

    public void createBroadcast()
    {
        Intent intent = new Intent();
        intent.setAction("com.example.audiocontroller");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        if(moving) // hareketli ve telefon cepte
        {
            intent.putExtra("Type","MovingInPocket");
            sendBroadcast(intent);
        }
        else if(underLight) // hareketsiz ve telefon masada
        {
            Log.e("Data","Masanın üstünde");
            intent.putExtra("Type","NotMovingOnTable");
            sendBroadcast(intent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
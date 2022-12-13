package com.example.android.tiltspot;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // System sensor manager instance.
    private SensorManager sManager;

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor sensorAccelerometer;
    private Sensor sensorRotation;

    // TextViews to display current sensor values.
    private TextView mTextSensorAzimuth;
    private TextView mTextSensorPitch;
    private TextView mTextSensorRoll;

    private ImageView mSpotStart, mSpotTop, mSpotEnd, mSpotBottom;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;

    //Type of Rotation Sensor found
    private int rotationSensorFoundType = -1;

    private Display mDisplay;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*// Lock the orientation to portrait (for now)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);*/

        mTextSensorAzimuth = findViewById(R.id.value_azimuth);
        mTextSensorPitch = findViewById(R.id.value_pitch);
        mTextSensorRoll = findViewById(R.id.value_roll);

        mSpotStart = findViewById(R.id.spot_start);
        mSpotTop = findViewById(R.id.spot_top);
        mSpotEnd = findViewById(R.id.spot_end);
        mSpotBottom = findViewById(R.id.spot_bottom);

        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensorAccelerometer = sManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);

        sensorRotation = sManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR);
        rotationSensorFoundType = Sensor.TYPE_ROTATION_VECTOR;

        if (sensorRotation == null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                sensorRotation = sManager.getDefaultSensor(
                        Sensor.TYPE_GAME_ROTATION_VECTOR);
                rotationSensorFoundType = Sensor.TYPE_GAME_ROTATION_VECTOR;
            }

            if (sensorRotation == null) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sensorRotation = sManager.getDefaultSensor(
                            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
                    rotationSensorFoundType = Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR;
                }

                if (sensorRotation == null)
                    Log.d("sensorRotation", "GG. None found!");
                else
                    Log.d("sensorRotation", sensorRotation.getName());
            } else
                Log.d("sensorRotation", sensorRotation.getName());
        } else
            Log.d("sensorRotation", sensorRotation.getName());

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (sensorAccelerometer != null) {
            sManager.registerListener(this, sensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
            Log.d("registered", "sensorAccelerometer");
        }
        if (sensorRotation != null) {
            sManager.registerListener(this, sensorRotation,
                    SensorManager.SENSOR_DELAY_NORMAL);
            Log.d("registered", "sensorRotation");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        sManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        int sensorType = sensorEvent.sensor.getType();

        //float[] accelerometerData = new float[3];
        float[] rotationVectorData = new float[3];
        float[] rotationMatrix = new float[9];
        float[] orientationValues = new float[3];

        float azimuth, pitch, roll;

        if (sensorType == rotationSensorFoundType) {
            rotationVectorData = sensorEvent.values.clone();
            Log.d("rotationVectorData[0]", String.valueOf(rotationVectorData[0]));
            Log.d("rotationVectorData[1]", String.valueOf(rotationVectorData[1]));
            Log.d("rotationVectorData[2]", String.valueOf(rotationVectorData[2]));
        }

        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVectorData);

        float[] rotationMatrixAdjusted = new float[9];

        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                        rotationMatrixAdjusted);
                break;
        }

        SensorManager.getOrientation(rotationMatrixAdjusted, orientationValues);

        azimuth = orientationValues[0];
        Log.d("azimuth", String.valueOf(azimuth));

        pitch = orientationValues[1];
        if (Math.abs(pitch) < VALUE_DRIFT)
            pitch = 0;
        Log.d("pitch", String.valueOf(pitch));

        roll = orientationValues[2];
        if (Math.abs(roll) < VALUE_DRIFT)
            roll = 0;
        Log.d("roll", String.valueOf(roll));

        mTextSensorAzimuth.setText(getString(R.string.value_format, azimuth));
        mTextSensorPitch.setText(getString(R.string.value_format, pitch));
        mTextSensorRoll.setText(getString(R.string.value_format, roll));

        mSpotStart.setAlpha(0f);
        mSpotTop.setAlpha(0f);
        mSpotEnd.setAlpha(0f);
        mSpotBottom.setAlpha(0f);

        if (pitch > 0 )
            mSpotBottom.setAlpha(pitch);
        else
            mSpotTop.setAlpha(Math.abs(pitch));

        if (roll > 0)
            mSpotStart.setAlpha(roll);
        else
            mSpotEnd.setAlpha(Math.abs(roll));
    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
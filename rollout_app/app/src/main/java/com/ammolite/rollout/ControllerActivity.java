package com.ammolite.rollout;

import android.app.ActionBar;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.LayoutDirection;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class ControllerActivity extends ActionBarActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Vibrator vibrator;
    private TextView txtGyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        txtGyroscope = (TextView)findViewById(R.id.txt_gyro);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        findViewById(R.id.btn_vibrate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(1000);
            }
        });

        //Create the thumbstick control and add it to the page
        ThumbstickControl thumbstick = new ThumbstickControl(this);
        thumbstick.setBackgroundColor(getResources().getColor(R.color.material_deep_teal_500));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(60, 60, 60, 60);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        thumbstick.setLayoutParams(params);
        ((RelativeLayout) findViewById(R.id.root)).addView(thumbstick);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_controller, menu);
        ViewGroup.LayoutParams params = (findViewById(R.id.thumbstick)).getLayoutParams();
        params.width = ((ThumbstickControl)findViewById(R.id.thumbstick)).getHeight();
        (findViewById(R.id.thumbstick)).setLayoutParams(params);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        txtGyroscope.setText("x: " + event.values[2] + ", y: " + event.values[1] +
        ", z: " + event.values[0] + ".");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

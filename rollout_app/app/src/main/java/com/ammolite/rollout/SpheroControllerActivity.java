package com.ammolite.rollout;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.concurrent.Callable;


public class SpheroControllerActivity extends ActionBarActivity implements SensorEventListener {
    private static final Vector2f FORWARD_VECTOR = new Vector2f(1, 0);
    private static final float    ROLL_DEAD_ZONE = 0.05f; // TODO Needs to be tuned.

    private Callable<Void>      updateFunc;
    private Vibrator            vibrator;
    private SensorManager       sensorManager;
    private Sensor              accelerometer;
    private ThumbstickControl   thumbstick;
    private Vector2f            rollVector;
    private Button[]            powerUpButtons;

    public void restart()
    {
        updateHealthBar();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sphero_controller);

        // Thumbstick control.
        thumbstick = new ThumbstickControl(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(60, 150, 60, 60);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        thumbstick.setLayoutParams(params);
        ((RelativeLayout)findViewById(R.id.root)).addView(thumbstick);

        vibrator = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
        sensorManager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        rollVector = new Vector2f();

        powerUpButtons = new Button[2];
        powerUpButtons[0] = (Button)findViewById(R.id.btn_powerup_0);
        powerUpButtons[1] = (Button)findViewById(R.id.btn_powerup_1);

        updateFunc = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Sphero.tick();

                float roll = rollVector.length() / accelerometer.getMaximumRange();
                if (roll > ROLL_DEAD_ZONE) {
                    Sphero.roll(rollVector.angle(FORWARD_VECTOR), roll);
                } else {
                    Sphero.roll(rollVector.angle(FORWARD_VECTOR), 0);
                }

                if (thumbstick.getAbsoluteMagnitude() > ThumbstickControl.DEAD_ZONE_MAGNITUDE && Sphero.weaponReady()) {
                    Sphero.shoot(thumbstick.getAngle());
                    vibrator.vibrate(150);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Sphero.getHasRecentDamage()) {
                            Sphero.setHasRecentDamage(false);
                            vibrator.vibrate(750);
                        }
                        updateHealthBar();

                        synchronized (Sphero.LOCK) {
                            int powerUpCount = Math.min(Sphero.getNumberOfPowerUps(), 2);
                            for (int i = 0; i < powerUpButtons.length; ++i) {
                                if (i < powerUpCount) {
                                    final PowerUp powerUp = PowerUpManager.get(Sphero.getPowerUp(i));
                                    powerUpButtons[i].setBackgroundColor(powerUp.getColour());
                                    powerUpButtons[i].setText(powerUp.getName());
                                    powerUpButtons[i].setOnLongClickListener(new View.OnLongClickListener() {
                                        @Override
                                        public boolean onLongClick(View v) {
                                            Toast.makeText(SpheroControllerActivity.this, powerUp.getDescription(), Toast.LENGTH_LONG).show();
                                            return true;
                                        }
                                    });
                                } else {
                                    powerUpButtons[i].setText("None");
                                    powerUpButtons[i].setOnLongClickListener(null);
                                    powerUpButtons[i].setBackgroundColor(getResources().getColor(R.color.transparentgrey));
                                }
                            }
                        }
                    }
                });

                return null;
            }
        };

        Sphero.startUpdateThread(updateFunc);
    }

    public void updateHealthBar() {
        //Get the health bar
        FrameLayout healthBar = (FrameLayout)findViewById(R.id.health);

        //Work out the spheros health as a ratio and scale it to the size of the original bar
        float normalisedSpheroHealth = Sphero.getHealth() / Sphero.getMaxHealth();
        float scaleFactor =  300 * this.getResources().getDisplayMetrics().densityDpi / 160f;

        //Change the colour if health is low
        if (normalisedSpheroHealth < 0.2) {
            healthBar.setBackgroundColor(getResources().getColor(R.color.red));
            ((FrameLayout)findViewById(R.id.health_start)).setBackgroundColor(getResources().getColor(R.color.red));
            ((FrameLayout)findViewById(R.id.health_end)).setBackgroundColor(getResources().getColor(R.color.red));
        }

        //Set the width to reflect the health
        ViewGroup.LayoutParams params = healthBar.getLayoutParams();
        params.width = (int)(normalisedSpheroHealth * scaleFactor);
        healthBar.setLayoutParams(params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sphero_controller, menu);
        return true;
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        sensorManager.unregisterListener(this);
        Sphero.stopUpdateThread();
        Server.leaveServerAsync();
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        Sphero.stopUpdateThread();
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        Sphero.startUpdateThread(updateFunc);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            rollVector.x(event.values[0]);
            rollVector.y(event.values[1]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO
    }

    /*public void btnPowerUpOnClick(View v) {
        FrameLayout frameLayout = (FrameLayout)v.getParent();
        TextView textView = (TextView)v;

        // Alternate between used and not used.
        if (v.getTag().equals(1)) {
            v.setTag(0);
            frameLayout.setBackgroundColor(getResources().getColor(R.color.transparentgrey));
            textView.setTextSize(36);
            textView.setText("?");
        } else {
            v.setTag(1);
            frameLayout.setBackgroundColor(getResources().getColor(R.color.blue));
            textView.setTextSize(14);
            textView.setText("Missiles");
        }
    }*/

    public void btnPowerUpOnClick0(View v) {
        Sphero.usePowerUp(0);
    }

    public void btnPowerUpOnClick1(View v) {
        Sphero.usePowerUp(1);
    }
}
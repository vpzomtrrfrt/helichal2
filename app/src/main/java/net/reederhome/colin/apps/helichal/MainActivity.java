package net.reederhome.colin.apps.helichal;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        GameView view = (GameView) findViewById(R.id.view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        view.setDeviceDefaultRotation(((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation());
        SensorManager sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensors.registerListener(view, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        sensors.registerListener(view, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AdView ad = ((AdView) findViewById(R.id.ad));
        AdRequest req = new AdRequest.Builder()
                .addTestDevice("67D888911D30A9627F48DA802F3BE746")
                .build();
        ad.loadAd(req);
    }

    @Override
    public void onBackPressed() {
        GameView view = (GameView) findViewById(R.id.view);
        view.handleBackButton();
    }
}

package net.reederhome.colin.apps.helichal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SensorEventListener {
    private static float PSC = 0.1f;
    private static float MAX_TILT = 0.1f;
    private static float HOLE_WIDTH = 0.3f;
    private static float PLATFORM_HEIGHT = 0.07f;

    private float x;
    private List<float[]> platforms;
    private int score;
    private float y = 0;
    private float speed = 0.005f;
    private GameState state;

    private float[] gravity;
    private float[] magnetic;
    private float[] rMat = new float[9];
    private float[] iMat = new float[9];
    private float tilt = 0;

    private long lastTime;
    private Random rand = new Random();

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        startGame();
        Thread thread = new Thread(new Painter());
        thread.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic = event.values;
        }
        if (gravity != null && magnetic != null) {
            if (SensorManager.getRotationMatrix(rMat, iMat, gravity, magnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rMat, orientation);
                tilt = orientation[2] * 0.1f;
                if (tilt > MAX_TILT) {
                    tilt = MAX_TILT;
                } else if (tilt < -MAX_TILT) {
                    tilt = -MAX_TILT;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class Painter implements Runnable {
        @Override
        public void run() {
            while (true) {
                Canvas cnvs = getHolder().lockCanvas();
                if (cnvs != null) {
                    int width = cnvs.getWidth();
                    int height = cnvs.getHeight();
                    float ratio = ((float) height) / width;
                    updateGame(ratio);
                    float psz = width * PSC;

                    cnvs.drawColor(Color.WHITE);
                    Paint charPaint = new Paint();
                    charPaint.setColor(Color.RED);
                    cnvs.drawRect(x * width, height - psz - y*width, x * width + psz - y*width, height, charPaint);

                    Paint platformPaint = new Paint();
                    platformPaint.setColor(Color.BLUE);
                    for (float[] platform : platforms) {
                        cnvs.drawRect(0, ((ratio - platform[1]) - PLATFORM_HEIGHT) * width, platform[0] * width, (ratio - platform[1]) * width, platformPaint);
                        cnvs.drawRect((platform[0] + HOLE_WIDTH) * width, ((ratio - platform[1]) - PLATFORM_HEIGHT) * width, width, (ratio - platform[1]) * width, platformPaint);
                    }

                    if(state == GameState.DEAD) {
                        cnvs.drawColor(Color.argb(140, 255, 255, 255));
                        Paint textPaint = new Paint();
                        textPaint.setColor(Color.BLACK);
                        textPaint.setTextAlign(Paint.Align.CENTER);
                        textPaint.setTextSize(width/7);
                        cnvs.drawText("You died!", width/2, height/2-width*.15f, textPaint);
                        textPaint.setTextSize(width/12);
                        cnvs.drawText("Score: "+score, width/2, height/2, textPaint);
                        textPaint.setTextSize(width/15);
                        cnvs.drawText("Touch to restart", width/2, height/2+width*.15f, textPaint);
                    }

                    getHolder().unlockCanvasAndPost(cnvs);
                }
            }
        }
    }

    private void updateGame(float height) {
        long newTime = System.currentTimeMillis();
        long time = newTime - lastTime;
        lastTime = newTime;

        if(state == GameState.PLAYING) {
            genPlatforms(height);

            x += tilt * time * speed * 1.5;
            if (x > 1 - PSC) {
                x = 1 - PSC;
            } else if (x < 0) {
                x = 0;
            }

            for (int i = 0; i < platforms.size(); i++) {
                float[] platform = platforms.get(i);
                platform[1] -= speed;
                if (y + PSC > platform[1] && y < platform[1] + PLATFORM_HEIGHT && (x <= platform[0] || x + PSC >= platform[0] + HOLE_WIDTH)) {
                    die();
                }
            }
            if (platforms.get(0)[1] < -PLATFORM_HEIGHT) {
                platforms.remove(0);
                score++;
            }
        }
    }

    private void die() {
        state = GameState.DEAD;
    }

    private void genPlatforms(float height) {
        while (true) {
            float[] lp = platforms.get(platforms.size() - 1);
            if (lp[1] > height) {
                break;
            }
            float npx = rand.nextFloat() * (1 - PSC);
            float dsc = (score + platforms.size()) / 200f;
            float dist = Math.abs(npx - lp[0]);
            float playerPart = PSC * (1 - dsc);
            float distPart = dist / (speed * 100);
            float randomPart = rand.nextFloat()/3;
            float nph = lp[1] + Math.max(PLATFORM_HEIGHT, Math.min(playerPart + distPart + randomPart, 1.2f));
            platforms.add(new float[]{npx, nph});
        }
    }

    private void startGame() {
        x = 0.5f - PSC / 2;
        platforms = new ArrayList<>();
        platforms.add(new float[]{0.5f-HOLE_WIDTH/2, PSC*3});
        score = 0;
        state = GameState.PLAYING;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean tr = super.onTouchEvent(event);
        if(tr) return true;
        if(state == GameState.DEAD) {
            startGame();
            return true;
        }
        return false;
    }
}

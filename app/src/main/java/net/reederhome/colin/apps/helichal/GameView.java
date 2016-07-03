package net.reederhome.colin.apps.helichal;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    private static int[] COLORS = {Color.RED, Color.GREEN, Color.CYAN, 0xFFFFA500};
    private static float MAX_TEXT_DIST = 0.12f;

    private float x;
    private List<float[]> platforms;
    private int score;
    private float y = 0;
    private float speed = 0.005f;
    private GameState state = GameState.HOME;
    private int charColor = COLORS[0];
    private float mainTextPos = 0.5f;
    private float mainTextDir = 0.0004f;

    private float[] gravity;
    private float[] magnetic;
    private float[] rMat = new float[9];
    private float[] iMat = new float[9];
    private float tilt = 0;
    private int width;
    private int height;

    private long lastTime;
    private Random rand = new Random();
    private int deviceDefaultRotation;
    private SharedPreferences prefs;
    private boolean newHighScore;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Thread thread = new Thread(new Painter());
        thread.start();
        prefs = context.getSharedPreferences("helichal", Context.MODE_PRIVATE);
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
                tilt = orientation[deviceDefaultRotation == 3 ? 1 : 2] * 0.1f;
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

    public void setDeviceDefaultRotation(int deviceDefaultRotation) {
        this.deviceDefaultRotation = deviceDefaultRotation;
        System.out.println(deviceDefaultRotation);
        System.out.println("LAWL");
    }

    private class Painter implements Runnable {
        @Override
        public void run() {
            while (true) {
                Canvas cnvs = getHolder().lockCanvas();
                if (cnvs != null) {
                    width = cnvs.getWidth();
                    height = cnvs.getHeight();
                    float ratio = ((float) height) / width;
                    updateGame(ratio);

                    cnvs.drawColor(Color.WHITE);

                    if (state != GameState.HOME) {
                        drawPlayer(cnvs, x, y, PSC);

                        Paint platformPaint = new Paint();
                        platformPaint.setColor(Color.BLUE);
                        for (float[] platform : platforms) {
                            cnvs.drawRect(0, ((ratio - platform[1]) - PLATFORM_HEIGHT) * width, platform[0] * width, (ratio - platform[1]) * width, platformPaint);
                            cnvs.drawRect((platform[0] + HOLE_WIDTH) * width, ((ratio - platform[1]) - PLATFORM_HEIGHT) * width, width, (ratio - platform[1]) * width, platformPaint);
                        }

                        if(state == GameState.PLAYING) {
                            Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            scorePaint.setTextSize(width/16);
                            cnvs.drawText("Score: "+score, 0, height-scorePaint.getFontMetrics().descent, scorePaint);
                        }
                    }

                    if (state == GameState.DEAD) {
                        cnvs.drawColor(Color.argb(140, 255, 255, 255));
                        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        textPaint.setColor(Color.BLACK);
                        textPaint.setTextAlign(Paint.Align.CENTER);
                        textPaint.setTextSize(width / 7);
                        cnvs.drawText("You died!", width / 2, height / 2 - width * .2f, textPaint);
                        textPaint.setTextSize(width / 12);
                        cnvs.drawText("Score: " + score, width / 2, height / 2 - width * .1f, textPaint);
                        textPaint.setTextSize(width / 15);
                        String highScoreText = "High Score: " + prefs.getInt("highscore", 0);
                        if(newHighScore) {
                            highScoreText = "New "+highScoreText;
                            textPaint.setColor(Color.RED);
                        }
                        cnvs.drawText(highScoreText, width / 2, height / 2, textPaint);
                        drawButton(cnvs, "play", width / 3, height / 2 + width * .05f, width / 6);
                        drawButton(cnvs, "home", width / 3, height / 2 + width * .25f, width / 6);
                    }

                    if (state == GameState.HOME) {
                        drawPlayer(cnvs, 0.5f - PSC, ((float) height) / (width * 3 / 2), PSC * 2);
                        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        textPaint.setColor(Color.BLACK);
                        textPaint.setTextSize(width / 5);
                        textPaint.setTextAlign(Paint.Align.CENTER);
                        cnvs.drawText("Helichal", width * mainTextPos, height / 2, textPaint);
                        drawButton(cnvs, "play", width / 3, height / 2 + width / 10, width / 6);
                    }

                    getHolder().unlockCanvasAndPost(cnvs);
                }
            }
        }

        private void drawButton(Canvas cnvs, String type, float x, float y, float sz) {
            Paint bgPaint = new Paint();
            Paint fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            switch (type) {
                case "play":
                    bgPaint.setColor(Color.YELLOW);
                    fgPaint.setColor(Color.GREEN);
                    break;
                default:
                    bgPaint.setColor(Color.BLACK);
                    fgPaint.setColor(Color.WHITE);
                    break;
            }

            cnvs.drawRect(x, y + sz / 8, x + sz * 2, y + sz * 7 / 8, bgPaint);
            cnvs.drawRect(x + sz / 8, y, x + sz * 15 / 8, y + sz / 8, bgPaint);
            cnvs.drawRect(x + sz / 8, y + sz * 7 / 8, x + sz * 15 / 8, y + sz, bgPaint);

            Path path = null;
            switch (type) {
                case "play":
                    path = new Path();
                    path.moveTo(x + sz * 3 / 4, y + sz * 3 / 16);
                    path.lineTo(x + sz * 5 / 4, y + sz / 2);
                    path.lineTo(x + sz * 3 / 4, y + sz * 13 / 16);
                    path.close();
                    break;
                case "home":
                    path = new Path();
                    path.moveTo(x + sz * 5 / 8, y + sz * 13 / 16);
                    path.lineTo(x + sz * 5 / 8, y + sz * 7 / 16);
                    path.lineTo(x + sz, y + sz / 8);
                    path.lineTo(x + sz * 11 / 8, y + sz * 7 / 16);
                    path.lineTo(x + sz * 11 / 8, y + sz * 13 / 16);
                    path.close();
                    break;
            }
            if (path != null) {
                cnvs.drawPath(path, fgPaint);
            }

            switch (type) {
                case "home":
                    cnvs.drawRect(x + sz * 7 / 8, y + sz / 2, x + sz * 9 / 8, y + sz * 3 / 4, bgPaint);
                    cnvs.drawRect(x + sz * 15 / 16, y + sz / 4, x + sz * 17 / 16, y + sz * 3 / 8, bgPaint);
            }
        }

        private void drawPlayer(Canvas cnvs, float x, float y, float sc) {
            Paint charPaint = new Paint();
            charPaint.setColor(charColor);
            cnvs.drawRect(x * width, height - (sc + y) * width, (x + sc) * width, height - y * width, charPaint);
            float eyeh = sc / 5;
            Paint irisPaint = new Paint();
            irisPaint.setColor(Color.YELLOW);
            cnvs.drawRect((x + sc / 5) * width, height - (y + sc - eyeh) * width, (x + sc * .4f) * width, height - (y + sc - eyeh * 2) * width, irisPaint);
            cnvs.drawRect((x + sc * .6f) * width, height - (y + sc - eyeh) * width, (x + sc * .8f) * width, height - (y + sc - eyeh * 2) * width, irisPaint);
            Paint pupilPaint = new Paint();
            pupilPaint.setColor(Color.BLACK);
            float dir = tilt / MAX_TILT;
            float px = .25f + dir * .05f;
            float py = .22f + Math.abs(dir) * .03f;
            cnvs.drawRect((x + px * sc) * width, height - (y + sc * (1 - py)) * width, (x + sc * (px + .1f)) * width, height - (y + sc * (.9f - py)) * width, pupilPaint);
            cnvs.drawRect((x + (px + .4f) * sc) * width, height - (y + sc * (1 - py)) * width, (x + sc * (px + .5f)) * width, height - (y + sc * (.9f - py)) * width, pupilPaint);
        }
    }

    private void updateGame(float height) {
        long newTime = System.currentTimeMillis();
        long time = newTime - lastTime;
        lastTime = newTime;

        if (state == GameState.PLAYING) {
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
        } else if (state == GameState.HOME) {
            mainTextPos += mainTextDir * time;
            if (mainTextPos > .5f + MAX_TEXT_DIST) {
                mainTextPos = .5f + MAX_TEXT_DIST;
                mainTextDir = -Math.abs(mainTextDir);
            } else if (mainTextPos < .5f - MAX_TEXT_DIST) {
                mainTextPos = .5f - MAX_TEXT_DIST;
                mainTextDir = Math.abs(mainTextDir);
            }
        }
    }

    private void die() {
        state = GameState.DEAD;
        if (score > prefs.getInt("highscore", 0)) {
            prefs.edit().putInt("highscore", score).apply();
            newHighScore = true;
        }
        else {
            newHighScore = false;
        }
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
            float distPart = dist / (speed * 200);
            float randomPart = rand.nextFloat() / 6;
            float nph = lp[1] + Math.max(PLATFORM_HEIGHT, Math.min(playerPart + distPart + randomPart, 1.2f));
            platforms.add(new float[]{npx, nph});
        }
    }

    private void startGame() {
        x = 0.5f - PSC / 2;
        platforms = new ArrayList<>();
        platforms.add(new float[]{0.5f - HOLE_WIDTH / 2, PSC * 3});
        score = 0;
        state = GameState.PLAYING;
        pickColor();
    }

    private void pickColor() {
        charColor = COLORS[rand.nextInt(COLORS.length)];
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean tr = super.onTouchEvent(event);
        if (tr) return true;
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) return false;
        float ex = event.getX();
        float ey = event.getY();
        if (state == GameState.DEAD) {
            if (ex > width / 3 && ex < width * 2 / 3 && ey > height / 2 + width / 20 && ey < height / 2 + width / 20 + width / 6) {
                startGame();
                return true;
            } else if (ex > width / 3 && ex < width * 2 / 3 && ey > height / 2 + width / 4 && ey < height / 2 + width / 4 + width / 6) {
                state = GameState.HOME;
                return true;
            }
        } else if (state == GameState.HOME) {
            if (ex > width / 3 && ex < width * 2 / 3 && ey > height / 2 + width / 10 && ey < height / 2 + width / 10 + width / 6) {
                startGame();
                return true;
            }
        }
        return false;
    }

}

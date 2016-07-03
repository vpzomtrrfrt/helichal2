package net.reederhome.colin.apps.helichal;

import android.content.SharedPreferences;
import android.graphics.Color;

public enum GameMode {
    NORMAL("Normal", Color.GRAY) {
        @Override
        protected String getHighScoreName() {
            return "highscore";
        }
    },
    FREE_FLY("Free Fly", Color.RED);

    private String name;
    private int color;

    GameMode(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name+" Mode";
    }

    public int getColor() {
        return color;
    }

    protected String getHighScoreName() {
        return "highscore_"+name;
    }

    public int getHighScore(SharedPreferences prefs) {
        return prefs.getInt(getHighScoreName(), 0);
    }

    public void saveHighScore(SharedPreferences prefs, int score) {
        prefs.edit().putInt(getHighScoreName(), score).apply();
    }
}

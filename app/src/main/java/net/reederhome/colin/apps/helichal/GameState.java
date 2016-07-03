package net.reederhome.colin.apps.helichal;

public enum GameState {
    PLAYING(true),
    HOME(false),
    DEAD(true),
    MODE_SELECT(false),
    PAUSED(true);

    private boolean drawGame;

    GameState(boolean drawGame) {
        this.drawGame = drawGame;
    }

    public boolean getDrawGame() {
        return drawGame;
    }
}

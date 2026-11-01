package Shop;

// Counts game time in frames so shop restocks, and later other timed systems, run on in-game time instead of real time.
// 1 in-game minute = 10 real seconds, so it is fps * 10 frames. The clock never reads the real clock, it only counts the
// frames it is told about. The caller decides when time passes: PlayLevelScreen calls tick() once per frame, and only
// while the state is RUNNING, so time stands still in menus, battles, and the inventory.
public class GameClock {
    public static final int DEFAULT_FPS = 60;
    private static final int REAL_SECONDS_PER_GAME_MINUTE = 10;

    private final int framesPerGameMinute;
    // long so a very long session cannot overflow
    private long frames = 0;

    public GameClock() { this(DEFAULT_FPS); }

    // fps (60 default, 30 in the slower mode)
    public GameClock(int fps) {
        if (fps <= 0) throw new IllegalArgumentException("fps must be positive, but got: " + fps);
        this.framesPerGameMinute = fps * REAL_SECONDS_PER_GAME_MINUTE;
    }

    // ticks one frame forward
    public void tick() { this.frames++; }

    // several frames at once, mainly for tests and for catching up
    public void advanceFrames(long framesToAdd) {
        if (framesToAdd < 0) throw new IllegalArgumentException("Cannot go back in time: " + framesToAdd);
        frames += framesToAdd;
    }

    public long getFrames() { return this.frames; }

    public int getFramesPerGameMinute() { return this.framesPerGameMinute; }

    // whole in-game minutes elapsed, a partial minute does not count
    public long getGameMinutes() { return this.frames / this.framesPerGameMinute; }
}

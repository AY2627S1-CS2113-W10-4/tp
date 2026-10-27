package seedu.jaweeper;

/**
 * Tracks elapsed time for a game session.
 */
public class GameTimer {
    private long startTime;
    private long elapsedMillis;
    private boolean isRunning;

    public GameTimer() {
        this.startTime = 0;
        this.elapsedMillis = 0;
        this.isRunning = false;
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.isRunning = true;
    }

    public void stop() {
        if (isRunning) {
            long endTime = System.currentTimeMillis();
            elapsedMillis += (endTime - startTime);
            isRunning = false;
        }
    }

    public void reset() {
        this.startTime = 0;
        this.elapsedMillis = 0;
        this.isRunning = false;
    }
    public void resumeFrom(long previousElapsedMillis) {
        assert previousElapsedMillis >= 0 : "Elapsed time cannot be negative";
        this.elapsedMillis = previousElapsedMillis;
        this.startTime = System.currentTimeMillis();
        this.isRunning = true;
    }

    /**
     * Returns the exact elapsed time in milliseconds, including any running interval.
     */
    public long getElapsedMillis() {
        long totalMillis = elapsedMillis;
        if (isRunning) {
            totalMillis += (System.currentTimeMillis() - startTime);
        }
        return totalMillis;
    }


    public long getElapsedTimeSeconds() {
        long totalMillis = elapsedMillis;
        if (isRunning) {
            totalMillis += (System.currentTimeMillis() - startTime);
        }
        // Ensure quick games register at least 1 second instead of rounding down to 0
        if (totalMillis > 0 && totalMillis < 1000) {
            return 1;
        }
        return totalMillis / 1000;
    }
}

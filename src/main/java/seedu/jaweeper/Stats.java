package seedu.jaweeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks player performance metrics per profile, including wins, losses,
 * win streaks, and historical completion times.
 */
public class Stats {
    private int wins;
    private int losses;
    private int currentStreak;
    private int maxStreak;
    private final List<Long> completionTimes;

    /**
     * Constructs a new Stats instance with zeroed counters and an empty time history.
     */
    public Stats() {
        this.wins = 0;
        this.losses = 0;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.completionTimes = new ArrayList<>();
    }

    /**
     * Records a successful game completion, updating wins, streaks, and completion time.
     *
     * @param timeInMillis The elapsed game duration in milliseconds.
     */
    public void recordWin(long timeInMillis) {
        wins++;
        currentStreak++;
        if (currentStreak > maxStreak) {
            maxStreak = currentStreak;
        }
        completionTimes.add(timeInMillis);
    }

    /**
     * Records a lost game, incrementing losses and resetting the current win streak.
     */
    public void recordLoss() {
        losses++;
        currentStreak = 0;
    }

    /**
     * Resets all statistics counters and clears the completion time history.
     */
    public void reset() {
        wins = 0;
        losses = 0;
        currentStreak = 0;
        maxStreak = 0;
        completionTimes.clear();
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public List<Long> getCompletionTimes() {
        return Collections.unmodifiableList(completionTimes);
    }

    /**
     * Calculates the player's win rate as a percentage.
     *
     * @return The win rate between 0.0 and 100.0.
     */
    public double getWinRate() {
        int total = wins + losses;
        if (total == 0) {
            return 0.0;
        }
        return ((double) wins / total) * 100.0;
    }

    /**
     * Finds the fastest completion time among recorded wins.
     *
     * @return The fastest time in milliseconds, or -1 if no games have been won.
     */
    public long getFastestTime() {
        if (completionTimes.isEmpty()) {
            return -1;
        }
        return Collections.min(completionTimes);
    }
}

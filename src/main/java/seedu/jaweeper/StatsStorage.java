package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes one profile's statistics in {@code stats.txt} inside that profile's directory.
 *
 * <p>File format: a {@code jaweeper-stats-v1} header, then one {@code key value} line each for
 * {@code wins}, {@code losses}, {@code current-streak} and {@code max-streak}, and a final
 * {@code times} line listing every completion time in milliseconds (possibly none).
 * A missing file means a new profile with no games. An unreadable or malformed file is logged
 * and treated as empty; it is only replaced the next time a result is recorded.</p>
 */
public final class StatsStorage {

    static final String FILE_NAME = "stats.txt";

    private static final String HEADER = "jaweeper-stats-v1";
    private static final Logger LOGGER = Logger.getLogger(StatsStorage.class.getName());

    private StatsStorage() {
    }

    /**
     * Loads the saved statistics for a profile into {@code stats}, replacing its contents.
     * Leaves {@code stats} unchanged if there is no valid file.
     */
    public static void readInto(Stats stats, Path profileDirectory) {
        assert stats != null && profileDirectory != null : "Stats and directory must not be null";
        List<String> lines;
        try {
            lines = Storage.readLines(profileDirectory.resolve(FILE_NAME));
        } catch (NoSuchFileException exception) {
            return;
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not read statistics; starting from zero", exception);
            return;
        }
        try {
            parseInto(stats, lines);
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "Ignoring malformed statistics file: " + exception.getMessage());
        }
    }

    /**
     * Writes {@code stats} to the profile's statistics file, replacing it.
     *
     * @throws IOException If the file cannot be written; the previous file is kept.
     */
    public static void write(Stats stats, Path profileDirectory) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add("wins " + stats.getWins());
        lines.add("losses " + stats.getLosses());
        lines.add("current-streak " + stats.getCurrentStreak());
        lines.add("max-streak " + stats.getMaxStreak());
        StringBuilder times = new StringBuilder("times");
        for (long time : stats.getCompletionTimes()) {
            times.append(' ').append(time);
        }
        lines.add(times.toString());
        Storage.writeLines(profileDirectory.resolve(FILE_NAME), lines);
    }

    private static void parseInto(Stats stats, List<String> lines) {
        if (lines.size() != 6 || !HEADER.equals(lines.get(0))) {
            throw new IllegalArgumentException("unexpected header or line count");
        }
        int wins = readCount(lines.get(1), "wins");
        int losses = readCount(lines.get(2), "losses");
        int currentStreak = readCount(lines.get(3), "current-streak");
        int maxStreak = readCount(lines.get(4), "max-streak");

        String[] parts = lines.get(5).strip().split("\\s+");
        if (!parts[0].equals("times")) {
            throw new IllegalArgumentException("expected a 'times' line");
        }
        List<Long> times = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].matches("\\d{1,15}")) {
                throw new IllegalArgumentException("invalid completion time '" + parts[i] + "'");
            }
            times.add(Long.parseLong(parts[i]));
        }
        if (times.size() != wins || currentStreak > wins || maxStreak > wins || currentStreak > maxStreak) {
            throw new IllegalArgumentException("inconsistent counts");
        }
        stats.restore(wins, losses, currentStreak, maxStreak, times);
    }

    private static int readCount(String line, String key) {
        String[] parts = line.strip().split("\\s+");
        if (parts.length != 2 || !parts[0].equals(key) || !parts[1].matches("\\d{1,9}")) {
            throw new IllegalArgumentException("expected '" + key + " NUMBER'");
        }
        return Integer.parseInt(parts[1]);
    }
}

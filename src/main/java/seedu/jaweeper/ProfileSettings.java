package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes one profile's player settings from {@code settings.txt} in that profile's
 * directory. Settings live in their own file so the profile registry format stays unchanged.
 *
 * <p>File format: a {@code jaweeper-settings-v1} header, then one {@code key value} line per
 * setting, for example {@code safeclick off}. Missing, unreadable or malformed files fall back
 * to the defaults and are never overwritten until the player changes a setting.</p>
 */
public final class ProfileSettings {

    static final String FILE_NAME = "settings.txt";
    static final boolean DEFAULT_SAFE_CLICK = true;

    private static final String HEADER = "jaweeper-settings-v1";
    private static final String SAFE_CLICK_KEY = "safeclick";
    private static final Logger LOGGER = Logger.getLogger(ProfileSettings.class.getName());

    private ProfileSettings() {
    }

    /**
     * Reads the safeclick setting for the profile stored in {@code profileDirectory}.
     * Does not create any file or directory.
     *
     * @return The saved value, or {@link #DEFAULT_SAFE_CLICK} if nothing valid is saved.
     */
    public static boolean readSafeClick(Path profileDirectory) {
        List<String> lines;
        try {
            lines = Storage.readLines(profileDirectory.resolve(FILE_NAME));
        } catch (NoSuchFileException exception) {
            return DEFAULT_SAFE_CLICK;
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not read settings; using defaults", exception);
            return DEFAULT_SAFE_CLICK;
        }
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
            LOGGER.warning("Ignoring settings file with an unexpected header");
            return DEFAULT_SAFE_CLICK;
        }
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.strip().split("\\s+");
            if (fields.length == 2 && fields[0].equals(SAFE_CLICK_KEY)) {
                if (fields[1].equals("on")) {
                    return true;
                }
                if (fields[1].equals("off")) {
                    return false;
                }
            }
        }
        return DEFAULT_SAFE_CLICK;
    }

    /**
     * Writes the safeclick setting, creating the profile directory if necessary.
     *
     * @throws IOException If the file cannot be written; the previous file, if any, is kept.
     */
    public static void writeSafeClick(Path profileDirectory, boolean enabled) throws IOException {
        Storage.writeLines(profileDirectory.resolve(FILE_NAME),
                List.of(HEADER, SAFE_CLICK_KEY + " " + (enabled ? "on" : "off")));
    }
}

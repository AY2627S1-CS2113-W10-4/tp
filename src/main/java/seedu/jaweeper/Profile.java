package seedu.jaweeper;

import java.util.UUID;

/**
 * A local profile with immutable identity/name and mutable session statistics.
 */
public final class Profile {

    private final String id;
    private final String name;
    private final Stats stats;

    /**
     * Constructs a profile using a canonical UUID and a validated display name.
     */
    public Profile(String id, String name) {
        this(id, name, new Stats());
    }

    private Profile(String id, String name, Stats stats) {
        if (id == null || !UUID.fromString(id).toString().equals(id)) {
            throw new IllegalArgumentException("Profile ID must be a canonical UUID.");
        }
        this.id = id;
        this.name = validateName(name);
        this.stats = stats;
    }

    /**
     * Returns a renamed profile sharing the same ID and existing statistics.
     * The registry can persist this candidate before replacing the original.
     */
    public Profile withName(String replacementName) {
        return new Profile(id, replacementName, stats);
    }

    /**
     * Trims surrounding spaces and validates a name suitable for the text registry.
     * Names contain 1-30 letters, digits, spaces, underscores or hyphens and start
     * with a letter or digit. Unicode letters and digits are accepted.
     */
    public static String validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Profile name must not be null.");
        }
        String trimmed = name.strip();
        if (!trimmed.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} _-]{0,29}")) {
            throw new IllegalArgumentException(
                    "Use 1-30 letters, digits, spaces, '_' or '-'; start with a letter or digit.");
        }
        return trimmed;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Stats getStats() {
        return stats;
    }
}

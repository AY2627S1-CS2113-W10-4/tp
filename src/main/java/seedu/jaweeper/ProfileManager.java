package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the local profile registry and this application's active profile.
 * Intended for one running application at a time, not concurrent file writers.
 */
public class ProfileManager {

    private static final String FILE_HEADER = "profiles-v1";

    private final Path dataDirectory;
    private final Path profilesFile;
    private Map<String, Profile> profiles;
    private String activeProfileId;

    /**
     * Loads profiles.txt from the supplied directory. A missing file means no
     * profiles yet; malformed or unreadable existing files are not overwritten.
     * Active selection is session-only and is not restored automatically.
     */
    public ProfileManager(Path dataDirectory) throws IOException {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        this.profilesFile = this.dataDirectory.resolve("profiles.txt");
        this.profiles = loadProfiles();
    }

    /**
     * Returns an immutable snapshot in creation order.
     */
    public List<Profile> getProfiles() {
        return List.copyOf(profiles.values());
    }

    public Optional<Profile> getActiveProfile() {
        return Optional.ofNullable(profiles.get(activeProfileId));
    }

    /**
     * Finds a profile by its stable ID, not its display name.
     */
    public Profile getProfile(String id) {
        Profile profile = profiles.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown profile ID: " + id);
        }
        return profile;
    }

    /**
     * Creates and persists a profile without changing the active selection.
     */
    public Profile createProfile(String name) throws IOException {
        String validatedName = Profile.validateName(name);
        requireUniqueName(validatedName, null);
        Profile profile = new Profile(UUID.randomUUID().toString(), validatedName);
        Map<String, Profile> updated = new LinkedHashMap<>(profiles);
        updated.put(profile.getId(), profile);
        saveProfiles(updated);
        return profile;
    }

    /**
     * Changes the session's active profile. The caller must handle any ongoing
     * game before invoking this method; this class does not manage game progress.
     */
    public Profile selectProfile(String id) {
        Profile profile = getProfile(id);
        activeProfileId = profile.getId();
        return profile;
    }

    /**
     * Persists a new display name while preserving the profile ID and selection.
     */
    public Profile renameProfile(String id, String name) throws IOException {
        Profile original = getProfile(id);
        String validatedName = Profile.validateName(name);
        requireUniqueName(validatedName, id);
        Profile renamed = new Profile(original.getId(), validatedName);
        Map<String, Profile> updated = new LinkedHashMap<>(profiles);
        updated.put(id, renamed);
        saveProfiles(updated);
        return renamed;
    }

    /**
     * Removes an inactive profile from the registry. Associated save/statistics
     * files are deliberately retained; this method never recursively deletes data.
     * Callers must explain this behaviour when requesting deletion confirmation.
     */
    public void deleteProfile(String id) throws IOException {
        Profile profile = getProfile(id);
        if (profile.getId().equals(activeProfileId)) {
            throw new IllegalArgumentException("Select another profile before deleting the active profile.");
        }
        Map<String, Profile> updated = new LinkedHashMap<>(profiles);
        updated.remove(id);
        saveProfiles(updated);
    }

    /**
     * Returns a stable directory for an existing profile without creating it.
     * Feature owners choose their own files beneath this directory.
     */
    public Path getProfileDirectory(String id) {
        Profile profile = getProfile(id);
        return dataDirectory.resolve("profiles").resolve(profile.getId());
    }

    /**
     * Rejects duplicate names, ignoring case and optionally excluding one ID.
     */
    private void requireUniqueName(String name, String excludedId) {
        for (Profile profile : profiles.values()) {
            if (!profile.getId().equals(excludedId) && profile.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("A profile with that name already exists.");
            }
        }
    }

    /**
     * Commits the replacement to disk before changing in-memory state.
     */
    private void saveProfiles(Map<String, Profile> updated) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(FILE_HEADER);
        for (Profile profile : updated.values()) {
            lines.add(profile.getId() + "\t" + profile.getName());
        }
        Storage.writeLines(profilesFile, lines);
        profiles = updated;
    }

    /**
     * Loads and validates the entire registry before making it available.
     */
    private Map<String, Profile> loadProfiles() throws IOException {
        List<String> lines;
        try {
            lines = Storage.readLines(profilesFile);
        } catch (NoSuchFileException exception) {
            return new LinkedHashMap<>();
        }
        if (lines.isEmpty() || !FILE_HEADER.equals(lines.get(0))) {
            throw new IOException("Invalid profiles.txt header. The file has not been changed.");
        }

        Map<String, Profile> loaded = new LinkedHashMap<>();
        for (int index = 1; index < lines.size(); index++) {
            try {
                String[] fields = lines.get(index).split("\t", -1);
                if (fields.length != 2) {
                    throw new IllegalArgumentException("Expected an ID and a name separated by one tab.");
                }
                Profile profile = new Profile(fields[0], fields[1]);
                if (loaded.containsKey(profile.getId())) {
                    throw new IllegalArgumentException("Duplicate profile ID.");
                }
                for (Profile existing : loaded.values()) {
                    if (existing.getName().equalsIgnoreCase(profile.getName())) {
                        throw new IllegalArgumentException("Duplicate profile name.");
                    }
                }
                loaded.put(profile.getId(), profile);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid profiles.txt line " + (index + 1)
                        + ": " + exception.getMessage(), exception);
            }
        }
        return loaded;
    }
}

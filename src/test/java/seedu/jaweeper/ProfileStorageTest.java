package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests shared storage and profile management without touching real player data.
 */
public class ProfileStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    public void storage_unicodeAndMissingParents_roundTrips() throws IOException {
        Path file = temporaryDirectory.resolve("nested/data.txt");
        List<String> lines = List.of("Jaweeper", "玩家 Zack", "");
        Storage.writeLines(file, lines);
        assertEquals(lines, Storage.readLines(file));
    }

    @Test
    public void storage_replacement_removesOldContentAndTemporaryFile() throws IOException {
        Path file = temporaryDirectory.resolve("data.txt");
        Storage.writeLines(file, List.of("old", "second line"));
        Storage.writeLines(file, List.of("new"));
        assertEquals(List.of("new"), Storage.readLines(file));
        try (var files = Files.list(temporaryDirectory)) {
            assertEquals(1L, files.count());
        }
    }

    @Test
    public void storage_missingFile_reportsError() {
        assertThrows(NoSuchFileException.class,
                () -> Storage.readLines(temporaryDirectory.resolve("missing.txt")));
    }

    @Test
    public void storage_parentIsFile_reportsErrorWithoutChangingParent() throws IOException {
        Path parent = temporaryDirectory.resolve("not-a-directory");
        Storage.writeLines(parent, List.of("keep me"));
        assertThrows(IOException.class,
                () -> Storage.writeLines(parent.resolve("data.txt"), List.of("new")));
        assertEquals(List.of("keep me"), Storage.readLines(parent));
    }

    @Test
    public void profile_invalidIdOrName_rejected() {
        String id = UUID.randomUUID().toString();
        assertThrows(IllegalArgumentException.class, () -> new Profile("../other", "Zack"));
        assertThrows(IllegalArgumentException.class, () -> new Profile(null, "Zack"));
        for (String name : List.of("", "   ", "../Zack", "Zack\tOther", "Zack\nOther", "a".repeat(31))) {
            assertThrows(IllegalArgumentException.class, () -> new Profile(id, name));
        }
        assertThrows(IllegalArgumentException.class, () -> new Profile(id, null));
    }

    @Test
    public void profile_validName_trimsSurroundingSpaces() {
        Profile profile = new Profile(UUID.randomUUID().toString(), "  玩家 Zack_1  ");
        assertEquals("玩家 Zack_1", profile.getName());
    }

    @Test
    public void manager_missingRegistry_startsEmptyWithoutCreatingFile() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        assertTrue(manager.getProfiles().isEmpty());
        assertTrue(manager.getActiveProfile().isEmpty());
        assertFalse(Files.exists(temporaryDirectory.resolve("profiles.txt")));
    }

    @Test
    public void manager_createAndReload_preservesProfilesButNotSelection() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile first = manager.createProfile("Zack");
        Profile second = manager.createProfile("Pranav");
        assertTrue(manager.getActiveProfile().isEmpty());
        manager.selectProfile(first.getId());

        ProfileManager reloaded = new ProfileManager(temporaryDirectory);
        assertEquals(List.of(first.getId(), second.getId()),
                reloaded.getProfiles().stream().map(Profile::getId).toList());
        assertEquals("Zack", reloaded.getProfile(first.getId()).getName());
        assertTrue(reloaded.getActiveProfile().isEmpty());
    }

    @Test
    public void manager_duplicateOrInvalidName_doesNotChangeRegistry() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        manager.createProfile("Zack");
        List<String> before = Storage.readLines(temporaryDirectory.resolve("profiles.txt"));
        assertThrows(IllegalArgumentException.class, () -> manager.createProfile(" zAcK "));
        assertThrows(IllegalArgumentException.class, () -> manager.createProfile("../name"));
        assertEquals(1, manager.getProfiles().size());
        assertEquals(before, Storage.readLines(temporaryDirectory.resolve("profiles.txt")));
    }

    @Test
    public void manager_profileList_cannotBeModifiedByCaller() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        manager.createProfile("Zack");
        List<Profile> snapshot = manager.getProfiles();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        manager.createProfile("Pranav");
        assertEquals(1, snapshot.size());
    }

    @Test
    public void manager_rename_keepsIdDirectoryAndSelection() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile profile = manager.createProfile("Zack");
        manager.selectProfile(profile.getId());
        Path directory = manager.getProfileDirectory(profile.getId());
        Profile renamed = manager.renameProfile(profile.getId(), "Zack New");

        assertEquals(profile.getId(), renamed.getId());
        assertEquals(directory, manager.getProfileDirectory(profile.getId()));
        assertEquals("Zack New", manager.getActiveProfile().orElseThrow().getName());
        assertEquals("Zack New", new ProfileManager(temporaryDirectory).getProfile(profile.getId()).getName());
        assertEquals("Zack", profile.getName());
    }

    @Test
    public void manager_renameToExistingName_rejected() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile first = manager.createProfile("Zack");
        manager.createProfile("Pranav");
        assertThrows(IllegalArgumentException.class, () -> manager.renameProfile(first.getId(), "PRANAV"));
        assertEquals("Zack", manager.getProfile(first.getId()).getName());
        assertEquals("ZACK", manager.renameProfile(first.getId(), "ZACK").getName());
    }

    @Test
    public void manager_deleteInactive_removesRegistryEntryButRetainsFeatureFiles() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile profile = manager.createProfile("Zack");
        Path save = manager.getProfileDirectory(profile.getId()).resolve("saves/game.txt");
        Storage.writeLines(save, List.of("saved game"));
        manager.deleteProfile(profile.getId());

        assertTrue(new ProfileManager(temporaryDirectory).getProfiles().isEmpty());
        assertEquals(List.of("saved game"), Storage.readLines(save));
        assertThrows(IllegalArgumentException.class, () -> manager.getProfileDirectory(profile.getId()));
        Profile replacement = manager.createProfile("Zack");
        assertFalse(profile.getId().equals(replacement.getId()));
    }

    @Test
    public void manager_deleteActive_rejectedWithoutChangingSelection() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile profile = manager.createProfile("Zack");
        manager.selectProfile(profile.getId());
        assertThrows(IllegalArgumentException.class, () -> manager.deleteProfile(profile.getId()));
        assertEquals(profile.getId(), manager.getActiveProfile().orElseThrow().getId());
        assertEquals(1, new ProfileManager(temporaryDirectory).getProfiles().size());
    }

    @Test
    public void manager_unknownId_rejectedWithoutChangingSelection() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile profile = manager.createProfile("Zack");
        manager.selectProfile(profile.getId());
        assertThrows(IllegalArgumentException.class, () -> manager.selectProfile("missing"));
        assertThrows(IllegalArgumentException.class, () -> manager.renameProfile("missing", "Other"));
        assertThrows(IllegalArgumentException.class, () -> manager.deleteProfile("missing"));
        assertEquals(profile.getId(), manager.getActiveProfile().orElseThrow().getId());
    }

    @Test
    public void manager_badHeaderOrEmptyFile_reportsErrorWithoutOverwriting() throws IOException {
        Path file = temporaryDirectory.resolve("profiles.txt");
        Storage.writeLines(file, List.of("wrong header"));
        assertThrows(IOException.class, () -> new ProfileManager(temporaryDirectory));
        assertEquals(List.of("wrong header"), Storage.readLines(file));
        Storage.writeLines(file, List.of());
        assertThrows(IOException.class, () -> new ProfileManager(temporaryDirectory));
        assertTrue(Storage.readLines(file).isEmpty());
    }

    @Test
    public void manager_badRecord_reportsItsLineWithoutOverwriting() throws IOException {
        Path file = temporaryDirectory.resolve("profiles.txt");
        List<String> lines = List.of("profiles-v1", "broken record");
        Storage.writeLines(file, lines);
        IOException exception = assertThrows(IOException.class, () -> new ProfileManager(temporaryDirectory));
        assertTrue(exception.getMessage().contains("line 2"));
        assertEquals(lines, Storage.readLines(file));
    }

    @Test
    public void manager_duplicateStoredIdOrName_rejected() throws IOException {
        Path file = temporaryDirectory.resolve("profiles.txt");
        String firstId = UUID.randomUUID().toString();
        String secondId = UUID.randomUUID().toString();
        Storage.writeLines(file, List.of("profiles-v1", firstId + "\tZack", firstId + "\tPranav"));
        assertThrows(IOException.class, () -> new ProfileManager(temporaryDirectory));
        Storage.writeLines(file, List.of("profiles-v1", firstId + "\tZack", secondId + "\tzack"));
        assertThrows(IOException.class, () -> new ProfileManager(temporaryDirectory));
    }

    @Test
    public void manager_registryIsDirectory_reportsError() throws IOException {
        Files.createDirectory(temporaryDirectory.resolve("profiles.txt"));
        assertThrows(IOException.class, () -> new ProfileManager(temporaryDirectory));
    }

    @Test
    public void manager_failedCreate_doesNotAddProfileInMemory() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        manager.createProfile("Zack");
        blockRegistryWriting();
        assertThrows(IOException.class, () -> manager.createProfile("Pranav"));
        assertEquals(1, manager.getProfiles().size());
    }

    @Test
    public void manager_failedRename_keepsOriginalNameAndSelection() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile profile = manager.createProfile("Zack");
        manager.selectProfile(profile.getId());
        blockRegistryWriting();
        assertThrows(IOException.class, () -> manager.renameProfile(profile.getId(), "Other"));
        assertEquals("Zack", manager.getProfile(profile.getId()).getName());
        assertEquals("Zack", manager.getActiveProfile().orElseThrow().getName());
    }

    @Test
    public void manager_failedDelete_keepsProfileInMemory() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile profile = manager.createProfile("Zack");
        blockRegistryWriting();
        assertThrows(IOException.class, () -> manager.deleteProfile(profile.getId()));
        assertEquals(profile.getId(), manager.getProfile(profile.getId()).getId());
    }

    /**
     * Places a nonempty directory at the registry path to simulate a failed write.
     * All files involved belong to this test's temporary directory.
     */
    private void blockRegistryWriting() throws IOException {
        Path file = temporaryDirectory.resolve("profiles.txt");
        Files.move(file, temporaryDirectory.resolve("profiles-backup.txt"));
        Files.createDirectory(file);
        Files.writeString(file.resolve("blocker.txt"), "block replacement");
    }
}

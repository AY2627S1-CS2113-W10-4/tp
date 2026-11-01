package seedu.jaweeper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces profile input inconsistencies and stale deletion confirmations through real routing.
 */
public class ProfileInputRegressionTest {

    @TempDir
    Path temporaryDirectory;

    private ProfileManager manager;
    private GameSession session;
    private Map<String, Function<String[], Command>> features;
    private PrintStream originalOutput;
    private PrintStream capturedOutput;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setUp() throws IOException {
        manager = new ProfileManager(temporaryDirectory);
        Profile active = manager.createProfile("Player");
        manager.selectProfile(active.getId());
        manager.createProfile("Guest");
        manager.createProfile("Third");
        session = new GameSession(new Board(), manager);
        features = Jaweeper.createFeatureCommands(session);
        originalOutput = System.out;
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    public void create_sameNameAsStartup_rejectsDuplicateWithoutCollapsingSpaces() throws IOException {
        try (Scanner input = new Scanner("create Alex  Lee\n")) {
            assertTrue(ProfileStartup.selectProfile(input, manager));
        }
        List<String> before = Storage.readLines(temporaryDirectory.resolve("profiles.txt"));
        assertThrows(IllegalArgumentException.class, () -> execute("profile create ALEX  LEE"));
        assertEquals(before, Storage.readLines(temporaryDirectory.resolve("profiles.txt")));
    }

    @Test
    public void createAndRename_preserveInternalSpacesAcrossRestart() throws IOException {
        execute("  PrOfIlE\tCrEaTe\t新玩家  Two   ");
        Profile created = manager.getProfiles().get(3);
        assertEquals("新玩家  Two", created.getName());
        execute("profile rename 4 New   Name");
        Profile reloaded = new ProfileManager(temporaryDirectory).getProfile(created.getId());
        assertEquals("New   Name", reloaded.getName());
        assertEquals("Player", session.getActiveProfile().getName());
    }

    @Test
    public void createAndRename_internalTabAndTooManySpaces_rejectedWithoutWriting() throws IOException {
        List<String> before = Storage.readLines(temporaryDirectory.resolve("profiles.txt"));
        String tooLong = "A" + " ".repeat(29) + "B";
        for (String input : List.of("profile create A\tB", "profile rename 2 A\tB",
                "profile create " + tooLong, "profile rename 2 " + tooLong)) {
            assertThrows(IllegalArgumentException.class, () -> execute(input), input);
        }
        assertEquals(before, Storage.readLines(temporaryDirectory.resolve("profiles.txt")));
    }

    @Test
    public void create_thirtyCharacterNameIncludingSpaces_accepted() throws IOException {
        String name = "A" + " ".repeat(28) + "B";
        execute("profile create " + name);
        assertEquals(name, new ProfileManager(temporaryDirectory).getProfiles().get(3).getName());
    }

    @Test
    public void delete_confirmWithoutWarning_onlyPromptsFirst() throws IOException {
        List<String> before = Storage.readLines(temporaryDirectory.resolve("profiles.txt"));
        execute("profile delete 2 confirm");
        assertEquals(before, Storage.readLines(temporaryDirectory.resolve("profiles.txt")));
        assertTrue(text().contains("Guest"));
        execute("profile delete 2 confirm");
        assertEquals(List.of("Player", "Third"), names());
    }

    @Test
    public void delete_repeatOldConfirmation_doesNotDeleteRenumberedNeighbour() throws IOException {
        execute("profile delete 2");
        execute("profile delete 2 confirm");
        List<String> before = Storage.readLines(temporaryDirectory.resolve("profiles.txt"));
        execute("profile delete 2 confirm");
        assertEquals(List.of("Player", "Third"), names());
        assertEquals(before, Storage.readLines(temporaryDirectory.resolve("profiles.txt")));
    }

    @Test
    public void delete_renamedTarget_requiresFreshConfirmation() {
        execute("profile delete 2");
        execute("profile rename 2 Renamed Guest");
        execute("profile delete 2 confirm");
        assertEquals(List.of("Player", "Renamed Guest", "Third"), names());
        execute("profile delete 2 confirm");
        assertEquals(List.of("Player", "Third"), names());
    }

    @Test
    public void delete_changedTargetIdWithSameNameAndNumber_requiresFreshConfirmation() throws IOException {
        execute("profile delete 3");
        Profile old = manager.getProfiles().get(2);
        manager.deleteProfile(old.getId());
        Profile replacement = manager.createProfile("Third");
        execute("profile delete 3 confirm");
        assertEquals(replacement.getId(), manager.getProfiles().get(2).getId());
    }

    @Test
    public void delete_promptForAnotherProfile_replacesPreviousConfirmation() {
        execute("profile delete 2");
        execute("profile delete 3");
        execute("profile delete 2 confirm");
        assertEquals(3, manager.getProfiles().size());
        execute("profile delete 2 confirm");
        assertEquals(List.of("Player", "Third"), names());
    }

    @Test
    public void delete_profileSwitch_clearsPendingConfirmation() {
        execute("profile delete 2");
        execute("profile select 3");
        execute("profile delete 2 confirm");
        assertEquals(List.of("Player", "Guest", "Third"), names());
        assertEquals("Third", session.getActiveProfile().getName());
    }

    @Test
    public void delete_failedWrite_consumesConfirmationAndKeepsProfile() throws IOException {
        execute("profile delete 2");
        Path registry = temporaryDirectory.resolve("profiles.txt");
        Path backup = temporaryDirectory.resolve("backup.txt");
        Files.move(registry, backup);
        Files.createDirectory(registry);
        Path blocker = Files.writeString(registry.resolve("block.txt"), "keep");
        execute("profile delete 2 confirm");
        assertEquals(3, manager.getProfiles().size());
        assertTrue(text().contains("Could not save profile changes"));
        assertFalse(text().contains("Deleted profile:"));
        Files.delete(blocker);
        Files.delete(registry);
        Files.move(backup, registry);
        execute("profile delete 2 confirm");
        assertEquals(3, manager.getProfiles().size());
        execute("profile delete 2 confirm");
        assertEquals(List.of("Player", "Third"), names());
    }

    @Test
    public void run_chainedDeletes_requireWarningForEachTargetAndKeepNames() {
        try (Scanner input = new Scanner("profile delete 2; profile delete 2 confirm; "
                + "profile delete 2 confirm; profile create Alex  Lee; profile list\nexit\n")) {
            Jaweeper.run(input, session, Jaweeper.createCoordinateCommands(session), features);
        }
        assertEquals(List.of("Player", "Third", "Alex  Lee"), names());
        assertTrue(text().contains("Goodbye!"));
    }

    private void execute(String input) {
        Parser.parse(input, Jaweeper.createCoordinateCommands(session), features,
                session.getRowCount(), session.getColumnCount()).execute(session.getBoard());
    }

    private List<String> names() {
        return manager.getProfiles().stream().map(Profile::getName).toList();
    }

    private String text() {
        return output.toString(StandardCharsets.UTF_8);
    }
}

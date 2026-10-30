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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises real parser registrations and profile operations with isolated files.
 */
public class ProfileCommandTest {

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
        Profile first = manager.createProfile("Zheng Yiting");
        manager.selectProfile(first.getId());
        manager.createProfile("Guest");
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
    public void create_multiWordUnicodeName_persistsWithoutSwitching() throws IOException {
        Profile first = session.getActiveProfile();
        Board board = session.getBoard();
        execute("PrOfIlE CrEaTe 新玩家 Two");
        assertEquals("新玩家 Two", manager.getProfiles().get(2).getName());
        assertEquals("新玩家 Two", new ProfileManager(temporaryDirectory).getProfiles().get(2).getName());
        assertSame(first, session.getActiveProfile());
        assertSame(board, session.getBoard());
        assertThrows(IllegalArgumentException.class, () -> execute("profile create 新玩家 two"));
        assertEquals(3, manager.getProfiles().size());
    }

    @Test
    public void parse_invalidSyntaxAndNames_leavesRegistryUnchanged() {
        for (String input : List.of("profile", "profile list extra", "profile create", "profile create ../bad",
                "profile select", "profile select zero", "profile select 0", "profile select -1",
                "profile select 999999999999999", "profile select 2 extra", "profile rename 2",
                "profile delete 2 yes", "profile delete 2 confirm extra", "profile unknown")) {
            assertThrows(IllegalArgumentException.class, () -> execute(input), input);
        }
        assertThrows(IllegalArgumentException.class, () -> execute("profile select 99"));
        assertEquals(2, manager.getProfiles().size());
        assertEquals("Zheng Yiting", session.getActiveProfile().getName());
    }

    @Test
    public void rename_activeProfile_keepsStatsBoardDirectoryAndIdentity() throws IOException {
        Profile original = session.getActiveProfile();
        original.getStats().recordWin(1234);
        original.getStats().recordLoss();
        Board board = session.getBoard();
        Path directory = session.getProfileDirectory();
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("keep.txt"), "saved data");
        execute("profile rename 1 Zheng Yiting New");
        Profile renamed = session.getActiveProfile();
        assertEquals(original.getId(), renamed.getId());
        assertEquals("Zheng Yiting New", renamed.getName());
        assertSame(original.getStats(), renamed.getStats());
        assertEquals(1, renamed.getStats().getWins());
        assertEquals(1, renamed.getStats().getLosses());
        assertEquals(List.of(1234L), renamed.getStats().getCompletionTimes());
        assertSame(board, session.getBoard());
        assertEquals(directory, session.getProfileDirectory());
        assertEquals("saved data", Files.readString(directory.resolve("keep.txt")));
        assertEquals(renamed.getName(), new ProfileManager(temporaryDirectory).getProfile(renamed.getId()).getName());
    }

    @Test
    public void rename_duplicateName_rejectsWithoutLosingStats() {
        Profile original = session.getActiveProfile();
        original.getStats().recordWin(4000);
        assertThrows(IllegalArgumentException.class, () -> execute("profile rename 1 GUEST"));
        assertSame(original, session.getActiveProfile());
        assertEquals(1, original.getStats().getWins());
    }

    @Test
    public void select_unsavedProgress_blocksUntilSaved() {
        Board original = session.getBoard();
        execute("f 1 1");
        execute("profile select 2");
        assertSame(original, session.getBoard());
        assertEquals("Zheng Yiting", session.getActiveProfile().getName());
        assertTrue(text().contains("Save your current game before switching"));
        execute("save first-game");
        execute("profile select 2");
        assertEquals("Guest", session.getActiveProfile().getName());
        assertNotSame(original, session.getBoard());
        assertEquals(9, session.getRowCount());
        assertFalse(session.getBoard().grid[0][0].isFlagged);
        execute("profile select 1");
        execute("load first-game");
        assertTrue(session.getBoard().grid[0][0].isFlagged);
    }

    @Test
    public void select_currentProfile_doesNotResetUnsavedGame() {
        execute("f 1 1");
        Board board = session.getBoard();
        execute("profile select 1");
        assertSame(board, session.getBoard());
        assertTrue(board.grid[0][0].isFlagged);
        assertTrue(text().contains("Already selected"));
    }

    @Test
    public void select_resetsHistoryTimerTrackerAndSettingsContext() throws IOException {
        SaveTracker tracker = new SaveTracker();
        DeleteCommand.Confirmation confirmation = new DeleteCommand.Confirmation();
        session.pushBoardHistory(session.getBoard());
        session.getGameTimer().resumeFrom(50000);
        session.setSafeClickEnabled(false);
        tracker.markSaved(session.getBoard(), "old-save");
        confirmation.confirm("same-name");
        ProfileCommand.parse(new String[]{"select", "2"}, session, tracker, confirmation)
                .execute(session.getBoard());
        assertFalse(session.undoLastMove(session.getBoard()));
        assertTrue(session.getGameTimer().getElapsedMillis() < 50000);
        assertNull(tracker.getCurrentSaveName(session.getBoard()));
        assertFalse(confirmation.confirm("same-name"));
        assertTrue(session.isSafeClickEnabled());
        execute("profile select 1");
        assertFalse(session.isSafeClickEnabled());
    }

    @Test
    public void select_doesNotCarrySaveDeletionConfirmationToAnotherPlayer() {
        execute("f 1 1");
        execute("save common");
        execute("delete common");
        execute("profile select 2");
        execute("f 1 1");
        execute("save common");
        execute("delete common");
        assertTrue(SaveStore.forSession(session).exists("common"));
        execute("delete common");
        assertFalse(SaveStore.forSession(session).exists("common"));
    }

    @Test
    public void delete_requiresConfirmation_retainsFilesAndDoesNotReuseId() throws IOException {
        Profile guest = manager.getProfiles().get(1);
        Path directory = manager.getProfileDirectory(guest.getId());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("keep.txt"), "retain");
        execute("profile delete 2");
        assertEquals(2, manager.getProfiles().size());
        execute("profile delete 2 confirm");
        assertEquals(1, manager.getProfiles().size());
        assertEquals(1, new ProfileManager(temporaryDirectory).getProfiles().size());
        assertEquals("retain", Files.readString(directory.resolve("keep.txt")));
        execute("profile create Guest");
        assertNotEquals(guest.getId(), manager.getProfiles().get(1).getId());
    }

    @Test
    public void delete_activeProfile_isRejectedEvenWithConfirmation() {
        assertThrows(IllegalArgumentException.class, () -> execute("profile delete 1"));
        assertThrows(IllegalArgumentException.class, () -> execute("profile delete 1 confirm"));
        assertEquals(2, manager.getProfiles().size());
    }

    @Test
    public void writes_fail_doNotChangeRegistrySelectionOrStats() throws IOException {
        Profile original = session.getActiveProfile();
        original.getStats().recordWin(1000);
        Path registry = temporaryDirectory.resolve("profiles.txt");
        Files.delete(registry);
        Files.createDirectory(registry);
        Files.writeString(registry.resolve("block.txt"), "keep");
        execute("profile create Third");
        execute("profile rename 1 Changed");
        execute("profile delete 2 confirm");
        assertEquals(2, manager.getProfiles().size());
        assertSame(original, session.getActiveProfile());
        assertEquals(1, original.getStats().getWins());
        assertEquals("Guest", manager.getProfiles().get(1).getName());
        assertEquals("keep", Files.readString(registry.resolve("block.txt")));
        assertTrue(text().contains("Could not save profile changes"));
        assertFalse(text().contains("Renamed profile:"));
        assertFalse(text().contains("Deleted profile:"));
    }

    @Test
    public void run_chainedProfileCommands_resolveCurrentListAndContinueAfterErrors() {
        try (Scanner input = new Scanner("profile create Third; profile rename 3 Third New; "
                + "profile select 3; profile delete 1 confirm; profile select 2; "
                + "profile select 99; profile list; help\nexit\n")) {
            Jaweeper.run(input, session, Jaweeper.createCoordinateCommands(session), features);
        }
        assertEquals("Third New", session.getActiveProfile().getName());
        assertEquals(2, manager.getProfiles().size());
        assertTrue(text().contains("2. Third New *"));
        assertTrue(text().contains("Unknown profile number"));
        assertTrue(text().contains("profile rename NUMBER NAME"));
        assertTrue(text().contains("Goodbye"));
    }

    private void execute(String input) {
        Parser.parse(input, Jaweeper.createCoordinateCommands(session), features,
                session.getRowCount(), session.getColumnCount()).execute(session.getBoard());
    }

    private String text() {
        return output.toString(StandardCharsets.UTF_8);
    }
}

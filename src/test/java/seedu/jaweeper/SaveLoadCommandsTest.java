package seedu.jaweeper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests saving, loading, the save store and the unsaved-progress warning, using temporary folders.
 */
public class SaveLoadCommandsTest {

    @TempDir
    Path temporaryDirectory;

    private PrintStream originalOutput;
    private ByteArrayOutputStream output;
    private GameSession session;
    private SaveTracker tracker;

    @BeforeEach
    public void setUp() throws IOException {
        originalOutput = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        ProfileManager manager = new ProfileManager(temporaryDirectory);
        manager.selectProfile(manager.createProfile("Yong Xiang").getId());
        session = new GameSession(new Board(9, 9, 10), manager);
        tracker = new SaveTracker();
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
    }

    private String takeOutput() {
        String text = output.toString(StandardCharsets.UTF_8);
        output.reset();
        return text;
    }

    /** Makes a move that changes the board without risking a mine. */
    private void flagFirstCell(Board board) {
        new FlagCommand(0, 0).execute(board);
    }

    private Path savesDirectory() {
        return session.getProfileDirectory().resolve("saves");
    }

    @Test
    public void store_saveListLoadDelete_worksInProfileFolder() throws Exception {
        SaveStore store = SaveStore.forSession(session);
        Board board = session.getBoard();
        flagFirstCell(board);

        store.save("Game_1", board, 1500);
        store.save("alpha", board, 0);

        assertTrue(Files.isRegularFile(savesDirectory().resolve("game_1.txt")));
        assertEquals(List.of("alpha", "game_1"), store.list());

        GameSaveCodec.SavedGame loaded = store.load("GAME_1");
        assertEquals(1500, loaded.elapsedMillis());
        assertEquals(GameSaveCodec.encodeGrid(board), GameSaveCodec.encodeGrid(loaded.board()));

        assertTrue(store.delete("alpha"));
        assertFalse(store.delete("alpha"));
        assertEquals(List.of("game_1"), store.list());
    }

    @Test
    public void store_invalidNames_rejected() {
        String[] invalid = {"", "   ", "../evil", "a/b", "with space", "a.txt", "nul", "COM1",
            "x".repeat(31)};
        for (String name : invalid) {
            assertThrows(IllegalArgumentException.class, () -> SaveStore.normalizeName(name), name);
        }
        assertEquals("my-game_2", SaveStore.normalizeName(" My-Game_2 "));
    }

    @Test
    public void store_listIgnoresOtherFilesAndMissingFolder() throws IOException {
        SaveStore store = SaveStore.forSession(session);
        assertEquals(List.of(), store.list());
        Files.createDirectories(savesDirectory());
        Files.writeString(savesDirectory().resolve("notes.md"), "hi");
        Files.writeString(savesDirectory().resolve(".jaweeper-123.tmp"), "partial");
        assertEquals(List.of(), store.list());
    }

    @Test
    public void parse_badArguments_rejected() {
        assertThrows(IllegalArgumentException.class, () -> SaveCommand.parse(new String[]{"a", "b"}, session, tracker));
        assertThrows(IllegalArgumentException.class, () -> SaveCommand.parse(new String[]{"../x"}, session, tracker));
        assertThrows(IllegalArgumentException.class, () -> LoadCommand.parse(new String[]{}, session, tracker));
        assertThrows(IllegalArgumentException.class, () -> LoadCommand.parse(new String[]{"a", "b"}, session, tracker));
    }

    @Test
    public void saveThenLoad_restoresBoardAndTimer() {
        Board original = session.getBoard();
        flagFirstCell(original);
        List<String> savedGrid = GameSaveCodec.encodeGrid(original);

        SaveCommand.parse(new String[]{"run1"}, session, tracker).execute(original);
        assertTrue(takeOutput().contains("Game saved as 'run1'."));

        session.replaceBoard(new Board(16, 16, 40));
        LoadCommand.parse(new String[]{"run1"}, session, tracker).execute(session.getBoard());

        assertTrue(takeOutput().contains("Loaded save 'run1'."));
        assertNotSame(original, session.getBoard());
        assertEquals(savedGrid, GameSaveCodec.encodeGrid(session.getBoard()));
        assertEquals("run1", tracker.getCurrentSaveName(session.getBoard()));
        assertFalse(tracker.hasUnsavedChanges(session.getBoard()));
    }

    @Test
    public void save_withoutName_reusesCurrentNameUntilNewGame() {
        Board board = session.getBoard();
        flagFirstCell(board);
        SaveCommand.parse(new String[]{}, session, tracker).execute(board);
        assertTrue(takeOutput().contains("has not been saved yet"));

        SaveCommand.parse(new String[]{"slot"}, session, tracker).execute(board);
        new FlagCommand(0, 1).execute(board);
        SaveCommand.parse(new String[]{}, session, tracker).execute(board);
        assertTrue(takeOutput().contains("Overwrote save 'slot'."));

        new NewGameCommand(session, Difficulty.EASY).execute(board);
        assertNull(tracker.getCurrentSaveName(board), "A new game must not overwrite the old save by default");
    }

    @Test
    public void save_finishedGame_refused() {
        Board board = session.getBoard();
        board.setGameState(GameState.LOST);
        SaveCommand.parse(new String[]{"lost"}, session, tracker).execute(board);
        assertTrue(takeOutput().contains("nothing to save"));
        assertFalse(Files.exists(savesDirectory().resolve("lost.txt")));
    }

    @Test
    public void load_corruptFile_keepsCurrentGame() throws IOException {
        Board current = session.getBoard();
        Files.createDirectories(savesDirectory());
        Files.write(savesDirectory().resolve("broken.txt"), List.of("jaweeper-save-v1", "size 9 9"));

        LoadCommand.parse(new String[]{"broken"}, session, tracker).execute(current);

        String text = takeOutput();
        assertTrue(text.contains("Could not load 'broken'"), text);
        assertTrue(text.contains("has not been changed"));
        assertSame(current, session.getBoard());
    }

    @Test
    public void load_missingSave_reported() {
        LoadCommand.parse(new String[]{"ghost"}, session, tracker).execute(session.getBoard());
        assertTrue(takeOutput().contains("No save named 'ghost'."));
    }

    @Test
    public void load_overUnsavedProgress_needsSecondConfirmation() throws IOException {
        Board saved = new Board(9, 9, 10);
        SaveStore.forSession(session).save("other", saved, 0);
        Board current = session.getBoard();
        flagFirstCell(current);
        takeOutput();

        LoadCommand.parse(new String[]{"other"}, session, tracker).execute(current);
        assertTrue(takeOutput().contains("type 'load other' again"));
        assertSame(current, session.getBoard());

        LoadCommand.parse(new String[]{"other"}, session, tracker).execute(current);
        assertTrue(takeOutput().contains("Loaded save 'other'."));
        assertNotSame(current, session.getBoard());
    }

    @Test
    public void exit_unsavedProgress_warnsThenQuitsOnRepeat() {
        Board board = session.getBoard();
        flagFirstCell(board);

        ExitCommand first = new ExitCommand(tracker);
        first.execute(board);
        assertFalse(first.isExit());
        assertTrue(takeOutput().contains("unsaved progress"));

        ExitCommand second = new ExitCommand(tracker);
        second.execute(board);
        assertTrue(second.isExit());
    }

    @Test
    public void exit_moveAfterWarning_warnsAgain() {
        Board board = session.getBoard();
        flagFirstCell(board);
        new ExitCommand(tracker).execute(board);
        new FlagCommand(0, 1).execute(board);

        ExitCommand again = new ExitCommand(tracker);
        again.execute(board);
        assertFalse(again.isExit(), "The player made a move, so the earlier warning no longer applies");
    }

    @Test
    public void exit_noMovesSavedOrFinished_quitsImmediately() {
        Board board = session.getBoard();
        ExitCommand untouched = new ExitCommand(tracker);
        untouched.execute(board);
        assertTrue(untouched.isExit());

        flagFirstCell(board);
        SaveCommand.parse(new String[]{"done"}, session, tracker).execute(board);
        ExitCommand afterSave = new ExitCommand(tracker);
        afterSave.execute(board);
        assertTrue(afterSave.isExit());

        new FlagCommand(0, 1).execute(board);
        board.setGameState(GameState.WON);
        ExitCommand afterWin = new ExitCommand(tracker);
        afterWin.execute(board);
        assertTrue(afterWin.isExit());
    }

    @Test
    public void runLoop_saveLoadAndGuardedExit_endToEnd() {
        String script = String.join("\n", "f 1 1", "save trip", "f 1 2", "exit", "load trip", "load trip",
                "exit") + "\n";
        try (Scanner input = new Scanner(script)) {
            Jaweeper.run(input, session, Jaweeper.createCoordinateCommands(session),
                    Jaweeper.createFeatureCommands(session));
        }
        String text = takeOutput();
        assertTrue(text.contains("Game saved as 'trip'."), text);
        assertTrue(text.contains("type 'exit' again"), text);
        assertTrue(text.contains("type 'load trip' again"), text);
        assertTrue(text.contains("Loaded save 'trip'."), text);
        assertTrue(text.contains("Goodbye!"), text);
        assertTrue(session.getBoard().grid[0][0].isFlagged());
        assertFalse(session.getBoard().grid[0][1].isFlagged(), "The flag placed after saving was discarded");
    }
}

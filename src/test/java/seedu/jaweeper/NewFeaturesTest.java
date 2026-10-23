package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/**
 * Tests showing all mines on a loss, the safeclick setting, command chaining, listing and
 * deleting saves, the clear command and the updated help text.
 */
public class NewFeaturesTest {

    @TempDir
    Path temporaryDirectory;

    private PrintStream originalOutput;
    private ByteArrayOutputStream output;
    private GameSession session;

    @BeforeEach
    public void setUp() throws IOException {
        originalOutput = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        session = newSession(new Board());
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
    }

    private GameSession newSession(Board board) throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        if (manager.getProfiles().isEmpty()) {
            manager.createProfile("Tester");
        }
        manager.selectProfile(manager.getProfiles().get(0).getId());
        return new GameSession(board, manager);
    }

    private String takeOutput() {
        String text = output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        output.reset();
        return text;
    }

    /** A 9x9 board whose only mines are at the given zero-based positions; first move still pending. */
    private static Board boardWithMines(int... rowColPairs) {
        Board board = new Board();
        for (Cell[] row : board.grid) {
            for (Cell cell : row) {
                cell.isMine = false;
            }
        }
        for (int i = 0; i < rowColPairs.length; i += 2) {
            board.grid[rowColPairs[i]][rowColPairs[i + 1]].isMine = true;
        }
        return board;
    }

    private void runScript(String script) {
        try (Scanner input = new Scanner(script)) {
            Jaweeper.run(input, session, Jaweeper.createCoordinateCommands(session),
                    Jaweeper.createFeatureCommands(session));
        }
    }

    /* ---------------------------------------------------------------- show all mines on loss */

    @Test
    public void loss_showsUnflaggedMinesAndWrongFlagsWithoutChangingCells() {
        Board board = boardWithMines(0, 0, 2, 2, 8, 8);
        board.skipSafeFirstClick();
        board.grid[8][8].isFlagged = true; // correct flag on a mine
        board.grid[4][4].isFlagged = true; // wrong flag on a safe cell

        new RevealCommand(0, 0).execute(board);

        String text = takeOutput();
        assertTrue(text.contains("BOOM"), text);
        List<String> lines = text.lines().toList();
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("  1 |") && l.contains(" * ")), text);
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("  3 |") && l.contains(" * ")),
                "An unrevealed mine must be shown\n" + text);
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("  5 |") && l.contains(" X ")),
                "A wrong flag must be marked X\n" + text);
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("  9 |") && l.endsWith(" F |")),
                "A correct flag stays F\n" + text);
        assertFalse(board.grid[2][2].isRevealed, "Showing mines must not change the cells");
    }

    @Test
    public void inProgressBoard_hidesMines() {
        Board board = boardWithMines(2, 2);
        board.printBoard();
        assertFalse(takeOutput().contains("*"));
    }

    /* ---------------------------------------------------------------- safeclick setting */

    @Test
    public void safeClickOn_firstRevealOnMine_isMovedAndSafe() {
        session.replaceBoard(boardWithMines(0, 0));
        new RevealCommand(session, 0, 0).execute(session.getBoard());
        // With a single mine the safe opening may even win, so only "not lost" is guaranteed.
        assertNotEquals(GameState.LOST, session.getBoard().getGameState());
    }

    @Test
    public void safeClickOff_firstRevealOnMine_losesTheGame() throws IOException {
        session.setSafeClickEnabled(false);
        session.replaceBoard(boardWithMines(0, 0));
        new RevealCommand(session, 0, 0).execute(session.getBoard());
        assertEquals(GameState.LOST, session.getBoard().getGameState());
    }

    @Test
    public void settingsCommand_togglesAndPersistsPerProfile() throws IOException {
        assertTrue(session.isSafeClickEnabled(), "Default is on");
        runScript("settings safeclick off\nsettings\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Safeclick is now OFF"), text);
        assertTrue(text.contains("safeclick  OFF"), text);

        GameSession reopened = newSession(new Board());
        assertFalse(reopened.isSafeClickEnabled(), "Setting must survive a restart");

        reopened.setSafeClickEnabled(true);
        assertTrue(newSession(new Board()).isSafeClickEnabled());
    }

    @Test
    public void settingsCommand_otherProfileKeepsItsOwnValue() throws IOException {
        session.setSafeClickEnabled(false);
        ProfileManager manager = session.getProfileManager();
        manager.selectProfile(manager.createProfile("Other").getId());
        assertTrue(session.isSafeClickEnabled(), "A different profile starts with the default");
    }

    @Test
    public void settingsCommand_badArguments_rejected() {
        for (String line : List.of("settings safeclick", "settings safeclick maybe", "settings volume on",
                "settings safeclick on extra")) {
            assertThrows(IllegalArgumentException.class,
                    () -> Parser.parse(line, Jaweeper.createCoordinateCommands(session),
                            Jaweeper.createFeatureCommands(session), 9, 9), line);
        }
    }

    @Test
    public void malformedSettingsFile_fallsBackToDefaultWithoutOverwriting() throws IOException {
        Path file = session.getProfileDirectory().resolve(ProfileSettings.FILE_NAME);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "garbage\n");
        assertTrue(newSession(new Board()).isSafeClickEnabled());
        assertEquals("garbage\n", Files.readString(file));
    }

    /* ---------------------------------------------------------------- chaining */

    @Test
    public void splitChain_trimsAndSkipsEmptySegments() {
        assertEquals(List.of("f 1 1", "f 2 3"), Jaweeper.splitChain("f 1 1; f 2 3"));
        assertEquals(List.of("f 1 1", "f 2 3"), Jaweeper.splitChain(" ;f 1 1;;  f 2 3 ; "));
        assertEquals(List.of("help"), Jaweeper.splitChain("help"));
        assertEquals(List.of("  "), Jaweeper.splitChain("  "));
    }

    @Test
    public void chain_runsEveryCommand() {
        runScript("f 1 1; f 2 3\nexit\n");
        assertTrue(session.getBoard().grid[0][0].isFlagged());
        assertTrue(session.getBoard().grid[1][2].isFlagged());
    }

    @Test
    public void chain_failingCommandReportsErrorAndRestStillRun() {
        runScript("f 1 1; f 99 99; f 2 3\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Coordinates must be from 1 to 9."), text);
        assertTrue(session.getBoard().grid[0][0].isFlagged());
        assertTrue(session.getBoard().grid[1][2].isFlagged());
    }

    @Test
    public void chain_unknownCommandInTheMiddle_doesNotStopTheLine() {
        runScript("f 1 1; nonsense; f 2 3\nexit\n");
        assertTrue(takeOutput().contains("Unknown command: nonsense"));
        assertTrue(session.getBoard().grid[1][2].isFlagged());
    }

    @Test
    public void chain_exitSkipsRemainingCommands() {
        runScript("f 1 1; exit; f 2 3\n");
        assertTrue(session.getBoard().grid[0][0].isFlagged());
        // exit warns once about unsaved progress, so the loop continues, then hits end of input.
        runScriptAfterReset("f 5 5; save keep; exit; f 6 6\n");
        assertFalse(session.getBoard().grid[5][5].isFlagged(), "Commands after a real exit must not run");
    }

    private void runScriptAfterReset(String script) {
        session.replaceBoard(new Board());
        runScript(script);
    }

    @Test
    public void chain_commandsSeeTheBoardLeftByEarlierCommands() {
        // 'new hard' replaces the board with 16x30; (15, 29) is only valid afterwards.
        runScript("new hard; f 15 29\nexit\n");
        String text = takeOutput();
        assertFalse(text.contains("Coordinates must be"), text);
        assertTrue(session.getBoard().grid[14][28].isFlagged());
    }

    @Test
    public void chain_gameEndingSkipsTheRestWithOneNote() throws IOException {
        session.setSafeClickEnabled(false);
        session.replaceBoard(boardWithMines(0, 0));
        runScript("r 1 1; f 2 2; f 3 3\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("the remaining 2 commands on this line were skipped."), text);
        assertFalse(text.contains("The game is over."), text);
        assertFalse(session.getBoard().grid[1][1].isFlagged());
    }

    @Test
    public void emptyLine_stillReportsMissingCommand() {
        runScript("\n;\nexit\n");
        assertTrue(takeOutput().contains("Enter a command."));
    }

    /* ---------------------------------------------------------------- saves and delete */

    @Test
    public void saves_emptyThenListedAlphabetically() {
        runScript("saves\nf 1 1; save zeta; save alpha\nsaves\nexit\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("No saved games yet."), text);
        assertTrue(text.contains("Saved games (2):"), text);
        assertTrue(text.indexOf("  alpha") < text.indexOf("  zeta"), text);
    }

    @Test
    public void delete_needsConfirmationThenRemovesSave() {
        runScript("f 1 1; save trip\ndelete trip\nsaves\ndelete trip\nsaves\nexit\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Type 'delete trip' again to confirm."), text);
        assertTrue(text.contains("Saved games (1):"), text);
        assertTrue(text.contains("Deleted save 'trip'."), text);
        assertTrue(text.contains("No saved games yet."), text);
        assertFalse(Files.exists(session.getProfileDirectory().resolve("saves").resolve("trip.txt")));
    }

    @Test
    public void delete_afterConfirmationForAnotherName_needsFreshConfirmation() {
        runScript("f 1 1; save a; save b\ndelete a\ndelete b\nexit\nexit\n");
        Path saves = session.getProfileDirectory().resolve("saves");
        assertTrue(Files.exists(saves.resolve("a.txt")));
        assertTrue(Files.exists(saves.resolve("b.txt")));
    }

    @Test
    public void delete_missingSave_reportsAndDoesNotArmConfirmation() {
        runScript("delete nope\ndelete nope\nexit\n");
        String text = takeOutput();
        assertEquals(2, text.split("No save named 'nope'", -1).length - 1, text);
        assertFalse(text.contains("again to confirm"), text);
    }

    @Test
    public void delete_invalidName_rejected() {
        runScript("delete ../x\ndelete\ndelete a b\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Save names use 1-30 letters"), text);
        assertTrue(text.contains("Usage: delete NAME"), text);
    }

    @Test
    public void delete_currentGamesSave_makesGameCountAsUnsaved() {
        runScript("f 1 1; save trip\ndelete trip\ndelete trip\nexit\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("now counts as unsaved"), text);
        assertTrue(text.contains("You have unsaved progress"), text);
        assertTrue(text.contains("Goodbye!"), text);
    }

    @Test
    public void delete_savedGameNotInPlay_keepsCurrentGameSaved() {
        runScript("f 1 1; save a\nsave b\ndelete a\ndelete a\nexit\n");
        String text = takeOutput();
        assertFalse(text.contains("now counts as unsaved"), text);
        assertTrue(text.contains("Goodbye!"), text);
    }

    /* ---------------------------------------------------------------- clear and help */

    @Test
    public void clear_printsAnsiClearSequence_andKeepsBoard() {
        Board board = new Board();
        board.grid[0][0].isFlagged = true;
        assertInstanceOf(ClearCommand.class, Parser.parse("clear"));
        new ClearCommand().execute(board);
        assertTrue(takeOutput().contains("\033[2J"));
        assertTrue(board.grid[0][0].isFlagged);
    }

    @Test
    public void clear_withArguments_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("clear screen"));
    }

    @Test
    public void help_listsNewCommandsAndBoardSpecificCoordinates() {
        new HelpCommand().execute(new Board(16, 30, 99));
        String text = takeOutput();
        for (String expected : List.of("settings safeclick on|off", "saves", "delete NAME", "clear",
                "stats", "leaderboard", "Chaining", "rows 1 to 16, columns 1 to 30")) {
            assertTrue(text.contains(expected), expected + "\n" + text);
        }
    }
}

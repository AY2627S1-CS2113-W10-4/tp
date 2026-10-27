// @@author ProNoob116
package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests undo after flags and reveals, including an empty history.
 */
public class UndoCommandTest {

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
        session = new GameSession(new Board(), new ProfileManager(temporaryDirectory));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
    }

    @ParameterizedTest
    @MethodSource("undoAfterFlags")
    public void undo_afterFlags_restoresEarlierFlags(int flagsBeforeUndo, int flaggedCellsAfterOneUndo,
                                                     String expectedMessage) {
        for (int column = 0; column < flagsBeforeUndo; column++) {
            new FlagCommand(session, 0, column).execute(session.getBoard());
        }
        output.reset();

        new UndoCommand(session).execute(session.getBoard());

        assertEquals(flaggedCellsAfterOneUndo, countFlags(session.getBoard()));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains(expectedMessage));
    }

    @Test
    public void undo_emptyStackTwice_staysEmpty() {
        UndoCommand undo = new UndoCommand(session);
        undo.execute(session.getBoard());
        undo.execute(session.getBoard());

        String text = output.toString(StandardCharsets.UTF_8);
        assertEquals(2, countOccurrences(text, "Nothing to undo."));
        assertFalse(session.getBoard().grid[0][0].isFlagged());
    }

    @Test
    public void undo_afterTwoFlags_restoresOneMoveAtATime() {
        Board board = session.getBoard();
        new FlagCommand(session, 0, 0).execute(board);
        new FlagCommand(session, 0, 1).execute(board);

        new UndoCommand(session).execute(board);
        assertTrue(board.grid[0][0].isFlagged());
        assertFalse(board.grid[0][1].isFlagged());

        new UndoCommand(session).execute(board);
        assertFalse(board.grid[0][0].isFlagged());
        assertFalse(board.grid[0][1].isFlagged());

        new UndoCommand(session).execute(board);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Nothing to undo."));
    }

    @Test
    public void undo_afterReveal_hidesTheCellAgain() {
        Board board = session.getBoard();
        new RevealCommand(session, 0, 0).execute(board);
        assertTrue(board.grid[0][0].isRevealed());
        assertFalse(board.isFirstMove());

        new UndoCommand(session).execute(board);

        assertFalse(board.grid[0][0].isRevealed());
        assertTrue(board.isFirstMove());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Undid the last move."));
    }

    @Test
    public void undo_afterUnflag_restoresTheFlag() {
        Board board = session.getBoard();
        new FlagCommand(session, 1, 1).execute(board);
        new UnflagCommand(session, 1, 1).execute(board);
        assertFalse(board.grid[1][1].isFlagged());

        new UndoCommand(session).execute(board);
        assertTrue(board.grid[1][1].isFlagged());
    }

    @ParameterizedTest
    @MethodSource("rejectedUndoInputs")
    public void parse_extraUndoArguments_rejected(String input) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> Parser.parse(input));
        assertTrue(exception.getMessage().contains("Usage: undo"), exception.getMessage());
    }

    @Test
    public void factory_undoWithoutMoves_reportsEmptyHistory() throws IOException {
        Command command = Jaweeper.createFeatureCommands(session).get("undo").apply(new String[]{});
        command.execute(session.getBoard());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Nothing to undo."));
    }

    @Test
    public void parse_undoWithoutSession_reportsEmptyHistory() {
        Command command = Parser.parse("undo");
        command.execute(new Board());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Nothing to undo."));
    }

    private static Stream<Arguments> undoAfterFlags() {
        return Stream.of(
                Arguments.of(0, 0, "Nothing to undo."),
                Arguments.of(1, 0, "Undid the last move."),
                Arguments.of(2, 1, "Undid the last move."),
                Arguments.of(3, 2, "Undid the last move.")
        );
    }

    private static Stream<Arguments> rejectedUndoInputs() {
        return Stream.of(
                Arguments.of("undo now"),
                Arguments.of("undo 1"),
                Arguments.of("undo extra words")
        );
    }

    private static int countFlags(Board board) {
        int flags = 0;
        for (Cell[] row : board.grid) {
            for (Cell cell : row) {
                if (cell.isFlagged()) {
                    flags++;
                }
            }
        }
        return flags;
    }

    private static int countOccurrences(String text, String fragment) {
        int count = 0;
        int from = 0;
        while (from >= 0) {
            from = text.indexOf(fragment, from);
            if (from >= 0) {
                count++;
                from += fragment.length();
            }
        }
        return count;
    }
}
// @@author

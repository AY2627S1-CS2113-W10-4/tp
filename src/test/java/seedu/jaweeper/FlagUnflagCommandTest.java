package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FlagUnflagCommandTest {
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream output;
    private Board board;

    @BeforeEach
    public void setUp() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        board = new Board();
        // Reset every cell so tests don't depend on random mine placement
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                board.grid[i][j].isRevealed = false;
                board.grid[i][j].isFlagged = false;
            }
        }
        output.reset(); // discard anything printed during setup
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    private String printed() {
        return output.toString();
    }

    // ---------- FlagCommand ----------

    @Test
    public void flag_unrevealedCell_setsFlag() {
        new FlagCommand(0, 0).execute(board);

        assertTrue(board.grid[0][0].isFlagged);
        assertTrue(printed().contains("Cell (1, 1) flagged."));
    }

    @Test
    public void flag_unrevealedCell_doesNotRevealCell() {
        new FlagCommand(2, 3).execute(board);

        assertFalse(board.grid[2][3].isRevealed);
    }

    @Test
    public void flag_revealedCell_doesNotFlag() {
        board.grid[1][1].isRevealed = true;

        new FlagCommand(1, 1).execute(board);

        assertFalse(board.grid[1][1].isFlagged);
        assertTrue(printed().contains("already revealed"));
    }

    @Test
    public void flag_alreadyFlaggedCell_staysFlaggedAndWarns() {
        board.grid[1][1].isFlagged = true;

        new FlagCommand(1, 1).execute(board);

        assertTrue(board.grid[1][1].isFlagged);
        assertTrue(printed().contains("already flagged"));
    }

    @Test
    public void flag_outOfBounds_doesNotThrowException() {
        assertDoesNotThrow(() -> new FlagCommand(-1, 0).execute(board));
        assertDoesNotThrow(() -> new FlagCommand(0, -1).execute(board));
        assertDoesNotThrow(() -> new FlagCommand(Board.SIZE, 0).execute(board));
        assertDoesNotThrow(() -> new FlagCommand(0, Board.SIZE).execute(board));
        assertTrue(printed().contains("out of bounds"));
    }

    @Test
    public void flag_validCell_onlyAffectsTargetCell() {
        new FlagCommand(2, 2).execute(board);

        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                boolean expected = (i == 2 && j == 2);
                assertEquals(expected, board.grid[i][j].isFlagged,
                        "Unexpected flag state at (" + i + ", " + j + ")");
            }
        }
    }

    // ---------- UnflagCommand ----------

    @Test
    public void unflag_flaggedCell_clearsFlag() {
        board.grid[0][0].isFlagged = true;

        new UnflagCommand(0, 0).execute(board);

        assertFalse(board.grid[0][0].isFlagged);
        assertTrue(printed().contains("Cell (1, 1) unflagged."));
    }

    @Test
    public void unflag_unflaggedCell_warnsAndStaysUnflagged() {
        new UnflagCommand(0, 0).execute(board);

        assertFalse(board.grid[0][0].isFlagged);
        assertTrue(printed().contains("is not flagged"));
    }

    @Test
    public void unflag_outOfBounds_doesNotThrowException() {
        assertDoesNotThrow(() -> new UnflagCommand(-1, 0).execute(board));
        assertDoesNotThrow(() -> new UnflagCommand(0, -1).execute(board));
        assertDoesNotThrow(() -> new UnflagCommand(Board.SIZE, 0).execute(board));
        assertDoesNotThrow(() -> new UnflagCommand(0, Board.SIZE).execute(board));
        assertTrue(printed().contains("out of bounds"));
    }

    @Test
    public void unflag_flaggedCell_leavesOtherFlagsAlone() {
        board.grid[1][1].isFlagged = true;
        board.grid[2][2].isFlagged = true;

        new UnflagCommand(1, 1).execute(board);

        assertFalse(board.grid[1][1].isFlagged);
        assertTrue(board.grid[2][2].isFlagged);
    }

    // ---------- Combined ----------

    @Test
    public void flagThenUnflag_returnsCellToOriginalState() {
        new FlagCommand(3, 3).execute(board);
        assertTrue(board.grid[3][3].isFlagged);

        new UnflagCommand(3, 3).execute(board);
        assertFalse(board.grid[3][3].isFlagged);
    }

    @Test
    public void flagUnflagFlag_canReflagAfterUnflag() {
        new FlagCommand(4, 4).execute(board);
        new UnflagCommand(4, 4).execute(board);
        new FlagCommand(4, 4).execute(board);

        assertTrue(board.grid[4][4].isFlagged);
    }
}
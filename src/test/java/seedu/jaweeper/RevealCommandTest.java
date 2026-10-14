package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RevealCommandTest {

    private static final int SIZE = 5;

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;
    private Board board;

    @BeforeEach
    public void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        board = createEmptyBoard();
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    /*
     * Tests for revealing a flagged cell ===================================================================
     */

    @Test
    public void execute_flaggedCell_notRevealed() {
        board.grid[1][1].isFlagged = true;

        new RevealCommand(1, 1).execute(board);

        assertFalse(board.grid[1][1].isRevealed);
    }

    @Test
    public void execute_flaggedCell_printsUnflagMessage() {
        board.grid[1][1].isFlagged = true;

        new RevealCommand(1, 1).execute(board);

        assertTrue(getOutput().contains("Cell is flagged! Unflag it first."));
    }

    @Test
    public void execute_flaggedMine_doesNotTriggerGameOver() {
        board.grid[2][2].isMine = true;
        board.grid[2][2].isFlagged = true;

        new RevealCommand(2, 2).execute(board);

        assertFalse(board.grid[2][2].isRevealed);
        assertFalse(getOutput().contains("GAME OVER"));
    }

    /*
     * Tests for revealing a mine ==========================================================================
     */

    @Test
    public void execute_mineCell_cellIsRevealed() {
        board.grid[0][0].isMine = true;

        new RevealCommand(0, 0).execute(board);

        assertTrue(board.grid[0][0].isRevealed);
    }

    @Test
    public void execute_mineCell_printsGameOver() {
        board.grid[0][0].isMine = true;

        new RevealCommand(0, 0).execute(board);

        final String output = getOutput();
        assertTrue(output.contains("BOOM"));
        assertTrue(output.contains("GAME OVER"));
    }

    @Test
    public void execute_mineCell_printsOneBasedCoordinates() {
        board.grid[3][4].isMine = true;

        new RevealCommand(3, 4).execute(board);

        // internal indices are 0-based, but the user should see 1-based coordinates
        assertTrue(getOutput().contains("(4, 5)"));
    }

    /*
     * Tests for revealing a safe cell =====================================================================
     */

    @Test
    public void execute_safeCell_cellIsRevealed() {
        board.grid[2][2].isMine = false;

        new RevealCommand(2, 2).execute(board);

        assertTrue(board.grid[2][2].isRevealed);
    }

    // @Test
    public void execute_safeCell_doesNotPrintGameOver() {
        new RevealCommand(2, 2).execute(board);

        final String output = getOutput();
        assertFalse(output.contains("BOOM"));
        assertFalse(output.contains("GAME OVER"));
    }

    @Test
    public void execute_safeCell_doesNotRevealMines() {
        board.grid[0][0].isMine = true;

        new RevealCommand(4, 4).execute(board);

        assertFalse(board.grid[0][0].isRevealed);
    }

    @Test
    public void execute_safeCell_otherCellsUnchangedWhenNeighbourIsMine() {
        // (2, 2) is adjacent to a mine, so flood fill should not spread from it
        board.grid[2][3].isMine = true;

        new RevealCommand(2, 2).execute(board);

        assertTrue(board.grid[2][2].isRevealed);
        assertFalse(board.grid[2][3].isRevealed);
    }

    /*
     * Tests for the safe first move ========================================================================
     */

    @Test
    public void execute_firstRevealOnMine_mineIsMovedAndGameContinues() {
        Board freshBoard = new Board();
        // Worst case: the clicked cell and all its neighbours are mines.
        for (int i = 3; i <= 5; i++) {
            for (int j = 3; j <= 5; j++) {
                freshBoard.grid[i][j].isMine = true;
            }
        }

        new RevealCommand(4, 4).execute(freshBoard);

        assertFalse(freshBoard.grid[4][4].isMine);
        assertTrue(freshBoard.grid[4][4].isRevealed);
        assertFalse(getOutput().contains("GAME OVER"));
    }

    @Test
    public void execute_firstReveal_floodFillOpensNeighbours() {
        Board freshBoard = new Board();

        new RevealCommand(4, 4).execute(freshBoard);

        assertEquals(0, freshBoard.grid[4][4].adjacentMines);
        for (int i = 3; i <= 5; i++) {
            for (int j = 3; j <= 5; j++) {
                assertTrue(freshBoard.grid[i][j].isRevealed, "Cell (" + i + ", " + j + ") should be opened.");
            }
        }
    }

    @Test
    public void execute_firstRevealOnFlaggedCell_doesNotUseUpSafeFirstMove() {
        Board freshBoard = new Board();
        freshBoard.grid[1][1].isFlagged = true;

        new RevealCommand(1, 1).execute(freshBoard);

        assertTrue(freshBoard.isFirstMove());
    }

    @Test
    public void execute_afterNewGame_firstRevealIsSafeAgain() {
        Board gameBoard = new Board();
        new RevealCommand(4, 4).execute(gameBoard);
        assertFalse(gameBoard.isFirstMove());

        new NewGameCommand().execute(gameBoard);
        assertTrue(gameBoard.isFirstMove());

        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                gameBoard.grid[i][j].isMine = true;
            }
        }
        new RevealCommand(0, 0).execute(gameBoard);

        assertFalse(gameBoard.grid[0][0].isMine);
        assertTrue(gameBoard.grid[0][0].isRevealed);
    }

    /*
     * Utility methods ====================================================================================
     */

    /**
     * Creates a board with no mines, no flags and no revealed cells.
     * The safe-first-click protection is already used up, so mines placed manually by a test stay where they are.
     */
    private Board createEmptyBoard() {
        Board emptyBoard = new Board();
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                emptyBoard.grid[i][j].isMine = false;
                emptyBoard.grid[i][j].adjacentMines = 0;
            }
        }
        emptyBoard.ensureSafeFirstClick(0, 0);
        return emptyBoard;
    }

    /**
     * Returns everything printed to System.out since setUp.
     */
    private String getOutput() {
        return outContent.toString().replace("\r\n", "\n");
    }
}

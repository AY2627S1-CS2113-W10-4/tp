package seedu.jaweeper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardTest {

    private Board board;

    @BeforeEach
    public void setUp() {
        board = new Board();
    }

    @Test
    public void boardInitialization_correctMineCount_success() {
        Board board = new Board();
        int mineCount = 0;

        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                if (board.grid[i][j].isMine) {
                    mineCount++;
                }
            }
        }

        assertEquals(10, mineCount, "The board should contain exactly 10 mines on initialization.");
    }

    @Test
    public void floodFill_outOfBounds_doesNotThrowException() {
        // Should safely return without throwing an ArrayIndexOutOfBoundsException
        board.floodFill(-1, 0);
        board.floodFill(0, Board.SIZE);
    }

    @Test
    public void floodFill_blankBoard_revealsEntireGrid() {
        // Clear all mines manually to create a controlled testing state
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                board.grid[i][j].isMine = false;
                board.grid[i][j].adjacentMines = 0;
                board.grid[i][j].isRevealed = false;
            }
        }

        board.floodFill(0, 0);

        // Verify every cell on the board gets revealed
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                assertTrue(board.grid[i][j].isRevealed,
                        "Cell (" + i + ", " + j + ") should be revealed.");
            }
        }
    }

    @Test
    public void floodFill_cellWithAdjacentMines_revealsSelfAndStops() {
        // Clear board state
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                board.grid[i][j].isMine = false;
                board.grid[i][j].adjacentMines = 0;
                board.grid[i][j].isRevealed = false;
            }
        }

        // Place a mine at (0, 1) and set adjacent count on (0, 0)
        board.grid[0][1].isMine = true;
        board.grid[0][0].adjacentMines = 1;

        board.floodFill(0, 0);

        assertTrue(board.grid[0][0].isRevealed, "Numbered cell (0, 0) should be revealed.");
        assertFalse(board.grid[0][1].isRevealed, "Mine cell (0, 1) should remain hidden.");
    }

    /*
     * Tests for ensureSafeFirstClick ======================================================================
     */

    @Test
    public void ensureSafeFirstClick_anyPosition_clickAndNeighboursHaveNoMines() {
        // Mines are random, so repeat many times, covering corners, edges and the middle.
        for (int trial = 0; trial < 300; trial++) {
            Board freshBoard = new Board();
            int row = trial % Board.SIZE;
            int col = (trial / Board.SIZE) % Board.SIZE;

            freshBoard.ensureSafeFirstClick(row, col);

            for (int r = Math.max(0, row - 1); r <= Math.min(Board.SIZE - 1, row + 1); r++) {
                for (int c = Math.max(0, col - 1); c <= Math.min(Board.SIZE - 1, col + 1); c++) {
                    assertFalse(freshBoard.grid[r][c].isMine, "Mine at (" + r + ", " + c + ") near click ("
                            + row + ", " + col + ")");
                }
            }
            assertEquals(0, freshBoard.grid[row][col].adjacentMines);
        }
    }

    @Test
    public void ensureSafeFirstClick_afterMovingMines_mineCountUnchanged() {
        for (int trial = 0; trial < 100; trial++) {
            Board freshBoard = new Board();
            // Force the worst case: a mine on every cell of the corner area.
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    freshBoard.grid[i][j].isMine = true;
                }
            }
            int minesBefore = countMines(freshBoard);

            freshBoard.ensureSafeFirstClick(0, 0);

            assertEquals(minesBefore, countMines(freshBoard));
        }
    }

    @Test
    public void ensureSafeFirstClick_afterMovingMines_adjacencyCountsAreCorrect() {
        Board freshBoard = new Board();

        freshBoard.ensureSafeFirstClick(4, 4);

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (freshBoard.grid[r][c].isMine) {
                    continue;
                }
                int expected = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr >= 0 && nr < Board.SIZE && nc >= 0 && nc < Board.SIZE
                                && freshBoard.grid[nr][nc].isMine) {
                            expected++;
                        }
                    }
                }
                assertEquals(expected, freshBoard.grid[r][c].adjacentMines,
                        "Wrong adjacent count at (" + r + ", " + c + ")");
            }
        }
    }

    @Test
    public void ensureSafeFirstClick_calledTwice_secondCallDoesNothing() {
        board.ensureSafeFirstClick(0, 0);
        assertFalse(board.isFirstMove());

        board.grid[4][4].isMine = true;
        board.ensureSafeFirstClick(4, 4);

        assertTrue(board.grid[4][4].isMine, "Only the first click is protected.");
    }

    @Test
    public void copyStateFrom_afterFirstClick_restoresFirstMove() {
        board.ensureSafeFirstClick(0, 0);
        assertFalse(board.isFirstMove());

        board.copyStateFrom(new Board());

        assertTrue(board.isFirstMove());
    }

    private int countMines(Board target) {
        int count = 0;
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                if (target.grid[i][j].isMine) {
                    count++;
                }
            }
        }
        return count;
    }
}

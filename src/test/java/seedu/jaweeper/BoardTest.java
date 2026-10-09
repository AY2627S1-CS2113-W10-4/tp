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
}

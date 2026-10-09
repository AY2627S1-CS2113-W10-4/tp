package seedu.jaweeper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BoardTest {
    
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
}
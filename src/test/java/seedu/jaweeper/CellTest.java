package seedu.jaweeper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CellTest {

    @Test
    public void cell_initialization_correctDefaultValues() {
        Cell cell = new Cell();
        assertFalse(cell.isMine);
        assertFalse(cell.isRevealed);
        assertFalse(cell.isFlagged);
        assertEquals(0, cell.adjacentMines);
    }

    @Test
    public void cell_setFlag_flagStatusUpdates() {
        Cell cell = new Cell();
        cell.isFlagged = true;
        assertTrue(cell.isFlagged);
    }

    @Test
    public void cell_setMine_mineStatusUpdates() {
        Cell cell = new Cell();
        cell.isMine = true;
        assertTrue(cell.isMine);
    }

    @Test
    public void getIcon_unrevealed_returnsDot() {
        Cell cell = new Cell();
        assertEquals(".", cell.getIcon(), "An unrevealed cell should return '.'");
    }
}

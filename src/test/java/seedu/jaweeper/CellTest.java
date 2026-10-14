package seedu.jaweeper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CellTest {
    
    @Test
    public void getIcon_unrevealed_returnsDot() {
        Cell cell = new Cell();
        assertEquals(".", cell.getIcon(), "An unrevealed cell should return '.'");
    }
}

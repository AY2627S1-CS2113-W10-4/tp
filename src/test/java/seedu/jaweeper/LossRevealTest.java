package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests that every mine is shown when the player loses.
 */
public class LossRevealTest extends SessionTestBase {

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
}

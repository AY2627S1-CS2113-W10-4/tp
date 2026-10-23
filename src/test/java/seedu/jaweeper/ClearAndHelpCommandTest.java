package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the clear command and the updated help text.
 */
public class ClearAndHelpCommandTest extends SessionTestBase {

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

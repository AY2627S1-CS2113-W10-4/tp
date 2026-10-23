package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests running several commands on one line, separated by semicolons.
 */
public class CommandChainingTest extends SessionTestBase {

    @Test
    public void splitChain_trimsAndSkipsEmptySegments() {
        assertEquals(List.of("f 1 1", "f 2 3"), Jaweeper.splitChain("f 1 1; f 2 3"));
        assertEquals(List.of("f 1 1", "f 2 3"), Jaweeper.splitChain(" ;f 1 1;;  f 2 3 ; "));
        assertEquals(List.of("help"), Jaweeper.splitChain("help"));
        assertEquals(List.of("  "), Jaweeper.splitChain("  "));
    }

    @Test
    public void chain_runsEveryCommand() {
        runScript("f 1 1; f 2 3\nexit\n");
        assertTrue(session.getBoard().grid[0][0].isFlagged());
        assertTrue(session.getBoard().grid[1][2].isFlagged());
    }

    @Test
    public void chain_failingCommandReportsErrorAndRestStillRun() {
        runScript("f 1 1; f 99 99; f 2 3\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Coordinates must be from 1 to 9."), text);
        assertTrue(session.getBoard().grid[0][0].isFlagged());
        assertTrue(session.getBoard().grid[1][2].isFlagged());
    }

    @Test
    public void chain_unknownCommandInTheMiddle_doesNotStopTheLine() {
        runScript("f 1 1; nonsense; f 2 3\nexit\n");
        assertTrue(takeOutput().contains("Unknown command: nonsense"));
        assertTrue(session.getBoard().grid[1][2].isFlagged());
    }

    @Test
    public void chain_exitSkipsRemainingCommands() {
        runScript("f 1 1; exit; f 2 3\n");
        assertTrue(session.getBoard().grid[0][0].isFlagged());
        // exit warns once about unsaved progress, so the loop continues, then hits end of input.
        runScriptAfterReset("f 5 5; save keep; exit; f 6 6\n");
        assertFalse(session.getBoard().grid[5][5].isFlagged(), "Commands after a real exit must not run");
    }

    @Test
    public void chain_commandsSeeTheBoardLeftByEarlierCommands() {
        // 'new hard' replaces the board with 16x30; (15, 29) is only valid afterwards.
        runScript("new hard; f 15 29\nexit\n");
        String text = takeOutput();
        assertFalse(text.contains("Coordinates must be"), text);
        assertTrue(session.getBoard().grid[14][28].isFlagged());
    }

    @Test
    public void chain_gameEndingSkipsTheRestWithOneNote() throws IOException {
        session.setSafeClickEnabled(false);
        session.replaceBoard(boardWithMines(0, 0));
        runScript("r 1 1; f 2 2; f 3 3\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("the remaining 2 commands on this line were skipped."), text);
        assertFalse(text.contains("The game is over."), text);
        assertFalse(session.getBoard().grid[1][1].isFlagged());
    }

    @Test
    public void emptyLine_stillReportsMissingCommand() {
        runScript("\n;\nexit\n");
        assertTrue(takeOutput().contains("Enter a command."));
    }

    private void runScriptAfterReset(String script) {
        session.replaceBoard(new Board());
        runScript(script);
    }
}

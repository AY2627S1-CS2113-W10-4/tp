package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for console logging, the mine counter, finished-game saves, delete
 * confirmation, deep flood fills, the help text and board copies.
 */
public class RemainingBugRegressionTest extends SessionTestBase {

    // ----- Logging -----

    @Test
    public void removeConsoleLogging_leavesNoConsoleHandler() {
        Logger root = Logger.getLogger("");
        Handler[] before = root.getHandlers();
        try {
            Jaweeper.removeConsoleLogging();
            for (Handler handler : root.getHandlers()) {
                assertFalse(handler instanceof ConsoleHandler, "Log records must not reach the console");
            }
        } finally {
            for (Handler handler : before) {
                root.removeHandler(handler);
                root.addHandler(handler);
            }
        }
    }

    // ----- Mine counter -----

    @Test
    public void flag_beyondMineCount_isRejectedAndCounterStaysAtZero() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}));
        runScript("f 1 1\nf 1 2\n");
        String text = takeOutput();
        assertTrue(text.contains("All 1 flags are already placed."), text);
        assertFalse(text.contains("Remaining Mines: -"), text);
        assertFalse(session.getBoard().grid[0][1].isFlagged());
        assertEquals(1, session.getBoard().countFlags());
    }

    @Test
    public void rejectedExtraFlag_isNotAnUndoStep() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}));
        runScript("f 1 1\nf 1 2\nundo\n");
        assertFalse(session.getBoard().grid[0][0].isFlagged(), "Undo must remove the one real flag");
    }

    @Test
    public void counter_withMoreFlagsThanMines_neverPrintsNegative() {
        Board board = restoredBoard(9, 9, new int[]{8, 8});
        board.grid[0][0].isFlagged = true;
        board.grid[0][1].isFlagged = true;
        board.grid[0][2].isFlagged = true;
        board.printBoard();
        assertTrue(takeOutput().contains("Remaining Mines: 0"));
    }

    // ----- Finished games' saves -----

    @Test
    public void winningASavedGame_removesItsSave() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}));
        runScript("f 1 1\nsave farm\nu 1 1\nr 1 1\nload farm\n");
        String text = takeOutput();
        assertTrue(text.contains("Removed save 'farm' because this game is finished."), text);
        assertTrue(text.contains("No save named 'farm'."), text);
        assertEquals(1, session.getActiveProfile().getStats().getWins());
        assertFalse(Files.exists(savesFolder().resolve("farm.txt")));
    }

    @Test
    public void losingASavedGame_removesEverySaveOfThatGame() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{0, 0}));
        session.setSafeClickEnabled(false);
        runScript("f 9 9\nsave one\nsave two\nr 1 1\n");
        assertEquals(GameState.LOST, session.getBoard().getGameState());
        assertFalse(Files.exists(savesFolder().resolve("one.txt")));
        assertFalse(Files.exists(savesFolder().resolve("two.txt")));
    }

    @Test
    public void loadedGame_win_removesTheLoadedSave() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}));
        runScript("f 1 1\nsave resume\n");
        session = newSession(new Board());
        runScript("load resume\nu 1 1\nr 1 1\n");
        assertEquals(GameState.WON, session.getBoard().getGameState());
        assertFalse(Files.exists(savesFolder().resolve("resume.txt")));
    }

    @Test
    public void savesOfAnEarlierGame_surviveTheNextGamesEnd() throws IOException {
        runScript("f 1 1\nsave keep\nnew\n");
        session.replaceBoard(restoredBoard(9, 9, new int[]{8, 8}));
        runScript("r 1 1\n");
        assertEquals(GameState.WON, session.getBoard().getGameState());
        assertTrue(Files.exists(savesFolder().resolve("keep.txt")));
    }

    // ----- Delete confirmation -----

    @Test
    public void delete_afterSaveWasOverwritten_needsFreshConfirmation() {
        runScript("f 1 1\nsave trip\ndelete trip\nf 2 2\nsave trip\ndelete trip\nsaves\n");
        String text = takeOutput();
        assertTrue(text.contains("Saved games (1):"), "The newer save must not be deleted: " + text);
        assertEquals(2, text.split("Type 'delete trip' again to confirm.", -1).length - 1, text);
        assertFalse(text.contains("Deleted save"), text);
    }

    @Test
    public void delete_overwrittenSave_canStillBeDeletedWithTwoCommands() {
        runScript("f 1 1\nsave trip\ndelete trip\nf 2 2\nsave trip\ndelete trip\ndelete trip\n");
        assertFalse(Files.exists(savesFolder().resolve("trip.txt")));
    }

    @Test
    public void delete_unchangedSave_stillConfirmsAcrossOtherCommands() {
        runScript("f 1 1\nsave trip\ndelete trip\nsaves\nf 2 2\ndelete trip\n");
        assertFalse(Files.exists(savesFolder().resolve("trip.txt")));
    }

    // ----- Flood fill -----

    @Test
    public void floodFill_largestSavedBoard_doesNotOverflow() {
        Board board = restoredBoard(99, 99, new int[]{98, 98});
        board.floodFill(0, 0);
        int revealed = 0;
        for (Cell[] row : board.grid) {
            for (Cell cell : row) {
                if (cell.isRevealed()) {
                    revealed++;
                }
            }
        }
        assertEquals(99 * 99 - 1, revealed);
        assertTrue(board.checkVictory());
    }

    @Test
    public void floodFill_stopsAtNumbersAndFlags() {
        Board board = restoredBoard(9, 9, new int[]{4, 4});
        board.grid[0][8].isFlagged = true;
        board.floodFill(0, 0);
        assertFalse(board.grid[4][4].isRevealed(), "Mines are never revealed by flood fill");
        assertTrue(board.grid[3][3].isRevealed(), "Numbered border cells are revealed");
        assertFalse(board.grid[0][8].isRevealed(), "Flagged cells are skipped");
    }

    // ----- Help -----

    @Test
    public void help_listsUndoHintAndCustomGames() {
        runScript("help\n");
        String text = takeOutput();
        assertTrue(text.contains("  undo "), text);
        assertTrue(text.contains("  hint "), text);
        assertTrue(text.contains("new custom ROWS COLS MINES"), text);
    }

    // ----- Board copies -----

    @Test
    public void deepCopy_isExactAndIndependent() {
        Board board = restoredBoard(9, 9, new int[]{2, 3, 7, 1});
        board.grid[0][0].isFlagged = true;
        Board copy = board.deepCopy();
        assertEquals(GameSaveCodec.encodeGrid(board), GameSaveCodec.encodeGrid(copy));
        assertEquals(board.isFirstMove(), copy.isFirstMove());
        copy.grid[0][0].isFlagged = false;
        assertTrue(board.grid[0][0].isFlagged(), "Changing the copy must not change the original");
    }

    // ----- Helpers -----

    private Path savesFolder() {
        return session.getProfileDirectory().resolve("saves");
    }

    /** Builds a board with mines at the given zero-based row/column pairs and nothing revealed. */
    private static Board restoredBoard(int rows, int cols, int[] mines) {
        Cell[][] cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
        for (int i = 0; i < mines.length; i += 2) {
            cells[mines[i]][mines[i + 1]].isMine = true;
        }
        return Board.restore(rows, cols, mines.length / 2, cells);
    }
}

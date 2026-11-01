package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for logic bugs in undo history, statistics, timing, the leaderboard,
 * safe first clicks on dense boards, hints and the game summary.
 */
public class LogicBugRegressionTest extends SessionTestBase {

    // ----- Undo history -----

    @Test
    public void undo_afterNewGame_doesNotRestorePreviousGame() {
        runScript("f 1 1\nnew hard\nundo\n");
        assertTrue(takeOutput().contains("Nothing to undo."));
        assertEquals(16, session.getBoard().getRows());
        assertEquals(30, session.getBoard().getCols());
    }

    @Test
    public void undo_afterLoad_doesNotRestorePreviousGame() {
        runScript("f 1 1\nsave first\nload first\nundo\n");
        assertTrue(takeOutput().contains("Nothing to undo."));
        assertTrue(session.getBoard().grid[0][0].isFlagged());
    }

    @Test
    public void undo_afterLoss_isRejectedAndLossStaysRecorded() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{0, 0}, new int[]{}));
        session.setSafeClickEnabled(false);
        runScript("r 1 1\nundo\n");
        assertTrue(takeOutput().contains("Nothing to undo."));
        assertEquals(GameState.LOST, session.getBoard().getGameState());
        assertEquals(1, session.getActiveProfile().getStats().getLosses());
    }

    @Test
    public void undo_afterWin_cannotWinTheSameGameTwice() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}, new int[]{}));
        runScript("r 1 1\nundo\nr 1 1\n");
        assertTrue(takeOutput().contains("Nothing to undo."));
        assertEquals(GameState.WON, session.getBoard().getGameState());
        assertEquals(1, session.getActiveProfile().getStats().getWins());
    }

    @Test
    public void rejectedMoves_doNotAddUndoSteps() {
        // One real flag, then a repeated flag, an unflag of a plain cell and a reveal of a flagged cell.
        runScript("f 1 1\nf 1 1\nu 2 2\nr 1 1\nundo\n");
        assertFalse(session.getBoard().grid[0][0].isFlagged(), "The single undo must remove the real flag");
        takeOutput();
        runScript("undo\n");
        assertTrue(takeOutput().contains("Nothing to undo."));
    }

    @Test
    public void undo_backToSavedPosition_keepsTheSaveLink() {
        runScript("f 1 1\nsave keep\nf 2 2\nundo\nsave\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Overwrote save 'keep'."), text);
        assertFalse(text.contains("You have unsaved progress"), text);
        assertTrue(text.contains("Goodbye!"), text);
    }

    @Test
    public void undo_keepsTheSameGridObject() {
        Board board = session.getBoard();
        Cell[][] grid = board.grid;
        runScript("f 3 3\nundo\n");
        assertSame(board, session.getBoard());
        assertSame(grid, session.getBoard().grid);
    }

    // ----- Timing and statistics -----

    @Test
    public void win_recordsCompletionTimeInMilliseconds() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}, new int[]{}));
        session.getGameTimer().resumeFrom(90_000);
        runScript("r 1 1\nstats\n");
        List<Long> times = session.getActiveProfile().getStats().getCompletionTimes();
        assertEquals(1, times.size());
        assertTrue(times.get(0) >= 90_000, "Time should be in milliseconds: " + times);
        assertTrue(takeOutput().contains("Fastest Time: 01:30"));
    }

    @Test
    public void timerStart_discardsPreviousGameTime() {
        GameTimer timer = new GameTimer();
        timer.resumeFrom(60_000);
        timer.stop();
        timer.start();
        assertTrue(timer.getElapsedMillis() < 60_000, "A new game must not inherit the old game's time");
    }

    @Test
    public void stats_surviveRestart() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}, new int[]{}));
        session.getActiveProfile().getStats().recordLoss();
        runScript("r 1 1\n");
        assertTrue(Files.exists(session.getProfileDirectory().resolve(StatsStorage.FILE_NAME)));

        Stats reloaded = newSession(new Board()).getActiveProfile().getStats();
        assertEquals(1, reloaded.getWins());
        assertEquals(1, reloaded.getLosses());
        assertEquals(1, reloaded.getCurrentStreak());
        assertEquals(1, reloaded.getCompletionTimes().size());
    }

    @Test
    public void statsClear_isSaved() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{8, 8}, new int[]{}));
        runScript("r 1 1\nstats clear\n");
        assertEquals(0, newSession(new Board()).getActiveProfile().getStats().getWins());
    }

    @Test
    public void malformedStatsFile_isIgnored() throws IOException {
        Files.createDirectories(session.getProfileDirectory());
        Files.writeString(session.getProfileDirectory().resolve(StatsStorage.FILE_NAME), "garbage\n");
        assertEquals(0, newSession(new Board()).getActiveProfile().getStats().getWins());
    }

    // ----- Leaderboard -----

    @Test
    public void leaderboard_ranksMostWinsFirst() throws IOException {
        ProfileManager manager = session.getProfileManager();
        Profile few = manager.createProfile("Few");
        Profile many = manager.createProfile("Many");
        few.getStats().recordWin(1000);
        for (int i = 0; i < 5; i++) {
            many.getStats().recordWin(1000);
        }
        many.getStats().recordLoss();
        takeOutput();
        new LeaderboardCommand(session).execute(session.getBoard());
        String text = takeOutput();
        assertTrue(text.indexOf("Many") < text.indexOf("Few"), text);
        assertTrue(text.indexOf("Few") < text.indexOf("Tester"), text);
    }

    @Test
    public void leaderboard_breaksTiesByHigherWinRate() throws IOException {
        ProfileManager manager = session.getProfileManager();
        Profile lower = manager.createProfile("Lower");
        Profile higher = manager.createProfile("Higher");
        lower.getStats().recordWin(1000);
        lower.getStats().recordLoss();
        higher.getStats().recordWin(1000);
        takeOutput();
        new LeaderboardCommand(session).execute(session.getBoard());
        String text = takeOutput();
        assertTrue(text.indexOf("Higher") < text.indexOf("Lower"), text);
    }

    // ----- Safe first click on dense boards -----

    @Test
    public void safeFirstClick_denseBoard_finishesAndKeepsMineCount() {
        int[] mines = new int[40];
        int index = 0;
        for (int r = 0; r < 5 && index < mines.length; r++) {
            for (int c = 0; c < 5 && index < mines.length; c++) {
                if (r * 5 + c < 20) {
                    mines[index++] = r;
                    mines[index++] = c;
                }
            }
        }
        Board board = restoredBoard(5, 5, mines, new int[]{});
        board.ensureSafeFirstClick(2, 2);
        assertFalse(board.grid[2][2].isMine());
        assertEquals(20, countMines(board));
    }

    @Test
    public void customGame_tooDenseForSafeArea_isRejected() {
        runScript("new custom 5 5 17\n");
        assertTrue(takeOutput().contains("allows at most 16 mines"));
    }

    @Test
    public void customGame_atLimit_firstRevealOpensBlankArea() {
        runScript("new custom 5 5 16\nr 3 3\n");
        Board board = session.getBoard();
        assertEquals(16, countMines(board));
        assertTrue(board.grid[2][2].isRevealed());
        assertFalse(board.grid[2][2].isMine());
    }

    // ----- Hint -----

    @Test
    public void hint_wrongFlag_neverPointsAtAMine() {
        // Mine at (1,2) [one-based]; everything else revealed except (1,1), which is safe but wrongly flagged.
        Board board = boardWithOnlyHidden(new int[]{0, 1}, new int[]{0, 0, 0, 1});
        board.grid[0][0].isFlagged = true;
        new HintCommand().execute(board);
        assertFalse(takeOutput().contains("(1, 2)"), "The hint must not suggest the mine");
    }

    @Test
    public void hint_withoutFlags_findsDeducedSafeCell() {
        Board board = boardWithOnlyHidden(new int[]{0, 1}, new int[]{0, 0, 0, 1});
        new HintCommand().execute(board);
        assertTrue(takeOutput().contains("Try revealing cell (1, 1)."));
    }

    // ----- Summary -----

    @Test
    public void summary_afterLoss_doesNotCountMineAsSafeCell() throws IOException {
        session = newSession(restoredBoard(9, 9, new int[]{0, 0}, new int[]{}));
        session.setSafeClickEnabled(false);
        runScript("r 1 1\n");
        takeOutput();
        session.getBoard().printGameStateSummary();
        String text = takeOutput();
        assertTrue(text.contains("Safe Cells Revealed  : 0 / 80"), text);
        assertTrue(text.contains("Safe Cells Remaining : 80"), text);
    }

    // ----- Helpers -----

    /** Builds a board with exact mines and revealed cells (zero-based row/column pairs). */
    private static Board restoredBoard(int rows, int cols, int[] mines, int[] revealed) {
        Cell[][] cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
        for (int i = 0; i < mines.length; i += 2) {
            cells[mines[i]][mines[i + 1]].isMine = true;
        }
        for (int i = 0; i < revealed.length; i += 2) {
            cells[revealed[i]][revealed[i + 1]].isRevealed = true;
        }
        return Board.restore(rows, cols, mines.length / 2, cells);
    }

    /** A 9x9 board with the given mines where every cell except {@code hidden} is revealed. */
    private static Board boardWithOnlyHidden(int[] mines, int[] hidden) {
        Board board = restoredBoard(9, 9, mines, new int[]{});
        for (Cell[] row : board.grid) {
            for (Cell cell : row) {
                cell.isRevealed = true;
            }
        }
        for (int i = 0; i < hidden.length; i += 2) {
            board.grid[hidden[i]][hidden[i + 1]].isRevealed = false;
        }
        return board;
    }

    private static int countMines(Board board) {
        int count = 0;
        for (Cell[] row : board.grid) {
            for (Cell cell : row) {
                if (cell.isMine()) {
                    count++;
                }
            }
        }
        return count;
    }
}

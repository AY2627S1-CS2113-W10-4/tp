package seedu.jaweeper;

import java.util.List;

/**
 * Remembers what was last saved or loaded, to tell whether the current game has unsaved progress.
 *
 * <p>Instead of asking every command to set a "dirty" flag, the tracker compares the board's
 * current grid with a snapshot taken at the last save or load. This keeps reveal, flag, unflag
 * and new-game commands unchanged. A grid array that is a different object from the snapshot
 * means a new game (or another load) replaced the board, so the old save name no longer applies.</p>
 *
 * <p>An alternative is a {@code markDirty()} call in every board-changing command; it would be
 * slightly cheaper but easy to forget when someone adds a new command.</p>
 */
public class SaveTracker {

    /** The grid array of the board that was last saved or loaded, compared by identity. */
    private Cell[][] trackedGrid;
    private List<String> savedSnapshot;
    private String currentSaveName;

    /** The action ("exit", "load name") that was warned about, and the position at that time. */
    private String pendingAction;
    private Cell[][] pendingGrid;
    private List<String> pendingSnapshot;

    /**
     * Records that the board now matches the save called {@code name}.
     */
    public void markSaved(Board board, String name) {
        trackedGrid = board.grid;
        savedSnapshot = GameSaveCodec.encodeGrid(board);
        currentSaveName = name;
        clearPending();
    }

    /**
     * Returns the name of the save this game was last saved to or loaded from, or null if the
     * current game has never been saved (including after starting a new game).
     */
    public String getCurrentSaveName(Board board) {
        return board.grid == trackedGrid ? currentSaveName : null;
    }

    /**
     * Returns whether quitting now would lose progress: the game is unfinished, the player
     * has revealed or flagged something, and the position differs from the last save.
     * Finished games have nothing to lose because their result is already in the statistics.
     */
    public boolean hasUnsavedChanges(Board board) {
        if (board.isGameOver() || !hasAnyMove(board)) {
            return false;
        }
        return board.grid != trackedGrid || !GameSaveCodec.encodeGrid(board).equals(savedSnapshot);
    }

    /**
     * Decides whether a progress-losing action may go ahead.
     * The first request with unsaved changes returns false and remembers the action; repeating
     * the same action without any move in between returns true. Any change to the board, or a
     * different action, requires a fresh confirmation.
     *
     * @param board The current board.
     * @param action A key identifying the action, e.g. "exit" or "load game1".
     * @return True if the action may proceed now.
     */
    public boolean confirmDiscard(Board board, String action) {
        if (!hasUnsavedChanges(board)) {
            clearPending();
            return true;
        }
        List<String> snapshot = GameSaveCodec.encodeGrid(board);
        boolean isRepeat = action.equals(pendingAction) && board.grid == pendingGrid
                && snapshot.equals(pendingSnapshot);
        if (isRepeat) {
            clearPending();
            return true;
        }
        pendingAction = action;
        pendingGrid = board.grid;
        pendingSnapshot = snapshot;
        return false;
    }

    private void clearPending() {
        pendingAction = null;
        pendingGrid = null;
        pendingSnapshot = null;
    }

    private static boolean hasAnyMove(Board board) {
        for (Cell[] row : board.grid) {
            for (Cell cell : row) {
                if (cell.isRevealed || cell.isFlagged) {
                    return true;
                }
            }
        }
        return false;
    }
}

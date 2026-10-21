package seedu.jaweeper;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Shares the current board and profile manager across commands in one application.
 * Save formats, statistics and unsaved-game policies belong to their feature owners.
 */
public final class GameSession {

    private final ProfileManager profileManager;
    private Board board;

    /**
     * Creates a session using the application's single profile manager.
     */
    public GameSession(Board board, ProfileManager profileManager) {
        this.board = Objects.requireNonNull(board, "Board must not be null");
        this.profileManager = Objects.requireNonNull(profileManager, "Profile manager must not be null");
    }

    public Board getBoard() {
        return board;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    /**
     * Returns the selected profile or reports a missing startup selection.
     */
    public Profile getActiveProfile() {
        return profileManager.getActiveProfile().orElseThrow(
                () -> new IllegalStateException("Select a profile before starting the game."));
    }

    /**
     * Returns the active player's stable directory without creating it.
     */
    public Path getProfileDirectory() {
        return profileManager.getProfileDirectory(getActiveProfile().getId());
    }

    /**
     * Installs an already validated board, for example after loading or starting a game.
     * The caller must handle unsaved progress before replacing the board.
     */
    public void replaceBoard(Board replacement) {
        board = Objects.requireNonNull(replacement, "Replacement board must not be null");
    }

    /**
     * Adapts the current board's actual dimensions for command input validation.
     */
    public int getRowCount() {
        return board.grid.length;
    }

    public int getColumnCount() {
        return board.grid[0].length;
    }
}

package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

/**
 * Shares the current board and profile manager across commands in one application.
 * Save formats, statistics and unsaved-game policies belong to their feature owners.
 */
public final class GameSession {

    private final ProfileManager profileManager;
    private final GameTimer gameTimer;
    private Board board;
    // @@author ProNoob116
    private final Stack<Board> boardHistory = new Stack<>();
    // @@author
    /** Cached safeclick value, valid for {@link #settingsProfileId} only. */
    private boolean safeClickEnabled = ProfileSettings.DEFAULT_SAFE_CLICK;
    private String settingsProfileId;

    /**
     * Creates a session using the application's single profile manager.
     */
    public GameSession(Board board, ProfileManager profileManager) {
        this.board = Objects.requireNonNull(board, "Board must not be null");
        this.profileManager = Objects.requireNonNull(profileManager, "Profile manager must not be null");
        this.gameTimer = new GameTimer();
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

    public GameTimer getGameTimer() {
        return gameTimer;
    }

    /**
     * Returns whether the first reveal of a game is guaranteed safe for the active profile.
     * The saved value is read once per profile and then cached. With no active profile
     * (legacy/test usage) the default of true applies.
     */
    public boolean isSafeClickEnabled() {
        Optional<Profile> active = profileManager.getActiveProfile();
        if (active.isEmpty()) {
            return ProfileSettings.DEFAULT_SAFE_CLICK;
        }
        String id = active.get().getId();
        if (!id.equals(settingsProfileId)) {
            safeClickEnabled = ProfileSettings.readSafeClick(profileManager.getProfileDirectory(id));
            settingsProfileId = id;
        }
        return safeClickEnabled;
    }

    /**
     * Saves the safeclick setting for the active profile. The file is written first, so a
     * failed write leaves both the file and the in-memory value unchanged.
     *
     * @throws IOException If the setting cannot be written.
     */
    public void setSafeClickEnabled(boolean enabled) throws IOException {
        ProfileSettings.writeSafeClick(getProfileDirectory(), enabled);
        safeClickEnabled = enabled;
        settingsProfileId = getActiveProfile().getId();
    }

    // @@author ProNoob116
    /**
     * Saves an independent copy of the board before a reveal or flag changes it.
     */
    public void pushBoardHistory(Board current) {
        assert current != null : "Cannot record a null board";
        boardHistory.push(current.deepCopy());
    }

    /**
     * Restores the most recent saved board into {@code current}.
     *
     * @return True if a previous board was restored.
     */
    public boolean undoLastMove(Board current) {
        assert current != null : "Current board must not be null";
        if (boardHistory.isEmpty()) {
            return false;
        }
        Board previous = boardHistory.pop();
        current.copyStateFrom(previous);
        this.board = current;
        return true;
    }

    /**
     * Forgets saved boards, for example when a new game replaces the current one.
     */
    public void clearBoardHistory() {
        boardHistory.clear();
    }
    // @@author
}

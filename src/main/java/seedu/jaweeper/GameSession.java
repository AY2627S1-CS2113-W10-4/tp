package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shares the current board and profile manager across commands in one application.
 * Save formats, statistics and unsaved-game policies belong to their feature owners.
 */
public final class GameSession {

    private static final Logger LOGGER = Logger.getLogger(GameSession.class.getName());

    private final ProfileManager profileManager;
    private final GameTimer gameTimer;
    private Board board;
    // @@author ProNoob116
    private final Stack<Board> boardHistory = new Stack<>();
    // @@author
    /** Saves written or loaded for the game in progress; removed once that game ends. */
    private final Set<String> linkedSaves = new LinkedHashSet<>();
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
        // Undo must never step back into a different game.
        boardHistory.clear();
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

    /**
     * Records that save {@code name} holds a position of the current game.
     */
    public void linkSave(String name) {
        linkedSaves.add(name);
    }

    /**
     * Forgets save {@code name}, for example after the player deleted it.
     */
    public void unlinkSave(String name) {
        linkedSaves.remove(name);
    }

    /**
     * Forgets every save link. Called when another game replaces the current one; the files stay.
     */
    public void forgetLinkedSaves() {
        linkedSaves.clear();
    }

    /**
     * Deletes the saves of a game that has just been won or lost. A finished game's result is
     * already in the statistics, so reloading one of its saves would let the same game be won
     * (or retried) again.
     */
    public void deleteFinishedGameSaves() {
        if (linkedSaves.isEmpty() || profileManager.getActiveProfile().isEmpty()) {
            linkedSaves.clear();
            return;
        }
        SaveStore store = SaveStore.forSession(this);
        for (String name : linkedSaves) {
            try {
                if (store.delete(name)) {
                    System.out.println("Removed save '" + name + "' because this game is finished.");
                }
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Could not delete a finished game's save", exception);
                System.out.println("Could not remove save '" + name + "': " + exception.getMessage());
            }
        }
        linkedSaves.clear();
    }

    /**
     * Writes the active profile's statistics to disk so they survive a restart.
     * A failed write is reported but does not interrupt the game. Does nothing when no
     * profile is active (legacy/test usage).
     */
    public void saveStats() {
        Optional<Profile> active = profileManager.getActiveProfile();
        if (active.isEmpty()) {
            return;
        }
        try {
            StatsStorage.write(active.get().getStats(), getProfileDirectory());
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not save statistics", exception);
            System.out.println("Could not save your statistics: " + exception.getMessage());
        }
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
        // Restore in place so the grid keeps its identity and save tracking still applies.
        current.restoreFrom(previous);
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

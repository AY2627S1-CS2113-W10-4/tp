package seedu.jaweeper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves the current unfinished game under a name in the active profile's saves folder.
 * {@code save NAME} saves (or overwrites) that name; plain {@code save} re-saves to the name
 * this game was last saved to or loaded from.
 */
public class SaveCommand extends Command {
    static final String USAGE = "Usage: save NAME (letters, digits, '_' or '-'), or save to re-save this game.";

    private static final Logger LOGGER = Logger.getLogger(SaveCommand.class.getName());

    private final GameSession session;
    private final SaveTracker tracker;
    /** Validated lowercase name, or null to reuse the current save name. */
    private final String name;

    SaveCommand(GameSession session, SaveTracker tracker, String name) {
        assert session != null && tracker != null : "Save needs a session and a tracker";
        this.session = session;
        this.tracker = tracker;
        this.name = name;
    }

    /**
     * Builds a save command from the argument tokens after "save". Performs no file access.
     *
     * @throws IllegalArgumentException If there are too many arguments or the name is invalid.
     */
    public static SaveCommand parse(String[] arguments, GameSession session, SaveTracker tracker) {
        if (arguments.length == 0) {
            return new SaveCommand(session, tracker, null);
        }
        if (arguments.length > 1) {
            throw new IllegalArgumentException(USAGE);
        }
        return new SaveCommand(session, tracker, SaveStore.normalizeName(arguments[0]));
    }

    @Override
    public void execute(Board board) {
        if (board.isGameOver()) {
            System.out.println("This game is finished, so there is nothing to save. Type 'new' to start again.");
            return;
        }
        String target = name != null ? name : tracker.getCurrentSaveName(board);
        if (target == null) {
            System.out.println("This game has not been saved yet. " + USAGE);
            return;
        }

        SaveStore store = SaveStore.forSession(session);
        boolean isOverwrite = store.exists(target);
        try {
            store.save(target, board, session.getGameTimer().getElapsedMillis());
        } catch (IOException exception) {
            // Do not mark as saved: the warning on exit must still protect this progress.
            LOGGER.log(Level.WARNING, "Saving failed", exception);
            System.out.println("Could not save the game: " + exception.getMessage());
            return;
        }
        tracker.markSaved(board, target);
        LOGGER.log(Level.INFO, "Game saved as {0}", target);
        System.out.println((isOverwrite ? "Overwrote save '" : "Game saved as '") + target + "'.");
    }
}

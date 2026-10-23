package seedu.jaweeper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Replaces the current game with a saved one from the active profile's saves folder.
 * The file is read and fully validated before anything changes, so a bad file never
 * destroys the game in progress. If the current game has unsaved progress, the player
 * must type the same load command twice.
 */
public class LoadCommand extends Command {
    static final String USAGE = "Usage: load NAME";

    private static final Logger LOGGER = Logger.getLogger(LoadCommand.class.getName());

    private final GameSession session;
    private final SaveTracker tracker;
    private final String name;

    LoadCommand(GameSession session, SaveTracker tracker, String name) {
        assert session != null && tracker != null && name != null : "Load needs a session, tracker and name";
        this.session = session;
        this.tracker = tracker;
        this.name = name;
    }

    /**
     * Builds a load command from the argument tokens after "load". Performs no file access.
     *
     * @throws IllegalArgumentException If the argument count or name is invalid.
     */
    public static LoadCommand parse(String[] arguments, GameSession session, SaveTracker tracker) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException(USAGE);
        }
        return new LoadCommand(session, tracker, SaveStore.normalizeName(arguments[0]));
    }

    @Override
    public void execute(Board board) {
        GameSaveCodec.SavedGame saved;
        try {
            saved = SaveStore.forSession(session).load(name);
        } catch (SaveFileException exception) {
            LOGGER.log(Level.INFO, "Load of {0} rejected: {1}", new Object[]{name, exception.getMessage()});
            System.out.println("Could not load '" + name + "': " + exception.getMessage());
            System.out.println("Your current game has not been changed.");
            return;
        }

        if (!tracker.confirmDiscard(board, "load " + name)) {
            System.out.println("Your current game has unsaved progress that will be lost.");
            System.out.println("Use 'save NAME' to keep it first, or type 'load " + name + "' again to load anyway.");
            return;
        }

        session.replaceBoard(saved.board());
        session.getGameTimer().resumeFrom(saved.elapsedMillis());
        tracker.markSaved(saved.board(), name);
        LOGGER.log(Level.INFO, "Loaded save {0}", name);

        System.out.println("Loaded save '" + name + "'.");
        saved.board().printBoard();
    }
}

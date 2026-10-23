package seedu.jaweeper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deletes a saved game from the active profile's saves folder.
 * Deleting is permanent, so the player must type the same {@code delete NAME} twice:
 * the first request only warns and remembers the name.
 */
public class DeleteCommand extends Command {
    static final String USAGE = "Usage: delete NAME";

    private static final Logger LOGGER = Logger.getLogger(DeleteCommand.class.getName());

    private final GameSession session;
    private final SaveTracker tracker;
    private final Confirmation confirmation;
    private final String name;

    DeleteCommand(GameSession session, SaveTracker tracker, Confirmation confirmation, String name) {
        assert session != null && tracker != null && confirmation != null && name != null
                : "Delete needs a session, tracker, confirmation and name";
        this.session = session;
        this.tracker = tracker;
        this.confirmation = confirmation;
        this.name = name;
    }

    /**
     * Builds a delete command from the argument tokens after "delete". Performs no file access.
     *
     * @param confirmation Shared between all delete commands of one run, so a repeat is recognised.
     * @throws IllegalArgumentException If the argument count or name is invalid.
     */
    public static DeleteCommand parse(String[] arguments, GameSession session, SaveTracker tracker,
                                      Confirmation confirmation) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException(USAGE);
        }
        return new DeleteCommand(session, tracker, confirmation, SaveStore.normalizeName(arguments[0]));
    }

    @Override
    public void execute(Board board) {
        SaveStore store = SaveStore.forSession(session);
        if (!store.exists(name)) {
            confirmation.clear();
            System.out.println("No save named '" + name + "'. Type 'saves' to see your saved games.");
            return;
        }
        if (!confirmation.confirm(name)) {
            System.out.println("This will permanently delete save '" + name + "'.");
            System.out.println("Type 'delete " + name + "' again to confirm.");
            return;
        }
        try {
            store.delete(name);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Deleting save failed", exception);
            System.out.println("Could not delete '" + name + "': " + exception.getMessage());
            return;
        }
        LOGGER.log(Level.INFO, "Deleted save {0}", name);
        System.out.println("Deleted save '" + name + "'.");
        if (tracker.forgetSave(name)) {
            System.out.println("That was the save for your current game, which now counts as unsaved.");
        }
    }

    /**
     * Remembers which save the player was warned about, so that repeating the command confirms it.
     */
    public static final class Confirmation {
        private String pendingName;

        /**
         * Returns true if {@code name} was already warned about (and consumes that warning);
         * otherwise remembers {@code name} and returns false.
         */
        boolean confirm(String name) {
            if (name.equals(pendingName)) {
                pendingName = null;
                return true;
            }
            pendingName = name;
            return false;
        }

        void clear() {
            pendingName = null;
        }
    }
}

package seedu.jaweeper;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lists the names of the active profile's saved games, in alphabetical order.
 */
public class SavesCommand extends Command {
    private static final Logger LOGGER = Logger.getLogger(SavesCommand.class.getName());

    private final GameSession session;

    SavesCommand(GameSession session) {
        assert session != null : "Saves needs a session";
        this.session = session;
    }

    @Override
    public void execute(Board board) {
        List<String> names;
        try {
            names = SaveStore.forSession(session).list();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Listing saves failed", exception);
            System.out.println("Could not list saved games: " + exception.getMessage());
            return;
        }
        if (names.isEmpty()) {
            System.out.println("No saved games yet. Use 'save NAME' to create one.");
            return;
        }
        System.out.println("Saved games (" + names.size() + "):");
        for (String name : names) {
            System.out.println("  " + name);
        }
        System.out.println("Use 'load NAME' to continue one, or 'delete NAME' to remove it.");
    }
}

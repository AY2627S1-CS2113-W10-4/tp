package seedu.jaweeper;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shows the active profile's settings, or changes one.
 * {@code settings} lists them; {@code settings safeclick on|off} toggles safe first clicks.
 */
public class SettingsCommand extends Command {
    static final String USAGE = "Usage: settings, or settings safeclick on|off";

    private static final Logger LOGGER = Logger.getLogger(SettingsCommand.class.getName());

    private final GameSession session;
    /** The requested safeclick value, or null to only display the settings. */
    private final Boolean newSafeClick;

    SettingsCommand(GameSession session, Boolean newSafeClick) {
        assert session != null : "Settings need a session";
        this.session = session;
        this.newSafeClick = newSafeClick;
    }

    /**
     * Builds a settings command from the argument tokens after "settings". Performs no file access.
     *
     * @throws IllegalArgumentException If the arguments are not a known setting and value.
     */
    public static SettingsCommand parse(String[] arguments, GameSession session) {
        if (arguments.length == 0) {
            return new SettingsCommand(session, null);
        }
        if (arguments.length != 2 || !arguments[0].equalsIgnoreCase("safeclick")) {
            throw new IllegalArgumentException(USAGE);
        }
        String value = arguments[1].toLowerCase(Locale.ROOT);
        if (value.equals("on")) {
            return new SettingsCommand(session, true);
        }
        if (value.equals("off")) {
            return new SettingsCommand(session, false);
        }
        throw new IllegalArgumentException(USAGE);
    }

    @Override
    public void execute(Board board) {
        if (newSafeClick != null) {
            try {
                session.setSafeClickEnabled(newSafeClick);
            } catch (IOException exception) {
                // The setting is only changed in memory after it was written, so nothing changed.
                LOGGER.log(Level.WARNING, "Saving settings failed", exception);
                System.out.println("Could not save the setting: " + exception.getMessage());
                return;
            }
            LOGGER.log(Level.INFO, "Safeclick set to {0}", newSafeClick);
            System.out.println("Safeclick is now " + describe(newSafeClick)
                    + ". It takes effect on the next first reveal.");
            return;
        }
        System.out.println("Settings for " + session.getActiveProfile().getName() + ":");
        System.out.println("  safeclick  " + describe(session.isSafeClickEnabled())
                + "  (your first reveal is never a mine)");
        System.out.println("Change with: settings safeclick on|off");
    }

    private static String describe(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }
}

package seedu.jaweeper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages profiles during play using the same registry as startup.
 * Numbers refer to the current creation-ordered list, not persistent IDs.
 */
public final class ProfileCommand extends Command {

    private static final Logger LOGGER = Logger.getLogger(ProfileCommand.class.getName());
    private static final String USAGE = "Usage: profile list | create NAME | select NUMBER"
            + " | rename NUMBER NAME | delete NUMBER [confirm]";

    private final GameSession session;
    private final SaveTracker tracker;
    private final DeleteCommand.Confirmation deleteConfirmation;
    private final String action;
    private final int number;
    private final String name;
    private final boolean confirmed;

    private ProfileCommand(GameSession session, SaveTracker tracker, DeleteCommand.Confirmation deleteConfirmation,
                           String action, int number, String name, boolean confirmed) {
        this.session = session;
        this.tracker = tracker;
        this.deleteConfirmation = deleteConfirmation;
        this.action = action;
        this.number = number;
        this.name = name;
        this.confirmed = confirmed;
    }

    /**
     * Validates syntax without modifying the registry or session.
     * Names use the remaining tokens so display names can contain spaces.
     */
    public static ProfileCommand parse(String[] arguments, GameSession session, SaveTracker tracker,
                                       DeleteCommand.Confirmation deleteConfirmation) {
        if (arguments.length == 0) {
            throw new IllegalArgumentException(USAGE);
        }
        String action = arguments[0].toLowerCase(Locale.ROOT);
        int number = 0;
        String name = null;
        boolean confirmed = false;
        switch (action) {
        case "list":
            require(arguments.length == 1);
            break;
        case "create":
            require(arguments.length >= 2);
            name = readName(arguments, 1);
            break;
        case "select":
            require(arguments.length == 2);
            number = readNumber(arguments[1]);
            break;
        case "rename":
            require(arguments.length >= 3);
            number = readNumber(arguments[1]);
            name = readName(arguments, 2);
            break;
        case "delete":
            require(arguments.length == 2 || arguments.length == 3);
            number = readNumber(arguments[1]);
            if (arguments.length == 3) {
                require(arguments[2].equalsIgnoreCase("confirm"));
                confirmed = true;
            }
            break;
        default:
            throw new IllegalArgumentException(USAGE);
        }
        return new ProfileCommand(session, tracker, deleteConfirmation, action, number, name, confirmed);
    }

    @Override
    public void execute(Board board) {
        ProfileManager manager = session.getProfileManager();
        try {
            switch (action) {
            case "list":
                printProfiles(manager);
                break;
            case "create":
                Profile created = manager.createProfile(name);
                System.out.println("Created profile: " + created.getName() + ". Current player unchanged.");
                printProfiles(manager);
                break;
            case "select":
                selectProfile(manager, profileAt(manager));
                break;
            case "rename":
                Profile renamed = manager.renameProfile(profileAt(manager).getId(), name);
                System.out.println("Renamed profile: " + renamed.getName());
                break;
            case "delete":
                deleteProfile(manager, profileAt(manager));
                break;
            default:
                throw new IllegalStateException("Unrecognised validated profile action");
            }
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not update profile registry", exception);
            System.out.println("Could not save profile changes: " + exception.getMessage());
        }
    }

    /**
     * Blocks loss of unfinished progress and resets player-specific game state.
     * Selecting the current profile is a no-op, even when progress is unsaved.
     */
    private void selectProfile(ProfileManager manager, Profile target) {
        if (target.getId().equals(session.getActiveProfile().getId())) {
            System.out.println("Already selected: " + target.getName());
            return;
        }
        if (tracker.hasUnsavedChanges(session.getBoard())) {
            System.out.println("Save your current game before switching profiles: save NAME.");
            return;
        }
        Board fresh = new Board();
        manager.selectProfile(target.getId());
        session.replaceBoard(fresh);
        session.clearBoardHistory();
        session.getGameTimer().reset();
        session.getGameTimer().start();
        tracker.reset();
        deleteConfirmation.clear();
        System.out.println("Selected profile: " + target.getName() + ". Started a fresh easy game.");
        fresh.printBoard();
    }

    /**
     * Removes only an inactive registry entry after explicit confirmation.
     * Files remain under the old UUID; creating the same name will use a new UUID.
     */
    private void deleteProfile(ProfileManager manager, Profile target) throws IOException {
        if (target.getId().equals(session.getActiveProfile().getId())) {
            throw new IllegalArgumentException("Select another profile before deleting the active profile.");
        }
        if (!confirmed) {
            System.out.println("Remove profile '" + target.getName() + "' from the list? Data files will be retained.");
            System.out.println("Type 'profile delete " + number + " confirm' to confirm."
                    + " Check 'profile list' again if profiles have changed.");
            return;
        }
        manager.deleteProfile(target.getId());
        System.out.println("Deleted profile: " + target.getName() + ". Data files were retained.");
        printProfiles(manager);
    }

    private Profile profileAt(ProfileManager manager) {
        List<Profile> profiles = manager.getProfiles();
        if (number > profiles.size()) {
            throw new IllegalArgumentException("Unknown profile number. Use profile list.");
        }
        return profiles.get(number - 1);
    }

    private void printProfiles(ProfileManager manager) {
        List<Profile> profiles = manager.getProfiles();
        String activeId = session.getActiveProfile().getId();
        System.out.println("Profiles (* = current player):");
        for (int index = 0; index < profiles.size(); index++) {
            Profile profile = profiles.get(index);
            String marker = profile.getId().equals(activeId) ? " *" : "";
            System.out.println((index + 1) + ". " + profile.getName() + marker);
        }
    }

    private static int readNumber(String value) {
        try {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException exception) {
            // Report the same user-facing error for malformed and overflowing numbers.
        }
        throw new IllegalArgumentException("Use a positive profile number from profile list.");
    }

    private static String readName(String[] arguments, int start) {
        return Profile.validateName(String.join(" ", Arrays.copyOfRange(arguments, start, arguments.length)));
    }

    private static void require(boolean valid) {
        if (!valid) {
            throw new IllegalArgumentException(USAGE);
        }
    }
}

package seedu.jaweeper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Selects a player before a board is created, so no ongoing game can be discarded.
 */
public final class ProfileStartup {

    private ProfileStartup() {
    }

    /**
     * Prompts until a profile is selected or the user exits/reaches end of input.
     * The caller owns the scanner. Profile creation also selects the new profile.
     *
     * @return True if a profile was selected; false if startup was cancelled.
     */
    public static boolean selectProfile(Scanner input, ProfileManager manager) {
        while (true) {
            List<Profile> available = manager.getProfiles();
            System.out.println("Profiles:");
            if (available.isEmpty()) {
                System.out.println("  No profiles yet. Use create NAME.");
            }
            for (int index = 0; index < available.size(); index++) {
                System.out.println("  " + (index + 1) + ". " + available.get(index).getName());
            }
            System.out.println("Enter create NAME, select NUMBER, or exit.");
            System.out.print("profile> ");
            if (!input.hasNextLine()) {
                System.out.println("End of input. Goodbye!");
                return false;
            }

            String[] words = input.nextLine().strip().split("\\s+", 2);
            String action = words[0].toLowerCase(Locale.ROOT);
            if (action.equals("exit") && words.length == 1) {
                System.out.println("Goodbye!");
                return false;
            }

            try {
                Profile selected;
                if (action.equals("create") && words.length == 2) {
                    Profile created = manager.createProfile(words[1]);
                    selected = manager.selectProfile(created.getId());
                } else if (action.equals("select") && words.length == 2) {
                    int number;
                    try {
                        number = Integer.parseInt(words[1].strip());
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("Enter a profile number from the list.");
                    }
                    if (number < 1 || number > available.size()) {
                        throw new IllegalArgumentException("Enter a profile number from the list.");
                    }
                    selected = manager.selectProfile(available.get(number - 1).getId());
                } else {
                    throw new IllegalArgumentException("Use create NAME, select NUMBER, or exit.");
                }
                System.out.println("Selected profile: " + selected.getName());
                return true;
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            } catch (IOException exception) {
                System.out.println("Could not save the profile: " + exception.getMessage());
            }
        }
    }
}

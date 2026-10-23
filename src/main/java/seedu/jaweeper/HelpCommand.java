package seedu.jaweeper;

/**
 * Displays instructions for using Jaweeper.
 */
public class HelpCommand extends Command {

    @Override
    public void execute(Board board) {
        System.out.println("Jaweeper commands:");
        System.out.println("Playing:");
        printEntry("r ROW COL", "Reveal a cell.");
        printEntry("f ROW COL", "Flag a cell.");
        printEntry("u ROW COL", "Remove a flag.");
        printEntry("board", "Display the current board.");
        printEntry("new [easy|medium|hard]", "Start a new game (default: easy).");
        System.out.println("Saved games:");
        printEntry("save NAME", "Save this game ('save' alone re-saves to the same name).");
        printEntry("load NAME", "Continue a saved game.");
        printEntry("saves", "List your saved games.");
        printEntry("delete NAME", "Delete a saved game (type it twice to confirm).");
        System.out.println("Player:");
        printEntry("stats", "Show your statistics ('stats clear' resets them).");
        printEntry("leaderboard", "Show the leaderboard.");
        printEntry("settings", "Show your settings.");
        printEntry("settings safeclick on|off", "Make your first reveal safe, or not (saved per profile).");
        System.out.println("Other:");
        printEntry("clear", "Clear the terminal.");
        printEntry("help", "Show this help.");
        printEntry("exit", "Exit Jaweeper (warns if you have unsaved progress).");
        System.out.println("Chaining: separate commands with ';' to run them in one line.");
        System.out.println("  If one fails, its error is shown and the rest still run.");
        System.out.println("Coordinates: rows 1 to " + board.getRows() + ", columns 1 to " + board.getCols()
                + ", row first, then column.");
        System.out.println("Example: r 2 3   or   f 1 1; f 2 3");
    }

    /** Prints one command and its description in aligned columns. */
    private static void printEntry(String usage, String description) {
        System.out.printf("  %-27s %s%n", usage, description);
    }
}

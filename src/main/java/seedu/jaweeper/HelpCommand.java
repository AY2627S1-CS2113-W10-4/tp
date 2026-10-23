package seedu.jaweeper;

/**
 * Displays instructions for using Jaweeper.
 */
public class HelpCommand extends Command {

    @Override
    public void execute(Board board) {
        System.out.println("Jaweeper commands:");
        System.out.println("  help       Show this help.");
        System.out.println("  board      Display the current board.");
        System.out.println("  new        Start a new game.");
        System.out.println("  save NAME  Save this game (plain 'save' re-saves to the same name).");
        System.out.println("  load NAME  Continue a saved game.");
        System.out.println("  exit       Exit Jaweeper (warns if you have unsaved progress).");
        System.out.println("  r ROW COL  Reveal a cell.");
        System.out.println("  f ROW COL  Flag a cell.");
        System.out.println("  u ROW COL  Remove a flag.");
        System.out.println("Coordinates: 1 to " + Board.SIZE + ", row first, then column.");
        System.out.println("Example: r 2 3");
    }
}

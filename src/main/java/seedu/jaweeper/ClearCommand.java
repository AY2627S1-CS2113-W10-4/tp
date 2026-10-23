package seedu.jaweeper;

/**
 * Clears the terminal screen using ANSI escape codes (home the cursor, erase the screen
 * and the scrollback). Most terminals support these; some IDE consoles ignore them.
 * The game state is untouched, and the board is not reprinted: use 'board' for that.
 */
public class ClearCommand extends Command {

    private static final String CLEAR_SEQUENCE = "\033[H\033[2J\033[3J";

    @Override
    public void execute(Board board) {
        System.out.print(CLEAR_SEQUENCE);
        System.out.flush();
    }
}

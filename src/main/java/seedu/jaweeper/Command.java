package seedu.jaweeper;

/**
 * Represents a command that operates on a Minesweeper board.
 */
public abstract class Command {

    /**
     * Executes this command on the current board.
     *
     * @param board The board on which the command operates.
     */
    public abstract void execute(Board board);

    /**
     * Returns whether this command requests application termination.
     *
     * @return True if the application should exit; false otherwise.
     */
    public boolean isExit() {
        return false;
    }
}
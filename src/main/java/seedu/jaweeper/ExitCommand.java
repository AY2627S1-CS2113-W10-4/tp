package seedu.jaweeper;

/**
 * Requests application termination.
 */
public class ExitCommand extends Command {

    @Override
    public void execute(Board board) {
        System.out.println("Goodbye!");
    }

    @Override
    public boolean isExit() {
        return true;
    }
}

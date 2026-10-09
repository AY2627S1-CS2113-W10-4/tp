package seedu.jaweeper;

/**
 * Displays the current board.
 */
public class PrintBoardCommand extends Command {

    @Override
    public void execute(Board board) {
        board.printBoard();
    }
}

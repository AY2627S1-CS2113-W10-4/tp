package seedu.jaweeper;

/**
 * Starts a fresh game using the existing board initialization logic.
 */
public class NewGameCommand extends Command {

    @Override
    public void execute(Board board) {
        // Preserve the Board reference while replacing its current grid.
        board.grid = new Board().grid;
        System.out.println("New game started.");
        board.printBoard();
    }
}

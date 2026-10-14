package seedu.jaweeper;

/**
 * Starts a fresh game using the existing board initialization logic.
 */
public class NewGameCommand extends Command {

    @Override
    public void execute(Board board) {
        board.setGameState(GameState.IN_PROGRESS);
        board.copyStateFrom(new Board()); // Preserve the Board reference while replacing its current grid.
        System.out.println("New game started.");
        board.printBoard();
    }
}

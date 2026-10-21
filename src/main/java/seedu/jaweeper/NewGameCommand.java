package seedu.jaweeper;

/**
 * Starts a fresh game using the existing board initialization logic.
 */
public class NewGameCommand extends Command {
    private final Difficulty difficulty;

    public NewGameCommand() {
        this.difficulty = Difficulty.EASY;
    }

    public NewGameCommand(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public void execute(Board board) {
        board.setGameState(GameState.IN_PROGRESS);
        Board newBoard = new Board(difficulty.rows, difficulty.cols, difficulty.mines);
        board.copyStateFrom(newBoard); 
        System.out.println("New " + difficulty.name().toLowerCase() + " game started.");
        board.printBoard();
    }
}
package seedu.jaweeper;

/**
 * Starts a fresh game using the existing board initialization logic and resets/starts the game timer.
 */
public class NewGameCommand extends Command {
    private final GameSession session;
    private final Difficulty difficulty;

    /**
     * Constructs a NewGameCommand with default EASY difficulty without a session.
     */
    public NewGameCommand() {
        this(null, Difficulty.EASY);
    }

    /**
     * Constructs a NewGameCommand with a specific difficulty without a session.
     *
     * @param difficulty The desired game difficulty.
     */
    public NewGameCommand(Difficulty difficulty) {
        this(null, difficulty);
    }

    /**
     * Constructs a NewGameCommand with session and difficulty support.
     *
     * @param session    The active game session containing the timer.
     * @param difficulty The desired game difficulty.
     */
    public NewGameCommand(GameSession session, Difficulty difficulty) {
        this.session = session;
        this.difficulty = difficulty != null ? difficulty : Difficulty.EASY;
    }

    /**
     * Executes the new game command, resetting the board state and starting the timer.
     *
     * @param board The current game board to be replaced with a fresh board.
     */
    @Override
    public void execute(Board board) {
        board.setGameState(GameState.IN_PROGRESS);
        Board newBoard = new Board(difficulty.rows, difficulty.cols, difficulty.mines);
        board.copyStateFrom(newBoard);

        // Start/reset the timer if session is present
        if (session != null) {
            session.getGameTimer().start();
        }

        System.out.println("New " + difficulty.name().toLowerCase() + " game started.");
        board.printBoard();
    }
}

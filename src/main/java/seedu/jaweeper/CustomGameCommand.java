// @@author ProNoob116
package seedu.jaweeper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts a game whose size and mine count are chosen by the player.
 */
public class CustomGameCommand extends Command {
    public static final int MIN_DIMENSION = 5;
    public static final int MAX_DIMENSION = 50;
    public static final String USAGE = "Usage: new custom ROWS COLS MINES";

    private static final Logger logger = Logger.getLogger(CustomGameCommand.class.getName());

    private final GameSession session;
    private final int rows;
    private final int columns;
    private final int mines;

    /**
     * Creates a command for an already validated custom board.
     */
    public CustomGameCommand(GameSession session, int rows, int columns, int mines) {
        validate(rows, columns, mines);
        this.session = session;
        this.rows = rows;
        this.columns = columns;
        this.mines = mines;
    }

    /**
     * Creates a custom game from the tokens that followed {@code new}.
     * The first token must be {@code custom}, followed by rows, columns, and mines.
     *
     * @param arguments Tokens after the command name.
     * @param session The active session, or null when history and the timer are unused.
     * @return A command that builds the requested board.
     */
    public static CustomGameCommand parse(String[] arguments, GameSession session) {
        assert arguments != null : "Custom game arguments must not be null";
        if (arguments.length != 4 || !"custom".equalsIgnoreCase(arguments[0])) {
            throw new IllegalArgumentException(USAGE);
        }
        int parsedRows = parseInteger(arguments[1]);
        int parsedColumns = parseInteger(arguments[2]);
        int parsedMines = parseInteger(arguments[3]);
        validate(parsedRows, parsedColumns, parsedMines);
        return new CustomGameCommand(session, parsedRows, parsedColumns, parsedMines);
    }

    /**
     * Rejects dimensions outside 5 to 50, and mine counts that cannot fit on the board.
     */
    public static void validate(int rowCount, int columnCount, int mineCount) {
        if (rowCount < MIN_DIMENSION || columnCount < MIN_DIMENSION) {
            throw new IllegalArgumentException("Custom boards must be at least 5 by 5.");
        }
        if (rowCount > MAX_DIMENSION || columnCount > MAX_DIMENSION) {
            throw new IllegalArgumentException("Custom boards cannot exceed 50 by 50.");
        }
        if (mineCount < 1) {
            throw new IllegalArgumentException("A custom board needs at least 1 mine.");
        }
        if (mineCount >= rowCount * columnCount) {
            throw new IllegalArgumentException("Mines must be fewer than the number of cells.");
        }
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getMines() {
        return mines;
    }

    /**
     * Replaces the current board with the requested custom board.
     *
     * @param board The board object the session is already using.
     */
    @Override
    public void execute(Board board) {
        assert board != null : "Board must not be null";
        logger.log(Level.INFO, "Starting custom game " + rows + "x" + columns + " with " + mines + " mines");
        board.setGameState(GameState.IN_PROGRESS);
        board.copyStateFrom(new Board(rows, columns, mines));
        if (session != null) {
            session.clearBoardHistory();
            session.getGameTimer().start();
        }
        System.out.println("New custom game started (" + rows + "x" + columns + ", " + mines + " mines).");
        board.printBoard();
    }

    /**
     * Converts one custom-game token into an integer.
     */
    private static int parseInteger(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Rows, columns, and mines must be integers.");
        }
    }
}
// @@author

package seedu.jaweeper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FlagCommand extends Command {
    private static final Logger logger = Logger.getLogger(FlagCommand.class.getName());

    private int row;
    private int col;
    // @@author ProNoob116
    private GameSession session;
    // @@author

    public FlagCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    // @@author ProNoob116
    /**
     * Creates a flag command that can record the board for undo.
     */
    public FlagCommand(GameSession session, int row, int col) {
        this(row, col);
        this.session = session;
    }

    /**
     * Saves the board before this flag command changes it.
     */
    private void recordHistory(Board board) {
        if (session != null) {
            session.pushBoardHistory(board);
        }
    }
    // @@author

    @Override
    public void execute(Board board) {
        assert board != null : "Board must not be null";
        assert board.grid != null : "Board grid must be initialized before flagging";

        // @@author ProNoob116
        recordHistory(board);
        // @@author

        logger.log(Level.FINE, "Flag requested at (" + row + ", " + col + ")");

        if (board.isGameOver()) {
            logger.log(Level.INFO, "Flag rejected: game is already over");
            System.out.println("The game is over. Type 'new' to start a new game.");
            return;
        }
        if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getCols()) {
            logger.log(Level.WARNING, "Flag rejected: (" + row + ", " + col + ") is out of bounds");
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is out of bounds.");
            return;
        }

        assert row >= 0 && row < board.getRows() : "Row must be within the board after bounds check";
        assert col >= 0 && col < board.getCols() : "Column must be within the board after bounds check";

        Cell target = board.grid[row][col];
        assert target != null : "Cell in grid must not be null";

        if (target.isRevealed) {
            logger.log(Level.INFO, "Flag rejected: cell (" + row + ", " + col + ") already revealed");
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is already revealed.");
        } else if (target.isFlagged) {
            logger.log(Level.INFO, "Flag rejected: cell (" + row + ", " + col + ") already flagged");
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is already flagged.");
        } else {
            target.isFlagged = true;
            assert target.isFlagged : "Cell should be flagged after flagging";
            assert !target.isRevealed : "A flagged cell must not be revealed";
            logger.log(Level.INFO, "Cell flagged at (" + row + ", " + col + ")");
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") flagged.");
        }

        board.printBoard();
    }
}


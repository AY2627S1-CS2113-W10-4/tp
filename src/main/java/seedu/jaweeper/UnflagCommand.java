package seedu.jaweeper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UnflagCommand extends Command {
    private static final Logger logger = Logger.getLogger(UnflagCommand.class.getName());

    private final int row;
    private final int col;

    public UnflagCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void execute(Board board) {
        assert board != null : "Board must not be null";
        assert board.grid != null : "Board grid must be initialized before unflagging";

        logger.log(Level.FINE, "Unflag requested at (" + row + ", " + col + ")");

        if (board.isGameOver()) {
            logger.log(Level.INFO, "Unflag rejected: game is already over");
            System.out.println("The game is over. Type 'new' to start a new game.");
            return;
        }
        if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getCols()) {
            logger.log(Level.WARNING, "Unflag rejected: (" + row + ", " + col + ") is out of bounds");
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is out of bounds.");
            return;
        }

        assert row >= 0 && row < board.getRows() : "Row must be within the board after bounds check";
        assert col >= 0 && col < board.getCols() : "Column must be within the board after bounds check";

        Cell target = board.grid[row][col];
        assert target != null : "Cell in grid must not be null";

        if (!target.isFlagged) {
            logger.log(Level.INFO, "Unflag rejected: cell (" + row + ", " + col + ") is not flagged");
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is not flagged.");
        } else {
            target.isFlagged = false;
            assert !target.isFlagged : "Cell should not be flagged after unflagging";
            logger.log(Level.INFO, "Cell unflagged at (" + row + ", " + col + ")");
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") unflagged.");
        }

        board.printBoard();
    }
}


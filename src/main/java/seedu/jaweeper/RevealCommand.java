package seedu.jaweeper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reveals a cell on the board and ends the game if the cell is a mine.
 */
public class RevealCommand extends Command {
    private static final Logger logger = Logger.getLogger(RevealCommand.class.getName());

    private final int row;
    private final int col;

    /**
     * Creates a command that reveals the cell at the given zero-based position.
     */
    public RevealCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Reveals the target cell, then updates the game state if the player wins or loses.
     * Prints an error message instead if the reveal is not allowed.
     */
    @Override
    public void execute(Board board) {
        assert board != null : "Board must not be null";
        assert board.grid != null : "Board grid must be initialized before revealing";

        logger.log(Level.FINE, "Reveal requested at (" + row + ", " + col + ")");

        try {
            checkCanReveal(board);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            logger.log(Level.INFO, "Reveal rejected: " + exception.getMessage());
            System.out.println(exception.getMessage());
            return;
        }

        Cell target = board.grid[row][col];
        assert target != null : "Cell in grid must not be null";

        // Classic Minesweeper: the first reveal of a game is never a mine and opens a blank area.
        board.ensureSafeFirstClick(row, col);

        if (target.isMine) {
            revealMine(board, target);
        } else {
            revealSafeCell(board, target);
        }
    }

    /**
     * Checks that the target cell can be revealed, throwing an exception if the game is over,
     * the coordinates are out of bounds, or the cell is flagged.
     */
    private void checkCanReveal(Board board) {
        if (board.isGameOver()) {
            throw new IllegalStateException("The game is over. Type 'new' to start a new game.");
        }
        if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getCols()) {
            throw new IllegalArgumentException("Coordinates are out of bounds.");
        }
        if (board.grid[row][col].isFlagged) {
            throw new IllegalArgumentException("Cell is flagged! Unflag it first.");
        }
    }

    /**
     * Reveals a mine, marks the game as lost and prints the game over message.
     */
    private void revealMine(Board board, Cell target) {
        target.isRevealed = true;
        board.setGameState(GameState.LOST);

        assert target.isRevealed : "Mine cell should be revealed after hitting it";
        assert board.getGameState() == GameState.LOST : "Game should be lost after hitting a mine";
        logger.log(Level.INFO, "Mine hit at (" + row + ", " + col + "); game lost");

        System.out.println("BOOM! You hit a mine at (" + (row + 1) + ", " + (col + 1) + ").");
        System.out.println("========================");
        System.out.println("       GAME OVER        ");
        System.out.println("========================");
    }

    /**
     * Reveals a safe cell and its blank neighbors, then checks whether the player has won.
     */
    private void revealSafeCell(Board board, Cell target) {
        board.floodFill(row, col);

        assert target.isRevealed : "Safe cell should be revealed after flood fill";
        logger.log(Level.FINE, "Safe cell revealed at (" + row + ", " + col + ")");

        board.printBoard();

        if (board.checkVictory()) {
            board.setGameState(GameState.WON);
            assert board.getGameState() == GameState.WON : "Game should be won after victory";
            logger.log(Level.INFO, "All safe cells revealed; game won");

            System.out.println("========================");
            System.out.println("        VICTORY!        ");
            System.out.println("========================");
        }
    }
}


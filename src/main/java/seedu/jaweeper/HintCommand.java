package seedu.jaweeper;

import java.util.logging.Level;
import java.util.logging.Logger;

// @@author ProNoob116
/**
 * Suggests one hidden cell that the revealed numbers prove is safe. Flags are ignored,
 * so a wrongly placed flag cannot lead the hint onto a mine.
 * The suggestion is printed and the board is left unchanged.
 */
public class HintCommand extends Command {
    private static final Logger logger = Logger.getLogger(HintCommand.class.getName());

    /**
     * Creates a hint command after rejecting extra arguments.
     *
     * @param arguments Tokens that followed the command name.
     * @return A command that searches for one logically safe cell.
     */
    public static HintCommand create(String[] arguments) {
        assert arguments != null : "Hint arguments must not be null";
        if (arguments.length != 0) {
            throw new IllegalArgumentException("Usage: hint");
        }
        return new HintCommand();
    }

    /**
     * Prints one deduced safe cell, or explains why no hint is available.
     * A broken grid is reported and does not leave the command loop.
     *
     * @param board The current board. It is not modified.
     */
    @Override
    public void execute(Board board) {
        assert board != null : "Board must not be null";
        logger.log(Level.INFO, "Hint requested");
        try {
            reportHint(board);
        } catch (IllegalArgumentException exception) {
            logger.log(Level.WARNING, "Hint rejected because the grid is unusable", exception);
            System.out.println(exception.getMessage());
        }
    }

    private void reportHint(Board board) {
        if (board.isGameOver()) {
            logger.log(Level.INFO, "Hint rejected: game is already over");
            System.out.println("The game is over. Type 'new' to start a new game.");
            return;
        }

        int[] hint = findSafeCell(board);
        if (hint == null) {
            logger.log(Level.INFO, "No logical hint is available");
            System.out.println("No safe cell can be deduced yet. Reveal more cells.");
            return;
        }

        int row = hint[0];
        int column = hint[1];
        assert row >= 0 && row < board.getRows() : "Hint row must be inside the board";
        assert column >= 0 && column < board.getCols() : "Hint column must be inside the board";
        Cell chosen = board.grid[row][column];
        assert chosen != null : "Hint cell must exist";
        assert !chosen.isRevealed : "Hint must refer to a hidden cell";
        assert !chosen.isFlagged : "Hint must refer to an unflagged cell";
        logger.log(Level.INFO, "Hint selected cell (" + row + ", " + column + ")");
        System.out.println("Try revealing cell (" + (row + 1) + ", " + (column + 1) + ").");
    }

    /**
     * Finds a hidden, unflagged cell that the revealed numbers prove is safe.
     * The player's flags are not trusted, because a wrong flag would make a mine look safe.
     * Instead, a hidden cell is a known mine when some number's hidden neighbours are exactly
     * as many as that number; a number already touching that many known mines makes its other
     * hidden neighbours safe.
     */
    private int[] findSafeCell(Board board) {
        requireRectangularGrid(board);
        boolean[][] knownMines = findKnownMines(board);
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getCols(); column++) {
                Cell cell = board.grid[row][column];
                if (!cell.isRevealed || cell.isMine) {
                    continue;
                }
                if (countKnownMineNeighbors(board, knownMines, row, column) != cell.adjacentMines) {
                    continue;
                }
                int[] candidate = firstSafeNeighbor(board, knownMines, row, column);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean[][] findKnownMines(Board board) {
        boolean[][] knownMines = new boolean[board.getRows()][board.getCols()];
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getCols(); column++) {
                Cell cell = board.grid[row][column];
                if (!cell.isRevealed || cell.isMine || cell.adjacentMines == 0) {
                    continue;
                }
                if (countHiddenNeighbors(board, row, column) != cell.adjacentMines) {
                    continue;
                }
                for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
                    for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                        int neighborRow = row + rowOffset;
                        int neighborColumn = column + columnOffset;
                        if (isInside(board, neighborRow, neighborColumn)
                                && !board.grid[neighborRow][neighborColumn].isRevealed) {
                            knownMines[neighborRow][neighborColumn] = true;
                        }
                    }
                }
            }
        }
        return knownMines;
    }

    private int[] firstSafeNeighbor(Board board, boolean[][] knownMines, int row, int column) {
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (rowOffset == 0 && columnOffset == 0) {
                    continue;
                }
                int neighborRow = row + rowOffset;
                int neighborColumn = column + columnOffset;
                if (!isInside(board, neighborRow, neighborColumn)) {
                    continue;
                }
                Cell neighbor = board.grid[neighborRow][neighborColumn];
                if (!neighbor.isRevealed && !neighbor.isFlagged && !knownMines[neighborRow][neighborColumn]) {
                    return new int[]{neighborRow, neighborColumn};
                }
            }
        }
        return null;
    }

    private int countKnownMineNeighbors(Board board, boolean[][] knownMines, int row, int column) {
        int count = 0;
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                int neighborRow = row + rowOffset;
                int neighborColumn = column + columnOffset;
                if ((rowOffset != 0 || columnOffset != 0) && isInside(board, neighborRow, neighborColumn)
                        && knownMines[neighborRow][neighborColumn]) {
                    count++;
                }
            }
        }
        assert count >= 0 && count <= 8 : "A cell has at most eight neighbors";
        return count;
    }

    private int countHiddenNeighbors(Board board, int row, int column) {
        int hidden = 0;
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                int neighborRow = row + rowOffset;
                int neighborColumn = column + columnOffset;
                if ((rowOffset != 0 || columnOffset != 0) && isInside(board, neighborRow, neighborColumn)
                        && !board.grid[neighborRow][neighborColumn].isRevealed) {
                    hidden++;
                }
            }
        }
        return hidden;
    }

    private boolean isInside(Board board, int row, int column) {
        return row >= 0 && row < board.getRows() && column >= 0 && column < board.getCols();
    }

    private void requireRectangularGrid(Board board) {
        if (board.grid == null || board.getRows() <= 0 || board.getCols() <= 0) {
            throw new IllegalArgumentException("The board grid is empty.");
        }
        if (board.grid.length != board.getRows()) {
            throw new IllegalArgumentException("The board grid does not match its row count.");
        }
        for (int row = 0; row < board.grid.length; row++) {
            Cell[] cells = board.grid[row];
            if (cells == null || cells.length != board.getCols()) {
                throw new IllegalArgumentException("The board grid must be rectangular.");
            }
            for (int column = 0; column < cells.length; column++) {
                if (cells[column] == null) {
                    throw new IllegalArgumentException("The board grid contains an empty cell.");
                }
            }
        }
    }
}
// @@author

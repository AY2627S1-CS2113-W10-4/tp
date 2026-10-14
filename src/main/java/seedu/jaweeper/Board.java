package seedu.jaweeper;

import java.util.Random;
import java.util.logging.Logger;

public class Board {
    public static final int SIZE = 9;
    public static final int MINES = 10;
    private static final Logger logger = Logger.getLogger(Board.class.getName());
    public Cell[][] grid;

    private boolean isFirstMove = true;

    public Board() {
        assert SIZE > 0 : "Board size must be positive";
        assert MINES > 0 && MINES < (SIZE * SIZE) : "Invalid number of mines";
        grid = new Cell[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Cell();
            }
        }
        placeMines();
        calculateAdjacencies();
        logger.info("New board initialized with 10 mines.");
    }

    private void placeMines() {
        Random rand = new Random();
        int minesPlaced = 0;
        while (minesPlaced < MINES) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);
            if (!grid[r][c].isMine) {
                grid[r][c].isMine = true;
                minesPlaced++;
            }
        }
        assert countMines() == MINES : "Board must contain exactly 10 mines!";
    }

    public boolean isFirstMove() {
        return isFirstMove;
    }

    /**
     * Ensures the first reveal of a game is safe, shifting mines out of the 3x3 safe area.
     */
    public void ensureSafeFirstClick(int row, int col) {
        assert row >= 0 && row < SIZE : "Row index out of bounds: " + row;
        assert col >= 0 && col < SIZE : "Column index out of bounds: " + col;

        if (!isFirstMove) {
            return;
        }
        isFirstMove = false;

        int minesBefore = countMines();

        int minesToMove = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isInSafeArea(r, c, row, col) && grid[r][c].isMine) {
                    grid[r][c].isMine = false;
                    minesToMove++;
                }
            }
        }

        Random rand = new Random();
        while (minesToMove > 0) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);
            if (!isInSafeArea(r, c, row, col) && !grid[r][c].isMine) {
                grid[r][c].isMine = true;
                minesToMove--;
            }
        }

        calculateAdjacencies();

        assert countMines() == minesBefore : "Moving mines must not change total mine count";
        assert grid[row][col].adjacentMines == 0 && !grid[row][col].isMine
                : "First click must be safe and have no adjacent mines";
    }

    private static boolean isInSafeArea(int r, int c, int clickRow, int clickCol) {
        return Math.abs(r - clickRow) <= 1 && Math.abs(c - clickCol) <= 1;
    }

    private int countMines() {
        int count = 0;
        for (Cell[] rowCells : grid) {
            for (Cell cell : rowCells) {
                if (cell.isMine) {
                    count++;
                }
            }
        }
        return count;
    }

    public void copyStateFrom(Board freshBoard) {
        assert freshBoard != null : "Board to copy from must not be null";
        this.grid = freshBoard.grid;
        this.isFirstMove = freshBoard.isFirstMove;
    }

    /**
     * Calculates the number of adjacent mines for every safe cell.
     */
    private void calculateAdjacencies() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j].isMine) {
                    continue;
                }

                int count = 0;
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        int ni = i + di;
                        int nj = j + dj;
                        if (ni >= 0 && ni < SIZE && nj >= 0 && nj < SIZE && grid[ni][nj].isMine) {
                            count++;
                        }
                    }
                }
                grid[i][j].adjacentMines = count;
                assert grid[i][j].adjacentMines >= 0 && grid[i][j].adjacentMines <= 8
                        : "Adjacent mines count out of range!";
            }
        }
    }

    /**
     * Recursively reveals blank cells (0 adjacent mines) and their surrounding neighbors.
     */
    public void floodFill(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return;
        }

        Cell currentCell = grid[row][col];

        if (currentCell.isRevealed || currentCell.isFlagged || currentCell.isMine) {
            return;
        }

        currentCell.isRevealed = true;
        logger.finer("Revealed cell at (" + row + ", " + col + ")");

        if (currentCell.adjacentMines > 0) {
            return;
        }

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                floodFill(row + dr, col + dc);
            }
        }
    }

    /**
     * Prints the current state of the board with grid coordinates and borders.
     */
    public void printBoard() {
        System.out.println("     1   2   3   4   5   6   7   8   9");
        String divider = "    +---+---+---+---+---+---+---+---+---+";

        for (int i = 0; i < SIZE; i++) {
            System.out.println(divider);
            System.out.print("  " + (i + 1) + " |");
            for (int j = 0; j < SIZE; j++) {
                System.out.print(" " + grid[i][j].getIcon() + " |");
            }
            System.out.println();
        }
        System.out.println(divider);
    }

    public boolean checkVictory() {
        for (Cell[] rowCells : grid) {
            for (Cell cell : rowCells) {
                if (!cell.isMine && !cell.isRevealed) {
                    return false;
                }
            }
        }
        return true;
    }
}

package seedu.jaweeper;

import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Board {
    public static final int SIZE = 9;
    public static final int MINES = 10;
    private static final Logger logger = Logger.getLogger(Board.class.getName());
    public Cell[][] grid;

    public Board() {
        assert SIZE > 0 : "Board size must be positive";
        assert MINES > 0 && MINES < (SIZE * SIZE) : "Invalid number of mines";
        
        logger.info("Initializing board of size " + SIZE + "x" + SIZE);
        grid = new Cell[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Cell();
            }
        }
        placeMines();
        calculateAdjacencies();
        logger.info("Board initialization complete with " + MINES + " mines");
    }

    /**
     * Randomly places 10 mines across the grid during initialization.
     */
    private void placeMines() {
        logger.info("Placing mines on the board");
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
        assert minesPlaced == MINES : "Exact number of mines must be placed";
    }

    /**
     * Calculates the number of adjacent mines for every safe cell.
     */
    private void calculateAdjacencies() {
        logger.info("Calculating mine adjacencies for all cells");
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
        assert currentCell != null : "Cell in grid should not be null";

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
        assert grid != null : "Grid must be initialized before printing";
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
        assert grid != null : "Grid must not be null when checking victory";
        for (Cell[] rowCells : grid) {
            for (Cell cell : rowCells) {
                if (!cell.isMine && !cell.isRevealed) {
                    return false;
                }
            }
        }
        logger.info("Victory condition met!");
        return true;
    }
}
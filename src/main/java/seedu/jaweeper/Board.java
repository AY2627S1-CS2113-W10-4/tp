package seedu.jaweeper;

import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Board {
    public static final int SIZE = 9;
    public static final int MINES = 10;
    public Cell[][] grid;
    private boolean areMinesPlaced = false;

    // Logging instance
    private static final Logger logger = Logger.getLogger(Board.class.getName());

    public Board() {
        grid = new Cell[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Cell();
            }
        }
        // Note: We no longer place mines here immediately.
        // We wait for the first click to ensure a safe start!
        logger.info("New board initialized without mines.");
    }

    /**
     * Randomly places 10 mines across the grid, avoiding the initial click and its 3x3 neighbors
     * to guarantee a safe, expansive first click.
     */
    private void placeMinesAvoiding(int safeRow, int safeCol) {
        assert !areMinesPlaced : "Mines should only be placed once.";
        Random rand = new Random();
        int minesPlaced = 0;

        while (minesPlaced < MINES) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);

            // Avoid placing a mine on or immediately around the first clicked cell
            if (Math.abs(r - safeRow) <= 1 && Math.abs(c - safeCol) <= 1) {
                continue;
            }

            if (!grid[r][c].isMine) {
                grid[r][c].isMine = true;
                minesPlaced++;
            }
        }
        areMinesPlaced = true;
        logger.info("Mines successfully placed, avoiding (" + safeRow + ", " + safeCol + ").");
    }

    /**
     * Calculates the number of adjacent mines for every safe cell.
     */
    private void calculateAdjacencies() {
        assert areMinesPlaced : "Cannot calculate adjacencies before mines are placed.";
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
        logger.fine("Adjacency counts calculated for all cells.");
    }

    /**
     * Recursively reveals blank cells (0 adjacent mines) and their surrounding neighbors.
     * Automatically triggers safe mine placement on the very first move.
     */
    public void floodFill(int row, int col) {
        // Assertions for input bounds validation
        assert row >= 0 && row < SIZE : "Row index out of bounds: " + row;
        assert col >= 0 && col < SIZE : "Column index out of bounds: " + col;

        // Lazy initialization: place mines on the first move ensuring the clicked spot is safe
        if (!areMinesPlaced) {
            logger.info("First move detected at (" + row + ", " + col + "). Initializing mines...");
            placeMinesAvoiding(row, col);
            calculateAdjacencies();
        }

        // Base case 1: Check bounds
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return;
        }

        Cell currentCell = grid[row][col];

        // Base cases 2 & 3: Stop if already revealed or if it's a mine
        if (currentCell.isRevealed || currentCell.isMine) {
            return;
        }

        // Reveal the current cell
        currentCell.isRevealed = true;
        logger.finer("Revealed cell at (" + row + ", " + col + ")");

        // If cell has numbers (>0 adjacent mines), stop expanding
        if (currentCell.adjacentMines > 0) {
            return;
        }

        // Recursively call floodFill on all 8 surrounding cells
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
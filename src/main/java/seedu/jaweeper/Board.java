package seedu.jaweeper;

import java.util.Random;

public class Board {
    public static final int SIZE = 9;
    public static final int MINES = 10;
    public Cell[][] grid;

    public Board() {
        grid = new Cell[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Cell();
            }
        }
        placeMines();
        calculateAdjacencies();
    }

    /**
     * Randomly places 10 mines across the grid during initialization.
     */
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
            }
        }
    }

    /**
     * Recursively reveals blank cells (0 adjacent mines) and their surrounding neighbors.
     */
    public void floodFill(int row, int col) {
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

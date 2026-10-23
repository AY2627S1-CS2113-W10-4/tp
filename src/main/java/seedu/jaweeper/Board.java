package seedu.jaweeper;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Board {
    // 1. Public fields first
    @Deprecated
    public static final int SIZE = 9;
    @Deprecated
    public static final int MINES = 10;

    private static final Logger logger = Logger.getLogger(Board.class.getName());

    public Cell[][] grid;

    // 2. Private fields next

    private int rows;
    private int cols;
    private int totalMines;
    private GameState gameState = GameState.IN_PROGRESS;
    private boolean isFirstMove = true;

    public Board() {
        this(Difficulty.EASY.rows, Difficulty.EASY.cols, Difficulty.EASY.mines);
    }

    public Board(int rows, int cols, int totalMines) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;

        assert rows > 0 && cols > 0 : "Board dimensions must be positive";
        assert totalMines > 0 && totalMines < (rows * cols) : "Invalid number of mines";

        grid = new Cell[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Cell();
            }
        }
        placeMines();
        calculateAdjacencies();
        logger.info("New board initialized with " + totalMines + " mines.");
    }

    

    private Board(int rows, int cols, int totalMines, Cell[][] cells) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.grid = cells;
        calculateAdjacencies();

        boolean hasRevealedCell = false;
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                hasRevealedCell |= cell.isRevealed;
            }
        }
        // The safe-first-click rule still applies to a game saved before any reveal.
        this.isFirstMove = !hasRevealedCell;
        assert countMines() == totalMines : "Restored board must contain exactly " + totalMines + " mines";
    }

    
    static Board restore(int rows, int cols, int totalMines, Cell[][] cells) {
        assert cells != null && cells.length == rows : "Row count must match the saved grid";
        assert rows > 0 && cols > 0 && cells[0].length == cols : "Column count must match the saved grid";
        assert totalMines > 0 && totalMines < rows * cols : "Invalid number of mines";
        return new Board(rows, cols, totalMines, cells);
    }


    private void placeMines() {
        Random rand = new Random();
        int minesPlaced = 0;
        while (minesPlaced < totalMines) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);
            if (!grid[r][c].isMine) {
                grid[r][c].isMine = true;
                minesPlaced++;
            }
        }
        assert countMines() == totalMines : "Board must contain exactly " + totalMines + " mines!";
    }

    public boolean isFirstMove() {
        return isFirstMove;
    }

    public void ensureSafeFirstClick(int row, int col) {
        assert row >= 0 && row < rows : "Row index out of bounds: " + row;
        assert col >= 0 && col < cols : "Column index out of bounds: " + col;

        if (!isFirstMove) {
            return;
        }
        isFirstMove = false;

        int minesBefore = countMines();
        int minesToMove = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (isInSafeArea(r, c, row, col) && grid[r][c].isMine) {
                    grid[r][c].isMine = false;
                    minesToMove++;
                }
            }
        }

        Random rand = new Random();
        while (minesToMove > 0) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);
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
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isMine) {
                    count++;
                }
            }
        }
        return count;
    }

    public void copyStateFrom(Board freshBoard) {
        assert freshBoard != null : "Board to copy from must not be null";
        this.rows = freshBoard.rows;
        this.cols = freshBoard.cols;
        this.totalMines = freshBoard.totalMines;
        this.gameState = freshBoard.gameState;
        this.isFirstMove = freshBoard.isFirstMove;

        // Perform a deep copy of the grid cells so test assertions match correctly
        this.grid = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell source = freshBoard.grid[r][c];
                Cell copy = new Cell();
                copy.isMine = source.isMine;
                copy.isRevealed = source.isRevealed;
                copy.isFlagged = source.isFlagged;
                copy.adjacentMines = source.adjacentMines;
                this.grid[r][c] = copy;
            }
        }
    }

    private void calculateAdjacencies() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j].isMine) {
                    continue;
                }

                int count = 0;
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        int ni = i + di;
                        int nj = j + dj;
                        if (ni >= 0 && ni < rows && nj >= 0 && nj < cols && grid[ni][nj].isMine) {
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

    public void floodFill(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
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

    public void printBoard() {
        int remainingMines = totalMines - countFlags();

        System.out.println("Remaining Mines: " + remainingMines);

        System.out.print("    ");
        for (int j = 0; j < cols; j++) {
            System.out.printf(" %2d ", (j + 1));
        }
        System.out.println();

        String divider = "    +" + "---+".repeat(cols);

        for (int i = 0; i < rows; i++) {
            System.out.println(divider);
            System.out.printf(" %2d |", (i + 1));
            for (int j = 0; j < cols; j++) {
                System.out.print(" " + grid[i][j].getIcon() + " |");
            }
            System.out.println();
        }
        System.out.println(divider);
    }

    public boolean checkVictory() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isMine && !grid[r][c].isRevealed) {
                    return false;
                }
            }
        }
        return true;
    }

    public int countFlags() {
        int flagCount = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != null && grid[i][j].isFlagged) {
                    flagCount++;
                }
            }
        }
        assert flagCount >= 0 : "Flag count cannot be negative";
        return flagCount;
    }

    public double calculateCompletionPercentage() {
        int revealedCount = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != null && grid[i][j].isRevealed && !grid[i][j].isMine) {
                    revealedCount++;
                }
            }
        }
        int totalSafeCells = (rows * cols) - totalMines;
        if (totalSafeCells == 0) {
            return 100.0;
        }
        return ((double) revealedCount / totalSafeCells) * 100.0;
    }

    public void printGameStateSummary() {
        logger.log(Level.INFO, "Generating game state summary for user.");
        int revealedCount = 0;
        int flagCount = countFlags();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != null && grid[i][j].isRevealed) {
                    revealedCount++;
                }
            }
        }

        int totalSafeCells = (rows * cols) - totalMines;
        int remainingSafeCells = totalSafeCells - revealedCount;
        double completion = calculateCompletionPercentage();

        System.out.println("--- Game State Summary ---");
        System.out.println("Total Mines on Board : " + totalMines);
        System.out.println("Active Flags Placed  : " + flagCount);
        System.out.println("Safe Cells Revealed  : " + revealedCount + " / " + totalSafeCells);
        System.out.println("Safe Cells Remaining : " + remainingSafeCells);
        System.out.printf("Completion Progress  : %.2f%%\n", completion);
        System.out.println("--------------------------");
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public boolean isGameOver() {
        return gameState == GameState.WON || gameState == GameState.LOST;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTotalMines() {
        return totalMines;
    }
}
